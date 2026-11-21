package com.caiji.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caiji.app.model.LotteryType
import com.caiji.app.ui.theme.AppleColors

enum class BallStyle {
    SSQ_RED,
    SSQ_BLUE,
    DLT_BLUE,
    DLT_ORANGE,
    MUTED_LIGHT,
    MUTED_DARK
}

fun frontBallStyle(type: LotteryType): BallStyle =
    if (type == LotteryType.SSQ) BallStyle.SSQ_RED else BallStyle.DLT_BLUE

fun backBallStyle(type: LotteryType): BallStyle =
    if (type == LotteryType.SSQ) BallStyle.SSQ_BLUE else BallStyle.DLT_ORANGE

@Composable
fun LotteryBall(
    number: Int,
    style: BallStyle,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    fontSize: TextUnit = 14.sp,
    highlightedHit: Boolean = false
) {
    val brush: Brush
    val textColor: Color
    val shadowColor: Color

    when (style) {
        BallStyle.SSQ_RED -> {
            brush = Brush.verticalGradient(listOf(AppleColors.SsqRedTop, AppleColors.SsqRedBottom))
            textColor = Color.White
            shadowColor = AppleColors.SsqRed.copy(alpha = 0.45f)
        }
        BallStyle.SSQ_BLUE, BallStyle.DLT_BLUE -> {
            brush = Brush.verticalGradient(listOf(AppleColors.SsqBlueTop, AppleColors.SsqBlueBottom))
            textColor = Color.White
            shadowColor = AppleColors.DltBlue.copy(alpha = 0.45f)
        }
        BallStyle.DLT_ORANGE -> {
            brush = Brush.verticalGradient(listOf(AppleColors.DltOrangeTop, AppleColors.DltOrangeBottom))
            textColor = Color.White
            shadowColor = Color(0xFFFF9500).copy(alpha = 0.45f)
        }
        BallStyle.MUTED_LIGHT -> {
            brush = Brush.verticalGradient(listOf(Color(0xFFE9E9EE), Color(0xFFE2E2E8)))
            textColor = Color(0xFF9A9AA2)
            shadowColor = Color.Transparent
        }
        BallStyle.MUTED_DARK -> {
            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.06f)))
            textColor = Color.White.copy(alpha = 0.72f)
            shadowColor = Color.Transparent
        }
    }

    val ringModifier = if (highlightedHit) {
        Modifier.border(2.dp, AppleColors.GreenSuccess, CircleShape)
    } else if (style == BallStyle.MUTED_DARK) {
        Modifier.border(0.5.dp, Color.White.copy(alpha = 0.10f), CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (shadowColor != Color.Transparent) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                } else Modifier
            )
            .clip(CircleShape)
            .background(brush)
            .then(ringModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = String.format("%02d", number),
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp
        )
    }
}
