package com.keyboardyou.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.BorderStyle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.keyboardyou.BuildConfig
import com.keyboardyou.data.AnimationLevel
import com.keyboardyou.data.BackgroundStyle
import com.keyboardyou.data.ClipboardRepository
import com.keyboardyou.data.ColorSource
import com.keyboardyou.data.DarkMode
import com.keyboardyou.data.KeyFont
import com.keyboardyou.data.KeyboardSettings
import com.keyboardyou.data.SettingsRepository
import com.keyboardyou.data.ThemeStyle
import com.keyboardyou.data.WordRepository
import com.keyboardyou.keyboard.EnterAction
import com.keyboardyou.keyboard.FieldKind
import com.keyboardyou.keyboard.KeyboardController
import com.keyboardyou.keyboard.Layouts
import com.keyboardyou.keyboard.ui.KeyboardBackground
import com.keyboardyou.keyboard.ui.KeyboardRoot
import com.keyboardyou.theme.KeyShape
import com.keyboardyou.theme.PolarShape
import com.keyboardyou.theme.family
import com.keyboardyou.theme.paletteFor
import com.keyboardyou.theme.schemeFor
import com.keyboardyou.translate.TranslatorEngine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private fun set(block: (KeyboardSettings) -> KeyboardSettings) = SettingsRepository.update(block)

@Composable
fun SubScreen(screen: Screen, back: () -> Unit) {
    val settings by SettingsRepository.state.collectAsState()
    val withPreview = screen in setOf(Screen.COLORS, Screen.SHAPES, Screen.FONT, Screen.FEEDBACK)
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад") }
            Spacer(Modifier.width(12.dp))
            Text(screen.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        }
        if (withPreview) KeyboardPreviewCard(settings)
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            when (screen) {
                Screen.COLORS -> colorsScreen(settings)
                Screen.SHAPES -> shapesScreen(settings)
                Screen.FONT -> fontScreen(settings)
                Screen.TYPING -> typingScreen(settings)
                Screen.LANGUAGES -> languagesScreen(settings)
                Screen.FEEDBACK -> feedbackScreen(settings)
                Screen.CLIPBOARD -> clipboardScreen(settings)
                Screen.TRANSLATOR -> translatorScreen(settings)
                Screen.ABOUT -> aboutScreen()
                Screen.HOME -> Unit
            }
            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
        }
    }
}

// ---------------- Живое превью ----------------

