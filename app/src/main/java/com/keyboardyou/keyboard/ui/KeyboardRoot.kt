package com.keyboardyou.keyboard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.keyboardyou.data.AnimationLevel
import com.keyboardyou.data.KeyboardSettings
import com.keyboardyou.keyboard.KeyboardController
import com.keyboardyou.keyboard.Mode
import com.keyboardyou.keyboard.Panel

/**
 * Корневой компонент клавиатуры. Используется и в IME-сервисе, и в живом превью настроек.
 */
@Composable
fun KeyboardRoot(
    controller: KeyboardController,
    settings: KeyboardSettings,
    modifier: Modifier = Modifier,
    backgroundAlpha: Float = 1f,
    insetNavigationBar: Boolean = true,
) {
    KeyboardTheme(settings) {
        val palette = LocalPalette.current
        Box(
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { controller.rootCoordinates = it }
        ) {
            KeyboardBackground(palette, settings.backgroundStyle, settings.animations, backgroundAlpha, Modifier.matchParentSize())
            if (!settings.effectOnTop) {
                BackgroundEffectLayer(
                    settings.effect, palette, settings.effectIntensity, Modifier.matchParentSize(),
                    animate = settings.animations != AnimationLevel.OFF,
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .then(if (insetNavigationBar) Modifier.navigationBarsPadding() else Modifier)
                    .padding(horizontal = 3.dp)
                    .padding(bottom = 4.dp)
            ) {
                AnimatedVisibility(
                    visible = controller.translatorOpen,
                    enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    TranslatorBar(controller)
                }
                SuggestionStrip(controller)
                val areaHeight = keysAreaHeight(settings)
                AnimatedContent(
                    targetState = controller.panel,
                    transitionSpec = { panelTransition(settings.animations, initialState, targetState) },
                    label = "panel",
                ) { panel ->
                    Box(Modifier.fillMaxWidth().height(areaHeight)) {
                        when (panel) {
                            Panel.KEYBOARD -> KeysArea(controller)
                            Panel.EMOJI -> EmojiPanel(controller)
                            Panel.CLIPBOARD -> ClipboardPanel(controller)
                            Panel.EDIT -> EditPanel(controller)
                            Panel.LANG_PICKER -> LanguagePickerPanel(controller)
                        }
                    }
                }
            }
            if (settings.effectOnTop) {
                BackgroundEffectLayer(
                    settings.effect, palette, settings.effectIntensity, Modifier.matchParentSize(),
                    animate = settings.animations != AnimationLevel.OFF,
                )
            }
            KeyOverlays(controller)
        }
    }
}

fun keysAreaHeight(settings: KeyboardSettings): Dp {
    val rows = if (settings.showNumberRow) 5 else 4
    return (settings.keyHeight * rows).dp
}

private fun panelTransition(level: AnimationLevel, from: Panel, to: Panel): ContentTransform {
    if (level == AnimationLevel.OFF) return fadeIn(tween(0)) togetherWith fadeOut(tween(0))
    val forward = to.ordinal > from.ordinal
    val spec = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
    return if (to == Panel.KEYBOARD || from == Panel.KEYBOARD) {
        (slideInVertically(spec) { if (to == Panel.KEYBOARD) -it / 6 else it / 4 } + fadeIn(tween(220)) +
            scaleIn(initialScale = 0.96f)) togetherWith
            (fadeOut(tween(120)) + scaleOut(targetScale = 0.98f))
    } else {
        (slideInHorizontally(spec) { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
            (slideOutHorizontally(spec) { if (forward) -it / 3 else it / 3 } + fadeOut())
    }
}

@Composable
fun KeysArea(controller: KeyboardController) {
    val settings = LocalKbSettings.current
    val layout = controller.layout()
    AnimatedContent(
        targetState = Triple(controller.mode, controller.language.code, layout.rows.size),
        transitionSpec = {
            if (settings.animations == AnimationLevel.OFF) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else {
                val toAlpha = targetState.first == Mode.ALPHA
                (fadeIn(tween(180)) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 900f)) { if (toAlpha) -it / 12 else it / 12 }) togetherWith
                    fadeOut(tween(100))
            }
        },
        label = "layout",
    ) { _ ->
        val rows = controller.layout().rows
        Column(Modifier.fillMaxWidth()) {
            rows.forEachIndexed { rowIndex, row ->
                val total = row.sumOf { it.width.toDouble() }.toFloat()
                // Ряды короче основного центрируем «распорками», как на настоящих клавиатурах.
                val maxUnits = rows.maxOf { r -> r.sumOf { it.width.toDouble() } }.toFloat()
                val side = ((maxUnits - total) / 2f).coerceAtLeast(0f)
                // Короткие ряды центрируем, но крайние клавиши забирают пустое место
                // себе как зону касания — промахнуться мимо них нельзя.
                // Ряды въезжают снизу каскадом при появлении клавиатуры.
                val enter = remember { androidx.compose.animation.core.Animatable(1f) }
                LaunchedEffect(controller.appearTick) {
                    if (controller.appearTick > 0 && settings.animations != AnimationLevel.OFF) {
                        enter.snapTo(0f)
                        kotlinx.coroutines.delay(rowIndex * 45L)
                        enter.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.62f, stiffness = 420f))
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(settings.keyHeight.dp)
                        .graphicsLayer {
                            val v = enter.value
                            translationY = (1f - v) * 36.dp.toPx()
                            alpha = v.coerceIn(0f, 1f)
                            scaleX = 0.92f + 0.08f * v
                        }
                ) {
                    row.forEachIndexed { i, key ->
                        val ps = if (i == 0) side else 0f
                        val pe = if (i == row.lastIndex) side else 0f
                        KeyView(key, controller, Modifier.weight(key.width + ps + pe), padStart = ps, padEnd = pe, row = rowIndex, index = i)
                    }
                }
            }
        }
    }
}
