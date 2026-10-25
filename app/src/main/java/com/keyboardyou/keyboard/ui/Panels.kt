package com.keyboardyou.keyboard.ui

import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keyboardyou.data.ClipboardRepository
import com.keyboardyou.data.EmojiData
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.rounded.Search
import com.keyboardyou.data.EmojiStore
import com.keyboardyou.keyboard.Key
import com.keyboardyou.keyboard.KeyType
import com.keyboardyou.keyboard.KeyboardController
import com.keyboardyou.keyboard.PickerTarget
import com.keyboardyou.theme.PolarShape
import com.keyboardyou.translate.TranslatorEngine
import kotlinx.coroutines.launch

// ---------------- Общие элементы ----------------

@Composable
fun PanelButton(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String? = null,
    active: Boolean = false,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val style = LocalKeyTextStyle.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "btn")
    val bg = when {
        accent -> palette.action
        active -> palette.accent
        else -> palette.special
    }
    val fg = when {
        accent -> palette.actionText
        active -> palette.accentText
        else -> palette.specialText
    }
    Box(
        modifier
            .padding(settings.keyGap.dp / 2)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .keySurface(bg, RoundedCornerShape(percent = (settings.keyCorner * 50).toInt().coerceAtLeast(18)), palette, settings.keyBorders)
            .clickable(interactionSource = interaction, indication = null) {
                LocalFeedback.feedback?.invoke()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(21.dp))
            if (icon != null && label != null) Spacer(Modifier.width(6.dp))
            if (label != null) Text(label, style = style.copy(fontSize = 14.sp), color = fg, maxLines = 1)
        }
    }
}

/** Простая «глобальная» ссылка на тактильный отклик для кнопок панелей. */
object LocalFeedback {
    var feedback: (() -> Unit)? = null
}

@Composable
private fun PanelHeader(title: String, controller: KeyboardController, actions: @Composable RowScope.() -> Unit = {}) {
    val palette = LocalPalette.current
    val style = LocalKeyTextStyle.current
    Row(
        Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = style.copy(fontSize = 16.sp), color = palette.keyText, modifier = Modifier.weight(1f))
        actions()
        Box(
            Modifier.size(32.dp).keySurface(palette.special, PolarShape(LocalKbSettings.current.accentShape), palette, false)
                .clickable { controller.closePanel() },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Close, null, tint = palette.specialText, modifier = Modifier.size(18.dp)) }
    }
}

