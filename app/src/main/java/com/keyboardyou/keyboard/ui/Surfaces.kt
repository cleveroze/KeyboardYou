package com.keyboardyou.keyboard.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keyboardyou.data.AnimationLevel
import com.keyboardyou.data.BackgroundStyle
import com.keyboardyou.theme.KeyboardPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Поверхность клавиши. Для Liquid Glass рисуем полупрозрачную заливку,
 * верхний блик и «зеркальный» градиентный кант, как у стекла iOS.
 */
fun Modifier.keySurface(
    color: Color,
    shape: Shape,
    palette: KeyboardPalette,
    borders: Boolean,
    glow: Float = 0f,
    borderWidth: Dp = 1.dp,
): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    drawOutline(outline, color)
    if (palette.glass) {
        // Внутренний блик сверху
        drawOutline(
            outline,
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (palette.dark) 0.16f else 0.45f),
                0.45f to Color.White.copy(alpha = 0.0f),
                1f to Color.White.copy(alpha = if (palette.dark) 0.04f else 0.12f),
            ),
        )
        // Зеркальный кант: ярко сверху-слева, мягко снизу-справа
        drawOutline(
            outline,
            Brush.linearGradient(
                0f to palette.border,
                0.35f to palette.border.copy(alpha = 0.08f),
                0.7f to palette.border.copy(alpha = 0.05f),
                1f to palette.border.copy(alpha = 0.5f),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            style = Stroke(width = borderWidth.toPx()),
        )
    } else if (borders) {
        drawOutline(outline, palette.border, style = Stroke(width = borderWidth.toPx()))
    }
    if (glow > 0f) {
        drawOutline(
            outline,
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.35f * glow), Color.Transparent),
                center = center,
                radius = size.maxDimension * 0.7f,
            ),
        )
    }
}

/** Фон клавиатуры: сплошной, градиент или живые размытые «пятна». */
@Composable
fun KeyboardBackground(
    palette: KeyboardPalette,
    style: BackgroundStyle,
    animations: AnimationLevel,
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val effective = if (palette.glass && style == BackgroundStyle.SOLID) BackgroundStyle.GRADIENT else style
    when (effective) {
        BackgroundStyle.SOLID -> Box(modifier.fillMaxSize().background(palette.background.copy(alpha = alpha)))
        BackgroundStyle.GRADIENT -> Box(
            modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(
                        lerp(palette.background, palette.backgroundAccent1, if (palette.glass) 0.45f else 0.22f),
                        palette.background,
                        lerp(palette.background, palette.backgroundAccent2, if (palette.glass) 0.4f else 0.18f),
                    )
                ),
                alpha = alpha,
            )
        )
        BackgroundStyle.BLOBS -> Blobs(palette, animations != AnimationLevel.OFF, alpha, modifier)
    }
}

@Composable
private fun Blobs(palette: KeyboardPalette, animate: Boolean, alpha: Float, modifier: Modifier) {
    val phase = if (animate) {
        val transition = rememberInfiniteTransition(label = "blobs")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart),
            label = "phase",
        )
        p
    } else 0.6f
    val strength = if (palette.glass) 0.9f else 0.42f
    Canvas(modifier.fillMaxSize()) {
        drawRect(palette.background.copy(alpha = alpha))
        val w = size.width
        val h = size.height
        val blobs = listOf(
            Triple(palette.backgroundAccent1, Offset(w * (0.18f + 0.08f * cos(phase)), h * (0.25f + 0.12f * sin(phase))), w * 0.42f),
            Triple(palette.backgroundAccent2, Offset(w * (0.82f + 0.06f * sin(phase * 2)), h * (0.35f + 0.15f * cos(phase))), w * 0.38f),
            Triple(palette.backgroundAccent3, Offset(w * (0.5f + 0.12f * sin(phase + 1f)), h * (0.95f + 0.08f * cos(phase * 2))), w * 0.45f),
        )
        blobs.forEach { (color, c, r) ->
            drawCircle(
                Brush.radialGradient(
                    listOf(color.copy(alpha = strength * alpha), color.copy(alpha = 0f)),
                    center = c,
                    radius = r,
                ),
                radius = r,
                center = c,
            )
        }
    }
}
