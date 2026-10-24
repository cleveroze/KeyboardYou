package com.keyboardyou.keyboard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.keyboardyou.data.BackgroundEffect
import com.keyboardyou.theme.KeyboardPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Детерминированный «случайный» шум: позиции частиц — чистые функции от времени, без аллокаций. */
private fun rnd(i: Int, k: Int): Float {
    var x = i * 374761393 + k * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFFFF) / 16777215f
}

private fun wrap(v: Float, size: Float): Float = ((v % size) + size) % size

/** Анимированный эффект поверх фона клавиатуры. Не перехватывает касания. */
@Composable
fun BackgroundEffectLayer(
    effect: BackgroundEffect,
    palette: KeyboardPalette,
    intensity: Float,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    if (effect == BackgroundEffect.NONE) return
    val time by produceState(3.7f, animate) {
        if (!animate) return@produceState
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { value = 3.7f + (it - start) / 1_000_000_000f }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val t = time
        val k = intensity.coerceIn(0.3f, 2f)
        when (effect) {
            BackgroundEffect.NONE -> Unit
            BackgroundEffect.SNOW -> snow(t, k, palette)
            BackgroundEffect.RAIN -> rain(t, k, palette)
            BackgroundEffect.STARS -> stars(t, k, palette)
            BackgroundEffect.BUBBLES -> bubbles(t, k, palette)
            BackgroundEffect.CONFETTI -> confetti(t, k, palette)
            BackgroundEffect.FIREFLIES -> fireflies(t, k, palette)
            BackgroundEffect.AURORA -> aurora(t, k, palette)
            BackgroundEffect.SAKURA -> sakura(t, k, palette)
            BackgroundEffect.WAVES -> waves(t, k, palette)
            BackgroundEffect.HEARTS -> hearts(t, k, palette)
        }
    }
}

private fun KeyboardPalette.light(): Color = if (dark) Color.White else accent

private fun DrawScope.snow(t: Float, k: Float, p: KeyboardPalette) {
    val dp = density
    val n = (55 * k).toInt()
    for (i in 0 until n) {
        val depth = rnd(i, 1)
        val speed = (14 + 34 * depth) * dp
        val y = wrap(rnd(i, 2) * size.height + t * speed, size.height + 20 * dp) - 10 * dp
        val x = wrap(rnd(i, 3) * size.width + sin(t * (0.4f + rnd(i, 4)) + i) * 14 * dp, size.width)
        val r = (1.1f + 2.6f * depth) * dp
        drawCircle(p.light().copy(alpha = 0.35f + 0.5f * depth), r, Offset(x, y))
    }
}

