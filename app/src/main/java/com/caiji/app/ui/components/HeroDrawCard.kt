package com.caiji.app.ui.components

import android.graphics.Matrix
import android.graphics.SweepGradient
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
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
        // 底部动态模糊呼吸弥散背光（Unbounded 避免矩形硬边裁切）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.78f)
                .height(64.dp)
                .graphicsLayer {
                    translationY = (6f + 6f * breath) * density
                    scaleX = 0.92f + 0.10f * breath
                    scaleY = 0.90f + 0.12f * breath
                    alpha = 0.28f + 0.34f * breath
                }
                .blur(radius = 28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(palette[0], palette[1], palette[2], palette[3])
                    ),
                    shape = RoundedCornerShape(32.dp)
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

                // 旋转渐变扫光描边（固定圆角矩形，仅旋转 SweepGradient Shader）
                val sweepColors = intArrayOf(
                    Color.Transparent.toArgb(),
                    palette[0].toArgb(),
                    palette[1].toArgb(),
                    Color.Transparent.toArgb(),
                    Color.Transparent.toArgb(),
                    palette[2].toArgb(),
                    palette[3].toArgb(),
                    Color.Transparent.toArgb()
                )
                val sweepStops = floatArrayOf(0f, 0.14f, 0.28f, 0.42f, 0.55f, 0.70f, 0.84f, 1f)
                val shader = SweepGradient(size.width / 2f, size.height / 2f, sweepColors, sweepStops)
                val matrix = Matrix().apply { setRotate(angle, size.width / 2f, size.height / 2f) }
                shader.setLocalMatrix(matrix)

                val strokePx = 2.dp.toPx()
                val halfStroke = strokePx / 2f
                drawRoundRect(
                    brush = ShaderBrush(shader),
                    topLeft = Offset(halfStroke, halfStroke),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - strokePx,
                        size.height - strokePx
                    ),
                    cornerRadius = CornerRadius(27.dp.toPx(), 27.dp.toPx()),
                    style = Stroke(width = strokePx)
                )
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
