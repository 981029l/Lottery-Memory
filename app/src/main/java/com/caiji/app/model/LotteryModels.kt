package com.caiji.app.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime

@Serializable
enum class LotteryType(
    val displayName: String,
    val frontMax: Int,
    val frontCount: Int,
    val backMax: Int,
    val backCount: Int,
    val sourceName: String
) {
    SSQ(
        displayName = "双色球",
        frontMax = 33,
        frontCount = 6,
        backMax = 16,
        backCount = 1,
        sourceName = "中国福利彩票发行管理中心"
    ),
    DLT(
        displayName = "大乐透",
        frontMax = 35,
        frontCount = 5,
        backMax = 12,
        backCount = 2,
        sourceName = "国家体育总局体育彩票管理中心"
    )
}

@Serializable
enum class PlayMode(val label: String) {
    SINGLE("单式"),
    MULTIPLE("复式"),
    DANTUO("胆拖")
}

@Serializable
data class PrizeLevel(
    val name: String,
    val count: String,
    val amount: String
)

@Serializable
data class RegionWin(
    val province: String,
    val count: Int
)

@Serializable
data class DrawRecord(
    val type: LotteryType,
    val issue: String,
    val date: String,
    val frontBalls: List<Int>,
    val backBalls: List<Int>,
    val poolMoney: String = "--",
    val salesMoney: String = "--",
    val firstPrizeCount: String = "--",
    val firstPrizeAmount: String = "--",
    val prizeLevels: List<PrizeLevel> = emptyList(),
    val regions: List<RegionWin> = emptyList(),
    val regionRaw: String = "",
    val noticePdfUrl: String = "",
    val isFallback: Boolean = false
)

@Serializable
data class UserTicket(
    val id: Long,
    val type: LotteryType,
    val issue: String,
    val frontBalls: List<Int>,
    val backBalls: List<Int>,
    val playMode: PlayMode = PlayMode.SINGLE,
    val frontDan: List<Int> = emptyList(),
    val backDan: List<Int> = emptyList(),
    val multiplier: Int = 1,
    val extra: Boolean = false,
    val source: String = "机选",
    val createdAt: Long = System.currentTimeMillis()
) {
    val betCount: Int
        get() = LotteryRules.calcBetCount(
            type = type,
            mode = playMode,
            frontCount = frontBalls.size,
            backCount = backBalls.size,
            frontDanCount = frontDan.size,
            backDanCount = backDan.size
        ).coerceAtLeast(1)

    val unitPrice: Int get() = if (type == LotteryType.DLT && extra) 3 else 2

    val cost: Int get() = betCount * unitPrice * multiplier.coerceAtLeast(1)
}

data class TicketCheckResult(
    val isPending: Boolean,
    val hitFront: Set<Int> = emptySet(),
    val hitBack: Set<Int> = emptySet(),
    val prizeName: String? = null,
    val prizeBreakdown: Map<String, Int> = emptyMap(),
    val fixedAmount: Int = 0,
    val totalAmount: Long = 0L,
    val afterTaxAmount: Long = 0L
) {
    val isWon: Boolean get() = !isPending && prizeName != null
    val totalWonBets: Int get() = prizeBreakdown.values.sum()
    val hasTaxDeducted: Boolean get() = totalAmount > 10_000L
}

data class DrawCountdown(
    val nextIssueHint: String,
    val subtitle: String,
    val formattedRemaining: String
)

object LotteryRules {

    fun nextIssueNumber(latestIssue: String): String {
        val numeric = latestIssue.toLongOrNull() ?: return latestIssue
        return (numeric + 1).toString()
    }

    fun generateRandom(type: LotteryType): Pair<List<Int>, List<Int>> {
        val front = (1..type.frontMax).shuffled().take(type.frontCount).sorted()
        val back = (1..type.backMax).shuffled().take(type.backCount).sorted()
        return front to back
    }

    fun comb(n: Int, k: Int): Int {
        if (k < 0 || k > n) return 0
        if (k == 0 || k == n) return 1
        val m = minOf(k, n - k)
        var res = 1L
        for (i in 1..m) {
            res = res * (n - m + i) / i
            if (res > 1_000_000L) return 1_000_000
        }
        return res.toInt()
    }

