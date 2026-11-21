package com.caiji.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.caiji.app.data.AppUpdater
import com.caiji.app.data.DownloadMirror
import com.caiji.app.data.LocalStore
import com.caiji.app.data.OfficialLotteryApi
import com.caiji.app.data.ReleaseInfo
import com.caiji.app.model.DrawRecord
import com.caiji.app.model.LotteryRules
import com.caiji.app.model.LotteryType
import com.caiji.app.model.UserTicket
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class IslandToastState(
    val visible: Boolean = false,
    val title: String = "",
    val body: String = "",
    val trailing: String = "",
    val openSheetOnTap: Boolean = false
)

data class UpdateUiState(
    val currentVersion: String = "1.0.0",
    val cnAccelEnabled: Boolean = true,
    val mirror: DownloadMirror = DownloadMirror.AUTO,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedText: String = "",
    val activeRoute: String = "",
    val latestRelease: ReleaseInfo? = null,
    val downloadedApk: File? = null,
    val statusText: String = "点击检查 GitHub 最新版本"
)

data class LotteryUiState(
    val selectedType: LotteryType = LotteryType.SSQ,
    val ssqDraws: List<DrawRecord> = emptyList(),
    val dltDraws: List<DrawRecord> = emptyList(),
    val tickets: List<UserTicket> = emptyList(),
    val isRefreshing: Boolean = false,
    val autoNotifyEnabled: Boolean = true,
    val statusNote: String? = null,
    val toast: IslandToastState = IslandToastState(),
    val update: UpdateUiState = UpdateUiState()
) {
    val currentDraws: List<DrawRecord>
        get() = if (selectedType == LotteryType.SSQ) ssqDraws else dltDraws

    val latestDraw: DrawRecord
        get() = currentDraws.firstOrNull() ?: LotteryRules.fallbackDraws(selectedType).first()

    val allDraws: List<DrawRecord>
        get() = ssqDraws + dltDraws
}

class LotteryViewModel(application: Application) : AndroidViewModel(application) {

    private val api = OfficialLotteryApi()
    private val store = LocalStore(application)
    private val updater = AppUpdater(application)

    private val _uiState = MutableStateFlow(
        LotteryUiState(
            ssqDraws = store.loadDraws(LotteryType.SSQ).ifEmpty { LotteryRules.fallbackDraws(LotteryType.SSQ) },
            dltDraws = store.loadDraws(LotteryType.DLT).ifEmpty { LotteryRules.fallbackDraws(LotteryType.DLT) },
            tickets = store.loadTickets(),
            autoNotifyEnabled = store.isAutoNotifyEnabled(),
            update = UpdateUiState(
                currentVersion = updater.currentVersion(),
                cnAccelEnabled = store.isCnAccelEnabled(),
                mirror = store.loadDownloadMirror(),
                statusText = if (updater.configuredRepo().isBlank()) {
                    "推送至 GitHub Actions 打包后自动启用"
                } else {
                    "点击检查新版本"
                }
            )
        )
    )
    val uiState: StateFlow<LotteryUiState> = _uiState.asStateFlow()

    init {
        refreshAll(silent = true)
        if (updater.configuredRepo().isNotBlank()) {
            checkForUpdate(silent = true)
        }
    }

    fun selectLottery(type: LotteryType) {
        if (_uiState.value.selectedType == type) return
        _uiState.update { it.copy(selectedType = type) }
    }

