package com.caiji.app.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 按钮按压 scale(0.96f) + 释放轻微超调回弹（Spring dampingRatio=0.35f）
 */
fun Modifier.springClickable(
    pressedScale: Float = 0.96f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = if (isPressed) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f)
        } else {
            spring(dampingRatio = 0.35f, stiffness = 420f)
        },
        label = "springPressScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * 卡片 3D 透视触摸跟随 + 松手 Spring 阻尼回正
 */
fun Modifier.tilt3D(
    maxAngleDeg: Float = 9f,
    onSpecularChange: (Offset, Float) -> Unit = { _, _ -> }
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val pressScale = remember { Animatable(1f) }
    var cardSize by remember { mutableStateOf(Offset(1f, 1f)) }

    this
        .pointerInput(maxAngleDeg) {
            cardSize = Offset(size.width.toFloat().coerceAtLeast(1f), size.height.toFloat().coerceAtLeast(1f))
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val w = size.width.toFloat().coerceAtLeast(1f)
                val h = size.height.toFloat().coerceAtLeast(1f)

                fun updateFor(pos: Offset) {
                    val px = (pos.x / w).coerceIn(0f, 1f)
                    val py = (pos.y / h).coerceIn(0f, 1f)
                    val targetRx = -(py - 0.5f) * 2f * maxAngleDeg
                    val targetRy = (px - 0.5f) * 2f * maxAngleDeg
                    onSpecularChange(Offset(px, py), 1f)
                    scope.launch {
                        rotX.animateTo(targetRx, spring(dampingRatio = 0.72f, stiffness = 340f))
                    }
                    scope.launch {
                        rotY.animateTo(targetRy, spring(dampingRatio = 0.72f, stiffness = 340f))
                    }
                }

                scope.launch {
                    pressScale.animateTo(0.975f, spring(dampingRatio = 0.9f, stiffness = 600f))
                }
                updateFor(down.position)

                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed) break
                    updateFor(change.position)
                }

                onSpecularChange(Offset(0.5f, 0.5f), 0f)
                scope.launch { rotX.animateTo(0f, spring(dampingRatio = 0.52f, stiffness = 170f)) }
                scope.launch { rotY.animateTo(0f, spring(dampingRatio = 0.52f, stiffness = 170f)) }
                scope.launch { pressScale.animateTo(1f, spring(dampingRatio = 0.36f, stiffness = 400f)) }
            }
        }
        .graphicsLayer {
            rotationX = rotX.value
            rotationY = rotY.value
            scaleX = pressScale.value
            scaleY = pressScale.value
            cameraDistance = 12f * density
        }
}

/**
 * 橡皮筋非线性阻尼：超出边界时阻力递增
 */
fun rubberBand(value: Float, minBound: Float, maxBound: Float, dimension: Float = 260f): Float {
    val c = 0.55f
    fun damp(delta: Float): Float = (1f - 1f / (abs(delta) * c / dimension + 1f)) * dimension
    return when {
        value < minBound -> minBound - damp(minBound - value)
        value > maxBound -> maxBound + damp(value - maxBound)
        else -> value
    }
}