@Composable
fun KeyboardPreviewCard(settings: KeyboardSettings) {
    val scope = rememberCoroutineScope()
    val sink = remember { PreviewSink("Привет") }
    val controller = remember {
        KeyboardController(scope, sink).also { it.onStartInput(FieldKind.TEXT, EnterAction.SEND, false, true) }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50)),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 4.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Keyboard, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    sink.text.replace('\n', ' ').takeLast(40) + "▏",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                IconButton(onClick = { sink.clear(); controller.refresh() }) { Icon(Icons.Rounded.Close, "Очистить") }
            }
        }
        Text(
            "Предпросмотр",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 6.dp),
        )
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).animateContentSize()
        ) {
            KeyboardRoot(controller, settings, insetNavigationBar = false)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---------------- Цвета ----------------

private val seeds = listOf(
    0xFF2BD9A0, 0xFF6750A4, 0xFF0B57D0, 0xFFE8546B, 0xFFFF8A3D, 0xFFF2C94C, 0xFF00A6C8, 0xFF7C4DFF, 0xFF8BC34A, 0xFFB0766A,
).map { it.toInt() }

private fun LazyListScope.colorsScreen(s: KeyboardSettings) {
    item {
        Text(
            "Стиль",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 28.dp, top = 12.dp, bottom = 4.dp),
        )
        ThemeStyleRow(s)
    }
    item {
        Text(
            "Анимированный фон",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 4.dp),
        )
        EffectRow(s)
    }
    if (s.effect != com.keyboardyou.data.BackgroundEffect.NONE) item {
        SettingsGroup {
            item {
                SliderRow("Интенсивность", s.effectIntensity, 0.4f..2f, icon = Icons.Rounded.AutoAwesome, label = { "${(it * 100).roundToInt()}%" }) { v ->
                    set { it.copy(effectIntensity = v) }
                }
            }
            item {
                SwitchRow("Поверх клавиш", "Эффект летит прямо по клавишам", Icons.Rounded.Visibility, s.effectOnTop) { v ->
                    set { it.copy(effectOnTop = v) }
                }
            }
        }
    }
    item {
        SettingsGroup("Источник цвета") {
            item {
                SegmentedRow(
                    null,
                    ColorSource.entries,
                    s.colorSource,
                    label = { if (it == ColorSource.WALLPAPER) "Из обоев" else "Свой цвет" },
                ) { v -> set { it.copy(colorSource = v) } }
            }
            if (s.colorSource == ColorSource.WALLPAPER && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                item { SettingRow("Monet недоступен", "Цвета из обоев работают на Android 12+. Используется свой цвет.", Icons.Rounded.Lightbulb) }
            }
            if (s.colorSource == ColorSource.CUSTOM) {
                item { SeedPicker(s.seedColor) { c -> set { it.copy(seedColor = c) } } }
            }
        }
    }
    item {
        SettingsGroup("Тема") { item {
            SegmentedRow(null, DarkMode.entries, s.darkMode, label = { it.title }) { v -> set { it.copy(darkMode = v) } }
        } }
    }
    item {
        SettingsGroup("Фон и поверхность") {
            item {
                SegmentedRow(null, BackgroundStyle.entries, s.backgroundStyle, label = { it.title }) { v -> set { it.copy(backgroundStyle = v) } }
            }
            item {
                SwitchRow("Контуры клавиш", "Тонкая обводка вокруг каждой клавиши", Icons.Rounded.BorderStyle, s.keyBorders) { v ->
                    set { it.copy(keyBorders = v) }
                }
            }
            if (s.themeStyle == ThemeStyle.LIQUID_GLASS) {
                item {
                    SwitchRow(
                        "Размытие под клавиатурой",
                        "Экспериментально, Android 12+: настоящее стекло поверх приложения",
                        Icons.Rounded.BlurOn,
                        s.blurBehind,
                    ) { v -> set { it.copy(blurBehind = v) } }
                }
            }
        }
    }
}

@Composable
private fun ThemeStyleRow(s: KeyboardSettings) {
    val context = LocalContext.current
    val dark = when (s.darkMode) {
        DarkMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val scheme = remember(s.colorSource, s.seedColor, dark) { schemeFor(context, s, dark) }
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(ThemeStyle.entries) { style ->
            val palette = remember(scheme, style, dark) { paletteFor(scheme, style, dark) }
            val selected = s.themeStyle == style
            val scale by animateFloatAsState(if (selected) 1f else 0.94f, spring(dampingRatio = 0.5f), label = "style")
            Column(
                Modifier
                    .width(132.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { set { it.copy(themeStyle = style) } },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(92.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp)) else Modifier)
                ) {
                    KeyboardBackground(palette, if (style == ThemeStyle.LIQUID_GLASS) BackgroundStyle.BLOBS else s.backgroundStyle, AnimationLevel.OFF)
                    Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(3) { row ->
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val count = if (row == 2) 4 else 5
                                repeat(count) { i ->
                                    val color = when {
                                        row == 2 && i == count - 1 -> palette.action
                                        row == 2 && i == 0 -> palette.accent
                                        else -> palette.key
                                    }
                                    Box(
                                        Modifier.weight(if (row == 2 && i == 1) 2f else 1f).fillMaxSize()
                                            .clip(RoundedCornerShape(50)).background(color)
                                            .then(if (palette.glass) Modifier.border(0.7.dp, palette.border, RoundedCornerShape(50)) else Modifier)
                                    )
                                }
                            }
                        }
                    }
                }
                Text(style.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp, top = 6.dp))
                Text(style.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}