// ---------------- Эмодзи ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmojiPanel(controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val style = LocalKeyTextStyle.current
    val scope = rememberCoroutineScope()
    // Недавние берём на момент открытия, чтобы сетка не прыгала под пальцем.
    val recents = remember { EmojiStore.recents.value }
    val favorites by EmojiStore.favorites.collectAsState()
    var selected by remember { mutableStateOf<String?>(null) }
    val sections = remember(recents, favorites) {
        buildList {
            if (favorites.isNotEmpty()) add(com.keyboardyou.data.EmojiCategory("⭐", "Избранное", favorites))
            if (recents.isNotEmpty()) add(com.keyboardyou.data.EmojiCategory("🕘", "Недавние", recents))
            addAll(EmojiData.categories)
        }
    }
    // Индексы заголовков в общей сетке — для переключения вкладок.
    val headerIndices = remember(sections) {
        var i = 0
        sections.map { s -> i.also { i += 1 + s.emojis.size } }
    }
    val gridState = rememberLazyGridState()
    val current by remember(headerIndices) {
        derivedStateOf { headerIndices.indexOfLast { it <= gridState.firstVisibleItemIndex }.coerceAtLeast(0) }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Поиск по словам
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .height(36.dp)
                    .keySurface(palette.key, RoundedCornerShape(50), palette, settings.keyBorders)
                    .clickable { controller.openEmojiSearch() }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = palette.keyHint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Поиск эмодзи: сердце, огонь, кот…", style = style.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = palette.keyHint, maxLines = 1)
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(42.dp),
                state = gridState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                sections.forEach { section ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "h_" + section.title) {
                        Text(
                            section.title,
                            style = style.copy(fontSize = 12.sp),
                            color = palette.keyHint,
                            modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp),
                        )
                    }
                    items(section.emojis, key = { section.title + it }) { emoji ->
                        EmojiCell(
                            emoji,
                            highlighted = emoji == selected,
                            onLongClick = {
                                controller.sink.feedback(com.keyboardyou.keyboard.FeedbackKind.SPECIAL)
                                selected = emoji
                            },
                        ) {
                            controller.commitEmoji(emoji)
                            EmojiStore.use(emoji)
                            selected = null
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
                PanelButton(Modifier.width(64.dp).fillMaxHeight(), label = "ABC") { controller.closePanel() }
                LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    itemsIndexed(sections) { index, section ->
                        val isCurrent = index == current
                        val s by animateFloatAsState(if (isCurrent) 1f else 0f, spring(dampingRatio = 0.5f), label = "tab")
                        Box(
                            Modifier
                                .size(38.dp)
                                .graphicsLayer { scaleX = 0.85f + 0.15f * s; scaleY = 0.85f + 0.15f * s }
                                .keySurface(palette.accent.copy(alpha = (s * palette.accent.alpha).coerceIn(0f, 1f)), PolarShape(settings.accentShape), palette.copy(glass = false), false)
                                .clickable { scope.launch { gridState.animateScrollToItem(headerIndices[index]) } },
                            contentAlignment = Alignment.Center,
                        ) { Text(section.icon, fontSize = 18.sp) }
                    }
                }
                PanelButton(Modifier.width(64.dp).fillMaxHeight(), icon = Icons.AutoMirrored.Rounded.Backspace) {
                    controller.onKeyTap(Key("del", KeyType.BACKSPACE))
                }
            }
        }
        // Долгое нажатие на эмодзи — панель «в избранное».
        androidx.compose.animation.AnimatedVisibility(
            visible = selected != null,
            enter = androidx.compose.animation.slideInVertically { it } + fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp, start = 8.dp, end = 8.dp),
        ) {
            val emoji = selected ?: favorites.firstOrNull() ?: ""
            val fav = emoji in favorites
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(26.dp), clip = false)
                    .keySurface(palette.popup, RoundedCornerShape(26.dp), palette, settings.keyBorders)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(emoji, fontSize = 28.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .keySurface(if (fav) palette.special else palette.accent, RoundedCornerShape(50), palette, false)
                        .clickable {
                            EmojiStore.toggleFavorite(emoji)
                            controller.sink.feedback(com.keyboardyou.keyboard.FeedbackKind.SPECIAL)
                            selected = null
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (fav) "Убрать из избранного" else "⭐ В избранное",
                        style = style.copy(fontSize = 14.sp),
                        color = if (fav) palette.specialText else palette.accentText,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier.size(38.dp).keySurface(palette.special, PolarShape(settings.accentShape), palette, false).clickable { selected = null },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, null, tint = palette.specialText, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiCell(emoji: String, highlighted: Boolean = false, onLongClick: () -> Unit = {}, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed || highlighted) 1.4f else 1f, spring(dampingRatio = 0.4f, stiffness = 600f), label = "emoji")
    Box(
        Modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (highlighted) Modifier.keySurface(palette.accent.copy(alpha = 0.35f), PolarShape(LocalKbSettings.current.accentShape), palette.copy(glass = false), false)
                else Modifier
            )
            .combinedClickable(interactionSource = interaction, indication = null, onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = 26.sp) }
}

