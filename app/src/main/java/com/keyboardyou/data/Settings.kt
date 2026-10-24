package com.keyboardyou.data

import android.content.Context
import android.content.SharedPreferences
import com.keyboardyou.theme.KeyShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeStyle(val title: String, val subtitle: String) {
    MATERIAL_YOU("Material You", "Тональная палитра Material 3"),
    EXPRESSIVE("Expressive", "Сочные акценты, M3 Expressive"),
    LIQUID_GLASS("Liquid Glass", "Полупрозрачное стекло с бликами"),
    AMOLED("AMOLED", "Глубокий чёрный и неон"),
    PASTEL("Пастель", "Мягкие светлые оттенки"),
}

enum class DarkMode(val title: String) { SYSTEM("Как в системе"), LIGHT("Светлая"), DARK("Тёмная") }

enum class ColorSource { WALLPAPER, CUSTOM }

enum class KeyFont(val title: String) {
    SYSTEM("Системный"), NUNITO("Nunito"), RUBIK("Rubik"), MANROPE("Manrope"),
    COMFORTAA("Comfortaa"), UNBOUNDED("Unbounded"), MONO("Моно"), SERIF("С засечками"),
}

enum class AnimationLevel(val title: String) { OFF("Выкл"), SUBTLE("Мягкие"), PLAYFUL("Игривые") }

enum class BackgroundEffect(val title: String, val emoji: String) {
    NONE("Без эффекта", "○"),
    SNOW("Снег", "❄️"),
    RAIN("Дождь", "🌧️"),
    STARS("Звёздное небо", "✨"),
    BUBBLES("Пузыри", "🫧"),
    CONFETTI("Конфетти", "🎉"),
    FIREFLIES("Светлячки", "🌟"),
    AURORA("Северное сияние", "🌌"),
    SAKURA("Сакура", "🌸"),
    WAVES("Волны", "🌊"),
    HEARTS("Сердечки", "💗"),
}

enum class BackgroundStyle(val title: String) { SOLID("Сплошной"), GRADIENT("Градиент"), BLOBS("Живые пятна") }

data class KeyboardSettings(
    // Внешний вид
    val themeStyle: ThemeStyle = ThemeStyle.MATERIAL_YOU,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val colorSource: ColorSource = ColorSource.WALLPAPER,
    val seedColor: Int = 0xFF2BD9A0.toInt(),
    val backgroundStyle: BackgroundStyle = BackgroundStyle.BLOBS,
    val blurBehind: Boolean = false,
    val effect: BackgroundEffect = BackgroundEffect.NONE,
    val effectIntensity: Float = 1f,
    val effectOnTop: Boolean = false,
    // Формы
    val keyCorner: Float = 0.5f,
    val accentShape: KeyShape = KeyShape.COOKIE_9,
    val pressShape: KeyShape = KeyShape.COOKIE_12,
    val keyHeight: Int = 54,
    val keyGap: Int = 4,
    val keyBorders: Boolean = false,
    // Шрифт
    val font: KeyFont = KeyFont.NUNITO,
    val fontScale: Float = 1f,
    val boldKeys: Boolean = true,
    // Ввод
    val showNumberRow: Boolean = false,
    val showHints: Boolean = true,
    val keyPopup: Boolean = true,
    val suggestions: Boolean = true,
    val autocorrect: Boolean = true,
    val showEmojiKey: Boolean = false,
    val autoCapitalize: Boolean = true,
    val doubleSpacePeriod: Boolean = true,
    val spaceCursor: Boolean = true,
    val longPressDelay: Int = 300,
    val languages: List<String> = listOf("ru", "en"),
    // Отклик
    val haptics: Boolean = true,
    val hapticStrength: Int = 40,
    val sound: Boolean = false,
    val soundVolume: Float = 0.4f,
    val animations: AnimationLevel = AnimationLevel.PLAYFUL,
    val particles: Boolean = true,
    // Буфер обмена
    val clipboardHistory: Boolean = true,
    val clipboardLimit: Int = 40,
    val clipboardSuggest: Boolean = true,
    // Переводчик
    val translateSource: String = "auto",
    val translateTarget: String = "en",
)