@Composable
private fun SeedPicker(seed: Int, onPick: (Int) -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(seeds) { c ->
                val selected = c == seed
                val scale by animateFloatAsState(if (selected) 1.15f else 1f, spring(dampingRatio = 0.4f), label = "seed")
                Box(
                    Modifier.size(46.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = if (selected) 20f else 0f }
                        .clip(PolarShape(if (selected) KeyShape.COOKIE_9 else KeyShape.CIRCLE))
                        .background(Color(c))
                        .clickable { onPick(c) },
                    contentAlignment = Alignment.Center,
                ) { if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White) }
            }
        }
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(seed, it) }
        SliderRow("Оттенок", hsl[0], 0f..360f, label = { "${it.roundToInt()}°" }) { h ->
            onPick(ColorUtils.HSLToColor(floatArrayOf(h, 0.7f, 0.55f)))
        }
    }
}

// ---------------- Формы ----------------

private fun LazyListScope.shapesScreen(s: KeyboardSettings) {
    item {
        SettingsGroup("Клавиши") { item {
            SliderRow("Скругление", s.keyCorner, 0f..1f, label = { "${(it * 100).roundToInt()}%" }) { v -> set { it.copy(keyCorner = v) } }
        } }
    }
    item {
        SettingsGroup("Акцентные кнопки") {
            item { ShapePicker(s.accentShape) { v -> set { it.copy(accentShape = v) } } }
        }
    }
    item {
        SettingsGroup("Форма при нажатии и всплывашках") {
            item { ShapePicker(s.pressShape) { v -> set { it.copy(pressShape = v) } } }
            item {
                SettingRow(
                    "Морфинг клавиш",
                    if (s.animations == AnimationLevel.PLAYFUL) "Клавиши перетекают в выбранную форму при нажатии"
                    else "Включите «Игривые» анимации, чтобы увидеть морфинг",
                    Icons.Rounded.AutoAwesome,
                )
            }
        }
    }
}

// ---------------- Шрифт ----------------