    fun calcBetCount(
        type: LotteryType,
        mode: PlayMode,
        frontCount: Int,
        backCount: Int,
        frontDanCount: Int = 0,
        backDanCount: Int = 0
    ): Int {
        return when (mode) {
            PlayMode.SINGLE -> {
                if (frontCount == type.frontCount && backCount == type.backCount) 1 else 0
            }
            PlayMode.MULTIPLE -> {
                if (frontCount < type.frontCount || backCount < type.backCount) return 0
                comb(frontCount, type.frontCount) * comb(backCount, type.backCount)
            }
            PlayMode.DANTUO -> {
                if (frontDanCount < 1 || frontDanCount >= type.frontCount) return 0
                val needFrontTuo = type.frontCount - frontDanCount
                if (frontCount < needFrontTuo) return 0
                if (backDanCount < 0 || backDanCount >= type.backCount) return 0
                val needBackTuo = type.backCount - backDanCount
                if (backCount < needBackTuo) return 0
                comb(frontCount, needFrontTuo) * comb(backCount, needBackTuo)
            }
        }
    }

    fun parseMoneyLong(raw: String): Long {
        val cleaned = raw.replace(",", "").replace("元", "").replace("¥", "").trim()
        return cleaned.toDoubleOrNull()?.toLong() ?: 0L
    }

    fun formatCompactYuan(amount: Long): String {
        return when {
            amount >= 100_000_000L -> String.format("¥%.2f亿", amount / 100_000_000.0)
            amount >= 10_000L -> String.format("¥%.1f万", amount / 10_000.0)
            else -> "¥$amount"
        }
    }

    fun afterTaxYuan(gross: Long): Long =
        if (gross > 10_000L) (gross * 80L) / 100L else gross

    private fun singlePrize(
        type: LotteryType,
        r: Int,
        b: Int,
        draw: DrawRecord,
        extra: Boolean
    ): Pair<String, Long>? {
        fun officialAmount(levelName: String, fallback: Long): Long {
            val raw = draw.prizeLevels.firstOrNull { it.name == levelName }?.amount.orEmpty()
            val parsed = parseMoneyLong(raw)
            return if (parsed > 0L) parsed else fallback
        }
        fun dltWithExtra(baseName: String): Long {
            val base = officialAmount(baseName, 0L)
            if (!extra) return base
            val addName = "$baseName(追加)"
            val add = officialAmount(addName, (base * 80L) / 100L)
            return base + add
        }
        return when (type) {
            LotteryType.SSQ -> when {
                r == 6 && b == 1 -> "一等奖" to officialAmount("一等奖", 0L)
                r == 6 -> "二等奖" to officialAmount("二等奖", 0L)
                r == 5 && b == 1 -> "三等奖" to officialAmount("三等奖", 3000L)
                r == 5 || (r == 4 && b == 1) -> "四等奖" to officialAmount("四等奖", 200L)
                r == 4 || (r == 3 && b == 1) -> "五等奖" to officialAmount("五等奖", 10L)
                b == 1 -> "六等奖" to officialAmount("六等奖", 5L)
                else -> null
            }
            // 2026年超级大乐透现行7奖级新规（一二等奖支持合并追加奖金）
            LotteryType.DLT -> when {
                r == 5 && b == 2 -> "一等奖" to dltWithExtra("一等奖")
                r == 5 && b == 1 -> "二等奖" to dltWithExtra("二等奖")
                r == 5 || (r == 4 && b == 2) -> "三等奖" to officialAmount("三等奖", 5000L)
                r == 4 && b == 1 -> "四等奖" to officialAmount("四等奖", 300L)
                (r == 3 && b == 2) || r == 4 -> "五等奖" to officialAmount("五等奖", 150L)
                (r == 3 && b == 1) || (r == 2 && b == 2) -> "六等奖" to officialAmount("六等奖", 15L)
                r == 3 || (r == 2 && b == 1) || (r == 1 && b == 2) || b == 2 -> "七等奖" to officialAmount("七等奖", 5L)
                else -> null
            }
        }
    }

