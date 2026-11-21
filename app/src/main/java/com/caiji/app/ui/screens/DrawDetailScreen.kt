package com.caiji.app.ui.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caiji.app.model.DrawRecord
import com.caiji.app.model.LotteryRules
import com.caiji.app.model.UserTicket
import com.caiji.app.ui.components.AppIcons
import com.caiji.app.ui.components.BallStyle
import com.caiji.app.ui.components.HeroDrawCardContent
import com.caiji.app.ui.components.LotteryBall
import com.caiji.app.ui.components.PdfNoticeImages
import com.caiji.app.ui.components.backBallStyle
import com.caiji.app.ui.components.frontBallStyle
import com.caiji.app.ui.motion.springClickable
import com.caiji.app.ui.theme.AppleColors

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)
@Composable
fun SharedTransitionScope.DrawDetailScreen(
    draw: DrawRecord,
    myTicketsForIssue: List<UserTicket>,
    allDraws: List<DrawRecord>,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClose: () -> Unit
) {
    val accent = AppleColors.accent(draw.type)
    val palette = AppleColors.glowPalette(draw.type)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .sharedBounds(
                sharedContentState = rememberSharedContentState(key = "draw-card-${draw.type.name}-${draw.issue}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1D1D20), Color(0xFF050505))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 吸顶固定顶栏（下滑看奖级和公告图时随时可关闭）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "开奖详情",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .springClickable(pressedScale = 0.90f, onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = "关闭",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 40.dp)
            ) {
                // 开奖大卡片内容
                HeroDrawCardContent(draw = draw, showChevron = false)

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // 我的本期号码核对
                if (myTicketsForIssue.isNotEmpty()) {
                    DetailSectionTitle("我的本期号码")
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        myTicketsForIssue.forEach { tk ->
                            val check = LotteryRules.checkTicket(tk, allDraws)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.06f))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val multText = if (tk.multiplier > 1) " · ${tk.multiplier}倍" else ""
                                    Text(
                                        text = "${tk.playMode.label} · ${tk.betCount} 注$multText",
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (check.isWon) {
                                            val amt = if (check.fixedAmount > 0) " · ¥${check.fixedAmount}" else " · 浮动奖"
                                            "${check.prizeName}$amt"
                                        } else "未中奖",
                                        color = if (check.isWon) AppleColors.GreenSuccess else Color.White.copy(alpha = 0.45f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    (tk.frontDan + tk.frontBalls).forEach { n ->
                                        LotteryBall(
                                            number = n,
                                            style = if (n in check.hitFront) frontBallStyle(tk.type) else BallStyle.MUTED_DARK,
                                            size = 28.dp,
                                            fontSize = 12.sp
                                        )
                                    }
                                    (tk.backDan + tk.backBalls).forEach { n ->
                                        LotteryBall(
                                            number = n,
                                            style = if (n in check.hitBack) backBallStyle(tk.type) else BallStyle.MUTED_DARK,
                                            size = 28.dp,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 一等奖出奖地
                DetailSectionTitle("一等奖出奖地")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (draw.regions.isNotEmpty()) {
                        val maxCount = draw.regions.maxOf { it.count }.coerceAtLeast(1)
                        draw.regions.forEach { region ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = region.province,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(44.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(region.count.toFloat() / maxCount.toFloat())
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(Brush.horizontalGradient(listOf(palette[0], palette[1])))
                                    )
                                }
                                Text(
                                    text = "${region.count} 注",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(44.dp)
                                )
                            }
                        }
                    } else if (draw.regionRaw.isNotBlank()) {
                        Text(
                            text = draw.regionRaw,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    } else if (draw.noticePdfUrl.isNotBlank()) {
                        Text(
                            text = "体彩出奖地区明细见下方官方开奖公告图",
                            color = Color.White.copy(alpha = 0.56f),
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "本期官方接口未返回细分省份分布",
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 13.sp
                        )
                    }
                }

                // 体彩官方开奖公告（App 内原生 PdfRenderer 渲染为高清图片）
                if (draw.noticePdfUrl.isNotBlank()) {
                    DetailSectionTitle("官方开奖公告")
                    PdfNoticeImages(pdfUrl = draw.noticePdfUrl)
                }

                // 奖级明细
                if (draw.prizeLevels.isNotEmpty()) {
                    DetailSectionTitle("奖级明细")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                    ) {
                        draw.prizeLevels.forEachIndexed { idx, lv ->
                            if (idx > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(0.5.dp)
                                        .background(Color.White.copy(alpha = 0.06f))
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lv.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (idx == 0) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "${lv.count} 注",
                                        color = Color.White.copy(alpha = 0.48f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(84.dp)
                                    )
                                    Text(
                                        text = "¥${lv.amount}",
                                        color = if (idx == 0) accent else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.width(96.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 本期概况
                DetailSectionTitle("本期概况")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewDarkTile("本期销量", draw.salesMoney, Modifier.weight(1f))
                    OverviewDarkTile("奖池滚存", draw.poolMoney, Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "数据来源：${draw.type.sourceName}官方开奖公告" +
                        if (draw.isFallback) "（当前为离线缓存示例，返回首页点击刷新同步）" else "",
                    color = Color.White.copy(alpha = 0.34f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            }
        }
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.48f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp)
    )
}

@Composable
private fun OverviewDarkTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(16.dp)
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.48f), fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
