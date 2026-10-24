package com.keyboardyou.keyboard.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.keyboardyou.data.DarkMode
import com.keyboardyou.data.KeyboardSettings
import com.keyboardyou.theme.KeyboardPalette
import com.keyboardyou.theme.family
import com.keyboardyou.theme.paletteFor
import com.keyboardyou.theme.schemeFor

val LocalPalette = staticCompositionLocalOf<KeyboardPalette> { error("Нет палитры") }
val LocalKbSettings = staticCompositionLocalOf { KeyboardSettings() }
val LocalKeyTextStyle = staticCompositionLocalOf { TextStyle.Default }

@Composable
fun KeyboardTheme(settings: KeyboardSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        DarkMode.SYSTEM -> systemDark
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val palette = remember(settings.colorSource, settings.seedColor, settings.themeStyle, dark) {
        paletteFor(schemeFor(context, settings, dark), settings.themeStyle, dark)
    }
    val textStyle = remember(settings.font, settings.fontScale, settings.boldKeys) {
        TextStyle(
            fontFamily = settings.font.family(),
            fontWeight = if (settings.boldKeys) FontWeight.Bold else FontWeight.Medium,
            fontSize = (21 * settings.fontScale).sp,
        )
    }
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalKbSettings provides settings,
        LocalKeyTextStyle provides textStyle,
        content = content,
    )
}
