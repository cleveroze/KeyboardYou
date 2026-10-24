package com.keyboardyou.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.keyboardyou.R
import com.keyboardyou.data.KeyFont

@OptIn(ExperimentalTextApi::class)
private fun variable(res: Int): FontFamily = FontFamily(
    listOf(300, 400, 500, 600, 700, 800, 900).map { w ->
        Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

private val families = HashMap<KeyFont, FontFamily>()

fun KeyFont.family(): FontFamily = families.getOrPut(this) {
    when (this) {
        KeyFont.SYSTEM -> FontFamily.Default
        KeyFont.NUNITO -> variable(R.font.nunito)
        KeyFont.RUBIK -> variable(R.font.rubik)
        KeyFont.MANROPE -> variable(R.font.manrope)
        KeyFont.COMFORTAA -> variable(R.font.comfortaa)
        KeyFont.UNBOUNDED -> variable(R.font.unbounded)
        KeyFont.MONO -> FontFamily.Monospace
        KeyFont.SERIF -> FontFamily.Serif
    }
}

val AppFontFamily: FontFamily get() = KeyFont.NUNITO.family()
