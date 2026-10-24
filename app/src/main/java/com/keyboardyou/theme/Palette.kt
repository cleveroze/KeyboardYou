package com.keyboardyou.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.ColorUtils
import com.keyboardyou.data.ColorSource
import com.keyboardyou.data.KeyboardSettings
import com.keyboardyou.data.ThemeStyle

/** Все цвета, которые нужны клавиатуре. */
@Immutable
data class KeyboardPalette(
    val background: Color,
    val backgroundAccent1: Color,
    val backgroundAccent2: Color,
    val backgroundAccent3: Color,
    val key: Color,
    val keyText: Color,
    val keyHint: Color,
    val special: Color,
    val specialText: Color,
    val accent: Color,
    val accentText: Color,
    val action: Color,
    val actionText: Color,
    val pressed: Color,
    val pressedText: Color,
    val chip: Color,
    val chipText: Color,
    val chipActive: Color,
    val chipActiveText: Color,
    val popup: Color,
    val popupText: Color,
    val border: Color,
    val glass: Boolean,
    val dark: Boolean,
)

fun ColorScheme.isDark() = background.luminance() < 0.5f

private fun Color.luminance(): Float = ColorUtils.calculateLuminance(
    android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
).toFloat()

/** Тон цвета в HSL: hue 0..360, saturation 0..1, lightness 0..1. */
private fun hsl(h: Float, s: Float, l: Float): Color {
    val out = FloatArray(3).also { it[0] = ((h % 360f) + 360f) % 360f; it[1] = s.coerceIn(0f, 1f); it[2] = l.coerceIn(0f, 1f) }
    return Color(ColorUtils.HSLToColor(out))
}

/** Строим схему Material 3 из одного «зерна» — упрощённая тональная палитра. */
fun seedColorScheme(seed: Int, dark: Boolean): ColorScheme {
    val hslArr = FloatArray(3)
    ColorUtils.colorToHSL(seed, hslArr)
    val h = hslArr[0]
    val s = hslArr[1].coerceIn(0.35f, 0.9f)
    val h2 = h + 25f   // secondary
    val h3 = h + 60f   // tertiary
    return if (dark) darkColorScheme(
        primary = hsl(h, s, 0.70f),
        onPrimary = hsl(h, s, 0.14f),
        primaryContainer = hsl(h, s * 0.7f, 0.28f),
        onPrimaryContainer = hsl(h, s, 0.88f),
        secondary = hsl(h2, s * 0.45f, 0.72f),
        onSecondary = hsl(h2, s * 0.4f, 0.16f),
        secondaryContainer = hsl(h2, s * 0.35f, 0.26f),
        onSecondaryContainer = hsl(h2, s * 0.4f, 0.88f),
        tertiary = hsl(h3, s * 0.6f, 0.74f),
        onTertiary = hsl(h3, s * 0.6f, 0.16f),
        tertiaryContainer = hsl(h3, s * 0.5f, 0.28f),
        onTertiaryContainer = hsl(h3, s * 0.6f, 0.9f),
        background = hsl(h, s * 0.25f, 0.07f),
        onBackground = hsl(h, s * 0.15f, 0.9f),
        surface = hsl(h, s * 0.25f, 0.07f),
        onSurface = hsl(h, s * 0.15f, 0.9f),
        surfaceVariant = hsl(h, s * 0.2f, 0.22f),
        onSurfaceVariant = hsl(h, s * 0.15f, 0.78f),
        surfaceContainerLowest = hsl(h, s * 0.25f, 0.05f),
        surfaceContainerLow = hsl(h, s * 0.25f, 0.09f),
        surfaceContainer = hsl(h, s * 0.25f, 0.11f),
        surfaceContainerHigh = hsl(h, s * 0.25f, 0.15f),
        surfaceContainerHighest = hsl(h, s * 0.25f, 0.19f),
        outline = hsl(h, s * 0.12f, 0.55f),
        outlineVariant = hsl(h, s * 0.12f, 0.3f),
        inverseSurface = hsl(h, s * 0.15f, 0.9f),
        inverseOnSurface = hsl(h, s * 0.2f, 0.15f),
        inversePrimary = hsl(h, s, 0.38f),
    ) else lightColorScheme(
        primary = hsl(h, s, 0.36f),
        onPrimary = Color.White,
        primaryContainer = hsl(h, s, 0.86f),
        onPrimaryContainer = hsl(h, s, 0.12f),
        secondary = hsl(h2, s * 0.4f, 0.38f),
        onSecondary = Color.White,
        secondaryContainer = hsl(h2, s * 0.5f, 0.88f),
        onSecondaryContainer = hsl(h2, s * 0.4f, 0.14f),
        tertiary = hsl(h3, s * 0.5f, 0.38f),
        onTertiary = Color.White,
        tertiaryContainer = hsl(h3, s * 0.6f, 0.88f),
        onTertiaryContainer = hsl(h3, s * 0.6f, 0.14f),
        background = hsl(h, s * 0.4f, 0.97f),
        onBackground = hsl(h, s * 0.2f, 0.1f),
        surface = hsl(h, s * 0.4f, 0.97f),
        onSurface = hsl(h, s * 0.2f, 0.1f),
        surfaceVariant = hsl(h, s * 0.25f, 0.88f),
        onSurfaceVariant = hsl(h, s * 0.15f, 0.3f),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = hsl(h, s * 0.35f, 0.95f),
        surfaceContainer = hsl(h, s * 0.35f, 0.93f),
        surfaceContainerHigh = hsl(h, s * 0.35f, 0.91f),
        surfaceContainerHighest = hsl(h, s * 0.3f, 0.88f),
        outline = hsl(h, s * 0.12f, 0.48f),
        outlineVariant = hsl(h, s * 0.15f, 0.8f),
        inverseSurface = hsl(h, s * 0.2f, 0.18f),
        inverseOnSurface = hsl(h, s * 0.3f, 0.94f),
        inversePrimary = hsl(h, s, 0.72f),
    )
}

