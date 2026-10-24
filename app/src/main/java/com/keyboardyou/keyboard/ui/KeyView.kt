package com.keyboardyou.keyboard.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keyboardyou.data.AnimationLevel
import com.keyboardyou.keyboard.EnterAction
import com.keyboardyou.keyboard.Key
import com.keyboardyou.keyboard.KeyType
import com.keyboardyou.keyboard.KeyboardController
import com.keyboardyou.keyboard.Mode
import com.keyboardyou.keyboard.ShiftState
import com.keyboardyou.theme.MorphShape
import com.keyboardyou.theme.Motion
import androidx.compose.ui.text.font.FontWeight
import com.keyboardyou.theme.PolarShape
import kotlin.math.abs

private enum class KeyRole { NORMAL, SPECIAL, ACCENT, ACTION }

@Composable
fun KeyView(
    key: Key,
    controller: KeyboardController,
    modifier: Modifier = Modifier,
    padStart: Float = 0f,
    padEnd: Float = 0f,
    row: Int = -1,
    index: Int = -1,
) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    var pressed by remember { mutableStateOf(false) }
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var touchCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val anim = settings.animations

    val role = when (key.type) {
        KeyType.CHAR, KeyType.COMMA, KeyType.PERIOD, KeyType.SPACE -> KeyRole.NORMAL
        KeyType.ENTER -> KeyRole.ACTION
        KeyType.SHIFT -> if (controller.shift != ShiftState.OFF) KeyRole.ACCENT else KeyRole.SPECIAL
        KeyType.EMOJI -> KeyRole.SPECIAL
        else -> KeyRole.SPECIAL
    }
    val (base, fg) = when (role) {
        KeyRole.NORMAL -> palette.key to palette.keyText
        KeyRole.SPECIAL -> palette.special to palette.specialText
        KeyRole.ACCENT -> palette.accent to palette.accentText
        KeyRole.ACTION -> palette.action to palette.actionText
    }

    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = when (anim) {
            AnimationLevel.OFF -> tween(0)
            AnimationLevel.SUBTLE -> spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1400f)
            AnimationLevel.PLAYFUL -> spring(dampingRatio = 0.45f, stiffness = 700f)
        },
        label = "press",
    )
    val colorProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) tween(40) else tween(if (anim == AnimationLevel.OFF) 0 else 260),
        label = "pressColor",
    )

    // Shift во включённом состоянии перетекает в акцентную форму (M3 Expressive).
    val active by animateFloatAsState(
        if (key.type == KeyType.SHIFT && controller.shift != ShiftState.OFF) 1f else 0f,
        if (anim == AnimationLevel.OFF) tween(0) else Motion.spatial(),
        label = "active",
    )
    // Соседи нажатой клавиши расступаются, как в группе кнопок. Зоны касания не двигаются.
    val pressedHere = controller.pressedInRow[row]
    val push by animateFloatAsState(
        when {
            anim != AnimationLevel.PLAYFUL || row < 0 || pressedHere == null -> 0f
            pressedHere == index - 1 -> 1f
            pressedHere == index + 1 -> -1f
            else -> 0f
        },
        Motion.fastSpatial(),
        label = "push",
    )
    val morphable = key.type != KeyType.SPACE && anim == AnimationLevel.PLAYFUL
    val target = if (key.type == KeyType.SHIFT || key.type == KeyType.ENTER) settings.accentShape else settings.pressShape
    val corner = if (key.type == KeyType.ENTER) 1f else settings.keyCorner
    val morph = maxOf(active, if (morphable) progress * 0.85f else 0f).coerceIn(0f, 1.15f)
    val shape = when {
        key.type == KeyType.EMOJI -> PolarShape(settings.accentShape)
        morph > 0.001f -> MorphShape(corner, target, morph, rotation = active * 0.35f)
        else -> RoundedCornerShape(percent = (corner * 50).toInt())
    }
    val pressedColor = if (role == KeyRole.NORMAL) palette.pressed else lerp(base, palette.pressed, 0.6f)
    val fill = lerp(base, pressedColor, colorProgress)
    val content = lerp(fg, if (role == KeyRole.NORMAL) palette.pressedText else fg, colorProgress)

    val ctrl by rememberUpdatedState(controller)
    val longPress = settings.longPressDelay.toLong()
    val spaceCursor = settings.spaceCursor
    val density = androidx.compose.ui.platform.LocalDensity.current
    val swipeStep = with(density) { 11.dp.toPx() }
    val swipeSlop = with(density) { 14.dp.toPx() }

    // Вся ячейка (включая отступы и «распорки» по краям ряда) ловит касания —
    // так между клавишами нет мёртвых зон, а рисуется только сама клавиша.
    Box(
        modifier
            .fillMaxHeight()
            .onGloballyPositioned { touchCoords = it }
            .pointerInput(key, longPress, spaceCursor, swipeStep) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    pressed = true
                    val c = ctrl
                    if (row >= 0) c.pressedInRow[row] = index
                    c.onKeyDown(key, c.boundsOf(coords))
                    try {
                        when (key.type) {
                            KeyType.BACKSPACE -> handleBackspace(down.id, c)
                            KeyType.SPACE -> handleSpace(down, c, longPress, spaceCursor, swipeStep, swipeSlop)
                            else -> handleRegular(down.id, key, c, longPress, { c.boundsOf(touchCoords).left }) { c.boundsOf(coords) }
                        }
                    } finally {
                        pressed = false
                        if (row >= 0 && c.pressedInRow[row] == index) c.pressedInRow.remove(row)
                        c.onKeyUp()
                    }
                }
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            if (padStart > 0.001f) Spacer(Modifier.weight(padStart))
            Box(
                Modifier
                    .weight(key.width)
                    .fillMaxHeight()
                    .padding(settings.keyGap.dp / 2)
                    .onGloballyPositioned { coords = it }
                    .graphicsLayer {
                        when (anim) {
                            AnimationLevel.OFF -> Unit
                            AnimationLevel.SUBTLE -> {
                                scaleX = 1f - 0.04f * progress
                                scaleY = 1f - 0.04f * progress
                            }
                            AnimationLevel.PLAYFUL -> {
                                // «Сжатие и растяжение»: нажатая клавиша шире и ниже.
                                val stretch = if (key.type == KeyType.SPACE) 0.02f else 0.09f
                                scaleX = 1f + stretch * progress - 0.05f * kotlin.math.abs(push)
                                scaleY = 1f - 0.08f * progress
                                translationX = push * 3.dp.toPx()
                            }
                        }
                        rotationZ = if (anim == AnimationLevel.PLAYFUL && key.type == KeyType.EMOJI) progress * 25f else 0f
                    }
                    .keySurface(fill, shape, palette, settings.keyBorders, glow = if (palette.glass) colorProgress else 0f),
                contentAlignment = Alignment.Center,
            ) {
                KeyContent(key, controller, content, if (anim == AnimationLevel.OFF) 0f else progress)
            }
            if (padEnd > 0.001f) Spacer(Modifier.weight(padEnd))
        }
    }
}

