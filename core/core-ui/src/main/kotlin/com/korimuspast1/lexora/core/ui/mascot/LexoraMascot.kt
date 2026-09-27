package com.korimuspast1.lexora.core.ui.mascot

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lexora mascot: Nori, a friendly axolotl guide. It is drawn fully with Compose Canvas.
 */
enum class MascotMood {
    Idle,
    Happy,
    Sad,
    Celebrating,
    Thinking,
    Sleeping,
    Jumping,
    Waving,
    Crying,
    LevelUp,
    StreakFire,
    Cheering,
}

@Composable
fun LexoraMascot(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
) {
    val transition = rememberInfiniteTransition(label = "noriMascot")
    val bob by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "noriBob",
    )
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "noriBlink",
    )
    val wave by transition.animateFloat(
        initialValue = -16f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "noriWave",
    )
    val jumpTarget = when (mood) {
        MascotMood.Jumping, MascotMood.Celebrating, MascotMood.LevelUp, MascotMood.Cheering -> -14f
        MascotMood.Sad, MascotMood.Crying, MascotMood.Sleeping -> 5f
        else -> 0f
    }
    val jump by animateFloatAsState(targetValue = jumpTarget, animationSpec = tween(360), label = "noriJump")
    val tiltTarget = when (mood) {
        MascotMood.Thinking -> -8f
        MascotMood.Waving -> wave
        MascotMood.Sad, MascotMood.Crying -> 5f
        MascotMood.Cheering -> -wave / 3f
        else -> 0f
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = bob + jump
                rotationZ = tiltTarget
            },
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val body = Color(0xFFFF8FB3)
            val bodyDark = Color(0xFFE95C8B)
            val belly = Color(0xFFFFC8D7)
            val gill = Color(0xFFFF5E9D)
            val eye = Color(0xFF10203A)
            val shine = Color.White.copy(alpha = 0.85f)
            val accent = Color(0xFF7CF4C9)
            val fire = Color(0xFFFF7A1A)

            val faceTop = h * 0.18f
            val faceSize = Size(w * 0.58f, h * 0.46f)
            val faceLeft = cx - faceSize.width / 2f

            fun drawGill(side: Int) {
                val baseX = cx + side * w * 0.26f
                val baseY = h * 0.36f
                repeat(3) { index ->
                    val spread = (index - 1) * h * 0.065f
                    val path = Path().apply {
                        moveTo(baseX, baseY + spread)
                        cubicTo(
                            baseX + side * w * 0.14f,
                            baseY + spread - h * 0.04f,
                            baseX + side * w * 0.18f,
                            baseY + spread + h * 0.04f,
                            baseX + side * w * 0.06f,
                            baseY + spread + h * 0.08f,
                        )
                    }
                    drawPath(path = path, color = gill, style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
                }
            }

            drawGill(-1)
            drawGill(1)

            drawRoundRect(
                color = bodyDark.copy(alpha = 0.22f),
                topLeft = Offset(faceLeft + w * 0.015f, faceTop + h * 0.02f),
                size = faceSize,
                cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
            )
            drawRoundRect(
                color = body,
                topLeft = Offset(faceLeft, faceTop),
                size = faceSize,
                cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
            )

            drawOval(
                color = belly,
                topLeft = Offset(cx - w * 0.16f, h * 0.51f),
                size = Size(w * 0.32f, h * 0.27f),
            )

            val leftArmStart = Offset(cx - w * 0.22f, h * 0.58f)
            val rightArmStart = Offset(cx + w * 0.22f, h * 0.58f)
            drawLine(
                color = bodyDark,
                start = leftArmStart,
                end = Offset(cx - w * 0.36f, h * 0.66f),
                strokeWidth = w * 0.055f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = bodyDark,
                start = rightArmStart,
                end = Offset(cx + w * 0.34f, h * if (mood == MascotMood.Waving) 0.47f else 0.66f),
                strokeWidth = w * 0.055f,
                cap = StrokeCap.Round,
            )

            drawCircle(color = body, radius = w * 0.065f, center = Offset(cx - w * 0.17f, h * 0.77f))
            drawCircle(color = body, radius = w * 0.065f, center = Offset(cx + w * 0.17f, h * 0.77f))

            val eyeHeight = if (mood == MascotMood.Sleeping) h * 0.008f else h * 0.032f * blink
            val leftEye = Offset(cx - w * 0.115f, h * 0.36f)
            val rightEye = Offset(cx + w * 0.115f, h * 0.36f)
            drawOval(color = eye, topLeft = Offset(leftEye.x - w * 0.03f, leftEye.y - eyeHeight / 2f), size = Size(w * 0.06f, eyeHeight.coerceAtLeast(h * 0.008f)))
            drawOval(color = eye, topLeft = Offset(rightEye.x - w * 0.03f, rightEye.y - eyeHeight / 2f), size = Size(w * 0.06f, eyeHeight.coerceAtLeast(h * 0.008f)))
            if (mood != MascotMood.Sleeping) {
                drawCircle(color = shine, radius = w * 0.012f, center = Offset(leftEye.x + w * 0.012f, leftEye.y - h * 0.012f))
                drawCircle(color = shine, radius = w * 0.012f, center = Offset(rightEye.x + w * 0.012f, rightEye.y - h * 0.012f))
            }

            when (mood) {
                MascotMood.Sad, MascotMood.Crying -> {
                    drawArc(
                        color = eye,
                        startAngle = 205f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(cx - w * 0.08f, h * 0.47f),
                        size = Size(w * 0.16f, h * 0.10f),
                        style = Stroke(width = w * 0.016f, cap = StrokeCap.Round),
                    )
                    if (mood == MascotMood.Crying) {
                        drawOval(color = Color(0xFF5BC8FF), topLeft = Offset(leftEye.x - w * 0.01f, leftEye.y + h * 0.035f), size = Size(w * 0.03f, h * 0.06f))
                    }
                }
                MascotMood.Sleeping -> {
                    drawLine(color = eye, start = Offset(cx - w * 0.06f, h * 0.48f), end = Offset(cx + w * 0.06f, h * 0.48f), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
                    drawTextDots(cx + w * 0.25f, h * 0.22f, eye)
                }
                else -> {
                    drawArc(
                        color = eye,
                        startAngle = 20f,
                        sweepAngle = 140f,
                        useCenter = false,
                        topLeft = Offset(cx - w * 0.09f, h * 0.42f),
                        size = Size(w * 0.18f, h * 0.13f),
                        style = Stroke(width = w * 0.018f, cap = StrokeCap.Round),
                    )
                }
            }

            if (mood == MascotMood.Thinking) {
                drawCircle(color = accent, radius = w * 0.025f, center = Offset(cx + w * 0.26f, h * 0.17f))
                drawCircle(color = accent.copy(alpha = 0.72f), radius = w * 0.017f, center = Offset(cx + w * 0.34f, h * 0.10f))
            }

            if (mood == MascotMood.LevelUp || mood == MascotMood.Celebrating || mood == MascotMood.Cheering) {
                repeat(5) { index ->
                    val x = w * (0.18f + index * 0.16f)
                    val y = h * (0.09f + (index % 2) * 0.06f)
                    drawCircle(color = listOf(accent, Color(0xFFFFC441), Color(0xFF9360CF))[index % 3], radius = w * 0.018f, center = Offset(x, y))
                }
            }

            if (mood == MascotMood.StreakFire) {
                val flame = Path().apply {
                    moveTo(cx, h * 0.08f)
                    cubicTo(cx + w * 0.08f, h * 0.17f, cx + w * 0.03f, h * 0.25f, cx, h * 0.29f)
                    cubicTo(cx - w * 0.08f, h * 0.22f, cx - w * 0.03f, h * 0.14f, cx, h * 0.08f)
                    close()
                }
                drawPath(path = flame, color = fire)
                drawPath(path = flame, color = Color(0xFFFFD95A).copy(alpha = 0.72f), style = Stroke(width = w * 0.014f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTextDots(x: Float, y: Float, color: Color) {
    drawCircle(color = color.copy(alpha = 0.35f), radius = size.width * 0.012f, center = Offset(x, y))
    drawCircle(color = color.copy(alpha = 0.55f), radius = size.width * 0.016f, center = Offset(x + size.width * 0.05f, y - size.height * 0.035f))
    drawCircle(color = color.copy(alpha = 0.75f), radius = size.width * 0.02f, center = Offset(x + size.width * 0.105f, y - size.height * 0.08f))
}