    fun refreshAll(silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, statusNote = null) }
            val ssqRes = api.fetchDraws(LotteryType.SSQ)
            val dltRes = api.fetchDraws(LotteryType.DLT)

            val newSsq = ssqRes.getOrNull()?.takeIf { it.isNotEmpty() }
            val newDlt = dltRes.getOrNull()?.takeIf { it.isNotEmpty() }

            if (newSsq != null) store.saveDraws(LotteryType.SSQ, newSsq)
            if (newDlt != null) store.saveDraws(LotteryType.DLT, newDlt)

            val failedCount = listOf(newSsq, newDlt).count { it == null }
            _uiState.update { s ->
                s.copy(
                    ssqDraws = newSsq ?: s.ssqDraws,
                    dltDraws = newDlt ?: s.dltDraws,
                    isRefreshing = false,
                    statusNote = if (failedCount == 2) "官方接口暂不可达，当前展示本地缓存" else null
                )
            }
            if (!silent) {
                if (failedCount == 0) {
                    showToast("数据已同步", "双色球与大乐透已更新至最新开奖", "最新")
                } else {
                    showToast("部分同步完成", "已保留最新缓存数据，可稍后重试", "")
                }
            }
        }
    }

    fun addTickets(
        type: LotteryType,
        picks: List<Pair<List<Int>, List<Int>>>,
        playMode: com.caiji.app.model.PlayMode = com.caiji.app.model.PlayMode.SINGLE,
        frontDan: List<Int> = emptyList(),
        backDan: List<Int> = emptyList(),
        multiplier: Int = 1,
        extra: Boolean = false,
        source: String = "机选"
    ) {
        val latest = (if (type == LotteryType.SSQ) _uiState.value.ssqDraws else _uiState.value.dltDraws)
            .firstOrNull()?.issue ?: LotteryRules.fallbackDraws(type).first().issue
        val nextIssue = LotteryRules.nextIssueNumber(latest)
        val now = System.currentTimeMillis()
        val safeMult = multiplier.coerceIn(1, 99)

        val created = picks.mapIndexed { idx, (front, back) ->
            UserTicket(
                id = now + idx,
                type = type,
                issue = nextIssue,
                frontBalls = front.sorted(),
                backBalls = back.sorted(),
                playMode = playMode,
                frontDan = frontDan.sorted(),
                backDan = backDan.sorted(),
                multiplier = safeMult,
                extra = extra && type == LotteryType.DLT,
                source = source,
                createdAt = now
            )
        }
        val merged = created + _uiState.value.tickets
        store.saveTickets(merged)
        _uiState.update { it.copy(tickets = merged) }

        val totalBets = created.sumOf { it.betCount }
        val multSuffix = if (safeMult > 1) " × ${safeMult}倍" else ""
        showToast(
            title = "已记入票夹 · ${playMode.label}",
            body = "${type.displayName} 第 $nextIssue 期 · ${totalBets} 注$multSuffix",
            trailing = "¥${created.sumOf { it.cost }}"
        )
    }

    fun deleteTicket(id: Long) {
        val updated = _uiState.value.tickets.filterNot { it.id == id }
        store.saveTickets(updated)
        _uiState.update { it.copy(tickets = updated) }
    }

    fun toggleAutoNotify() {
        val next = !_uiState.value.autoNotifyEnabled
        store.setAutoNotifyEnabled(next)
        _uiState.update { it.copy(autoNotifyEnabled = next) }
        showToast(
            title = if (next) "开奖提醒已开启" else "开奖提醒已暂停",
            body = if (next) "开奖日后台自动拉取结果并核对票夹" else "已关闭后台自动核对通知",
            trailing = ""
        )
    }

    fun triggerCheckNotificationPreview() {
        val s = _uiState.value
        val latest = s.latestDraw
        val checks = s.tickets
            .filter { it.type == s.selectedType }
            .map { it to LotteryRules.checkTicket(it, s.allDraws) }
        val won = checks.firstOrNull { it.second.isWon }
        if (won != null) {
            val amt = if (won.second.fixedAmount > 0) "+¥${won.second.fixedAmount}" else "浮动奖"
            showToast(
                title = "${s.selectedType.displayName} 第 ${latest.issue} 期已开奖",
                body = "你的号码命中${won.second.prizeName}，展开票夹查看",
                trailing = amt
            )
        } else {
            showToast(
                title = "${s.selectedType.displayName} 第 ${latest.issue} 期已开奖",
                body = "最新开奖号码已核对，祝下期好运",
                trailing = "已核对"
            )
        }
    }

    fun toggleCnAccel() {
        val next = !_uiState.value.update.cnAccelEnabled
        store.setCnAccelEnabled(next)
        _uiState.update { it.copy(update = it.update.copy(cnAccelEnabled = next)) }
    }

    fun cycleDownloadMirror() {
        val all = DownloadMirror.entries
        val curIdx = all.indexOf(_uiState.value.update.mirror)
        val next = all[(curIdx + 1) % all.size]
        store.saveDownloadMirror(next)
        _uiState.update { it.copy(update = it.update.copy(mirror = next)) }
    }

    fun checkForUpdate(silent: Boolean = false) {
        val u = _uiState.value.update
        if (u.isChecking || u.isDownloading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(update = it.update.copy(isChecking = true, statusText = "正在检查更新..."))
            }
            val result = updater.checkLatestRelease(u.cnAccelEnabled, u.mirror)
            result.onSuccess { info ->
                _uiState.update {
                    it.copy(
                        update = it.update.copy(
                            isChecking = false,
                            latestRelease = info,
                            statusText = if (info.hasNewVersion) {
                                "发现新版本 v${info.versionName}"
                            } else {
                                "已是最新版本 v${updater.currentVersion()}"
                            }
                        )
                    )
                }
                if (info.hasNewVersion) {
                    showToast(
                        title = "发现新版本 v${info.versionName}",
                        body = "点击打开我的票夹，支持中国地区加速下载",
                        trailing = "更新",
                        openSheetOnTap = true
                    )
                } else if (!silent) {
                    showToast("已是最新版本", "当前版本 v${updater.currentVersion()}", "最新")
                }
            }.onFailure { err ->
                val msg = err.message ?: "检查更新失败"
                _uiState.update {
                    it.copy(update = it.update.copy(isChecking = false, statusText = msg))
                }
                if (!silent) {
                    showToast("检查更新失败", msg, "")
                }
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val u = _uiState.value.update
        if (u.isDownloading) return

        val readyApk = u.downloadedApk
        if (readyApk != null && readyApk.exists()) {
            val started = updater.installApk(readyApk)
            if (!started) {
                showToast("需要安装权限", "请允许「彩记」安装应用后再次点击安装", "")
            }
            return
        }

        val apkUrl = u.latestRelease?.apkUrl.orEmpty()
        if (apkUrl.isBlank()) {
            checkForUpdate(silent = false)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    update = it.update.copy(
                        isDownloading = true,
                        downloadProgress = 0f,
                        downloadedText = "正在连接加速节点...",
                        statusText = "正在下载 v${u.latestRelease?.versionName}"
                    )
                )
            }

            val res = updater.downloadApk(
                apkUrl = apkUrl,
                useCnAccel = u.cnAccelEnabled,
                mirror = u.mirror
            ) { downloaded, total, route ->
                val pct = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                val dlMb = String.format("%.1f MB", downloaded / 1048576.0)
                val totalMb = if (total > 0L) String.format(" / %.1f MB", total / 1048576.0) else ""
                _uiState.update {
                    it.copy(
                        update = it.update.copy(
                            downloadProgress = pct,
                            activeRoute = route,
                            downloadedText = "$route · $dlMb$totalMb"
                        )
                    )
                }
            }

            res.onSuccess { file ->
                _uiState.update {
                    it.copy(
                        update = it.update.copy(
                            isDownloading = false,
                            downloadProgress = 1f,
                            downloadedApk = file,
                            statusText = "下载完成，点击安装 v${u.latestRelease?.versionName}"
                        )
                    )
                }
                val started = updater.installApk(file)
                if (!started) {
                    showToast("需要安装权限", "授权后返回点击「立即安装」即可", "")
                }
            }.onFailure { err ->
                val msg = err.message ?: "下载失败"
                _uiState.update {
                    it.copy(
                        update = it.update.copy(
                            isDownloading = false,
                            statusText = "下载失败：$msg"
                        )
                    )
                }
                showToast("下载失败", "可切换国内加速线路后重试", "")
            }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = it.toast.copy(visible = false)) }
    }

    private fun showToast(
        title: String,
        body: String,
        trailing: String,
        openSheetOnTap: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(toast = IslandToastState(true, title, body, trailing, openSheetOnTap))
            }
            delay(3600)
            _uiState.update { it.copy(toast = it.toast.copy(visible = false)) }
        }
    }
}