object SettingsRepository {
    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(KeyboardSettings())
    val state: StateFlow<KeyboardSettings> = _state.asStateFlow()
    val current: KeyboardSettings get() = _state.value

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _state.value = load()
    }

    fun update(block: (KeyboardSettings) -> KeyboardSettings) {
        val new = block(_state.value)
        _state.value = new
        if (::prefs.isInitialized) save(new)
    }

    fun reset() = update { KeyboardSettings() }

    private inline fun <reified E : Enum<E>> SharedPreferences.enum(key: String, def: E): E =
        getString(key, null)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: def

    private fun load(): KeyboardSettings = with(prefs) {
        val d = KeyboardSettings()
        KeyboardSettings(
            themeStyle = enum("themeStyle", d.themeStyle),
            darkMode = enum("darkMode", d.darkMode),
            colorSource = enum("colorSource", d.colorSource),
            seedColor = getInt("seedColor", d.seedColor),
            backgroundStyle = enum("backgroundStyle", d.backgroundStyle),
            blurBehind = getBoolean("blurBehind", d.blurBehind),
            effect = enum("effect", d.effect),
            effectIntensity = getFloat("effectIntensity", d.effectIntensity),
            effectOnTop = getBoolean("effectOnTop", d.effectOnTop),
            keyCorner = getFloat("keyCorner", d.keyCorner),
            accentShape = enum("accentShape", d.accentShape),
            pressShape = enum("pressShape", d.pressShape),
            keyHeight = getInt("keyHeight", d.keyHeight),
            keyGap = getInt("keyGap", d.keyGap),
            keyBorders = getBoolean("keyBorders", d.keyBorders),
            font = enum("font", d.font),
            fontScale = getFloat("fontScale", d.fontScale),
            boldKeys = getBoolean("boldKeys", d.boldKeys),
            showNumberRow = getBoolean("showNumberRow", d.showNumberRow),
            showHints = getBoolean("showHints", d.showHints),
            keyPopup = getBoolean("keyPopup", d.keyPopup),
            suggestions = getBoolean("suggestions", d.suggestions),
            autocorrect = getBoolean("autocorrect", d.autocorrect),
            showEmojiKey = getBoolean("showEmojiKey", d.showEmojiKey),
            autoCapitalize = getBoolean("autoCapitalize", d.autoCapitalize),
            doubleSpacePeriod = getBoolean("doubleSpacePeriod", d.doubleSpacePeriod),
            spaceCursor = getBoolean("spaceCursor", d.spaceCursor),
            longPressDelay = getInt("longPressDelay", d.longPressDelay),
            languages = getString("languages", null)?.split(",")?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() } ?: d.languages,
            haptics = getBoolean("haptics", d.haptics),
            hapticStrength = getInt("hapticStrength", d.hapticStrength),
            sound = getBoolean("sound", d.sound),
            soundVolume = getFloat("soundVolume", d.soundVolume),
            animations = enum("animations", d.animations),
            particles = getBoolean("particles", d.particles),
            clipboardHistory = getBoolean("clipboardHistory", d.clipboardHistory),
            clipboardLimit = getInt("clipboardLimit", d.clipboardLimit),
            clipboardSuggest = getBoolean("clipboardSuggest", d.clipboardSuggest),
            translateSource = getString("translateSource", d.translateSource) ?: d.translateSource,
            translateTarget = getString("translateTarget", d.translateTarget) ?: d.translateTarget,
        )
    }

    private fun save(s: KeyboardSettings) {
        prefs.edit()
            .putString("themeStyle", s.themeStyle.name)
            .putString("darkMode", s.darkMode.name)
            .putString("colorSource", s.colorSource.name)
            .putInt("seedColor", s.seedColor)
            .putString("backgroundStyle", s.backgroundStyle.name)
            .putBoolean("blurBehind", s.blurBehind)
            .putString("effect", s.effect.name)
            .putFloat("effectIntensity", s.effectIntensity)
            .putBoolean("effectOnTop", s.effectOnTop)
            .putFloat("keyCorner", s.keyCorner)
            .putString("accentShape", s.accentShape.name)
            .putString("pressShape", s.pressShape.name)
            .putInt("keyHeight", s.keyHeight)
            .putInt("keyGap", s.keyGap)
            .putBoolean("keyBorders", s.keyBorders)
            .putString("font", s.font.name)
            .putFloat("fontScale", s.fontScale)
            .putBoolean("boldKeys", s.boldKeys)
            .putBoolean("showNumberRow", s.showNumberRow)
            .putBoolean("showHints", s.showHints)
            .putBoolean("keyPopup", s.keyPopup)
            .putBoolean("suggestions", s.suggestions)
            .putBoolean("autocorrect", s.autocorrect)
            .putBoolean("showEmojiKey", s.showEmojiKey)
            .putBoolean("autoCapitalize", s.autoCapitalize)
            .putBoolean("doubleSpacePeriod", s.doubleSpacePeriod)
            .putBoolean("spaceCursor", s.spaceCursor)
            .putInt("longPressDelay", s.longPressDelay)
            .putString("languages", s.languages.joinToString(","))
            .putBoolean("haptics", s.haptics)
            .putInt("hapticStrength", s.hapticStrength)
            .putBoolean("sound", s.sound)
            .putFloat("soundVolume", s.soundVolume)
            .putString("animations", s.animations.name)
            .putBoolean("particles", s.particles)
            .putBoolean("clipboardHistory", s.clipboardHistory)
            .putInt("clipboardLimit", s.clipboardLimit)
            .putBoolean("clipboardSuggest", s.clipboardSuggest)
            .putString("translateSource", s.translateSource)
            .putString("translateTarget", s.translateTarget)
            .apply()
    }
}