private fun LazyListScope.fontScreen(s: KeyboardSettings) {
    item {
        Text(
            "Шрифт",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 28.dp, top = 12.dp, bottom = 4.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(KeyFont.entries) { font ->
                val selected = s.font == font
                val scale by animateFloatAsState(if (selected) 1.05f else 1f, spring(dampingRatio = 0.5f), label = "font")
                Surface(
                    shape = RoundedCornerShape(if (selected) 28.dp else 18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(96.dp, 104.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(if (selected) 28.dp else 18.dp))
                        .clickable { set { it.copy(font = font) } },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            "Аа",
                            style = TextStyle(fontFamily = font.family(), fontWeight = FontWeight.Bold, fontSize = 30.sp),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            font.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    item {
        SettingsGroup("Текст") {
            item { SwitchRow("Жирные буквы", null, Icons.Rounded.FormatBold, s.boldKeys) { v -> set { it.copy(boldKeys = v) } } }
            item { SliderRow("Размер текста", s.fontScale, 0.8f..1.3f, icon = Icons.Rounded.TextFields, label = { "${(it * 100).roundToInt()}%" }) { v -> set { it.copy(fontScale = v) } } }
        }
    }
    item {
        SettingsGroup("Размеры") {
            item { SliderRow("Высота клавиш", s.keyHeight.toFloat(), 40f..66f, label = { "${it.roundToInt()} dp" }) { v -> set { it.copy(keyHeight = v.roundToInt()) } } }
            item { SliderRow("Отступы", s.keyGap.toFloat(), 1f..10f, label = { "${it.roundToInt()} dp" }) { v -> set { it.copy(keyGap = v.roundToInt()) } } }
        }
    }
}

// ---------------- Ввод ----------------

private fun LazyListScope.typingScreen(s: KeyboardSettings) {
    item {
        SettingsGroup("Подсказки") {
            item { SwitchRow("Подсказки слов", "Строка предложений над клавиатурой", Icons.Rounded.Lightbulb, s.suggestions) { v -> set { it.copy(suggestions = v) } } }
            item {
                SwitchRow(
                    "Автоисправление",
                    "Исправляет опечатки при пробеле и учится на ваших словах. ⌫ сразу после исправления — отменить",
                    Icons.Rounded.Spellcheck,
                    s.autocorrect,
                ) { v -> set { it.copy(autocorrect = v) } }
            }
            item { LearnedWordsRow() }
        }
    }
    item {
        SettingsGroup("Умный ввод") {
            item {
                SegmentedRow(
                    "Заглавные буквы",
                    listOf(true, false),
                    s.autoCapitalize,
                    label = { if (it) "Автоматически" else "Только строчные" },
                ) { v -> set { it.copy(autoCapitalize = v) } }
            }
            item { SwitchRow("Точка двойным пробелом", null, Icons.Rounded.SpaceBar, s.doubleSpacePeriod) { v -> set { it.copy(doubleSpacePeriod = v) } } }
            item { SwitchRow("Курсор свайпом по пробелу", null, Icons.Rounded.TouchApp, s.spaceCursor) { v -> set { it.copy(spaceCursor = v) } } }
        }
    }
    item {
        SettingsGroup("Клавиши") {
            item { SwitchRow("Ряд цифр", "Отдельный ряд 1–0 сверху", Icons.Rounded.Dialpad, s.showNumberRow) { v -> set { it.copy(showNumberRow = v) } } }
            item { SwitchRow("Подсказки символов", "Маленькие цифры и буквы в углу клавиш", Icons.Rounded.Visibility, s.showHints) { v -> set { it.copy(showHints = v) } } }
            item { SwitchRow("Кнопка эмодзи на буквах", "Иначе эмодзи — в верхней панели ⊞", Icons.Rounded.EmojiEmotions, s.showEmojiKey) { v -> set { it.copy(showEmojiKey = v) } } }
            item { SwitchRow("Всплывающая буква", "Увеличенная клавиша над пальцем", Icons.Rounded.Keyboard, s.keyPopup) { v -> set { it.copy(keyPopup = v) } } }
            item { SliderRow("Долгое нажатие", s.longPressDelay.toFloat(), 150f..700f, icon = Icons.Rounded.Timer, label = { "${it.roundToInt()} мс" }) { v -> set { it.copy(longPressDelay = v.roundToInt()) } } }
        }
    }
}

@Composable
private fun LearnedWordsRow() {
    var count by remember { mutableStateOf(WordRepository.learnedCount()) }
    SettingRow("Выученные слова", "Клавиатура запомнила: $count", Icons.Rounded.Psychology) {
        OutlinedButton(onClick = { WordRepository.clearLearned(); count = 0 }, enabled = count > 0) { Text("Забыть") }
    }
}

// ---------------- Языки ----------------

private fun LazyListScope.languagesScreen(s: KeyboardSettings) {
    item {
        SettingsGroup("Раскладки") { Layouts.languages.forEach { lang ->
            item {
                val enabled = lang.code in s.languages
                SwitchRow(lang.name, lang.rows.first().uppercase(), null, enabled) { v ->
                    set {
                        val list = if (v) it.languages + lang.code else it.languages - lang.code
                        it.copy(languages = list.distinct().ifEmpty { listOf(lang.code) })
                    }
                }
            }
        } }
    }
    item {
        SettingsGroup("Подсказка") { item {
            SettingRow("Смена языка", "Долгое нажатие на пробел переключает раскладку. Название языка показывается на пробеле.", Icons.Rounded.SpaceBar)
        } }
    }
}

// ---------------- Отклик ----------------

private fun LazyListScope.feedbackScreen(s: KeyboardSettings) {
    item {
        SettingsGroup("Анимации") { item {
            SegmentedRow("Уровень", AnimationLevel.entries, s.animations, label = { it.title }) { v -> set { it.copy(animations = v) } }
        }
            item {
                SwitchRow("Искры при наборе", "Частицы и волна из нажатой клавиши (при «Игривых»)", Icons.Rounded.AutoAwesome, s.particles) { v ->
                    set { it.copy(particles = v) }
                }
            }
        }
    }
    item {
        SettingsGroup("Вибрация") {
            item { SwitchRow("Вибрация при нажатии", null, Icons.Rounded.Vibration, s.haptics) { v -> set { it.copy(haptics = v) } } }
            if (s.haptics) item {
                SliderRow("Сила", s.hapticStrength.toFloat(), 0f..100f, label = { "${it.roundToInt()}%" }) { v -> set { it.copy(hapticStrength = v.roundToInt()) } }
            }
        }
    }
    item {
        SettingsGroup("Звук") {
            item { SwitchRow("Звук нажатий", null, Icons.Rounded.MusicNote, s.sound) { v -> set { it.copy(sound = v) } } }
            if (s.sound) item {
                SliderRow("Громкость", s.soundVolume, 0f..1f, label = { "${(it * 100).roundToInt()}%" }) { v -> set { it.copy(soundVolume = v) } }
            }
        }
    }
}

// ---------------- Буфер обмена ----------------

private fun LazyListScope.clipboardScreen(s: KeyboardSettings) {
    item {
        SettingsGroup("История") {
            item { SwitchRow("Сохранять историю", "Скопированный текст доступен в клавиатуре", Icons.Rounded.ContentPaste, s.clipboardHistory) { v -> set { it.copy(clipboardHistory = v) } } }
            item { SwitchRow("Подсказка последнего", "Только что скопированное — в строке подсказок", Icons.Rounded.Lightbulb, s.clipboardSuggest) { v -> set { it.copy(clipboardSuggest = v) } } }
            item { SliderRow("Хранить записей", s.clipboardLimit.toFloat(), 10f..100f, steps = 8, label = { "${it.roundToInt()}" }) { v -> set { it.copy(clipboardLimit = v.roundToInt()) } } }
        }
    }
    item { ClipboardList() }
}

@Composable
private fun ClipboardList() {
    val items by ClipboardRepository.items.collectAsState()
    SettingsGroup("Записи (${items.size})") {
        if (items.isEmpty()) item { SettingRow("Пока пусто", "Скопируйте что-нибудь, пока клавиатура активна", Icons.Rounded.ContentPaste) }
        items.take(30).forEach { clip ->
            item {
                SettingRow(clip.text.replace('\n', ' ').take(80), if (clip.pinned) "Закреплено" else null) {
                    IconButton(onClick = { ClipboardRepository.remove(clip.id) }) { Icon(Icons.Rounded.DeleteOutline, "Удалить") }
                }
            }
        }
        if (items.any { !it.pinned }) item {
            SettingRow("Очистить незакреплённые", null, Icons.Rounded.DeleteOutline, onClick = { ClipboardRepository.clearUnpinned() })
        }
    }
}

// ---------------- Переводчик ----------------

private fun LazyListScope.translatorScreen(s: KeyboardSettings) {
    item {
        SettingsGroup("Как пользоваться") { item {
            SettingRow(
                "Перевод прямо при наборе",
                "Откройте меню ⊞ на клавиатуре → «Перевод». Печатайте — перевод появится сверху, «Вставить» отправит его в поле. «Всё поле» переведёт уже написанный текст.",
                Icons.Rounded.AutoAwesome,
            )
        } }
    }
    item {
        Text(
            "Переводить на",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 4.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TranslatorEngine.languages) { lang ->
                val selected = s.translateTarget == lang.code
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { set { it.copy(translateTarget = lang.code) } },
                ) {
                    Text(
                        "${lang.flag} ${lang.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
    item { ModelsList() }
}

@Composable
private fun ModelsList() {
    val scope = rememberCoroutineScope()
    var downloaded by remember { mutableStateOf<Set<String>?>(null) }
    var busy by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(Unit) { downloaded = TranslatorEngine.downloadedModels() }
    SettingsGroup("Офлайн-модели (~30 МБ каждая)") { TranslatorEngine.languages.forEach { lang ->
        item {
            val isDownloaded = downloaded?.contains(lang.code) == true
            val isBusy = lang.code in busy
            SettingRow("${lang.flag}  ${lang.name}", if (isDownloaded) "Скачана" else "Не скачана") {
                when {
                    downloaded == null || isBusy -> com.keyboardyou.theme.MorphLoader(MaterialTheme.colorScheme.primary, size = 30.dp)
                    isDownloaded && lang.code != "en" -> IconButton(onClick = {
                        busy = busy + lang.code
                        scope.launch {
                            TranslatorEngine.delete(lang.code)
                            downloaded = TranslatorEngine.downloadedModels()
                            busy = busy - lang.code
                        }
                    }) { Icon(Icons.Rounded.DeleteOutline, "Удалить") }
                    isDownloaded -> Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                    else -> FilledTonalButton(onClick = {
                        busy = busy + lang.code
                        scope.launch {
                            TranslatorEngine.download(lang.code)
                            downloaded = TranslatorEngine.downloadedModels()
                            busy = busy - lang.code
                        }
                    }) {
                        Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Скачать")
                    }
                }
            }
        }
    } }
}

// ---------------- О приложении ----------------

private fun LazyListScope.aboutScreen() {
    item {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            MorphingLogo(size = 120) {
                Icon(Icons.Rounded.Keyboard, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Keyboard You", style = MaterialTheme.typography.headlineMedium)
            Text("Версия ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    item {
        SettingsGroup("Возможности") {
            listOf(
            "Material You: цвета из обоев (Monet) или свой цвет",
            "5 стилей, включая Liquid Glass со стеклянными бликами",
            "11 выразительных форм Material 3 с морфингом",
            "Офлайн-переводчик на 20 языков прямо в клавиатуре",
            "Буфер обмена с историей и закреплением",
            "Эмодзи, редактирование текста, курсор свайпом",
            ).forEach { text -> item { SettingRow(text, null, Icons.Rounded.Check) } }
        }
    }
    item {
        var confirm by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedVisibility(!confirm, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                OutlinedButton(onClick = { confirm = true }) {
                    Icon(Icons.Rounded.RestartAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Сбросить настройки")
                }
            }
            AnimatedVisibility(confirm, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { confirm = false }) { Text("Отмена") }
                    Button(onClick = { SettingsRepository.reset(); confirm = false }) { Text("Да, сбросить") }
                }
            }
        }
    }
}


@Composable
private fun EffectRow(s: KeyboardSettings) {
    val context = LocalContext.current
    val dark = when (s.darkMode) {
        DarkMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val palette = remember(s.colorSource, s.seedColor, s.themeStyle, dark) { paletteFor(schemeFor(context, s, dark), s.themeStyle, dark) }
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(com.keyboardyou.data.BackgroundEffect.entries) { effect ->
            val selected = s.effect == effect
            val scale by animateFloatAsState(if (selected) 1f else 0.93f, spring(dampingRatio = 0.5f), label = "fx")
            Column(
                Modifier
                    .width(108.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { set { it.copy(effect = effect) } },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(palette.background)
                        .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp)) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    com.keyboardyou.keyboard.ui.BackgroundEffectLayer(effect, palette, 1f)
                    if (effect == com.keyboardyou.data.BackgroundEffect.NONE) Text("○", color = palette.keyText)
                }
                Text(
                    effect.emoji + " " + effect.title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                )
            }
        }
    }
}
