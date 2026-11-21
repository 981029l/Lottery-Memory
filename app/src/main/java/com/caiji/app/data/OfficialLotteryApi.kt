package com.caiji.app.data

import com.caiji.app.model.DrawRecord
import com.caiji.app.model.LotteryRules
import com.caiji.app.model.LotteryType
import com.caiji.app.model.PrizeLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class OfficialLotteryApi {

    private val json = Json { ignoreUnknownKeys = true }

    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                val merged = (cookieStore[url.host].orEmpty() + cookies).associateBy { it.name }.values.toList()
                cookieStore[url.host] = merged
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host].orEmpty()
            }
        })
        .build()

    suspend fun fetchDraws(type: LotteryType, pageSize: Int = 20): Result<List<DrawRecord>> =
        withContext(Dispatchers.IO) {
            runCatching {
                when (type) {
                    LotteryType.SSQ -> fetchSsq(pageSize)
                    LotteryType.DLT -> fetchDlt(pageSize)
                }
            }
        }

    private fun fetchSsq(pageSize: Int): List<DrawRecord> {
        // 先访问福彩主页拿到防刷会话 Cookie
        val warmReq = Request.Builder()
            .url("https://www.cwl.gov.cn/ygkj/wqkjgg/ssq/")
            .header("User-Agent", UA)
            .build()
        runCatching { client.newCall(warmReq).execute().close() }

        val url = "https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice" +
            "?name=ssq&issueCount=&issueStart=&issueEnd=&dayStart=&dayEnd=&pageNo=1&pageSize=$pageSize&week=&systemType=PC"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://www.cwl.gov.cn/ygkj/wqkjgg/ssq/")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .build()

        val body = client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("福彩接口响应异常: HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }

        val root = json.parseToJsonElement(body).jsonObject
        val resultArr = root["result"]?.jsonArray ?: error("福彩返回数据为空")

        return resultArr.mapNotNull { el ->
            val obj = el.jsonObject
            val issue = obj.str("code")
            val red = obj.str("red").split(",").mapNotNull { it.trim().toIntOrNull() }
            val blue = obj.str("blue").split(",").mapNotNull { it.trim().toIntOrNull() }
            if (issue.isBlank() || red.size < 6 || blue.isEmpty()) return@mapNotNull null

            val grades = obj["prizegrades"] as? JsonArray
            val levels = grades?.mapNotNull { g ->
                val go = g.jsonObject
                val typeNum = go.str("type").toIntOrNull() ?: return@mapNotNull null
                val name = when (typeNum) {
                    1 -> "一等奖"
                    2 -> "二等奖"
                    3 -> "三等奖"
                    4 -> "四等奖"
                    5 -> "五等奖"
                    6 -> "六等奖"
                    else -> "${typeNum}等奖"
                }
                PrizeLevel(
                    name = name,
                    count = go.str("typenum").ifBlank { "0" },
                    amount = go.str("typemoney").ifBlank { "0" }
                )
            }.orEmpty()

            val first = levels.firstOrNull()
            val content = obj.str("content")
            DrawRecord(
                type = LotteryType.SSQ,
                issue = issue,
                date = obj.str("date").substringBefore("("),
                frontBalls = red.sorted(),
                backBalls = blue.sorted(),
                poolMoney = LotteryRules.formatCompactMoney(obj.str("poolmoney")),
                salesMoney = LotteryRules.formatCompactMoney(obj.str("sales")),
                firstPrizeCount = first?.count ?: "--",
                firstPrizeAmount = LotteryRules.formatCompactMoney(first?.amount.orEmpty()),
                prizeLevels = levels,
                regions = LotteryRules.parseRegionsFromText(content),
                regionRaw = content,
                isFallback = false
            )
        }
    }

    private fun fetchDlt(pageSize: Int): List<DrawRecord> {
        val url = "https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry" +
            "?gameNo=85&provinceId=0&pageSize=$pageSize&isVerify=1&pageNo=1"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://www.lottery.gov.cn/")
            .header("Origin", "https://www.lottery.gov.cn")
            .build()

        val body = client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("体彩接口响应异常: HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }

        val root = json.parseToJsonElement(body).jsonObject
        val list = root["value"]?.jsonObject?.get("list")?.jsonArray ?: error("体彩返回数据为空")

        return list.mapNotNull { el ->
            val obj = el.jsonObject
            val issue = obj.str("lotteryDrawNum")
            val nums = obj.str("lotteryDrawResult").trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }
            if (issue.isBlank() || nums.size < 7) return@mapNotNull null

            val front = nums.take(5).sorted()
            val back = nums.drop(5).take(2).sorted()

            val prizeArr = obj["prizeLevelList"] as? JsonArray
            val levels = prizeArr?.mapNotNull { p ->
                val po = p.jsonObject
                val levelName = po.str("prizeLevel")
                if (levelName.isBlank()) return@mapNotNull null
                PrizeLevel(
                    name = levelName,
                    count = po.str("stakeCount").ifBlank { "0" },
                    amount = po.str("stakeAmount").ifBlank { "0" }
                )
            }.orEmpty()

            val first = levels.firstOrNull { it.name == "一等奖" } ?: levels.firstOrNull()
            val pdfUrl = obj.str("drawPdfUrl").takeIf { it.startsWith("http") }.orEmpty()
            val noticeText = obj.str("lotteryDrawResultNotice")

            DrawRecord(
                type = LotteryType.DLT,
                issue = issue,
                date = obj.str("lotteryDrawTime"),
                frontBalls = front,
                backBalls = back,
                poolMoney = LotteryRules.formatCompactMoney(obj.str("poolBalanceAfterdraw")),
                salesMoney = LotteryRules.formatCompactMoney(obj.str("totalSaleAmount")),
                firstPrizeCount = first?.count ?: "--",
                firstPrizeAmount = LotteryRules.formatCompactMoney(first?.amount.orEmpty()),
                prizeLevels = levels,
                regions = LotteryRules.parseRegionsFromText(noticeText),
                regionRaw = noticeText,
                noticePdfUrl = pdfUrl,
                isFallback = false
            )
        }
    }

    private fun JsonObject.str(key: String): String =
        this[key]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

    companion object {
        private const val UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    }
}
