package com.keyboardyou.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.keyboardyou.data.DarkMode
import com.keyboardyou.data.KeyFont
import com.keyboardyou.data.SettingsRepository
import com.keyboardyou.theme.family
import com.keyboardyou.theme.schemeFor

private fun TextStyle.with(font: KeyFont, weight: FontWeight? = null) =
    copy(fontFamily = font.family(), fontWeight = weight ?: fontWeight)

private val AppTypography: Typography by lazy {
    val base = Typography()
    val body = KeyFont.NUNITO
    val display = KeyFont.UNBOUNDED
    Typography(
        displayLarge = base.displayLarge.with(display, FontWeight.SemiBold),
        displayMedium = base.displayMedium.with(display, FontWeight.SemiBold),
        displaySmall = base.displaySmall.with(display, FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.with(display, FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.with(display, FontWeight.Medium),
        headlineSmall = base.headlineSmall.with(display, FontWeight.Medium),
        titleLarge = base.titleLarge.with(body, FontWeight.ExtraBold),
        titleMedium = base.titleMedium.with(body, FontWeight.Bold),
        titleSmall = base.titleSmall.with(body, FontWeight.Bold),
        bodyLarge = base.bodyLarge.with(body, FontWeight.Medium),
        bodyMedium = base.bodyMedium.with(body, FontWeight.Medium),
        bodySmall = base.bodySmall.with(body, FontWeight.Medium),
        labelLarge = base.labelLarge.with(body, FontWeight.Bold),
        labelMedium = base.labelMedium.with(body, FontWeight.Bold),
        labelSmall = base.labelSmall.with(body, FontWeight.Bold),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val settings by SettingsRepository.state.collectAsState()
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        DarkMode.SYSTEM -> systemDark
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val scheme = remember(settings.colorSource, settings.seedColor, dark) { schemeFor(context, settings, dark) }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
