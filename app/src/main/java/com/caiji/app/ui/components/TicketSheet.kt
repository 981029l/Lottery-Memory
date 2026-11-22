package com.caiji.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caiji.app.model.DrawRecord
import com.caiji.app.model.LotteryRules
import com.caiji.app.model.UserTicket
import com.caiji.app.ui.motion.rubberBand
import com.caiji.app.ui.motion.springClickable
import com.caiji.app.ui.theme.AppleColors
import com.caiji.app.viewmodel.UpdateUiState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 支持阻尼橡皮筋回弹 + 手势滑动速度投射吸附的底部抽屉（我的票夹）
 */
@Composable
fun TicketSheet(
    sheetOffsetY: Animatable<Float, AnimationVector1D>,
    fullY: Float,
    halfY: Float,
    collapsedY: Float,
    tickets: List<UserTicket>,
    allDraws: List<DrawRecord>,
    autoNotifyEnabled: Boolean,
    updateState: UpdateUiState,
    onToggleAutoNotify: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDownloadInstallUpdate: () -> Unit,
    onToggleCnAccel: () -> Unit,
    onCycleMirror: () -> Unit,
    onDeleteTicket: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }

    val results = remember(tickets, allDraws) {
        tickets.map { it to LotteryRules.checkTicket(it, allDraws) }
    }
    val pendingCount = results.count { it.second.isPending }
    val wonCount = results.count { it.second.isWon }
    val totalCost = tickets.sumOf { it.cost }
    val totalPrize = results.sumOf { it.second.afterTaxAmount }

    fun animateToAnchor(target: Float, initialVelocity: Float = 0f) {
        scope.launch {
            sheetOffsetY.animateTo(
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 320f),
                initialVelocity = initialVelocity
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, sheetOffsetY.value.roundToInt()) }
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
            .background(Color(0xFFF6F6FA))
    ) {
        // 顶部把手区：支持橡皮筋拖动与速度投射吸附
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(fullY, halfY, collapsedY) {
                    var rawY = sheetOffsetY.value
                    detectVerticalDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            rawY = sheetOffsetY.value
                            scope.launch { sheetOffsetY.stop() }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            rawY += dragAmount
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val damped = rubberBand(rawY, fullY, collapsedY)
                            scope.launch { sheetOffsetY.snapTo(damped) }
                        },
                        onDragEnd = {
                            val vy = velocityTracker.calculateVelocity().y
                            val projected = sheetOffsetY.value + vy * 0.18f
                            val anchors = listOf(fullY, halfY, collapsedY)
                            val target = anchors.minByOrNull { abs(it - projected) } ?: collapsedY
                            animateToAnchor(target, vy)
                        },
                        onDragCancel = {
                            val anchors = listOf(fullY, halfY, collapsedY)
                            val target = anchors.minByOrNull { abs(it - sheetOffsetY.value) } ?: collapsedY
                            animateToAnchor(target)
                        }
                    )
                }
                .springClickable(pressedScale = 0.99f) {
                    val cur = sheetOffsetY.value
                    val next = when {
                        cur > halfY + 40f -> halfY
                        cur > fullY + 40f -> fullY
                        else -> collapsedY
                    }
                    animateToAnchor(next)
                }
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(38.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC7C7CC))
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "我的票夹",
                        color = AppleColors.InkPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$pendingCount 注待开奖 · $wonCount 注已中奖",
                        color = AppleColors.InkSecondary,
                        fontSize = 13.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White)
                        .springClickable(pressedScale = 0.95f, onClick = onToggleAutoNotify)
                        .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(18.dp)
                            .clip(CircleShape)
                            .background(if (autoNotifyEnabled) AppleColors.GreenSuccess else Color(0xFFD1D1D6))
                            .padding(2.dp),
                        contentAlignment = if (autoNotifyEnabled) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    Text(
                        text = "开奖提醒",
                        color = AppleColors.InkPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 内容区
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryTile("待开奖", "$pendingCount 注", AppleColors.InkPrimary, Modifier.weight(1f))
                    SummaryTile("累计投入", "¥$totalCost", AppleColors.InkPrimary, Modifier.weight(1f))
                    SummaryTile("累计到手", LotteryRules.formatCompactYuan(totalPrize), AppleColors.GreenText, Modifier.weight(1f))
                }
            }

            item {
                AppUpdateCard(
                    state = updateState,
                    onCheckUpdate = onCheckUpdate,
                    onDownloadInstall = onDownloadInstallUpdate,
                    onToggleCnAccel = onToggleCnAccel,
                    onCycleMirror = onCycleMirror
                )
            }

            item {
                Text(
                    text = "全部记录",
                    color = AppleColors.InkSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            if (results.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无购彩记录，点击底部「选号」机选或自选后即可存入票夹",
                            color = AppleColors.InkSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(results, key = { it.first.id }) { (ticket, check) ->
                    TicketItemCard(
                        ticket = ticket,
                        hitFront = check.hitFront,
                        hitBack = check.hitBack,
                        isPending = check.isPending,
                        prizeName = check.prizeName,
                        totalAmount = check.totalAmount,
                        afterTaxAmount = check.afterTaxAmount,
                        onDelete = { onDeleteTicket(ticket.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUpdateCard(
    state: UpdateUiState,
    onCheckUpdate: () -> Unit,
    onDownloadInstall: () -> Unit,
    onToggleCnAccel: () -> Unit,
    onCycleMirror: () -> Unit
) {
    val hasNew = state.latestRelease?.hasNewVersion == true
    val hasDownloaded = state.downloadedApk?.exists() == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (hasNew) AppleColors.GreenSuccess.copy(alpha = 0.14f)
                            else AppleColors.Background
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Download,
                        contentDescription = null,
                        tint = if (hasNew) AppleColors.GreenText else AppleColors.InkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "应用更新 · v${state.currentVersion}",
                            color = AppleColors.InkPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (hasNew) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AppleColors.GreenSuccess)
                            )
                        }
                    }
                    Text(
                        text = state.statusText,
                        color = AppleColors.InkSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            val actionLabel = when {
                state.isDownloading -> "${(state.downloadProgress * 100).roundToInt()}%"
                state.isChecking -> "检查中"
                hasDownloaded -> "立即安装"
                hasNew -> if (state.cnAccelEnabled) "加速下载" else "下载更新"
                else -> "检查更新"
            }
            val actionBg = when {
                hasDownloaded || hasNew -> AppleColors.DltBlue
                else -> AppleColors.Background
            }
            val actionFg = if (hasDownloaded || hasNew) Color.White else AppleColors.InkPrimary

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(actionBg)
                    .springClickable(
                        pressedScale = 0.94f,
                        enabled = !state.isChecking && !state.isDownloading
                    ) {
                        if (hasDownloaded || hasNew) onDownloadInstall() else onCheckUpdate()
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionLabel,
                    color = actionFg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (state.isDownloading) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { state.downloadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = AppleColors.DltBlue,
                trackColor = AppleColors.Background
            )
            if (state.downloadedText.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.downloadedText,
                    color = AppleColors.InkSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(AppleColors.Divider)
        )
        Spacer(Modifier.height(10.dp))

        // 中国地区下载加速开关 + 线路切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.springClickable(pressedScale = 0.96f, onClick = onToggleCnAccel),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Bolt,
                    contentDescription = null,
                    tint = if (state.cnAccelEnabled) AppleColors.GreenText else AppleColors.InkSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "中国地区下载加速",
                    color = AppleColors.InkPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(if (state.cnAccelEnabled) AppleColors.GreenSuccess else Color(0xFFD1D1D6))
                        .padding(2.dp),
                    contentAlignment = if (state.cnAccelEnabled) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppleColors.Background)
                    .springClickable(pressedScale = 0.95f, onClick = onCycleMirror)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "线路：${if (state.cnAccelEnabled) state.mirror.label else "直连"}",
                    color = AppleColors.InkSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        Text(text = label, color = AppleColors.InkSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(text = value, color = valueColor, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TicketItemCard(
    ticket: UserTicket,
    hitFront: Set<Int>,
    hitBack: Set<Int>,
    isPending: Boolean,
    prizeName: String?,
    totalAmount: Long,
    afterTaxAmount: Long,
    onDelete: () -> Unit
) {
    val accent = AppleColors.accent(ticket.type)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = ticket.type.displayName,
                    color = AppleColors.InkPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppleColors.Background)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = ticket.playMode.label,
                        color = AppleColors.InkSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "第 ${ticket.issue} 期",
                    color = AppleColors.InkSecondary,
                    fontSize = 12.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when {
                    isPending -> {
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFE9F2FF))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Clock,
                                contentDescription = null,
                                tint = AppleColors.DltBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Text("待开奖", color = AppleColors.DltBlue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    prizeName != null -> {
                        val amt = when {
                            totalAmount > 10_000L -> " · 税后约 ${LotteryRules.formatCompactYuan(afterTaxAmount)}"
                            totalAmount > 0L -> " · ¥$totalAmount"
                            else -> " · 浮动奖"
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AppleColors.GreenSuccess.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("$prizeName$amt", color = AppleColors.GreenText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AppleColors.Background)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("未中奖", color = AppleColors.InkSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Icon(
                    imageVector = AppIcons.Trash,
                    contentDescription = "删除记录",
                    tint = Color(0xFFC7C7CC),
                    modifier = Modifier
                        .size(18.dp)
                        .springClickable(pressedScale = 0.88f, onClick = onDelete)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (ticket.frontDan.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("胆", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                ticket.frontDan.forEach { n ->
                    val active = isPending || n in hitFront
                    LotteryBall(
                        number = n,
                        style = if (active) frontBallStyle(ticket.type) else BallStyle.MUTED_LIGHT,
                        size = 28.dp,
                        fontSize = 12.sp,
                        highlightedHit = !isPending && n in hitFront
                    )
                }
            }
            ticket.frontBalls.forEach { n ->
                val active = isPending || n in hitFront
                LotteryBall(
                    number = n,
                    style = if (active) frontBallStyle(ticket.type) else BallStyle.MUTED_LIGHT,
                    size = 28.dp,
                    fontSize = 12.sp,
                    highlightedHit = !isPending && n in hitFront
                )
            }
            if (ticket.backDan.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppleColors.DltBlue.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("后胆", color = AppleColors.DltBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                ticket.backDan.forEach { n ->
                    val active = isPending || n in hitBack
                    LotteryBall(
                        number = n,
                        style = if (active) backBallStyle(ticket.type) else BallStyle.MUTED_LIGHT,
                        size = 28.dp,
                        fontSize = 12.sp,
                        highlightedHit = !isPending && n in hitBack
                    )
                }
            }
            ticket.backBalls.forEach { n ->
                val active = isPending || n in hitBack
                LotteryBall(
                    number = n,
                    style = if (active) backBallStyle(ticket.type) else BallStyle.MUTED_LIGHT,
                    size = 28.dp,
                    fontSize = 12.sp,
                    highlightedHit = !isPending && n in hitBack
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        val extraLabel = if (ticket.extra) " · 追加" else ""
        val multLabel = if (ticket.multiplier > 1) " · ${ticket.multiplier}倍" else ""
        Text(
            text = "${ticket.betCount} 注$multLabel · ¥${ticket.cost}$extraLabel · ${ticket.source}" +
                if (isPending) " · 开奖后自动核对" else "",
            color = AppleColors.InkSecondary,
            fontSize = 12.sp
        )
    }
}
