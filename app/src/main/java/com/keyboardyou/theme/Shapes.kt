package com.keyboardyou.theme

import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Формы в духе Material 3 Expressive. Каждая форма описана полярной функцией r(θ) ∈ (0, 1],
 * поэтому любые две формы можно плавно морфить друг в друга, просто интерполируя радиус.
 */
enum class KeyShape(val title: String) {
    CIRCLE("Круг"),
    SQUIRCLE("Сквиркл"),
    COOKIE_4("Клевер"),
    COOKIE_6("Печенье 6"),
    COOKIE_9("Печенье 9"),
    COOKIE_12("Солнце"),
    PUFFY_DIAMOND("Ромб"),
    FLOWER("Цветок"),
    TRIANGLE("Треугольник"),
    PENTAGON("Пятиугольник"),
    GHOST("Пузырь"),
    ;

    fun radius(theta: Float): Float = when (this) {
        CIRCLE -> 1f
        SQUIRCLE -> superellipse(theta, 4.2f)
        COOKIE_4 -> wave(theta, 4, 0.16f, PI.toFloat() / 4f)
        COOKIE_6 -> wave(theta, 6, 0.09f, 0f)
        COOKIE_9 -> wave(theta, 9, 0.07f, 0f)
        COOKIE_12 -> wave(theta, 12, 0.06f, 0f)
        PUFFY_DIAMOND -> roundedPolygon(theta, 4, 0.55f, 0f)
        FLOWER -> {
            val c = cos(8 * theta)
            0.86f + 0.14f * c * c * c
        }
        TRIANGLE -> roundedPolygon(theta, 3, 0.45f, 0f)
        PENTAGON -> roundedPolygon(theta, 5, 0.4f, -PI.toFloat() / 2f)
        GHOST -> {
            // Капля-пузырь: чуть сплюснутый снизу круг с волной.
            val base = 0.92f + 0.08f * cos(theta - (PI / 2).toFloat())
            base * (1f - 0.04f * (1 + cos(5 * theta)) / 2f)
        }
    }

    companion object {
        private fun wave(theta: Float, lobes: Int, amp: Float, phase: Float): Float =
            (1f + amp * cos(lobes * (theta - phase))) / (1f + amp)

        private fun superellipse(theta: Float, n: Float): Float {
            val c = abs(cos(theta)).pow(n)
            val s = abs(sin(theta)).pow(n)
            return (c + s).pow(-1f / n)
        }

        /** Правильный многоугольник со скруглёнными углами (смешивание с вписанной окружностью). */
        private fun roundedPolygon(theta: Float, n: Int, round: Float, rotation: Float): Float {
            val seg = (2 * PI / n).toFloat()
            var t = (theta - rotation) % seg
            if (t < 0) t += seg
            val inscribed = cos(PI.toFloat() / n)
            val poly = inscribed / cos(t - seg / 2f)
            val r = poly * (1 - round) + inscribed * round
            // Нормируем так, чтобы максимальный радиус был 1.
            val maxR = 1f * (1 - round) + inscribed * round
            return r / maxR
        }
    }
}

private const val SAMPLES = 120

/** Форма по полярной функции; растягивается по ширине и высоте контейнера. */
class PolarShape(private val shape: KeyShape, private val rotation: Float = 0f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        for (i in 0 until SAMPLES) {
            val t = (2 * PI * i / SAMPLES).toFloat()
            val r = shape.radius(t - rotation)
            val x = cx + cos(t) * r * cx
            val y = cy + sin(t) * r * cy
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) = other is PolarShape && other.shape == shape && other.rotation == rotation
    override fun hashCode() = shape.hashCode() * 31 + rotation.hashCode()
}

/**
 * Морф «скруглённый прямоугольник → выразительная форма».
 * [cornerFraction] — радиус угла как доля от половины меньшей стороны.
 * [progress] 0 — прямоугольник, 1 — форма [target].
 */
class MorphShape(
    private val cornerFraction: Float,
    private val target: KeyShape,
    private val progress: Float,
    private val rotation: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val hw = size.width / 2f
        val hh = size.height / 2f
        val radius = min(hw, hh) * cornerFraction.coerceIn(0f, 1f)
        if (progress <= 0.001f) {
            return Outline.Rounded(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius)))
        }
        // Для очень широких клавиш форма — круг в центре, а не вытянутый эллипс.
        val sx = if (hw > hh * 1.6f) hh else hw
        val sy = hh
        val path = Path()
        for (i in 0 until SAMPLES) {
            val t = (2 * PI * i / SAMPLES).toFloat()
            val dx = cos(t)
            val dy = sin(t)
            val rectT = rayRoundRect(dx * hw, dy * hh, hw, hh, radius)
            val rx = dx * hw * rectT
            val ry = dy * hh * rectT
            val r = target.radius(t - rotation)
            val px = dx * sx * r
            val py = dy * sy * r
            val x = hw + rx + (px - rx) * progress
            val y = hh + ry + (py - ry) * progress
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) = other is MorphShape && other.cornerFraction == cornerFraction &&
        other.target == target && other.progress == progress && other.rotation == rotation

    override fun hashCode(): Int {
        var h = cornerFraction.hashCode()
        h = h * 31 + target.hashCode()
        h = h * 31 + progress.hashCode()
        return h * 31 + rotation.hashCode()
    }

    companion object {
        private fun sdf(px: Float, py: Float, bx: Float, by: Float, r: Float): Float {
            val qx = abs(px) - bx + r
            val qy = abs(py) - by + r
            val outside = sqrt(max(qx, 0f).pow(2) + max(qy, 0f).pow(2))
            return outside + min(max(qx, qy), 0f) - r
        }

        /** Находит t, при котором точка (dx·t, dy·t) лежит на границе скруглённого прямоугольника. */
        private fun rayRoundRect(dx: Float, dy: Float, bx: Float, by: Float, r: Float): Float {
            var lo = 0f
            var hi = 1.5f
            repeat(18) {
                val mid = (lo + hi) / 2f
                if (sdf(dx * mid, dy * mid, bx, by, r) > 0f) hi = mid else lo = mid
            }
            return (lo + hi) / 2f
        }
    }
}

/** Морф между двумя полярными формами. */
class PolarMorphShape(
    private val from: KeyShape,
    private val to: KeyShape,
    private val progress: Float,
    private val rotation: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        for (i in 0 until SAMPLES) {
            val t = (2 * PI * i / SAMPLES).toFloat()
            val a = from.radius(t - rotation)
            val r = a + (to.radius(t - rotation) - a) * progress
            val x = cx + cos(t) * r * cx
            val y = cy + sin(t) * r * cy
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?) = other is PolarMorphShape && other.from == from && other.to == to &&
        other.progress == progress && other.rotation == rotation

    override fun hashCode(): Int = ((from.hashCode() * 31 + to.hashCode()) * 31 + progress.hashCode()) * 31 + rotation.hashCode()
}
