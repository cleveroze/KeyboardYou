package com.keyboardyou.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.floor

private val loaderShapes = listOf(
    KeyShape.COOKIE_9, KeyShape.PENTAGON, KeyShape.COOKIE_4, KeyShape.FLOWER, KeyShape.PUFFY_DIAMOND, KeyShape.COOKIE_12,
)

/** Индикатор загрузки M3 Expressive: фигуры перетекают друг в друга и вращаются. */
@Composable
fun MorphLoader(color: Color, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val transition = rememberInfiniteTransition(label = "loader")
    val t by transition.animateFloat(
        0f, loaderShapes.size.toFloat(),
        infiniteRepeatable(tween(650 * loaderShapes.size, easing = LinearEasing), RepeatMode.Restart),
        label = "t",
    )
    val rot by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "rot")
    val index = floor(t).toInt() % loaderShapes.size
    val raw = t - floor(t)
    // Пауза на каждой форме, затем быстрый пружинистый переход.
    val f = ((raw - 0.45f) / 0.55f).coerceIn(0f, 1f).let { x -> 1 - (1 - x) * (1 - x) * (1 - x) }
    val shape = PolarMorphShape(loaderShapes[index], loaderShapes[(index + 1) % loaderShapes.size], f)
    Box(
        modifier
            .size(size)
            .graphicsLayer { rotationZ = rot }
            .clip(shape)
            .background(color)
    )
}
