package com.caiji.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.caiji.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

enum class DownloadMirror(val label: String, val prefix: String) {
    AUTO("自动加速", ""),
    GH_PROXY("国内节点一", "https://gh-proxy.com/"),
    GH_FAST("国内节点二", "https://ghfast.top/"),
    GH_MIRROR("国内节点三", "https://mirror.ghproxy.com/"),
    DIRECT("GitHub直连", "")
}

data class ReleaseInfo(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val hasNewVersion: Boolean
)

class AppUpdater(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun currentVersion(): String = BuildConfig.VERSION_NAME

    fun configuredRepo(): String = BuildConfig.GITHUB_REPO.trim()

    /**
     * 检查 GitHub Releases 最新版本；开启中国地区加速时优先经加速代理请求并自动回退直连
     */
    suspend fun checkLatestRelease(
        useCnAccel: Boolean,
        mirror: DownloadMirror
    ): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val repo = configuredRepo()
            if (repo.isBlank()) {
                error("尚未关联 GitHub 仓库（推送到 GitHub Actions 打包后将自动注入仓库地址）")
            }

            val apiRawUrl = "https://api.github.com/repos/$repo/releases/latest"
            val candidates = buildCandidateUrls(apiRawUrl, useCnAccel, mirror, includeProxiesForApi = true)

            var lastError: Throwable? = null
            var bodyText: String? = null

            for (url in candidates) {
                val attempt = runCatching {
                    val req = Request.Builder()
                        .url(url)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "Caiji-Android/${currentVersion()}")
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) error("HTTP ${resp.code}")
                        resp.body?.string().orEmpty()
                    }
                }
                if (attempt.isSuccess && attempt.getOrNull()?.trim()?.startsWith("{") == true) {
                    bodyText = attempt.getOrNull()
                    break
                } else {
                    lastError = attempt.exceptionOrNull()
                }
            }

            val raw = bodyText ?: throw (lastError ?: IllegalStateException("无法连接更新服务器"))
            val root = json.parseToJsonElement(raw).jsonObject
            val tagName = root["tag_name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val cleanRemote = tagName.removePrefix("v").removePrefix("V")
            val notes = root["body"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

            val assets = root["assets"] as? JsonArray
            val apkAsset = assets?.map { it.jsonObject }?.firstOrNull { asset ->
                val name = asset["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
                name.endsWith(".apk", ignoreCase = true)
            }
            val apkUrl = apkAsset?.get("browser_download_url")?.jsonPrimitive?.contentOrNull.orEmpty()
            val apkSize = apkAsset?.get("size")?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L

            ReleaseInfo(
                versionName = cleanRemote.ifBlank { tagName },
                notes = notes,
                apkUrl = apkUrl,
                apkSizeBytes = apkSize,
                hasNewVersion = compareVersions(cleanRemote, currentVersion()) > 0
            )
        }
    }

    /**
     * 流式下载 APK（支持中国地区多加速节点自动切换与直连回退）
     */
    suspend fun downloadApk(
        apkUrl: String,
        useCnAccel: Boolean,
        mirror: DownloadMirror,
        onProgress: (downloadedBytes: Long, totalBytes: Long, activeRoute: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (apkUrl.isBlank()) error("该版本未附带 APK 文件")

            val apkDir = File(context.cacheDir, "apk").apply { mkdirs() }
            val outFile = File(apkDir, "caiji-update.apk")
            if (outFile.exists()) outFile.delete()

            val routes = buildDownloadRoutes(apkUrl, useCnAccel, mirror)
            var lastError: Throwable? = null

            for ((routeLabel, targetUrl) in routes) {
                val res = runCatching {
                    val req = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "Caiji-Android/${currentVersion()}")
                        .build()

                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) error("HTTP ${resp.code}")
                        val body = resp.body ?: error("响应体为空")
                        val total = body.contentLength()
                        var downloaded = 0L
                        body.byteStream().use { input ->
                            outFile.outputStream().use { output ->
                                val buf = ByteArray(16 * 1024)
                                var lastNotify = 0L
                                while (true) {
                                    val read = input.read(buf)
                                    if (read == -1) break
                                    output.write(buf, 0, read)
                                    downloaded += read
                                    val now = System.currentTimeMillis()
                                    if (now - lastNotify >= 120L || (total > 0 && downloaded >= total)) {
                                        lastNotify = now
                                        onProgress(downloaded, total, routeLabel)
                                    }
                                }
                            }
                        }
                        if (outFile.length() < 100_000L) {
                            error("下载文件异常过小")
                        }
                    }
                    outFile
                }
                if (res.isSuccess) return@runCatching outFile
                lastError = res.exceptionOrNull()
                if (outFile.exists()) outFile.delete()
            }

            throw (lastError ?: IllegalStateException("所有下载线路均失败"))
        }
    }

    /**
     * 拉起安卓系统安装器；若未授权“安装未知应用”，跳转系统授权页
     */
    fun installApk(apkFile: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        return true
    }

    private fun buildCandidateUrls(
        rawUrl: String,
        useCnAccel: Boolean,
        mirror: DownloadMirror,
        includeProxiesForApi: Boolean
    ): List<String> {
        if (!useCnAccel || mirror == DownloadMirror.DIRECT) return listOf(rawUrl)
        val list = mutableListOf<String>()
        if (includeProxiesForApi) {
            // GitHub API 先直连优先，失败再尝试加速代理
            list.add(rawUrl)
        }
        when (mirror) {
            DownloadMirror.AUTO -> {
                list.add(DownloadMirror.GH_PROXY.prefix + rawUrl)
                list.add(DownloadMirror.GH_FAST.prefix + rawUrl)
                list.add(DownloadMirror.GH_MIRROR.prefix + rawUrl)
            }
            DownloadMirror.GH_PROXY, DownloadMirror.GH_FAST, DownloadMirror.GH_MIRROR -> {
                list.add(mirror.prefix + rawUrl)
            }
            DownloadMirror.DIRECT -> Unit
        }
        if (!includeProxiesForApi) list.add(rawUrl)
        return list.distinct()
    }

    private fun buildDownloadRoutes(
        apkUrl: String,
        useCnAccel: Boolean,
        mirror: DownloadMirror
    ): List<Pair<String, String>> {
        if (!useCnAccel || mirror == DownloadMirror.DIRECT) {
            return listOf("GitHub直连" to apkUrl)
        }
        return when (mirror) {
            DownloadMirror.AUTO -> listOf(
                DownloadMirror.GH_PROXY.label to (DownloadMirror.GH_PROXY.prefix + apkUrl),
                DownloadMirror.GH_FAST.label to (DownloadMirror.GH_FAST.prefix + apkUrl),
                DownloadMirror.GH_MIRROR.label to (DownloadMirror.GH_MIRROR.prefix + apkUrl),
                DownloadMirror.DIRECT.label to apkUrl
            )
            DownloadMirror.GH_PROXY, DownloadMirror.GH_FAST, DownloadMirror.GH_MIRROR -> listOf(
                mirror.label to (mirror.prefix + apkUrl),
                DownloadMirror.DIRECT.label to apkUrl
            )
            DownloadMirror.DIRECT -> listOf("GitHub直连" to apkUrl)
        }
    }

    private fun compareVersions(remote: String, local: String): Int {
        val rParts = remote.split(".").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val rv = rParts.getOrElse(i) { 0 }
            val lv = lParts.getOrElse(i) { 0 }
            if (rv != lv) return rv.compareTo(lv)
        }
        return 0
    }
}
