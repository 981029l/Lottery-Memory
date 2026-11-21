package com.caiji.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 使用 Android 系统原生 PdfRenderer（零额外体积）将体彩官方开奖公告 PDF 渲染成高清图片展示
 */
@Composable
fun PdfNoticeImages(
    pdfUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmaps by remember(pdfUrl) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember(pdfUrl) { mutableStateOf(true) }
    var errorMsg by remember(pdfUrl) { mutableStateOf<String?>(null) }

    LaunchedEffect(pdfUrl) {
        loading = true
        errorMsg = null
        val res = renderPdfUrlToBitmaps(context.cacheDir, pdfUrl)
        res.onSuccess { list ->
            bitmaps = list
            loading = false
        }.onFailure { err ->
            errorMsg = err.message ?: "官方开奖公告图加载失败"
            loading = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "正在生成官方开奖公告高清图...",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
            errorMsg != null -> {
                Text(
                    text = errorMsg.orEmpty(),
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }
            else -> {
                bitmaps.forEach { bmp ->
                    val ratio = (bmp.width.toFloat() / bmp.height.toFloat()).coerceIn(0.4f, 2.5f)
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "官方开奖公告",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratio)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

private suspend fun renderPdfUrlToBitmaps(cacheDir: File, url: String): Result<List<Bitmap>> =
    withContext(Dispatchers.IO) {
        runCatching {
            val noticeDir = File(cacheDir, "notices").apply { mkdirs() }
            val safeName = url.hashCode().toUInt().toString() + ".pdf"
            val pdfFile = File(noticeDir, safeName)

            if (!pdfFile.exists() || pdfFile.length() < 1024L) {
                val client = OkHttpClient.Builder()
                    .connectTimeout(12, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build()
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .header("Referer", "https://www.lottery.gov.cn/")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("公告下载失败 HTTP ${resp.code}")
                    val bytes = resp.body?.bytes() ?: error("公告内容为空")
                    pdfFile.writeBytes(bytes)
                }
            }

            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val result = mutableListOf<Bitmap>()
            pfd.use { fd ->
                PdfRenderer(fd).use { renderer ->
                    val pages = minOf(renderer.pageCount, 3)
                    for (i in 0 until pages) {
                        renderer.openPage(i).use { page ->
                            val targetW = 1280
                            val targetH = (page.height.toFloat() / page.width.toFloat() * targetW).toInt()
                                .coerceIn(400, 3200)
                            val bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            Canvas(bmp).drawColor(AndroidColor.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            result.add(bmp)
                        }
                    }
                }
            }
            if (result.isEmpty()) error("未解析到公告页面")
            result
        }
    }