/** Схема Material 3 для текущих настроек: из обоев (Monet) или из своего цвета. */
fun schemeFor(context: Context, settings: KeyboardSettings, dark: Boolean): ColorScheme {
    val useDynamic = settings.colorSource == ColorSource.WALLPAPER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return if (useDynamic) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else seedColorScheme(settings.seedColor, dark)
}

fun paletteFor(scheme: ColorScheme, style: ThemeStyle, dark: Boolean): KeyboardPalette = when (style) {
    ThemeStyle.MATERIAL_YOU -> KeyboardPalette(
        background = scheme.surfaceContainer,
        backgroundAccent1 = scheme.primaryContainer,
        backgroundAccent2 = scheme.tertiaryContainer,
        backgroundAccent3 = scheme.secondaryContainer,
        key = scheme.surfaceContainerHighest,
        keyText = scheme.onSurface,
        keyHint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
        special = scheme.secondaryContainer,
        specialText = scheme.onSecondaryContainer,
        // Expressive: смелые акценты — Enter основным цветом, Shift и всплывашки третичным.
        accent = scheme.tertiary,
        accentText = scheme.onTertiary,
        action = scheme.primary,
        actionText = scheme.onPrimary,
        pressed = scheme.primary,
        pressedText = scheme.onPrimary,
        chip = scheme.secondaryContainer,
        chipText = scheme.onSecondaryContainer,
        chipActive = scheme.primary,
        chipActiveText = scheme.onPrimary,
        popup = scheme.tertiaryContainer,
        popupText = scheme.onTertiaryContainer,
        border = scheme.outlineVariant,
        glass = false,
        dark = dark,
    )
    ThemeStyle.EXPRESSIVE -> KeyboardPalette(
        background = if (dark) lerp(scheme.surfaceContainerLowest, scheme.primaryContainer, 0.35f)
        else lerp(scheme.surfaceContainer, scheme.primaryContainer, 0.45f),
        backgroundAccent1 = scheme.tertiary,
        backgroundAccent2 = scheme.primary,
        backgroundAccent3 = scheme.secondary,
        key = if (dark) lerp(scheme.surfaceContainerHigh, scheme.secondaryContainer, 0.6f) else scheme.surfaceContainerLowest,
        keyText = scheme.onSurface,
        keyHint = scheme.primary,
        special = scheme.tertiaryContainer,
        specialText = scheme.onTertiaryContainer,
        accent = scheme.tertiary,
        accentText = scheme.onTertiary,
        action = scheme.primary,
        actionText = scheme.onPrimary,
        pressed = scheme.tertiary,
        pressedText = scheme.onTertiary,
        chip = scheme.tertiaryContainer,
        chipText = scheme.onTertiaryContainer,
        chipActive = scheme.primary,
        chipActiveText = scheme.onPrimary,
        popup = scheme.tertiary,
        popupText = scheme.onTertiary,
        border = scheme.outline,
        glass = false,
        dark = dark,
    )
    ThemeStyle.LIQUID_GLASS -> {
        val glassFill = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.42f)
        val text = if (dark) Color.White else Color(0xFF111418)
        KeyboardPalette(
            background = if (dark) Color(0xFF0B0D12) else Color(0xFFE9EDF3),
            backgroundAccent1 = scheme.primary,
            backgroundAccent2 = scheme.tertiary,
            backgroundAccent3 = scheme.secondary,
            key = glassFill,
            keyText = text,
            keyHint = text.copy(alpha = 0.55f),
            special = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.25f),
            specialText = text,
            accent = scheme.primary.copy(alpha = 0.85f),
            accentText = scheme.onPrimary,
            action = scheme.primary.copy(alpha = 0.9f),
            actionText = scheme.onPrimary,
            pressed = Color.White.copy(alpha = if (dark) 0.32f else 0.75f),
            pressedText = text,
            chip = glassFill,
            chipText = text,
            chipActive = scheme.primary.copy(alpha = 0.85f),
            chipActiveText = scheme.onPrimary,
            popup = if (dark) Color(0xCC2A2E36) else Color(0xE6FFFFFF),
            popupText = text,
            border = Color.White.copy(alpha = if (dark) 0.35f else 0.9f),
            glass = true,
            dark = dark,
        )
    }
    ThemeStyle.AMOLED -> KeyboardPalette(
        background = Color.Black,
        backgroundAccent1 = scheme.primary.copy(alpha = 0.5f),
        backgroundAccent2 = scheme.tertiary.copy(alpha = 0.4f),
        backgroundAccent3 = scheme.secondary.copy(alpha = 0.4f),
        key = Color(0xFF141414),
        keyText = Color.White,
        keyHint = scheme.primary,
        special = Color(0xFF1F1F1F),
        specialText = scheme.primary,
        accent = scheme.primary,
        accentText = Color.Black,
        action = scheme.primary,
        actionText = Color.Black,
        pressed = scheme.primary,
        pressedText = Color.Black,
        chip = Color(0xFF1A1A1A),
        chipText = Color.White,
        chipActive = scheme.primary,
        chipActiveText = Color.Black,
        popup = scheme.primary,
        popupText = Color.Black,
        border = scheme.primary.copy(alpha = 0.45f),
        glass = false,
        dark = true,
    )
    ThemeStyle.PASTEL -> {
        val bg = lerp(Color(0xFFFFFBF7), scheme.primaryContainer, 0.35f)
        KeyboardPalette(
            background = if (dark) lerp(Color(0xFF1C1B1F), scheme.primaryContainer, 0.25f) else bg,
            backgroundAccent1 = lerp(scheme.primaryContainer, Color.White, 0.2f),
            backgroundAccent2 = lerp(scheme.tertiaryContainer, Color.White, 0.2f),
            backgroundAccent3 = lerp(Color(0xFFFFE8A3), scheme.secondaryContainer, 0.3f),
            key = if (dark) lerp(Color(0xFF2B2930), scheme.secondaryContainer, 0.3f) else Color.White.copy(alpha = 0.85f),
            keyText = if (dark) Color.White else Color(0xFF2B2930),
            keyHint = scheme.tertiary,
            special = if (dark) scheme.secondaryContainer else lerp(scheme.secondaryContainer, Color.White, 0.3f),
            specialText = scheme.onSecondaryContainer,
            accent = if (dark) scheme.tertiary else lerp(scheme.tertiaryContainer, scheme.tertiary, 0.3f),
            accentText = if (dark) scheme.onTertiary else scheme.onTertiaryContainer,
            action = if (dark) scheme.primary else lerp(scheme.primaryContainer, scheme.primary, 0.3f),
            actionText = if (dark) scheme.onPrimary else scheme.onPrimaryContainer,
            pressed = scheme.tertiaryContainer,
            pressedText = scheme.onTertiaryContainer,
            chip = if (dark) scheme.secondaryContainer else Color.White.copy(alpha = 0.7f),
            chipText = if (dark) scheme.onSecondaryContainer else Color(0xFF2B2930),
            chipActive = scheme.tertiaryContainer,
            chipActiveText = scheme.onTertiaryContainer,
            popup = scheme.tertiaryContainer,
            popupText = scheme.onTertiaryContainer,
            border = scheme.outlineVariant,
            glass = false,
            dark = dark,
        )
    }
}
