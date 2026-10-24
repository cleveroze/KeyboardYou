package com.keyboardyou.keyboard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keyboardyou.data.AnimationLevel
import com.keyboardyou.keyboard.KeyboardController
import com.keyboardyou.keyboard.Panel
import com.keyboardyou.keyboard.Suggestion
import com.keyboardyou.theme.Motion
import com.keyboardyou.theme.PolarShape
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing

private data class ToolItem(val icon: ImageVector, val active: Boolean, val onClick: () -> Unit)

@Composable
fun SuggestionStrip(controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val showToolbar = controller.toolbarOpen || controller.suggestions.isEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val rotation by animateFloatAsState(
            if (controller.toolbarOpen) 135f else 0f,
            spring(dampingRatio = 0.5f, stiffness = 300f),
            label = "appsRotation",
        )
        StripButton(
            icon = if (controller.toolbarOpen) Icons.Rounded.Close else Icons.Rounded.Apps,
            background = if (controller.toolbarOpen) palette.chipActive else palette.chip,
            tint = if (controller.toolbarOpen) palette.chipActiveText else palette.chipText,
            rotation = rotation,
        ) { controller.toolbarOpen = !controller.toolbarOpen }

        Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = showToolbar,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically { if (targetState) it else -it }) togetherWith
                        (fadeOut(tween(120)) + slideOutVertically { if (targetState) -it else it })
                },
                label = "strip",
            ) { toolbar ->
                if (toolbar) Toolbar(controller) else Suggestions(controller)
            }
        }

        StripButton(
            icon = Icons.Rounded.Mic,
            background = palette.chip,
            tint = palette.chipText,
            rotation = 0f,
        ) { controller.sink.switchToVoice() }
    }
}

@Composable
private fun StripButton(icon: ImageVector, background: Color, tint: Color, rotation: Float, onClick: () -> Unit) {
    val settings = LocalKbSettings.current
    val palette = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.82f else 1f, Motion.fastSpatial(), label = "stripScale")
    // Акцентные «печенья» медленно вращаются, как в M3 Expressive.
    val spin = if (settings.animations == AnimationLevel.PLAYFUL) {
        val t = rememberInfiniteTransition(label = "spin")
        val v by t.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "spinV")
        v
    } else 0f
    Box(
        Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = spin + if (settings.animations == AnimationLevel.PLAYFUL) rotation / 3f else 0f
            }
            .keySurface(background, PolarShape(settings.accentShape), palette, settings.keyBorders)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation - rotation / 3f - spin })
    }
}

@Composable
private fun Toolbar(controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val items = listOf(
        ToolItem(Icons.Rounded.ContentPaste, controller.panel == Panel.CLIPBOARD) { controller.openPanel(Panel.CLIPBOARD) },
        ToolItem(Icons.Rounded.Translate, controller.translatorOpen) { controller.toggleTranslator() },
        ToolItem(Icons.Rounded.EmojiEmotions, controller.panel == Panel.EMOJI) { controller.openPanel(Panel.EMOJI) },
        ToolItem(Icons.Rounded.OpenWith, controller.panel == Panel.EDIT) { controller.openPanel(Panel.EDIT) },
        ToolItem(Icons.Rounded.Palette, false) { controller.cycleTheme() },
        ToolItem(Icons.Rounded.Settings, false) { controller.sink.openSettings() },
        ToolItem(Icons.Rounded.KeyboardHide, false) { controller.sink.hideKeyboard() },
    )
    LazyRow(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        itemsIndexed(items) { index, item ->
            val state = remember { MutableTransitionState(settings.animations == AnimationLevel.OFF).apply { targetState = true } }
            AnimatedVisibility(
                visibleState = state,
                enter = fadeIn(tween(220, delayMillis = index * 35)) +
                    scaleIn(spring(dampingRatio = 0.5f, stiffness = 500f), initialScale = 0.4f),
            ) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                // Связанная группа кнопок M3 Expressive: внутренние углы маленькие,
                // активная или нажатая кнопка становится пилюлей и раздаётся вширь.
                val round = item.active || pressed
                val first = index == 0
                val last = index == items.lastIndex
                val inner by animateDpAsState(if (round) 18.dp else 6.dp, Motion.fastSpatial(), label = "inner")
                val width by animateDpAsState(if (round) 58.dp else 44.dp, Motion.fastSpatial(), label = "toolWidth")
                Box(
                    Modifier
                        .size(width = width, height = 36.dp)
                        .keySurface(
                            if (item.active) palette.chipActive else palette.chip.copy(alpha = if (palette.glass) palette.chip.alpha else 0.75f),
                            RoundedCornerShape(
                                topStart = if (first) 18.dp else inner,
                                bottomStart = if (first) 18.dp else inner,
                                topEnd = if (last) 18.dp else inner,
                                bottomEnd = if (last) 18.dp else inner,
                            ),
                            palette,
                            settings.keyBorders,
                        )
                        .clickable(interactionSource = interaction, indication = null) {
                            controller.sink.feedback(com.keyboardyou.keyboard.FeedbackKind.SPECIAL)
                            item.onClick()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(item.icon, null, tint = if (item.active) palette.chipActiveText else palette.chipText, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun Suggestions(controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val textStyle = LocalKeyTextStyle.current
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        controller.suggestions.toList().forEach { s ->
            val primary = s is Suggestion.Word && s.primary
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.45f), label = "chipScale")
            Box(
                Modifier
                    .weight(if (s is Suggestion.Clip) 2f else 1f)
                    .fillMaxHeight()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .keySurface(if (primary) palette.chipActive else palette.chip, RoundedCornerShape(50), palette, settings.keyBorders)
                    .clickable(interactionSource = interaction, indication = null) { controller.pickSuggestion(s) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = s.text,
                    transitionSpec = { (fadeIn(tween(150)) + slideInVertically { it / 2 }) togetherWith fadeOut(tween(90)) },
                    label = "chip",
                ) { text ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (s is Suggestion.Clip) {
                            Icon(Icons.Rounded.ContentPaste, null, tint = palette.chipText, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            when (s) {
                                is Suggestion.Typed -> "«$text»"
                                is Suggestion.Revert -> "↶ $text"
                                is Suggestion.Clip -> text.replace('\n', ' ')
                                else -> text
                            },
                            style = textStyle.copy(fontSize = 15.sp * settings.fontScale),
                            color = if (primary) palette.chipActiveText else palette.chipText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