private fun DrawScope.rain(t: Float, k: Float, p: KeyboardPalette) {
    val dp = density
    val n = (70 * k).toInt()
    val color = (if (p.dark) Color(0xFFB3D4FF) else p.accent)
    for (i in 0 until n) {
        val depth = rnd(i, 1)
        val speed = (420 + 380 * depth) * dp
        val len = (9 + 12 * depth) * dp
        val y = wrap(rnd(i, 2) * size.height + t * speed, size.height + len * 2) - len
        val x = wrap(rnd(i, 3) * size.width - y * 0.18f, size.width)
        drawLine(
            color.copy(alpha = 0.18f + 0.32f * depth),
            Offset(x, y), Offset(x - len * 0.18f, y + len),
            strokeWidth = (0.8f + 0.9f * depth) * dp, cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.stars(t: Float, k: Float, p: KeyboardPalette) {
    val dp = density
    val n = (50 * k).toInt()
    val base = p.light()
    for (i in 0 until n) {
        val x = rnd(i, 1) * size.width
        val y = rnd(i, 2) * size.height
        val tw = 0.5f + 0.5f * sin(t * (0.8f + 2.5f * rnd(i, 3)) + i * 1.7f)
        val r = (0.6f + 1.6f * rnd(i, 4)) * dp
        val a = 0.15f + 0.75f * tw
        drawCircle(base.copy(alpha = a), r, Offset(x, y))
        if (rnd(i, 5) > 0.8f) {
            // Крупные звёзды «искрят» лучиками
            val l = r * (2.5f + 2.5f * tw)
            drawLine(base.copy(alpha = a * 0.7f), Offset(x - l, y), Offset(x + l, y), strokeWidth = 0.8f * dp)
            drawLine(base.copy(alpha = a * 0.7f), Offset(x, y - l), Offset(x, y + l), strokeWidth = 0.8f * dp)
        }
    }
    // Падающая звезда раз в несколько секунд
    val cycle = 4.2f
    val idx = (t / cycle).toInt()
    val ph = (t % cycle) / 0.9f
    if (ph < 1f) {
        val sx = size.width * (0.15f + 0.7f * rnd(idx, 9))
        val sy = size.height * 0.1f * rnd(idx, 8)
        val hx = sx + ph * size.width * 0.35f
        val hy = sy + ph * size.height * 0.5f
        drawLine(
            Brush.linearGradient(listOf(Color.Transparent, base.copy(alpha = 0.9f * (1 - ph))), Offset(hx - 60 * dp, hy - 40 * dp), Offset(hx, hy)),
            Offset(hx - 60 * dp, hy - 40 * dp), Offset(hx, hy), strokeWidth = 2 * dp, cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.bubbles(t: Float, k: Float, p: KeyboardPalette) {
    val dp = density
    val n = (24 * k).toInt()
    val color = p.light()
    for (i in 0 until n) {
        val r = (3 + 8 * rnd(i, 1)) * dp
        val speed = (14 + 26 * rnd(i, 2)) * dp
        val y = size.height + r - wrap(rnd(i, 3) * size.height + t * speed, size.height + r * 2)
        val x = wrap(rnd(i, 4) * size.width + sin(t * 1.3f + i) * 8 * dp, size.width)
        drawCircle(color.copy(alpha = 0.45f), r, Offset(x, y), style = Stroke(1.2f * dp))
        drawCircle(color.copy(alpha = 0.12f), r, Offset(x, y))
        drawCircle(Color.White.copy(alpha = 0.7f), r * 0.22f, Offset(x - r * 0.35f, y - r * 0.35f))
    }
}

private fun DrawScope.confetti(t: Float, k: Float, p: KeyboardPalette) {
    val dp = density
    val n = (42 * k).toInt()
    val colors = listOf(p.accent, p.action, p.backgroundAccent1, p.backgroundAccent2, p.backgroundAccent3, p.pressed)
    for (i in 0 until n) {
        val speed = (40 + 50 * rnd(i, 1)) * dp
        val y = wrap(rnd(i, 2) * size.height + t * speed, size.height + 20 * dp) - 10 * dp
        val x = wrap(rnd(i, 3) * size.width + sin(t * (1 + rnd(i, 4)) + i) * 16 * dp, size.width)
        val angle = t * (120 + 240 * rnd(i, 5)) + i * 40
        val w = (3 + 3 * rnd(i, 6)) * dp
        // «Переворот» бумажки: высота пульсирует
        val h = (7 * dp) * (0.3f + 0.7f * kotlin.math.abs(cos(t * 4 * (0.5f + rnd(i, 7)) + i)))
        rotate(angle, Offset(x, y)) {
            drawRect(colors[i % colors.size].copy(alpha = 0.9f), Offset(x - w / 2, y - h / 2), Size(w, h))
        }
    }
}

private fun DrawScope.fireflies(t: Float, k: Float, p: KeyboardPalette) {
    val dp = density
    val n = (18 * k).toInt()
    val glow = if (p.dark) Color(0xFFFFF59D) else p.accent
    for (i in 0 until n) {
        val x = size.width * (0.5f + 0.45f * sin(t * (0.13f + 0.12f * rnd(i, 1)) + i * 2.1f) * cos(t * 0.07f + i))
        val y = size.height * (0.5f + 0.45f * cos(t * (0.11f + 0.1f * rnd(i, 2)) + i * 1.3f))
        val pulse = 0.5f + 0.5f * sin(t * (1.5f + 2f * rnd(i, 3)) + i)
        val r = (10 + 8 * pulse) * dp
        drawCircle(
            Brush.radialGradient(listOf(glow.copy(alpha = 0.55f * pulse), glow.copy(alpha = 0f)), Offset(x, y), r),
            r, Offset(x, y),
        )
        drawCircle(glow.copy(alpha = 0.6f + 0.4f * pulse), 1.8f * dp, Offset(x, y))
    }
}

private fun DrawScope.aurora(t: Float, k: Float, p: KeyboardPalette) {
    val dp = density
    val colors = listOf(p.backgroundAccent1, p.accent, p.backgroundAccent2)
    for (band in 0 until 3) {
        val path = Path()
        val steps = 32
        for (s in 0..steps) {
            val fx = s / steps.toFloat()
            val x = fx * size.width
            val y = size.height * (0.25f + 0.2f * band) +
                sin(fx * 2 * PI.toFloat() * (1.1f + 0.3f * band) + t * (0.5f + 0.15f * band) + band) * size.height * 0.12f +
                sin(fx * 9f + t * 1.1f) * size.height * 0.03f
            if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val c = colors[band]
        drawPath(path, c.copy(alpha = 0.16f * k.coerceAtMost(1.5f)), style = Stroke(width = 46 * dp, cap = StrokeCap.Round))
        drawPath(path, c.copy(alpha = 0.22f * k.coerceAtMost(1.5f)), style = Stroke(width = 16 * dp, cap = StrokeCap.Round))
    }
}

private fun DrawScope.sakura(t: Float, k: Float, p: KeyboardPalette) {
    val dp = density
    val n = (26 * k).toInt()
    val pink = Color(0xFFFFB7C5)
    for (i in 0 until n) {
        val speed = (22 + 26 * rnd(i, 1)) * dp
        val y = wrap(rnd(i, 2) * size.height + t * speed, size.height + 30 * dp) - 15 * dp
        val x = wrap(rnd(i, 3) * size.width + sin(t * 0.8f + i) * 26 * dp + t * 10 * dp, size.width)
        val s = (4 + 4 * rnd(i, 4)) * dp
        val angle = t * (60 + 120 * rnd(i, 5)) + i * 30
        val color = if (i % 3 == 0) p.accent.copy(alpha = 0.8f) else pink.copy(alpha = 0.85f)
        rotate(angle, Offset(x, y)) {
            val petal = Path().apply {
                moveTo(x, y - s)
                quadraticBezierTo(x + s, y - s * 0.2f, x, y + s)
                quadraticBezierTo(x - s, y - s * 0.2f, x, y - s)
                close()
            }
            drawPath(petal, color)
        }
    }
}

private fun DrawScope.waves(t: Float, k: Float, p: KeyboardPalette) {
    val colors = listOf(p.backgroundAccent1, p.backgroundAccent2, p.accent)
    for (layer in 0 until 3) {
        val path = Path()
        val baseY = size.height * (0.55f + 0.15f * layer)
        path.moveTo(0f, size.height)
        val steps = 36
        for (s in 0..steps) {
            val fx = s / steps.toFloat()
            val y = baseY + sin(fx * 2 * PI.toFloat() * (1.2f + 0.4f * layer) + t * (0.9f + 0.4f * layer) * (if (layer % 2 == 0) 1 else -1)) *
                size.height * 0.06f * k.coerceAtMost(1.5f)
            path.lineTo(fx * size.width, y)
        }
        path.lineTo(size.width, size.height)
        path.close()
        drawPath(path, colors[layer].copy(alpha = 0.18f))
    }
}

private fun DrawScope.hearts(t: Float, k: Float, p: KeyboardPalette) {
    val dp = density
    val n = (18 * k).toInt()
    val colors = listOf(Color(0xFFFF6B9A), p.accent, Color(0xFFFF8FB1))
    for (i in 0 until n) {
        val s = (5 + 6 * rnd(i, 1)) * dp
        val speed = (18 + 22 * rnd(i, 2)) * dp
        val travel = wrap(rnd(i, 3) * size.height + t * speed, size.height + s * 4)
        val y = size.height + s * 2 - travel
        val x = wrap(rnd(i, 4) * size.width + sin(t * 1.5f + i) * 10 * dp, size.width)
        val alpha = (travel / size.height).coerceIn(0f, 1f).let { 1f - it * 0.7f }
        val heart = Path().apply {
            moveTo(x, y + s * 0.9f)
            cubicTo(x - s * 1.4f, y - s * 0.1f, x - s * 0.6f, y - s * 1.1f, x, y - s * 0.35f)
            cubicTo(x + s * 0.6f, y - s * 1.1f, x + s * 1.4f, y - s * 0.1f, x, y + s * 0.9f)
            close()
        }
        drawPath(heart, colors[i % colors.size].copy(alpha = 0.75f * alpha))
    }
}
