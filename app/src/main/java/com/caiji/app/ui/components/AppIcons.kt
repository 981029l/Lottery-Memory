package com.caiji.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 轻量自绘线性矢量图标，替代体积约 10MB 的 material-icons-extended 全家桶
 */
object AppIcons {

    private fun strokeIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply(block).build()
    }

    private fun ImageVector.Builder.strokePath(d: String, width: Float = 1.9f) {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 使用标准 path builder
        }
    }

    val Refresh: ImageVector by lazy {
        strokeIcon("Refresh") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 12f)
                arcTo(8f, 8f, 0f, true, true, 17.66f, 6.34f)
                moveTo(20f, 4f)
                verticalLineTo(9f)
                horizontalLineTo(15f)
            }
        }
    }

    val Bell: ImageVector by lazy {
        strokeIcon("Bell") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 16f)
                verticalLineTo(11f)
                arcTo(6f, 6f, 0f, false, false, 6f, 11f)
                verticalLineTo(16f)
                lineTo(4.5f, 18f)
                horizontalLineTo(19.5f)
                close()
                moveTo(10f, 20.5f)
                arcTo(2.2f, 2.2f, 0f, false, false, 14f, 20.5f)
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        strokeIcon("ChevronRight") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 6f)
                lineTo(15f, 12f)
                lineTo(9f, 18f)
            }
        }
    }

    val Close: ImageVector by lazy {
        strokeIcon("Close") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }
    }

    val Clock: ImageVector by lazy {
        strokeIcon("Clock") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3.5f)
                arcTo(8.5f, 8.5f, 0f, true, true, 11.99f, 3.5f)
                moveTo(12f, 7.5f)
                verticalLineTo(12f)
                lineTo(15f, 14f)
            }
        }
    }

    val Pin: ImageVector by lazy {
        strokeIcon("Pin") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 21f)
                curveTo(12f, 21f, 5.5f, 15.4f, 5.5f, 9.8f)
                arcTo(6.5f, 6.5f, 0f, true, true, 18.5f, 9.8f)
                curveTo(18.5f, 15.4f, 12f, 21f, 12f, 21f)
                close()
                moveTo(14.3f, 9.8f)
                arcTo(2.3f, 2.3f, 0f, true, true, 9.7f, 9.8f)
                arcTo(2.3f, 2.3f, 0f, true, true, 14.3f, 9.8f)
            }
        }
    }

    val Shuffle: ImageVector by lazy {
        strokeIcon("Shuffle") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 7f)
                horizontalLineTo(6.5f)
                lineTo(14.5f, 17f)
                horizontalLineTo(21f)
                moveTo(18f, 14f)
                lineTo(21f, 17f)
                lineTo(18f, 20f)
                moveTo(3f, 17f)
                horizontalLineTo(6.5f)
                lineTo(9.2f, 13.6f)
                moveTo(14.8f, 10.4f)
                lineTo(16.5f, 7f)
                horizontalLineTo(21f)
                moveTo(18f, 4f)
                lineTo(21f, 7f)
                lineTo(18f, 10f)
            }
        }
    }

    val Add: ImageVector by lazy {
        strokeIcon("Add") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }
    }

    val Remove: ImageVector by lazy {
        strokeIcon("Remove") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }
    }

    val Trash: ImageVector by lazy {
        strokeIcon("Trash") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 7f)
                horizontalLineTo(20f)
                moveTo(9f, 7f)
                verticalLineTo(4.5f)
                horizontalLineTo(15f)
                verticalLineTo(7f)
                moveTo(6.5f, 7f)
                lineTo(7.5f, 19.5f)
                horizontalLineTo(16.5f)
                lineTo(17.5f, 7f)
            }
        }
    }

    val Ticket: ImageVector by lazy {
        strokeIcon("Ticket") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 8.5f)
                verticalLineTo(6.5f)
                horizontalLineTo(20.5f)
                verticalLineTo(8.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 20.5f, 13.5f)
                verticalLineTo(17.5f)
                horizontalLineTo(3.5f)
                verticalLineTo(13.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 3.5f, 8.5f)
                close()
                moveTo(14.5f, 7f)
                verticalLineTo(17f)
            }
        }
    }

    val Download: ImageVector by lazy {
        strokeIcon("Download") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 4f)
                verticalLineTo(15f)
                moveTo(7.5f, 11f)
                lineTo(12f, 15.5f)
                lineTo(16.5f, 11f)
                moveTo(5f, 19.5f)
                horizontalLineTo(19f)
            }
        }
    }

    val Bolt: ImageVector by lazy {
        strokeIcon("Bolt") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.5f, 3f)
                lineTo(6f, 13.5f)
                horizontalLineTo(12f)
                lineTo(10.5f, 21f)
                lineTo(18f, 10.5f)
                horizontalLineTo(12f)
                close()
            }
        }
    }
}