/** Ждём отпускания конкретного пальца. true — отпущен, false — жест отменён. */
private suspend fun AwaitPointerEventScope.awaitRelease(
    id: PointerId,
    onMove: (PointerInputChange) -> Unit = {},
): Boolean {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == id } ?: return false
        if (!change.pressed) {
            change.consume()
            return true
        }
        if (change.position != change.previousPosition) onMove(change)
        change.consume()
    }
}

private suspend fun AwaitPointerEventScope.handleRegular(
    id: PointerId,
    key: Key,
    c: KeyboardController,
    longPress: Long,
    touchLeft: () -> Float,
    bounds: () -> androidx.compose.ui.geometry.Rect,
) {
    val released = withTimeoutOrNull(longPress) { awaitRelease(id) }
    if (released != null) {
        if (released) c.onKeyTap(key)
        return
    }
    if (c.onKeyLongPress(key, bounds())) {
        if (c.altPopup != null) {
            val left = touchLeft()
            val ok = awaitRelease(id) { change -> c.onAltMove(left + change.position.x) }
            if (ok) c.onAltRelease() else c.altPopup = null
        } else {
            awaitRelease(id)
        }
    } else {
        if (awaitRelease(id)) c.onKeyTap(key)
    }
}

private suspend fun AwaitPointerEventScope.handleBackspace(id: PointerId, c: KeyboardController) {
    c.onKeyTap(Key("del", KeyType.BACKSPACE))
    val released = withTimeoutOrNull(380) { awaitRelease(id) }
    if (released != null) return
    var interval = 70L
    while (true) {
        c.onBackspaceRepeat()
        val r = withTimeoutOrNull(interval) { awaitRelease(id) }
        if (r != null) return
        interval = (interval - 4).coerceAtLeast(28)
    }
}

private const val RELEASED = 0
private const val CANCELLED = 1
private const val SWIPE = 2

private suspend fun AwaitPointerEventScope.awaitReleaseOrSwipe(down: PointerInputChange, slop: Float, swipe: Boolean): Int {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: return CANCELLED
        if (!change.pressed) {
            change.consume()
            return RELEASED
        }
        change.consume()
        if (swipe && abs(change.position.x - down.position.x) > slop) return SWIPE
    }
}

