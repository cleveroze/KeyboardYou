package com.keyboardyou.keyboard.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.keyboardyou.data.AnimationLevel
import com.keyboardyou.keyboard.AltPopup
import com.keyboardyou.keyboard.KeyPreview
import com.keyboardyou.keyboard.KeyboardController
import com.keyboardyou.theme.MorphShape
import com.keyboardyou.theme.PolarShape
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun BoxScope.KeyOverlays(controller: KeyboardController) {
    Box(Modifier.matchParentSize()) {
        controller.bursts.toList().forEach { b -> key("b" + b.id) { BurstView(b, controller) } }
        controller.preview?.let { p -> key(p.id) { PreviewBubble(p, controller) } }
        controller.altPopup?.let { AltPopupView(it, controller) }
    }
}

@Composable
private fun PreviewBubble(p: KeyPreview, controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val style = LocalKeyTextStyle.current
    val density = LocalDensity.current
    val anim = settings.animations
    val scale = remember { Animatable(if (anim == AnimationLevel.OFF) 1f else 0.4f) }
    val morph = remember { Animatable(if (anim == AnimationLevel.PLAYFUL) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (anim != AnimationLevel.OFF) scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 900f))
    }
    LaunchedEffect(Unit) {
        if (anim == AnimationLevel.PLAYFUL) morph.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 500f))
    }
    val rootWidth = controller.rootCoordinates?.size?.width?.toFloat() ?: 0f
    val side = max(p.bounds.width * 1.3f, p.bounds.height * 1.12f)
    val x = (p.bounds.center.x - side / 2f).coerceIn(0f, max(0f, rootWidth - side))
    val y = (p.bounds.top - side * 0.92f).coerceAtLeast(0f)
    val shape = if (anim == AnimationLevel.PLAYFUL) MorphShape(settings.keyCorner, settings.pressShape, morph.value, rotation = (1f - morph.value) * 0.6f)
    else RoundedCornerShape(percent = 30)
    with(density) {
        Box(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(side.toDp())
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .shadow(if (palette.glass) 0.dp else 6.dp, shape, clip = false)
                .keySurface(palette.popup, shape, palette, settings.keyBorders),
            contentAlignment = Alignment.Center,
        ) {
            Text(p.label, style = style.copy(fontSize = style.fontSize * 1.35f), color = palette.popupText)
        }
    }
}

@Composable
private fun AltPopupView(p: AltPopup, controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val style = LocalKeyTextStyle.current
    val density = LocalDensity.current
    val cell = controller.popupCellWidth(p)
    val height = p.bounds.height * 1.05f
    val x = controller.popupStart(p)
    val y = (p.bounds.top - height - with(density) { 6.dp.toPx() }).coerceAtLeast(0f)
    val appear = remember { Animatable(if (settings.animations == AnimationLevel.OFF) 1f else 0.6f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 800f)) }
    with(density) {
        Row(
            Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .size(width = (cell * p.options.size).toDp(), height = height.toDp())
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                    alpha = appear.value.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(((p.bounds.center.x - x) / (cell * p.options.size)).coerceIn(0f, 1f), 1f)
                }
                .shadow(if (palette.glass) 0.dp else 8.dp, RoundedCornerShape(22.dp), clip = false)
                .keySurface(palette.popup, RoundedCornerShape(22.dp), palette, settings.keyBorders)
                .padding(3.dp),
        ) {
            p.options.forEachIndexed { index, option ->
                val selected = index == p.selected
                val s by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 700f), label = "alt")
                Box(
                    Modifier
                        .width((cell - 6.dp.toPx() / p.options.size).coerceAtLeast(1f).toDp())
                        .fillMaxHeight()
                        .graphicsLayer { scaleX = 0.9f + 0.1f * s; scaleY = 0.9f + 0.1f * s }
                        .keySurface(
                            palette.accent.copy(alpha = (s * palette.accent.alpha).coerceIn(0f, 1f)),
                            PolarShape(settings.pressShape),
                            palette.copy(glass = false),
                            borders = false,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(option, style = style, color = if (selected) palette.accentText else palette.popupText)
                }
            }
        }
    }
}

/** Искры-«печеньки» и кольцо-волна из нажатой клавиши. */
@Composable
private fun BurstView(b: com.keyboardyou.keyboard.Burst, controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        t.animateTo(1f, androidx.compose.animation.core.tween(520, easing = androidx.compose.animation.core.FastOutSlowInEasing))
        controller.bursts.removeAll { it.id == b.id }
    }
    val shape = settings.pressShape
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val p = t.value
        val fade = (1f - p).coerceIn(0f, 1f)
        // Кольцо-волна
        drawCircle(
            palette.accent.copy(alpha = 0.45f * fade),
            radius = 10.dp.toPx() + 26.dp.toPx() * p,
            center = androidx.compose.ui.geometry.Offset(b.x, b.y),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx() * fade + 0.5f),
        )
        // Частицы-формы
        val count = 7
        for (i in 0 until count) {
            val angle = (i * 2 * Math.PI / count + (b.seed % 100) / 100.0).toFloat()
            val dist = 14.dp.toPx() + 30.dp.toPx() * p * (0.8f + 0.4f * ((b.seed shr i) and 1))
            val cx = b.x + kotlin.math.cos(angle) * dist
            val cy = b.y + kotlin.math.sin(angle) * dist - 10.dp.toPx() * p
            val r = 4.dp.toPx() * (1f - 0.5f * p)
            val path = androidx.compose.ui.graphics.Path()
            for (k in 0 until 24) {
                val a = (2 * Math.PI * k / 24).toFloat()
                val rr = r * shape.radius(a + p * 3f)
                val x = cx + kotlin.math.cos(a) * rr
                val y = cy + kotlin.math.sin(a) * rr
                if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, (if (i % 2 == 0) palette.accent else palette.action).copy(alpha = fade))
        }
    }
}
