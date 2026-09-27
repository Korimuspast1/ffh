package com.korimuspast1.lexora.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathFillType
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object LexoraIcons {
    val Heart: ImageVector by lazyIcon("Heart") {
        moveTo(12f, 21f)
        cubicTo(9.7f, 18.9f, 4f, 14.1f, 4f, 8.8f)
        cubicTo(4f, 5.8f, 6.2f, 3.6f, 9.1f, 3.6f)
        cubicTo(10.8f, 3.6f, 11.8f, 4.4f, 12f, 5.1f)
        cubicTo(12.2f, 4.4f, 13.2f, 3.6f, 14.9f, 3.6f)
        cubicTo(17.8f, 3.6f, 20f, 5.8f, 20f, 8.8f)
        cubicTo(20f, 14.1f, 14.3f, 18.9f, 12f, 21f)
        close()
    }

    val BrokenHeart: ImageVector by lazyIcon("BrokenHeart") {
        moveTo(11.3f, 21f)
        cubicTo(8.8f, 18.8f, 4f, 14.4f, 4f, 8.8f)
        cubicTo(4f, 5.8f, 6.2f, 3.6f, 9f, 3.6f)
        cubicTo(10.4f, 3.6f, 11.4f, 4.2f, 12f, 5.1f)
        lineTo(9.7f, 9.1f)
        lineTo(13.1f, 9.1f)
        lineTo(10.5f, 14.1f)
        lineTo(13f, 14.1f)
        lineTo(11.3f, 21f)
        close()
        moveTo(13f, 20.6f)
        lineTo(14.5f, 15.8f)
        lineTo(12f, 15.8f)
        lineTo(14.6f, 10.7f)
        lineTo(11.5f, 10.7f)
        lineTo(13.2f, 3.9f)
        cubicTo(13.7f, 3.7f, 14.3f, 3.6f, 15f, 3.6f)
        cubicTo(17.8f, 3.6f, 20f, 5.8f, 20f, 8.8f)
        cubicTo(20f, 13.9f, 15.4f, 18.2f, 13f, 20.6f)
        close()
    }

    val Gem: ImageVector by lazyIcon("Gem") {
        moveTo(6f, 3.5f)
        lineTo(18f, 3.5f)
        lineTo(22f, 9f)
        lineTo(12f, 21f)
        lineTo(2f, 9f)
        close()
        moveTo(5.4f, 9f)
        lineTo(9.5f, 9f)
        lineTo(12f, 16.1f)
        lineTo(14.5f, 9f)
        lineTo(18.6f, 9f)
        lineTo(16.7f, 6f)
        lineTo(7.3f, 6f)
        close()
    }

    val Flame: ImageVector by lazyIcon("Flame") {
        moveTo(12.3f, 22f)
        cubicTo(7.8f, 22f, 5f, 18.8f, 5f, 15.1f)
        cubicTo(5f, 11.8f, 7.1f, 9.4f, 8.7f, 7.5f)
        cubicTo(10.2f, 5.8f, 10.9f, 4.2f, 10.8f, 2f)
        cubicTo(14.4f, 3.6f, 16.9f, 6.9f, 16.5f, 11.1f)
        cubicTo(17.5f, 10.6f, 18.2f, 9.6f, 18.5f, 8.2f)
        cubicTo(20f, 10.1f, 21f, 12.4f, 21f, 15.1f)
        cubicTo(21f, 18.9f, 18f, 22f, 12.3f, 22f)
        close()
        moveTo(12.1f, 19f)
        cubicTo(14.1f, 19f, 15.5f, 17.7f, 15.5f, 15.9f)
        cubicTo(15.5f, 14.2f, 14.2f, 13.1f, 12.7f, 12.1f)
        cubicTo(12.7f, 13.7f, 11.7f, 14.5f, 10.9f, 15.4f)
        cubicTo(10.2f, 16.1f, 9.7f, 16.8f, 9.7f, 17.6f)
        cubicTo(9.7f, 18.4f, 10.6f, 19f, 12.1f, 19f)
        close()
    }

    val Trophy: ImageVector by lazyIcon("Trophy") {
        moveTo(7f, 3f)
        lineTo(17f, 3f)
        lineTo(17f, 5f)
        lineTo(21f, 5f)
        lineTo(21f, 8.4f)
        cubicTo(21f, 11.3f, 18.9f, 13.3f, 16.2f, 13.8f)
        cubicTo(15.6f, 15.3f, 14.5f, 16.4f, 13f, 16.8f)
        lineTo(13f, 19f)
        lineTo(17f, 19f)
        lineTo(17f, 21f)
        lineTo(7f, 21f)
        lineTo(7f, 19f)
        lineTo(11f, 19f)
        lineTo(11f, 16.8f)
        cubicTo(9.5f, 16.4f, 8.4f, 15.3f, 7.8f, 13.8f)
        cubicTo(5.1f, 13.3f, 3f, 11.3f, 3f, 8.4f)
        lineTo(3f, 5f)
        lineTo(7f, 5f)
        close()
        moveTo(17f, 7f)
        lineTo(17f, 11.5f)
        cubicTo(18.2f, 11f, 19f, 10f, 19f, 8.5f)
        lineTo(19f, 7f)
        close()
        moveTo(5f, 7f)
        lineTo(5f, 8.5f)
        cubicTo(5f, 10f, 5.8f, 11f, 7f, 11.5f)
        lineTo(7f, 7f)
        close()
    }

    val Crown: ImageVector by lazyIcon("Crown") {
        moveTo(3f, 7f)
        lineTo(8f, 11f)
        lineTo(12f, 4f)
        lineTo(16f, 11f)
        lineTo(21f, 7f)
        lineTo(19f, 19f)
        lineTo(5f, 19f)
        close()
        moveTo(6.8f, 16.5f)
        lineTo(17.2f, 16.5f)
        lineTo(18f, 11.6f)
        lineTo(15.3f, 13.8f)
        lineTo(12f, 8.4f)
        lineTo(8.7f, 13.8f)
        lineTo(6f, 11.6f)
        close()
    }

    val Shield: ImageVector by lazyIcon("Shield") {
        moveTo(12f, 2f)
        lineTo(20f, 5.2f)
        lineTo(20f, 11.5f)
        cubicTo(20f, 16.6f, 16.6f, 20.1f, 12f, 22f)
        cubicTo(7.4f, 20.1f, 4f, 16.6f, 4f, 11.5f)
        lineTo(4f, 5.2f)
        close()
        moveTo(12f, 5f)
        lineTo(7f, 7f)
        lineTo(7f, 11.5f)
        cubicTo(7f, 14.9f, 8.9f, 17.4f, 12f, 19f)
        cubicTo(15.1f, 17.4f, 17f, 14.9f, 17f, 11.5f)
        lineTo(17f, 7f)
        close()
    }

    val Star: ImageVector by lazyIcon("Star") {
        moveTo(12f, 2.5f)
        lineTo(14.9f, 8.5f)
        lineTo(21.5f, 9.4f)
        lineTo(16.7f, 14f)
        lineTo(17.9f, 20.5f)
        lineTo(12f, 17.3f)
        lineTo(6.1f, 20.5f)
        lineTo(7.3f, 14f)
        lineTo(2.5f, 9.4f)
        lineTo(9.1f, 8.5f)
        close()
    }

    val Check: ImageVector by lazyIcon("Check") {
        moveTo(9.2f, 16.6f)
        lineTo(4.9f, 12.3f)
        lineTo(3.2f, 14f)
        lineTo(9.2f, 20f)
        lineTo(21f, 8.2f)
        lineTo(19.3f, 6.5f)
        close()
    }

    val Close: ImageVector by lazyIcon("Close") {
        moveTo(6.4f, 4.8f)
        lineTo(12f, 10.4f)
        lineTo(17.6f, 4.8f)
        lineTo(19.2f, 6.4f)
        lineTo(13.6f, 12f)
        lineTo(19.2f, 17.6f)
        lineTo(17.6f, 19.2f)
        lineTo(12f, 13.6f)
        lineTo(6.4f, 19.2f)
        lineTo(4.8f, 17.6f)
        lineTo(10.4f, 12f)
        lineTo(4.8f, 6.4f)
        close()
    }

    val Lock: ImageVector by lazyIcon("Lock") {
        moveTo(7f, 10f)
        lineTo(7f, 8f)
        cubicTo(7f, 5.2f, 9.2f, 3f, 12f, 3f)
        cubicTo(14.8f, 3f, 17f, 5.2f, 17f, 8f)
        lineTo(17f, 10f)
        lineTo(19f, 10f)
        lineTo(19f, 21f)
        lineTo(5f, 21f)
        lineTo(5f, 10f)
        close()
        moveTo(9.5f, 10f)
        lineTo(14.5f, 10f)
        lineTo(14.5f, 8f)
        cubicTo(14.5f, 6.6f, 13.4f, 5.5f, 12f, 5.5f)
        cubicTo(10.6f, 5.5f, 9.5f, 6.6f, 9.5f, 8f)
        close()
    }

    val Paw: ImageVector by lazyIcon("Paw") {
        moveTo(12f, 12.5f)
        cubicTo(15.1f, 12.5f, 18f, 15.4f, 18f, 18f)
        cubicTo(18f, 20f, 16.3f, 21.2f, 14.8f, 20.2f)
        cubicTo(13.1f, 19.1f, 10.9f, 19.1f, 9.2f, 20.2f)
        cubicTo(7.7f, 21.2f, 6f, 20f, 6f, 18f)
        cubicTo(6f, 15.4f, 8.9f, 12.5f, 12f, 12.5f)
        close()
        moveTo(7f, 5f)
        cubicTo(8.3f, 5f, 9.2f, 6.3f, 9.2f, 7.8f)
        cubicTo(9.2f, 9.3f, 8.3f, 10.6f, 7f, 10.6f)
        cubicTo(5.7f, 10.6f, 4.8f, 9.3f, 4.8f, 7.8f)
        cubicTo(4.8f, 6.3f, 5.7f, 5f, 7f, 5f)
        close()
        moveTo(17f, 5f)
        cubicTo(18.3f, 5f, 19.2f, 6.3f, 19.2f, 7.8f)
        cubicTo(19.2f, 9.3f, 18.3f, 10.6f, 17f, 10.6f)
        cubicTo(15.7f, 10.6f, 14.8f, 9.3f, 14.8f, 7.8f)
        cubicTo(14.8f, 6.3f, 15.7f, 5f, 17f, 5f)
        close()
        moveTo(12f, 3.7f)
        cubicTo(13.2f, 3.7f, 14.1f, 4.9f, 14.1f, 6.3f)
        cubicTo(14.1f, 7.7f, 13.2f, 9f, 12f, 9f)
        cubicTo(10.8f, 9f, 9.9f, 7.7f, 9.9f, 6.3f)
        cubicTo(9.9f, 4.9f, 10.8f, 3.7f, 12f, 3.7f)
        close()
    }

    val Lightning: ImageVector by lazyIcon("Lightning") {
        moveTo(13.5f, 2f)
        lineTo(5f, 13f)
        lineTo(11f, 13f)
        lineTo(9f, 22f)
        lineTo(19f, 9.5f)
        lineTo(13f, 9.5f)
        close()
    }

    val Book: ImageVector by lazyIcon("Book") {
        moveTo(5f, 4f)
        cubicTo(6.5f, 3.2f, 8.5f, 3f, 11f, 4.2f)
        lineTo(11f, 20f)
        cubicTo(8.9f, 18.9f, 6.9f, 18.9f, 5f, 20f)
        close()
        moveTo(13f, 4.2f)
        cubicTo(15.5f, 3f, 17.5f, 3.2f, 19f, 4f)
        lineTo(19f, 20f)
        cubicTo(17.1f, 18.9f, 15.1f, 18.9f, 13f, 20f)
        close()
    }

    val Mic: ImageVector by lazyIcon("Mic") {
        moveTo(12f, 3f)
        cubicTo(10.1f, 3f, 8.8f, 4.4f, 8.8f, 6.2f)
        lineTo(8.8f, 11f)
        cubicTo(8.8f, 12.8f, 10.1f, 14.2f, 12f, 14.2f)
        cubicTo(13.9f, 14.2f, 15.2f, 12.8f, 15.2f, 11f)
        lineTo(15.2f, 6.2f)
        cubicTo(15.2f, 4.4f, 13.9f, 3f, 12f, 3f)
        close()
        moveTo(5.5f, 10.5f)
        lineTo(7.8f, 10.5f)
        cubicTo(7.8f, 13.4f, 9.5f, 16f, 12f, 16f)
        cubicTo(14.5f, 16f, 16.2f, 13.4f, 16.2f, 10.5f)
        lineTo(18.5f, 10.5f)
        cubicTo(18.5f, 14.3f, 16.3f, 17.5f, 13f, 18.2f)
        lineTo(13f, 21f)
        lineTo(11f, 21f)
        lineTo(11f, 18.2f)
        cubicTo(7.7f, 17.5f, 5.5f, 14.3f, 5.5f, 10.5f)
        close()
    }

    val Speaker: ImageVector by lazyIcon("Speaker") {
        moveTo(4f, 9f)
        lineTo(8f, 9f)
        lineTo(13f, 5f)
        lineTo(13f, 19f)
        lineTo(8f, 15f)
        lineTo(4f, 15f)
        close()
        moveTo(16f, 8f)
        cubicTo(17.3f, 9.1f, 18f, 10.5f, 18f, 12f)
        cubicTo(18f, 13.5f, 17.3f, 14.9f, 16f, 16f)
        lineTo(14.7f, 14.7f)
        cubicTo(15.5f, 14f, 16f, 13.1f, 16f, 12f)
        cubicTo(16f, 10.9f, 15.5f, 10f, 14.7f, 9.3f)
        close()
        moveTo(18.6f, 5.4f)
        cubicTo(20.7f, 7.1f, 22f, 9.5f, 22f, 12f)
        cubicTo(22f, 14.5f, 20.7f, 16.9f, 18.6f, 18.6f)
        lineTo(17.2f, 17.2f)
        cubicTo(19f, 15.9f, 20f, 14f, 20f, 12f)
        cubicTo(20f, 10f, 19f, 8.1f, 17.2f, 6.8f)
        close()
    }

    val Key: ImageVector by lazyIcon("Key") {
        moveTo(8f, 14f)
        cubicTo(5.8f, 14f, 4f, 12.2f, 4f, 10f)
        cubicTo(4f, 7.8f, 5.8f, 6f, 8f, 6f)
        cubicTo(9.6f, 6f, 11f, 7f, 11.6f, 8.4f)
        lineTo(21f, 8.4f)
        lineTo(21f, 11f)
        lineTo(19f, 11f)
        lineTo(19f, 13f)
        lineTo(16.5f, 13f)
        lineTo(16.5f, 11f)
        lineTo(11.6f, 11f)
        cubicTo(11f, 12.8f, 9.6f, 14f, 8f, 14f)
        close()
        moveTo(8f, 8.4f)
        cubicTo(7.1f, 8.4f, 6.4f, 9.1f, 6.4f, 10f)
        cubicTo(6.4f, 10.9f, 7.1f, 11.6f, 8f, 11.6f)
        cubicTo(8.9f, 11.6f, 9.6f, 10.9f, 9.6f, 10f)
        cubicTo(9.6f, 9.1f, 8.9f, 8.4f, 8f, 8.4f)
        close()
    }

    val Flag: ImageVector by lazyIcon("Flag") {
        moveTo(5f, 3f)
        lineTo(7f, 3f)
        lineTo(7f, 5f)
        cubicTo(9.5f, 3.8f, 12f, 4.5f, 14.5f, 5.2f)
        cubicTo(16f, 5.7f, 17.5f, 6.1f, 19f, 5.5f)
        lineTo(19f, 14.5f)
        cubicTo(17.5f, 15.1f, 16f, 14.7f, 14.5f, 14.2f)
        cubicTo(12f, 13.5f, 9.5f, 12.8f, 7f, 14f)
        lineTo(7f, 22f)
        lineTo(5f, 22f)
        close()
    }

    val User: ImageVector by lazyIcon("User") {
        moveTo(12f, 12f)
        cubicTo(9.5f, 12f, 7.5f, 10f, 7.5f, 7.5f)
        cubicTo(7.5f, 5f, 9.5f, 3f, 12f, 3f)
        cubicTo(14.5f, 3f, 16.5f, 5f, 16.5f, 7.5f)
        cubicTo(16.5f, 10f, 14.5f, 12f, 12f, 12f)
        close()
        moveTo(4f, 21f)
        cubicTo(4.8f, 16.8f, 7.9f, 14.3f, 12f, 14.3f)
        cubicTo(16.1f, 14.3f, 19.2f, 16.8f, 20f, 21f)
        close()
    }
}

private fun lazyIcon(name: String, block: PathBuilder.() -> Unit): Lazy<ImageVector> = lazy {
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero,
        ) {
            block()
        }
    }.build()
}