/** Строка поиска эмодзи над буквами: печатаете — внизу сразу результаты. */
@Composable
fun EmojiSearchBar(controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val style = LocalKeyTextStyle.current
    val query = controller.emojiQuery
    val results = remember(query) { EmojiStore.search(query) }
    val recents by EmojiStore.recents.collectAsState()
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .keySurface(palette.key, RoundedCornerShape(50), palette, true)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (query.isEmpty()) "Ищите словами: смех, кот, огонь…" else query + "▏",
                    style = style.copy(fontSize = 15.sp, fontWeight = if (query.isEmpty()) FontWeight.Medium else FontWeight.Bold),
                    color = if (query.isEmpty()) palette.keyHint else palette.keyText,
                    maxLines = 1,
                )
            }
            Box(
                Modifier.padding(start = 6.dp).size(36.dp)
                    .keySurface(palette.special, PolarShape(settings.accentShape), palette, false)
                    .clickable { controller.closeEmojiSearch() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, null, tint = palette.specialText, modifier = Modifier.size(18.dp)) }
        }
        val shown = if (query.isBlank()) recents else results
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterStart) {
            if (shown.isEmpty()) {
                Text(
                    if (query.isBlank()) "Начните печатать" else "Ничего не нашлось 🤷",
                    style = style.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                    color = palette.keyHint,
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else {
                LazyRow(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    items(shown, key = { it }) { emoji ->
                        Box(Modifier.size(46.dp).animateItem()) {
                            EmojiCell(emoji) { controller.commitSearchedEmoji(emoji) }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Буфер обмена ----------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardPanel(controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val style = LocalKeyTextStyle.current
    val items by ClipboardRepository.items.collectAsState()
    Column(Modifier.fillMaxSize()) {
        PanelHeader("Буфер обмена", controller) {
            if (items.any { !it.pinned }) {
                Box(
                    Modifier.padding(end = 6.dp).height(32.dp)
                        .keySurface(palette.special, RoundedCornerShape(50), palette, false)
                        .clickable { ClipboardRepository.clearUnpinned() }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ClearAll, null, tint = palette.specialText, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Очистить", style = style.copy(fontSize = 13.sp), color = palette.specialText)
                    }
                }
            }
        }
        if (!settings.clipboardHistory) {
            EmptyState(Icons.Rounded.ContentPaste, "История буфера выключена", "Включите её в приложении Keyboard You")
            return@Column
        }
        if (items.isEmpty()) {
            EmptyState(Icons.Rounded.ContentPaste, "Здесь пока пусто", "Скопируйте текст — он появится тут")
            return@Column
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            verticalItemSpacing = 6.dp,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(items, key = { it.id }) { item ->
                Column(
                    Modifier
                        .animateItem()
                        .keySurface(if (item.pinned) palette.special else palette.key, RoundedCornerShape(18.dp), palette, settings.keyBorders)
                        .combinedClickable(onClick = { controller.commitClip(item.text) }, onLongClick = { ClipboardRepository.togglePin(item.id) })
                        .padding(10.dp)
                        .animateContentSize(),
                ) {
                    Text(
                        item.text,
                        style = style.copy(fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                        color = palette.keyText,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
                        SmallIcon(Icons.Rounded.PushPin, if (item.pinned) palette.accent else palette.keyHint) { ClipboardRepository.togglePin(item.id) }
                        Spacer(Modifier.width(8.dp))
                        SmallIcon(Icons.Rounded.DeleteOutline, palette.keyHint) { ClipboardRepository.remove(item.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallIcon(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(26.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val style = LocalKeyTextStyle.current
    val transition = rememberInfiniteTransition(label = "empty")
    val rot by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart), label = "rot")
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            Modifier.size(64.dp).graphicsLayer { rotationZ = rot }.keySurface(palette.accent, PolarShape(settings.accentShape), palette, false),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = palette.accentText, modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = -rot }) }
        Spacer(Modifier.height(10.dp))
        Text(title, style = style.copy(fontSize = 16.sp), color = palette.keyText)
        Text(subtitle, style = style.copy(fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal), color = palette.keyHint, textAlign = TextAlign.Center)
    }
}

// ---------------- Редактирование / курсор ----------------

@Composable
fun EditPanel(controller: KeyboardController) {
    val row = Modifier.fillMaxWidth()
    Column(Modifier.fillMaxSize()) {
        Row(row.weight(1f)) {
            PanelButton(Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.SelectAll, label = "Всё") { controller.sink.contextAction(android.R.id.selectAll) }
            PanelButton(Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.KeyboardArrowUp) { controller.editKey(KeyEvent.KEYCODE_DPAD_UP) }
            PanelButton(Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.ContentCopy, label = "Копир.") { controller.sink.contextAction(android.R.id.copy) }
        }
        Row(row.weight(1f)) {
            PanelButton(Modifier.weight(1f).fillMaxHeight(), icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft) { controller.editKey(KeyEvent.KEYCODE_DPAD_LEFT) }
            PanelButton(Modifier.weight(1f).fillMaxHeight(), label = if (controller.selectMode) "Выделение ✓" else "Выделить", active = controller.selectMode) {
                controller.selectMode = !controller.selectMode
            }
            PanelButton(Modifier.weight(1f).fillMaxHeight(), icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight) { controller.editKey(KeyEvent.KEYCODE_DPAD_RIGHT) }
        }
        Row(row.weight(1f)) {
            PanelButton(Modifier.weight(1f).fillMaxHeight(), label = "⇤ Начало") { controller.editKey(KeyEvent.KEYCODE_MOVE_HOME) }
            PanelButton(Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.KeyboardArrowDown) { controller.editKey(KeyEvent.KEYCODE_DPAD_DOWN) }
            PanelButton(Modifier.weight(1f).fillMaxHeight(), label = "Конец ⇥") { controller.editKey(KeyEvent.KEYCODE_MOVE_END) }
        }
        Row(row.weight(1f)) {
            PanelButton(Modifier.weight(1f).fillMaxHeight(), label = "ABC") { controller.closePanel() }
            PanelButton(Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.ContentCut) { controller.sink.contextAction(android.R.id.cut) }
            PanelButton(Modifier.weight(1f).fillMaxHeight(), icon = Icons.Rounded.ContentPaste) { controller.sink.contextAction(android.R.id.paste) }
            PanelButton(Modifier.weight(1f).fillMaxHeight(), icon = Icons.AutoMirrored.Rounded.Backspace) { controller.onKeyTap(Key("del", KeyType.BACKSPACE)) }
        }
    }
}

// ---------------- Переводчик ----------------

@Composable
fun TranslatorBar(controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val style = LocalKeyTextStyle.current
    val source = TranslatorEngine.lang(settings.translateSource)
    val target = TranslatorEngine.lang(settings.translateTarget)
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            LangChip(
                if (source.code == "auto" && controller.translatorDetected != null)
                    "✨ " + TranslatorEngine.lang(controller.translatorDetected!!).name
                else source.flag + " " + source.name,
                Modifier.weight(1f),
            ) { controller.openLanguagePicker(PickerTarget.SOURCE) }
            val rot by animateFloatAsState(if (settings.translateTarget.hashCode() % 2 == 0) 0f else 180f, spring(dampingRatio = 0.5f), label = "swap")
            Box(
                Modifier.padding(horizontal = 6.dp).size(34.dp)
                    .graphicsLayer { rotationZ = rot }
                    .keySurface(palette.accent, PolarShape(settings.accentShape), palette, false)
                    .clickable { controller.swapTranslateLanguages() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.SwapHoriz, null, tint = palette.accentText, modifier = Modifier.size(18.dp)) }
            LangChip(target.flag + " " + target.name, Modifier.weight(1f)) { controller.openLanguagePicker(PickerTarget.TARGET) }
            Box(
                Modifier.padding(start = 6.dp).size(34.dp)
                    .keySurface(palette.special, PolarShape(settings.accentShape), palette, false)
                    .clickable { controller.toggleTranslator() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Close, null, tint = palette.specialText, modifier = Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().height(58.dp)
                .keySurface(palette.key, RoundedCornerShape(18.dp), palette, settings.keyBorders)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    controller.translatorInput.takeLast(60).ifEmpty { "Печатайте здесь…" } + if (controller.translatorInput.isNotEmpty()) "▏" else "",
                    style = style.copy(fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
                    color = if (controller.translatorInput.isEmpty()) palette.keyHint else palette.keyText.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                AnimatedContent(
                    targetState = when {
                        controller.translatorError != null -> "⚠ " + controller.translatorError
                        controller.translatorBusy && controller.translatorOutput.isEmpty() -> "Перевожу… (первый раз скачивается модель)"
                        else -> controller.translatorOutput
                    },
                    transitionSpec = { fadeIn(tween(160)) + scaleIn(initialScale = 0.98f) togetherWith fadeOut(tween(100)) },
                    label = "tr",
                ) { text ->
                    Text(
                        text,
                        style = style.copy(fontSize = 16.sp),
                        color = palette.keyText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(if (controller.translatorBusy) 0.6f else 1f),
                    )
                }
            }
            if (controller.translatorBusy) {
                com.keyboardyou.theme.MorphLoader(palette.accent, Modifier.padding(end = 8.dp), size = 26.dp)
            }
            Column(horizontalAlignment = Alignment.End) {
                MiniAction("Вставить", palette.action, palette.actionText) { controller.insertTranslation() }
                Spacer(Modifier.height(4.dp))
                MiniAction("Всё поле", palette.special, palette.specialText) { controller.translateWholeField() }
            }
        }
    }
}

@Composable
private fun LangChip(text: String, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    Box(
        modifier.fillMaxHeight()
            .keySurface(palette.chip, RoundedCornerShape(50), palette, settings.keyBorders)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LocalKeyTextStyle.current.copy(fontSize = 13.sp), color = palette.chipText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MiniAction(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier.height(22.dp)
            .keySurface(bg, RoundedCornerShape(50), palette, false)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = LocalKeyTextStyle.current.copy(fontSize = 11.sp), color = fg, maxLines = 1) }
}

@Composable
fun LanguagePickerPanel(controller: KeyboardController) {
    val palette = LocalPalette.current
    val settings = LocalKbSettings.current
    val selectedCode = if (controller.pickerTarget == PickerTarget.SOURCE) settings.translateSource else settings.translateTarget
    val options = remember(controller.pickerTarget) {
        if (controller.pickerTarget == PickerTarget.SOURCE) listOf(TranslatorEngine.auto) + TranslatorEngine.languages
        else TranslatorEngine.languages
    }
    Column(Modifier.fillMaxSize()) {
        PanelHeader(if (controller.pickerTarget == PickerTarget.SOURCE) "Переводить с" else "Переводить на", controller)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(options, key = { it.code }) { lang ->
                val selected = lang.code == selectedCode
                Row(
                    Modifier.height(40.dp)
                        .keySurface(if (selected) palette.accent else palette.key, RoundedCornerShape(50), palette, settings.keyBorders)
                        .clickable { controller.pickTranslateLanguage(lang.code) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(lang.flag, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        lang.name,
                        style = LocalKeyTextStyle.current.copy(fontSize = 14.sp),
                        color = if (selected) palette.accentText else palette.keyText,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

