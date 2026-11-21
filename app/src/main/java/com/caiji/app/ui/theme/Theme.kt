package com.caiji.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.caiji.app.model.LotteryType

object AppleColors {
    val Background = Color(0xFFF2F2F7)
    val CardWhite = Color(0xFFFFFFFF)
    val CardDarkTop = Color(0xFF252528)
    val CardDarkBottom = Color(0xFF0B0B0C)
    val InkPrimary = Color(0xFF1C1C1E)
    val InkSecondary = Color(0xFF8E8E93)
    val Divider = Color(0xFFE5E5EA)
    val GreenSuccess = Color(0xFF30D158)
    val GreenText = Color(0xFF248A3D)

    val SsqRed = Color(0xFFFF3B30)
    val SsqRedTop = Color(0xFFFF6259)
    val SsqRedBottom = Color(0xFFE5302A)
    val SsqBlueTop = Color(0xFF4AA3FF)
    val SsqBlueBottom = Color(0xFF0A6CFF)

    val DltBlue = Color(0xFF0A84FF)
    val DltOrangeTop = Color(0xFFFFBD4A)
    val DltOrangeBottom = Color(0xFFFF8A00)

    fun accent(type: LotteryType): Color =
        if (type == LotteryType.SSQ) SsqRed else DltBlue

    fun glowPalette(type: LotteryType): List<Color> =
        if (type == LotteryType.SSQ) {
            listOf(Color(0xFFFF3B30), Color(0xFFFF9F0A), Color(0xFFFF2D55), Color(0xFFBF5AF2))
        } else {
            listOf(Color(0xFF0A84FF), Color(0xFF64D2FF), Color(0xFF5E5CE6), Color(0xFFFF9F0A))
        }
}

private val LightColors = lightColorScheme(
    primary = AppleColors.SsqRed,
    background = AppleColors.Background,
    surface = AppleColors.CardWhite,
    onBackground = AppleColors.InkPrimary,
    onSurface = AppleColors.InkPrimary
)

@Composable
fun CaijiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