private suspend fun AwaitPointerEventScope.handleSpace(
    down: PointerInputChange,
    c: KeyboardController,
    longPress: Long,
    spaceCursor: Boolean,
    step: Float,
    slop: Float,
) {
    val result = withTimeoutOrNull(longPress + 220) { awaitReleaseOrSwipe(down, slop, spaceCursor) }
    when (result) {
        RELEASED -> c.onKeyTap(Key("space", KeyType.SPACE))
        CANCELLED -> Unit
        null -> {
            // Долгое нажатие на пробел — следующий язык.
            c.onKeyLongPress(Key("space", KeyType.SPACE), androidx.compose.ui.geometry.Rect.Zero)
            awaitRelease(down.id)
        }
        SWIPE -> {
            // Свайп по пробелу двигает курсор.
            c.onKeyUp()
            var anchorX = down.position.x
            awaitRelease(down.id) { change ->
                val steps = ((change.position.x - anchorX) / step).toInt()
                if (steps != 0) {
                    c.onSpaceSwipe(steps)
                    anchorX += steps * step
                }
            }
        }
    }
}

@Composable
private fun KeyContent(key: Key, controller: KeyboardController, color: Color, press: Float = 0f) {
    val textStyle = LocalKeyTextStyle.current
    val settings = LocalKbSettings.current
    val palette = LocalPalette.current
    when (key.type) {
        KeyType.CHAR, KeyType.COMMA, KeyType.PERIOD -> {
            Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                // Вариативный шрифт: при нажатии буква «наливается» жирностью.
                val baseWeight = textStyle.fontWeight?.weight ?: 700
                val weight = (baseWeight + (900 - baseWeight) * press).toInt().coerceIn(100, 1000)
                Text(
                    controller.displayLabel(key),
                    style = textStyle.copy(fontWeight = FontWeight(weight)),
                    color = color,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            val hint = key.hint
            if (hint != null && controller.mode == Mode.ALPHA) {
                Box(Modifier.fillMaxHeight().padding(top = 3.dp, end = 5.dp), contentAlignment = Alignment.TopEnd) {
                    Text(
                        hint,
                        style = textStyle.copy(fontSize = 10.sp * settings.fontScale),
                        color = palette.keyHint,
                    )
                }
            }
        }
        KeyType.SHIFT -> ShiftIcon(controller.shift, color)
        KeyType.BACKSPACE -> KeyIcon(Icons.AutoMirrored.Rounded.Backspace, color)
        KeyType.EMOJI -> KeyIcon(Icons.Rounded.EmojiEmotions, color)
        KeyType.LANGUAGE -> KeyIcon(Icons.Rounded.Language, color)
        KeyType.ENTER -> {
            val icon = when (controller.enterAction) {
                EnterAction.NEWLINE -> Icons.AutoMirrored.Rounded.KeyboardReturn
                EnterAction.SEARCH -> Icons.Rounded.Search
                EnterAction.SEND -> Icons.AutoMirrored.Rounded.Send
                EnterAction.GO, EnterAction.NEXT -> Icons.AutoMirrored.Rounded.ArrowForward
                EnterAction.DONE -> Icons.Rounded.Check
                EnterAction.PREVIOUS -> Icons.AutoMirrored.Rounded.ArrowBack
            }
            AnimatedContent(
                targetState = if (controller.translatorOpen) Icons.Rounded.Check else icon,
                transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith (fadeOut() + slideOutVertically { -it / 2 }) },
                label = "enterIcon",
            ) { KeyIcon(it, color) }
        }
        KeyType.SPACE -> {
            val label = if (controller.mode == Mode.ALPHA) controller.language.name else ""
            AnimatedContent(
                targetState = controller.languageFlash to label,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically { it }) togetherWith (fadeOut(tween(160)) + slideOutVertically { -it })
                },
                label = "spaceLabel",
            ) { (_, l) ->
                Text(l, style = textStyle.copy(fontSize = 13.sp * settings.fontScale), color = color.copy(alpha = 0.75f), maxLines = 1)
            }
        }
        else -> Text(
            key.label,
            style = textStyle.copy(fontSize = 15.sp * settings.fontScale),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
fun KeyIcon(icon: ImageVector, color: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size))
}

/** Своя иконка Shift: контур, заливка и заливка с подчёркиванием для Caps Lock. */
@Composable
fun ShiftIcon(state: ShiftState, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            lineTo(w * 0.92f, h * 0.52f)
            lineTo(w * 0.68f, h * 0.52f)
            lineTo(w * 0.68f, if (state == ShiftState.LOCKED) h * 0.72f else h * 0.86f)
            lineTo(w * 0.32f, if (state == ShiftState.LOCKED) h * 0.72f else h * 0.86f)
            lineTo(w * 0.32f, h * 0.52f)
            lineTo(w * 0.08f, h * 0.52f)
            close()
        }
        if (state == ShiftState.OFF) {
            drawPath(path, color, style = Stroke(width = 2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
        } else {
            drawPath(path, color)
            drawPath(path, color, style = Stroke(width = 2.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
        if (state == ShiftState.LOCKED) {
            drawRoundRect(
                color,
                topLeft = Offset(w * 0.3f, h * 0.82f),
                size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.1f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.05f),
            )
        }
    }
}