    /**
     * 精确支持单式、复式、胆拖拆单中奖统计与固定奖金计算
     */
    fun checkTicket(ticket: UserTicket, draws: List<DrawRecord>): TicketCheckResult {
        val draw = draws.firstOrNull { it.type == ticket.type && it.issue == ticket.issue }
            ?: return TicketCheckResult(isPending = true)

        val drawFront = draw.frontBalls.toSet()
        val drawBack = draw.backBalls.toSet()

        val allFront = (ticket.frontDan + ticket.frontBalls).distinct()
        val allBack = (ticket.backDan + ticket.backBalls).distinct()
        val hitFront = allFront.filter { it in drawFront }.toSet()
        val hitBack = allBack.filter { it in drawBack }.toSet()

        val breakdown = linkedMapOf<String, Int>()
        var grossTotal = 0L
        val mult = ticket.multiplier.coerceAtLeast(1)

        if (ticket.playMode == PlayMode.SINGLE) {
            val prize = singlePrize(ticket.type, hitFront.size, hitBack.size, draw, ticket.extra)
            if (prize != null) {
                breakdown[prize.first] = mult
                grossTotal = prize.second * mult
            }
        } else {
            val fDanHits = ticket.frontDan.count { it in drawFront }
            val fTuoHits = ticket.frontBalls.count { it in drawFront }
            val fTuoMiss = ticket.frontBalls.size - fTuoHits
            val needFTuo = ticket.type.frontCount - ticket.frontDan.size

            val bDanHits = ticket.backDan.count { it in drawBack }
            val bTuoHits = ticket.backBalls.count { it in drawBack }
            val bTuoMiss = ticket.backBalls.size - bTuoHits
            val needBTuo = ticket.type.backCount - ticket.backDan.size

            for (fh in 0..minOf(fTuoHits, needFTuo)) {
                val fWays = comb(fTuoHits, fh) * comb(fTuoMiss, needFTuo - fh)
                if (fWays == 0) continue
                for (bh in 0..minOf(bTuoHits, needBTuo)) {
                    val bWays = comb(bTuoHits, bh) * comb(bTuoMiss, needBTuo - bh)
                    if (bWays == 0) continue
                    val ways = fWays * bWays
                    val prize = singlePrize(ticket.type, fDanHits + fh, bDanHits + bh, draw, ticket.extra) ?: continue
                    breakdown[prize.first] = (breakdown[prize.first] ?: 0) + ways * mult
                    grossTotal += prize.second * ways * mult
                }
            }
        }

        val order = listOf("一等奖", "二等奖", "三等奖", "四等奖", "五等奖", "六等奖", "七等奖", "八等奖", "九等奖")
        val sortedBreakdown = linkedMapOf<String, Int>()
        order.forEach { lv -> breakdown[lv]?.let { sortedBreakdown[lv] = it } }

        val topPrize = sortedBreakdown.keys.firstOrNull()
        val displayPrize = when {
            topPrize == null -> null
            sortedBreakdown.size > 1 -> "${topPrize}等 ${sortedBreakdown.values.sum()} 注"
            (sortedBreakdown[topPrize] ?: 1) > 1 -> "$topPrize ×${sortedBreakdown[topPrize]}"
            else -> topPrize
        }

        val netTotal = afterTaxYuan(grossTotal)
        return TicketCheckResult(
            isPending = false,
            hitFront = hitFront,
            hitBack = hitBack,
            prizeName = displayPrize,
            prizeBreakdown = sortedBreakdown,
            fixedAmount = netTotal.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            totalAmount = grossTotal,
            afterTaxAmount = netTotal
        )
    }

    fun computeCountdown(type: LotteryType, latestIssue: String, now: LocalDateTime = LocalDateTime.now()): DrawCountdown {
        val drawDays = if (type == LotteryType.SSQ) {
            setOf(DayOfWeek.SUNDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY)
        } else {
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY)
        }
        val hour = 21
        val minute = if (type == LotteryType.SSQ) 15 else 25

        var candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        for (i in 0..7) {
            val test = candidate.plusDays(i.toLong())
            if (test.dayOfWeek in drawDays && test.isAfter(now)) {
                candidate = test
                break
            }
        }
        val dur = Duration.between(now, candidate).coerceAtLeast(Duration.ZERO)
        val days = dur.toDays()
        val hours = dur.toHours() % 24
        val mins = dur.toMinutes() % 60
        val secs = dur.seconds % 60

