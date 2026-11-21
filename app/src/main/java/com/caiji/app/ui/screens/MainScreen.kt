package com.caiji.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caiji.app.model.DrawRecord
import com.caiji.app.model.LotteryRules
import com.caiji.app.model.LotteryType
import com.caiji.app.ui.components.DynamicToast
import com.caiji.app.ui.components.HeroDrawCard
import com.caiji.app.ui.components.LotteryBall
import com.caiji.app.ui.components.MorphPickerPanel
import com.caiji.app.ui.components.TicketSheet
import com.caiji.app.ui.components.backBallStyle
import com.caiji.app.ui.components.frontBallStyle
import com.caiji.app.ui.motion.springClickable
import com.caiji.app.ui.motion.tilt3D
import com.caiji.app.ui.theme.AppleColors
import com.caiji.app.viewmodel.LotteryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(viewModel: LotteryViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var activeDetailDraw by remember { mutableStateOf<DrawRecord?>(null) }
    var morphExpanded by remember { mutableStateOf(false) }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalDateTime.now()
        }
    }

    val latestDraw = uiState.latestDraw
    val countdown = remember(uiState.selectedType, latestDraw.issue, now) {
        LotteryRules.computeCountdown(uiState.selectedType, latestDraw.issue, now)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = LocalDensity.current
        val fullHeightPx = with(density) { maxHeight.toPx() }
        val fullY = with(density) { 56.dp.toPx() }
        val halfY = fullHeightPx * 0.46f
        val collapsedY = (fullHeightPx - with(density) { 104.dp.toPx() }).coerceAtLeast(halfY + 120f)

        val sheetOffsetY = remember(collapsedY) { Animatable(collapsedY) }

        val sheetProgress = ((halfY - sheetOffsetY.value) / (halfY - fullY)).coerceIn(0f, 1f)
        val detailScale by animateFloatAsState(
            targetValue = if (activeDetailDraw != null) 0.92f else 1f,
            animationSpec = spring(dampingRatio = 0.88f, stiffness = 240f),
            label = "bgDetailScale"
        )
        val bgScale = detailScale - 0.06f * sheetProgress

        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = activeDetailDraw,
                transitionSpec = {
                    fadeIn(spring(stiffness = 280f)) togetherWith fadeOut(spring(stiffness = 280f))
                },
                label = "detailTransition"
            ) { detailTarget ->
                if (detailTarget == null) {
                    val animScope = this@AnimatedContent
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 主内容层（随抽屉拉起与详情展开做连续平滑缩放）
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = bgScale
                                    scaleY = bgScale
                                    clip = true
                                    shape = RoundedCornerShape(if (bgScale < 0.995f) 32.dp else 0.dp)
                                }
                                .background(AppleColors.Background)
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(Modifier.height(10.dp))

                            // 顶部日期 + 标题 + 刷新与铃铛
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = formatTodayChinese(),
                                        color = AppleColors.InkSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "开奖",
                                        color = AppleColors.InkPrimary,
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HeaderCircleButton(
                                        onClick = { viewModel.refreshAll(silent = false) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "刷新",
                                            tint = AppleColors.InkPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    HeaderCircleButton(
                                        onClick = { viewModel.triggerCheckNotificationPreview() }
                                    ) {
                                        Box {
                                            Icon(
                                                imageVector = Icons.Rounded.Notifications,
                                                contentDescription = "通知预览",
                                                tint = AppleColors.InkPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(AppleColors.accent(uiState.selectedType))
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // 双色球 / 大乐透 苹果分段控件
                            SegmentedLotteryControl(
                                selected = uiState.selectedType,
                                onSelect = { viewModel.selectLottery(it) }
                            )

                            Spacer(Modifier.height(18.dp))

                            // 主开奖卡片（共享元素起点 + 3D 透视 + 旋转渐变描边 + 呼吸背光）
                            HeroDrawCard(
                                draw = latestDraw,
                                onClick = {
                                    morphExpanded = false
                                    activeDetailDraw = latestDraw
                                },
                                modifier = Modifier.sharedBounds(
                                    sharedContentState = rememberSharedContentState(
                                        key = "draw-card-${latestDraw.type.name}-${latestDraw.issue}"
                                    ),
                                    animatedVisibilityScope = animScope
                                )
                            )

                            Spacer(Modifier.height(18.dp))

                            // 倒计时卡片
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .springClickable(pressedScale = 0.98f) {
                                        morphExpanded = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AppleColors.accent(uiState.selectedType).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccessTime,
                                            contentDescription = null,
                                            tint = AppleColors.accent(uiState.selectedType),
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = countdown.nextIssueHint,
                                            color = AppleColors.InkPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = countdown.subtitle,
                                            color = AppleColors.InkSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Text(
                                    text = countdown.formattedRemaining,
                                    color = AppleColors.accent(uiState.selectedType),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            // 一等奖出奖地卡片（带轻度 3D 透视）
                            RegionCard(
                                draw = latestDraw,
                                onClick = {
                                    morphExpanded = false
                                    activeDetailDraw = latestDraw
                                }
                            )

                            // 往期开奖列表
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "往期开奖",
                                    color = AppleColors.InkPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "共 ${uiState.currentDraws.size} 期",
                                    color = AppleColors.InkSecondary,
                                    fontSize = 13.sp
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                            ) {
                                uiState.currentDraws.drop(1).take(12).forEachIndexed { idx, item ->
                                    if (idx > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(0.5.dp)
                                                .background(AppleColors.Divider)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .springClickable(pressedScale = 0.98f) {
                                                morphExpanded = false
                                                activeDetailDraw = item
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "第 ${item.issue} 期",
                                                color = AppleColors.InkPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${item.date} · 一等奖 ${item.firstPrizeCount} 注",
                                                color = AppleColors.InkSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            item.frontBalls.forEach { n ->
                                                LotteryBall(n, frontBallStyle(item.type), size = 22.dp, fontSize = 10.sp)
                                            }
                                            item.backBalls.forEach { n ->
                                                LotteryBall(n, backBallStyle(item.type), size = 22.dp, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(210.dp))
                        }

                        // 抽屉拉起时的背景压暗层
                        if (sheetProgress > 0.02f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.30f * sheetProgress))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        scope.launch {
                                            sheetOffsetY.animateTo(
                                                collapsedY,
                                                spring(dampingRatio = 0.86f, stiffness = 320f)
                                            )
                                        }
                                    }
                            )
                        }

                        // 底部橡皮筋抽屉：我的票夹
                        TicketSheet(
                            sheetOffsetY = sheetOffsetY,
                            fullY = fullY,
                            halfY = halfY,
                            collapsedY = collapsedY,
                            tickets = uiState.tickets,
                            allDraws = uiState.allDraws,
                            autoNotifyEnabled = uiState.autoNotifyEnabled,
                            updateState = uiState.update,
                            onToggleAutoNotify = { viewModel.toggleAutoNotify() },
                            onCheckUpdate = { viewModel.checkForUpdate(silent = false) },
                            onDownloadInstallUpdate = { viewModel.downloadAndInstallUpdate() },
                            onToggleCnAccel = { viewModel.toggleCnAccel() },
                            onCycleMirror = { viewModel.cycleDownloadMirror() },
                            onDeleteTicket = { viewModel.deleteTicket(it) }
                        )

                        // 选号面板展开遮罩
                        if (morphExpanded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.42f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { morphExpanded = false }
                            )
                        }

                        // 底部胶囊 → 选号面板 Fluid Morph
                        val capsuleVisible = morphExpanded || sheetOffsetY.value > halfY + 80f
                        if (capsuleVisible) {
                            val capsuleBottomOffset = (fullHeightPx - sheetOffsetY.value).coerceAtLeast(0f) +
                                with(density) { 12.dp.toPx() }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .offset { IntOffset(0, -capsuleBottomOffset.roundToInt()) }
                            ) {
                                MorphPickerPanel(
                                    expanded = morphExpanded,
                                    lotteryType = uiState.selectedType,
                                    nextIssue = LotteryRules.nextIssueNumber(latestDraw.issue),
                                    onExpand = {
                                        scope.launch {
                                            sheetOffsetY.animateTo(
                                                collapsedY,
                                                spring(dampingRatio = 0.86f, stiffness = 320f)
                                            )
                                        }
                                        morphExpanded = true
                                    },
                                    onDismiss = { morphExpanded = false },
                                    onSaveTickets = { sub ->
                                        viewModel.addTickets(
                                            type = uiState.selectedType,
                                            picks = sub.picks,
                                            playMode = sub.playMode,
                                            frontDan = sub.frontDan,
                                            backDan = sub.backDan,
                                            multiplier = sub.multiplier,
                                            extra = sub.extra,
                                            source = sub.source
                                        )
                                        morphExpanded = false
                                        scope.launch {
                                            delay(260)
                                            sheetOffsetY.animateTo(
                                                halfY,
                                                spring(dampingRatio = 0.86f, stiffness = 300f)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    DrawDetailScreen(
                        draw = detailTarget,
                        myTicketsForIssue = uiState.tickets.filter {
                            it.type == detailTarget.type && it.issue == detailTarget.issue
                        },
                        allDraws = uiState.allDraws,
                        animatedVisibilityScope = this@AnimatedContent,
                        onClose = { activeDetailDraw = null }
                    )
                }
            }
        }

        // 顶部胶囊通知
        DynamicToast(
            state = uiState.toast,
            lotteryType = uiState.selectedType,
            onDismiss = {
                val shouldOpenSheet = uiState.toast.openSheetOnTap
                viewModel.dismissToast()
                if (shouldOpenSheet && activeDetailDraw == null) {
                    morphExpanded = false
                    scope.launch {
                        sheetOffsetY.animateTo(
                            halfY,
                            spring(dampingRatio = 0.86f, stiffness = 300f)
                        )
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun HeaderCircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.08f))
            .clip(CircleShape)
            .background(Color.White)
            .springClickable(pressedScale = 0.94f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SegmentedLotteryControl(
    selected: LotteryType,
    onSelect: (LotteryType) -> Unit
) {
    val thumbBias by animateFloatAsState(
        targetValue = if (selected == LotteryType.SSQ) 0f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 380f),
        label = "segThumb"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1F767680))
            .padding(2.dp)
    ) {
        val halfWidth = maxWidth / 2
        val halfWidthPx = with(LocalDensity.current) { halfWidth.toPx() }
        Box(
            modifier = Modifier
                .width(halfWidth)
                .height(32.dp)
                .offset { IntOffset((thumbBias * halfWidthPx).roundToInt(), 0) }
                .shadow(3.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black.copy(alpha = 0.14f))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            LotteryType.entries.forEach { type ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.displayName,
                        color = AppleColors.InkPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionCard(
    draw: DrawRecord,
    onClick: () -> Unit
) {
    val accent = AppleColors.accent(draw.type)
    val palette = AppleColors.glowPalette(draw.type)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tilt3D(maxAngleDeg = 5f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Place,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "一等奖出奖地",
                    color = AppleColors.InkPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "第 ${draw.issue} 期 · 共 ${draw.firstPrizeCount} 注",
                color = AppleColors.InkSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        if (draw.regions.isNotEmpty()) {
            val maxCount = draw.regions.maxOf { it.count }.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                draw.regions.take(5).forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = r.province,
                            color = Color(0xFF3A3A3C),
                            fontSize = 13.sp,
                            modifier = Modifier.width(42.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(AppleColors.Background)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(r.count.toFloat() / maxCount.toFloat())
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Brush.horizontalGradient(listOf(palette[0], palette[1])))
                            )
                        }
                        Text(
                            text = "${r.count} 注",
                            color = AppleColors.InkPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(42.dp)
                        )
                    }
                }
            }
        } else if (draw.regionRaw.isNotBlank()) {
            Text(
                text = draw.regionRaw,
                color = AppleColors.InkSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        } else {
            Text(
                text = "点击查看本期奖级与开奖详情",
                color = AppleColors.InkSecondary,
                fontSize = 13.sp
            )
        }
    }
}

private fun formatTodayChinese(): String {
    val today = LocalDate.now()
    val week = when (today.dayOfWeek) {
        DayOfWeek.MONDAY -> "一"
        DayOfWeek.TUESDAY -> "二"
        DayOfWeek.WEDNESDAY -> "三"
        DayOfWeek.THURSDAY -> "四"
        DayOfWeek.FRIDAY -> "五"
        DayOfWeek.SATURDAY -> "六"
        DayOfWeek.SUNDAY -> "日"
    }
    return "${today.monthValue}月${today.dayOfMonth}日 星期$week"
}
