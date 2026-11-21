package com.caiji.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caiji.app.model.DrawRecord
import com.caiji.app.ui.motion.tilt3D
import com.caiji.app.ui.theme.AppleColors

@Composable
fun HeroDrawCard(
    draw: DrawRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AppleColors.glowPalette(draw.type)
    val accent = AppleColors.accent(draw.type)

    val infinite = rememberInfiniteTransition(label = "heroGlow")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing)),
        label = "borderAngle"
    )
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(3600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "backlightBreath"
    )

    var specularPos by remember { mutableStateOf(Offset(0.5f, 0.2f)) }
    var specularAlpha by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .tilt3D(maxAngleDeg = 9f) { pos, alpha ->
                specularPos = pos
                specularAlpha = alpha
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // 底部动态模糊呼吸弥散背光
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.84f)
                .height(110.dp)
                .graphicsLayer {
                    translationY = (10f + 10f * breath) * density
                    scaleX = 0.90f + 0.14f * breath
                    scaleY = 0.88f + 0.12f * breath
                    alpha = 0.34f + 0.42f * breath
                }
                .blur(28.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(palette[0], palette[1], palette[2], palette[3])
                    )
                )
        )

        // 主卡片容器 + 旋转渐变扫光描边
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppleColors.CardDarkTop, AppleColors.CardDarkBottom),
                        start = Offset(0f, 0f),
                        end = Offset(600f, 900f)
                    )
                )
        ) {
            // 顶部右侧柔光氛围
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.05f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width * 0.92f, size.height * 0.05f),
                    radius = size.width * 0.85f
                )

                // 跟随手指的镜面高光
                if (specularAlpha > 0.01f) {
                    val c = Offset(size.width * specularPos.x, size.height * specularPos.y)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.16f * specularAlpha), Color.Transparent),
                            center = c,
                            radius = size.width * 0.65f
                        ),
                        center = c,
                        radius = size.width * 0.65f
                    )
                }

                // 旋转渐变扫光描边
                rotate(degrees = angle) {
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.14f to palette[0],
                            0.28f to palette[1],
                            0.42f to Color.Transparent,
                            0.55f to Color.Transparent,
                            0.70f to palette[2],
                            0.84f to palette[3],
                            1.0f to Color.Transparent
                        ),
                        cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            HeroDrawCardContent(draw = draw, showChevron = true)
        }
    }
}

@Composable
fun HeroDrawCardContent(
    draw: DrawRecord,
    showChevron: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = AppleColors.accent(draw.type)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = draw.type.displayName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "第 ${draw.issue} 期",
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 13.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = draw.date,
                    color = Color.White.copy(alpha = 0.48f),
                    fontSize = 12.sp
                )
                if (showChevron) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.48f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            draw.frontBalls.forEach { n ->
                LotteryBall(number = n, style = frontBallStyle(draw.type), size = 36.dp)
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(22.dp)
                    .background(Color.White.copy(alpha = 0.16f))
            )
            draw.backBalls.forEach { n ->
                LotteryBall(number = n, style = backBallStyle(draw.type), size = 36.dp)
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color.White.copy(alpha = 0.12f))
        )

        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            CardMetric(label = "奖池滚存", value = draw.poolMoney, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(34.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            CardMetric(
                label = "一等奖",
                value = if (draw.firstPrizeCount == "--") "--" else "${draw.firstPrizeCount} 注",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            )
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(34.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )
            CardMetric(
                label = "单注奖金",
                value = draw.firstPrizeAmount,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            )
        }
    }
}

@Composable
private fun CardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = Color.White.copy(alpha = 0.42f), fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
