package com.caiji.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caiji.app.model.LotteryRules
import com.caiji.app.model.LotteryType
import com.caiji.app.model.PlayMode
import com.caiji.app.ui.motion.springClickable
import com.caiji.app.ui.theme.AppleColors

enum class PickerTab(val label: String) {
    AUTO("机选"),
    SINGLE("单式"),
    MULTIPLE("复式"),
    DANTUO("胆拖")
}

data class PickerSubmission(
    val picks: List<Pair<List<Int>, List<Int>>>,
    val playMode: PlayMode,
    val frontDan: List<Int> = emptyList(),
    val backDan: List<Int> = emptyList(),
    val multiplier: Int = 1,
    val extra: Boolean = false,
    val source: String = "机选"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MorphPickerPanel(
    expanded: Boolean,
    lotteryType: LotteryType,
    nextIssue: String,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSaveTickets: (PickerSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = AppleColors.accent(lotteryType)
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val targetExpandedWidth = (screenWidthDp - 32.dp).coerceAtMost(396.dp)

    var activeTab by remember { mutableStateOf(PickerTab.AUTO) }
    var autoCount by remember { mutableIntStateOf(3) }
    var multiplier by remember { mutableIntStateOf(1) }
    var dltExtra by remember { mutableStateOf(false) }

    var autoPicks by remember(lotteryType, autoCount) {
        mutableStateOf(List(autoCount) { LotteryRules.generateRandom(lotteryType) })
    }
    var selFront by remember(lotteryType) { mutableStateOf(emptyList<Int>()) }
    var selBack by remember(lotteryType) { mutableStateOf(emptyList<Int>()) }
    var selFrontDan by remember(lotteryType) { mutableStateOf(emptyList<Int>()) }
    var selBackDan by remember(lotteryType) { mutableStateOf(emptyList<Int>()) }

    LaunchedEffect(lotteryType) {
        autoPicks = List(autoCount) { LotteryRules.generateRandom(lotteryType) }
        selFront = emptyList()
        selBack = emptyList()
        selFrontDan = emptyList()
        selBackDan = emptyList()
    }

    LaunchedEffect(activeTab) {
        // 切换回单式时若超出单式上限则自动裁剪
        if (activeTab == PickerTab.SINGLE) {
            if (selFront.size > lotteryType.frontCount) selFront = selFront.take(lotteryType.frontCount)
            if (selBack.size > lotteryType.backCount) selBack = selBack.take(lotteryType.backCount)
        }
    }

    val unitPrice = if (lotteryType == LotteryType.DLT && dltExtra) 3 else 2
    val currentPlayMode = when (activeTab) {
        PickerTab.AUTO, PickerTab.SINGLE -> PlayMode.SINGLE
        PickerTab.MULTIPLE -> PlayMode.MULTIPLE
        PickerTab.DANTUO -> PlayMode.DANTUO
    }
    val betCount = when (activeTab) {
        PickerTab.AUTO -> autoPicks.size
        else -> LotteryRules.calcBetCount(
            type = lotteryType,
            mode = currentPlayMode,
            frontCount = selFront.size,
            backCount = selBack.size,
            frontDanCount = selFrontDan.size,
            backDanCount = selBackDan.size
        )
    }
    val totalCost = betCount * unitPrice * multiplier

    // 高阻尼流体曲线（Fluid Morph）
    val morphSpecDp = spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.90f, stiffness = 260f)
    val morphSpecFloat = spring<Float>(dampingRatio = 0.90f, stiffness = 260f)

    val containerWidth by animateDpAsState(
        targetValue = if (expanded) targetExpandedWidth else 150.dp,
        animationSpec = morphSpecDp,
        label = "morphWidth"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 34.dp else 26.dp,
        animationSpec = morphSpecDp,
        label = "morphCorner"
    )
    val panelAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = morphSpecFloat,
        label = "morphAlpha"
    )
    val panelScale by animateFloatAsState(
        targetValue = if (expanded) 1f else 0.92f,
        animationSpec = morphSpecFloat,
        label = "morphScale"
    )

    Box(
        modifier = modifier
            .width(containerWidth)
            .wrapContentHeight()
            .shadow(
                elevation = if (expanded) 28.dp else 14.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xF518181A))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !expanded,
                onClick = onExpand
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = if (expanded) 0.22f else 0.14f), Color.Transparent)
                    )
                )
        )

        if (!expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .springClickable(pressedScale = 0.95f, onClick = onExpand),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "选号",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .graphicsLayer {
                        alpha = panelAlpha
                        scaleX = panelScale
                        scaleY = panelScale
                    }
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "选号 · ${activeTab.label}",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${lotteryType.displayName} · 第 $nextIssue 期 · 单注 ¥$unitPrice",
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .springClickable(pressedScale = 0.92f, onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 玩法分段：机选 / 单式 / 复式 / 胆拖
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(3.dp)
                ) {
                    PickerTab.entries.forEach { tab ->
                        val selected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                                .springClickable(pressedScale = 0.95f) { activeTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.52f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 中间可滚动选号内容区
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "pickerTabContent"
                    ) { tab ->
                        when (tab) {
                            PickerTab.AUTO -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // 机选注数快捷切换：1注 / 3注 / 5注
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(1, 3, 5).forEach { cnt ->
                                            val on = autoCount == cnt
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (on) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.06f)
                                                    )
                                                    .border(
                                                        width = if (on) 1.dp else 0.dp,
                                                        color = if (on) accent else Color.Transparent,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .springClickable(pressedScale = 0.95f) {
                                                        autoCount = cnt
                                                        autoPicks = List(cnt) { LotteryRules.generateRandom(lotteryType) }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "机选 $cnt 注",
                                                    color = if (on) Color.White else Color.White.copy(alpha = 0.62f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    autoPicks.forEachIndexed { idx, (front, back) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.06f))
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                color = Color.White.copy(alpha = 0.38f),
                                                fontSize = 12.sp,
                                                modifier = Modifier.width(16.dp)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                front.forEach { n ->
                                                    LotteryBall(n, frontBallStyle(lotteryType), size = 27.dp, fontSize = 11.sp)
                                                }
                                                Spacer(Modifier.width(2.dp))
                                                back.forEach { n ->
                                                    LotteryBall(n, backBallStyle(lotteryType), size = 27.dp, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            PickerTab.SINGLE, PickerTab.MULTIPLE -> {
                                val isMultiple = tab == PickerTab.MULTIPLE
                                val maxFront = if (isMultiple) minOf(16, lotteryType.frontMax) else lotteryType.frontCount
                                val maxBack = if (isMultiple) minOf(8, lotteryType.backMax) else lotteryType.backCount
                                Column {
                                    BallZoneHeader(
                                        title = if (lotteryType == LotteryType.SSQ) "红球" else "前区",
                                        hint = if (isMultiple) "选 ${lotteryType.frontCount}–$maxFront 个" else "选 ${lotteryType.frontCount} 个",
                                        countText = "${selFront.size}/${if (isMultiple) maxFront else lotteryType.frontCount}",
                                        countColor = accent
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        (1..lotteryType.frontMax).forEach { n ->
                                            val on = n in selFront
                                            LotteryBall(
                                                number = n,
                                                style = if (on) frontBallStyle(lotteryType) else BallStyle.MUTED_DARK,
                                                size = 28.dp,
                                                fontSize = 11.sp,
                                                modifier = Modifier.springClickable(pressedScale = 0.88f) {
                                                    selFront = if (on) {
                                                        selFront - n
                                                    } else if (selFront.size < maxFront) {
                                                        (selFront + n).sorted()
                                                    } else if (!isMultiple) {
                                                        (selFront.drop(1) + n).sorted()
                                                    } else {
                                                        selFront
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    BallZoneHeader(
                                        title = if (lotteryType == LotteryType.SSQ) "蓝球" else "后区",
                                        hint = if (isMultiple) "选 ${lotteryType.backCount}–$maxBack 个" else "选 ${lotteryType.backCount} 个",
                                        countText = "${selBack.size}/${if (isMultiple) maxBack else lotteryType.backCount}",
                                        countColor = Color.White
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        (1..lotteryType.backMax).forEach { n ->
                                            val on = n in selBack
                                            LotteryBall(
                                                number = n,
                                                style = if (on) backBallStyle(lotteryType) else BallStyle.MUTED_DARK,
                                                size = 28.dp,
                                                fontSize = 11.sp,
                                                modifier = Modifier.springClickable(pressedScale = 0.88f) {
                                                    selBack = if (on) {
                                                        selBack - n
                                                    } else if (selBack.size < maxBack) {
                                                        (selBack + n).sorted()
                                                    } else if (!isMultiple) {
                                                        (selBack.drop(1) + n).sorted()
                                                    } else {
                                                        selBack
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            PickerTab.DANTUO -> {
                                val maxFrontDan = lotteryType.frontCount - 1
                                val maxBackDan = lotteryType.backCount - 1
                                Column {
                                    BallZoneHeader(
                                        title = if (lotteryType == LotteryType.SSQ) "红球胆码" else "前区胆码",
                                        hint = "必出号码，选 1–$maxFrontDan 个",
                                        countText = "${selFrontDan.size}/$maxFrontDan",
                                        countColor = accent
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        (1..lotteryType.frontMax).forEach { n ->
                                            val on = n in selFrontDan
                                            LotteryBall(
                                                number = n,
                                                style = if (on) frontBallStyle(lotteryType) else BallStyle.MUTED_DARK,
                                                size = 27.dp,
                                                fontSize = 11.sp,
                                                highlightedHit = on,
                                                modifier = Modifier.springClickable(pressedScale = 0.88f) {
                                                    if (on) {
                                                        selFrontDan = selFrontDan - n
                                                    } else if (selFrontDan.size < maxFrontDan) {
                                                        selFrontDan = (selFrontDan + n).sorted()
                                                        selFront = selFront - n
                                                    }
                                                }
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    val needTuo = (lotteryType.frontCount - selFrontDan.size).coerceAtLeast(1)
                                    BallZoneHeader(
                                        title = if (lotteryType == LotteryType.SSQ) "红球拖码" else "前区拖码",
                                        hint = "至少选 $needTuo 个（与胆码互斥）",
                                        countText = "${selFront.size} 个",
                                        countColor = accent
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        (1..lotteryType.frontMax).forEach { n ->
                                            val isDan = n in selFrontDan
                                            val on = n in selFront
                                            LotteryBall(
                                                number = n,
                                                style = if (on) frontBallStyle(lotteryType) else BallStyle.MUTED_DARK,
                                                size = 27.dp,
                                                fontSize = 11.sp,
                                                modifier = Modifier
                                                    .graphicsLayer { alpha = if (isDan) 0.28f else 1f }
                                                    .springClickable(pressedScale = 0.88f, enabled = !isDan) {
                                                        selFront = if (on) selFront - n else (selFront + n).sorted()
                                                    }
                                            )
                                        }
                                    }

                                    if (maxBackDan > 0) {
                                        Spacer(Modifier.height(12.dp))
                                        BallZoneHeader(
                                            title = "后区胆码（可选）",
                                            hint = "最多选 $maxBackDan 个",
                                            countText = "${selBackDan.size}/$maxBackDan",
                                            countColor = Color.White
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            (1..lotteryType.backMax).forEach { n ->
                                                val on = n in selBackDan
                                                LotteryBall(
                                                    number = n,
                                                    style = if (on) backBallStyle(lotteryType) else BallStyle.MUTED_DARK,
                                                    size = 27.dp,
                                                    fontSize = 11.sp,
                                                    highlightedHit = on,
                                                    modifier = Modifier.springClickable(pressedScale = 0.88f) {
                                                        if (on) {
                                                            selBackDan = selBackDan - n
                                                        } else {
                                                            selBackDan = listOf(n)
                                                            selBack = selBack - n
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    val needBack = (lotteryType.backCount - selBackDan.size).coerceAtLeast(1)
                                    BallZoneHeader(
                                        title = if (lotteryType == LotteryType.SSQ) "蓝球" else "后区拖码",
                                        hint = "至少选 $needBack 个",
                                        countText = "${selBack.size} 个",
                                        countColor = Color.White
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        (1..lotteryType.backMax).forEach { n ->
                                            val isDan = n in selBackDan
                                            val on = n in selBack
                                            LotteryBall(
                                                number = n,
                                                style = if (on) backBallStyle(lotteryType) else BallStyle.MUTED_DARK,
                                                size = 27.dp,
                                                fontSize = 11.sp,
                                                modifier = Modifier
                                                    .graphicsLayer { alpha = if (isDan) 0.28f else 1f }
                                                    .springClickable(pressedScale = 0.88f, enabled = !isDan) {
                                                        selBack = if (on) selBack - n else (selBack + n).sorted()
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 倍数 + 大乐透追加栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "倍数", color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .springClickable(pressedScale = 0.90f) {
                                    if (multiplier > 1) multiplier--
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "减倍", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = "${multiplier}倍",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .springClickable(pressedScale = 0.90f) {
                                    if (multiplier < 99) multiplier++
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "加倍", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (lotteryType == LotteryType.DLT) {
                        Row(
                            modifier = Modifier.springClickable(pressedScale = 0.95f) { dltExtra = !dltExtra },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "追加", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(18.dp)
                                    .clip(CircleShape)
                                    .background(if (dltExtra) AppleColors.GreenSuccess else Color.White.copy(alpha = 0.18f))
                                    .padding(2.dp),
                                contentAlignment = if (dltExtra) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(Color.White))
                            }
                        }
                    } else {
                        Text(
                            text = if (betCount > 0) "共 $betCount 注 · ¥$totalCost" else "未成注",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 底部操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .springClickable(pressedScale = 0.96f) {
                                when (activeTab) {
                                    PickerTab.AUTO -> {
                                        autoPicks = List(autoCount) { LotteryRules.generateRandom(lotteryType) }
                                    }
                                    PickerTab.SINGLE, PickerTab.MULTIPLE -> {
                                        val (rf, rb) = LotteryRules.generateRandom(lotteryType)
                                        val needF = if (activeTab == PickerTab.MULTIPLE) lotteryType.frontCount + 1 else lotteryType.frontCount
                                        val extraF = (1..lotteryType.frontMax).shuffled()
                                        selFront = (selFront + rf + extraF).distinct().take(needF).sorted()
                                        selBack = (selBack + rb).distinct().take(lotteryType.backCount).sorted()
                                    }
                                    PickerTab.DANTUO -> {
                                        selFrontDan = emptyList()
                                        selFront = emptyList()
                                        selBackDan = emptyList()
                                        selBack = emptyList()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (activeTab == PickerTab.AUTO) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = when (activeTab) {
                                    PickerTab.AUTO -> "换一批"
                                    PickerTab.DANTUO -> "清空"
                                    else -> "随机补全"
                                },
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    val canSave = betCount > 0
                    Box(
                        modifier = Modifier
                            .weight(1.45f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(if (canSave) accent else accent.copy(alpha = 0.35f))
                            .springClickable(pressedScale = 0.96f, enabled = canSave) {
                                when (activeTab) {
                                    PickerTab.AUTO -> {
                                        onSaveTickets(
                                            PickerSubmission(
                                                picks = autoPicks,
                                                playMode = PlayMode.SINGLE,
                                                multiplier = multiplier,
                                                extra = dltExtra,
                                                source = "机选"
                                            )
                                        )
                                        autoPicks = List(autoCount) { LotteryRules.generateRandom(lotteryType) }
                                    }
                                    PickerTab.SINGLE, PickerTab.MULTIPLE -> {
                                        onSaveTickets(
                                            PickerSubmission(
                                                picks = listOf(selFront to selBack),
                                                playMode = currentPlayMode,
                                                multiplier = multiplier,
                                                extra = dltExtra,
                                                source = activeTab.label
                                            )
                                        )
                                        selFront = emptyList()
                                        selBack = emptyList()
                                    }
                                    PickerTab.DANTUO -> {
                                        onSaveTickets(
                                            PickerSubmission(
                                                picks = listOf(selFront to selBack),
                                                playMode = PlayMode.DANTUO,
                                                frontDan = selFrontDan,
                                                backDan = selBackDan,
                                                multiplier = multiplier,
                                                extra = dltExtra,
                                                source = "胆拖"
                                            )
                                        )
                                        selFrontDan = emptyList()
                                        selFront = emptyList()
                                        selBackDan = emptyList()
                                        selBack = emptyList()
                                    }
                                }
                                multiplier = 1
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (canSave) "记入票夹 · ¥$totalCost" else "请选满号码",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BallZoneHeader(
    title: String,
    hint: String,
    countText: String,
    countColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(text = hint, color = Color.White.copy(alpha = 0.42f), fontSize = 11.sp)
        }
        Text(text = countText, color = countColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