        val dayDiff = Duration.between(
            now.toLocalDate().atStartOfDay(),
            candidate.toLocalDate().atStartOfDay()
        ).toDays()
        val dayText = when (dayDiff) {
            0L -> "今晚"
            1L -> "明天"
            else -> when (candidate.dayOfWeek) {
                DayOfWeek.MONDAY -> "周一"
                DayOfWeek.TUESDAY -> "周二"
                DayOfWeek.WEDNESDAY -> "周三"
                DayOfWeek.THURSDAY -> "周四"
                DayOfWeek.FRIDAY -> "周五"
                DayOfWeek.SATURDAY -> "周六"
                DayOfWeek.SUNDAY -> "周日"
            }
        }
        val formatted = buildString {
            if (days > 0) append("${days}天 ")
            append(String.format("%02d:%02d:%02d", hours, mins, secs))
        }
        val nextIssue = nextIssueNumber(latestIssue)
        return DrawCountdown(
            nextIssueHint = "距第 $nextIssue 期开奖",
            subtitle = "$dayText ${String.format("%02d:%02d", hour, minute)} 开奖 · 20:00 停售",
            formattedRemaining = formatted
        )
    }

    fun formatCompactMoney(raw: String): String {
        val cleaned = raw.replace(",", "").replace("元", "").trim()
        val value = cleaned.toDoubleOrNull() ?: return raw.ifBlank { "--" }
        return when {
            value >= 100_000_000.0 -> String.format("%.2f亿", value / 100_000_000.0)
            value >= 10_000.0 -> String.format("%.0f万", value / 10_000.0)
            value > 0 -> String.format("%.0f", value)
            else -> "--"
        }
    }

    fun parseRegionsFromText(text: String): List<RegionWin> {
        if (text.isBlank()) return emptyList()
        val regex = Regex("([\\u4e00-\\u9fa5]{2,4})\\s*[（(]?\\s*(\\d+)\\s*注")
        val map = linkedMapOf<String, Int>()
        regex.findAll(text).forEach { m ->
            val province = m.groupValues[1]
                .removePrefix("其中")
                .removePrefix("基本")
                .removePrefix("追加")
            val count = m.groupValues[2].toIntOrNull() ?: 0
            if (province.length >= 2 && count > 0) {
                map[province] = (map[province] ?: 0) + count
            }
        }
        return map.entries
            .sortedByDescending { it.value }
            .map { RegionWin(it.key, it.value) }
    }

    fun fallbackDraws(type: LotteryType): List<DrawRecord> = when (type) {
        LotteryType.SSQ -> listOf(
            DrawRecord(
                type = LotteryType.SSQ,
                issue = "2026134",
                date = "2026-11-20",
                frontBalls = listOf(4, 11, 17, 23, 28, 31),
                backBalls = listOf(9),
                poolMoney = "23.40亿",
                salesMoney = "3.86亿",
                firstPrizeCount = "7",
                firstPrizeAmount = "768万",
                prizeLevels = listOf(
                    PrizeLevel("一等奖", "7", "7,683,412"),
                    PrizeLevel("二等奖", "132", "218,906"),
                    PrizeLevel("三等奖", "1,846", "3,000"),
                    PrizeLevel("四等奖", "89,410", "200"),
                    PrizeLevel("五等奖", "1,602,377", "10"),
                    PrizeLevel("六等奖", "9,876,210", "5")
                ),
                regions = listOf(
                    RegionWin("广东", 3),
                    RegionWin("浙江", 2),
                    RegionWin("北京", 1),
                    RegionWin("四川", 1)
                ),
                regionRaw = "广东3注，浙江2注，北京1注，四川1注",
                isFallback = true
            ),
            DrawRecord(
                type = LotteryType.SSQ,
                issue = "2026133",
                date = "2026-11-18",
                frontBalls = listOf(2, 8, 14, 22, 27, 33),
                backBalls = listOf(12),
                poolMoney = "22.91亿",
                salesMoney = "3.72亿",
                firstPrizeCount = "5",
                firstPrizeAmount = "892万",
                regions = listOf(RegionWin("江苏", 2), RegionWin("山东", 1), RegionWin("福建", 1), RegionWin("河北", 1)),
                isFallback = true
            )
        )
        LotteryType.DLT -> listOf(
            DrawRecord(
                type = LotteryType.DLT,
                issue = "26134",
                date = "2026-11-21",
                frontBalls = listOf(6, 13, 21, 29, 35),
                backBalls = listOf(4, 10),
                poolMoney = "9.82亿",
                salesMoney = "3.12亿",
                firstPrizeCount = "4",
                firstPrizeAmount = "1000万",
                prizeLevels = listOf(
                    PrizeLevel("一等奖", "4", "10,000,000"),
                    PrizeLevel("二等奖", "61", "182,345"),
                    PrizeLevel("三等奖", "312", "10,000"),
                    PrizeLevel("四等奖", "1,203", "3,000"),
                    PrizeLevel("五等奖", "21,560", "300"),
                    PrizeLevel("六等奖", "38,910", "200"),
                    PrizeLevel("七等奖", "96,412", "100"),
                    PrizeLevel("八等奖", "1,520,331", "15"),
                    PrizeLevel("九等奖", "8,912,004", "5")
                ),
                regions = listOf(
                    RegionWin("江苏", 2),
                    RegionWin("上海", 1),
                    RegionWin("河南", 1)
                ),
                regionRaw = "江苏2注，上海1注，河南1注",
                isFallback = true
            ),
            DrawRecord(
                type = LotteryType.DLT,
                issue = "26133",
                date = "2026-11-19",
                frontBalls = listOf(2, 9, 18, 24, 32),
                backBalls = listOf(1, 8),
                poolMoney = "9.54亿",
                salesMoney = "2.98亿",
                firstPrizeCount = "3",
                firstPrizeAmount = "1000万",
                regions = listOf(RegionWin("山东", 1), RegionWin("湖北", 1), RegionWin("云南", 1)),
                isFallback = true
            )
        )
    }
}
