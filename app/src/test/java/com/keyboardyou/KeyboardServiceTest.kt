package com.keyboardyou

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.keyboardyou.data.SettingsRepository
import com.keyboardyou.data.ThemeStyle
import com.keyboardyou.ime.KeyboardService
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Запускаем настоящую службу клавиатуры с настоящей отрисовкой. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeyboardServiceTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun launch(style: ThemeStyle = ThemeStyle.MATERIAL_YOU) {
        SettingsRepository.update { it.copy(themeStyle = style) }
        val service = Robolectric.buildService(KeyboardService::class.java).create().get()
        rule.runOnUiThread {
            val view = service.onCreateInputView()
            rule.activity.setContentView(view)
            service.onStartInputView(EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }, false)
        }
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(500)
    }

    private fun key(label: String) = rule.onAllNodesWithText(label, useUnmergedTree = true)[0]

    private fun holdAndRelease(label: String) {
        key(label).performTouchInput { down(center) }
        repeat(12) {
            rule.mainClock.advanceTimeBy(100)
            rule.onRoot().captureToImage()
        }
        key(label).performTouchInput { up() }
        repeat(5) {
            rule.mainClock.advanceTimeBy(100)
            rule.onRoot().captureToImage()
        }
    }

    @Test
    fun longPressInServiceMaterial() {
        launch()
        holdAndRelease("ф")
        holdAndRelease("е")
        holdAndRelease("й")
        key("в").performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.onRoot().captureToImage()
    }

    @Test
    fun longPressInServiceGlass() {
        launch(ThemeStyle.LIQUID_GLASS)
        holdAndRelease("ф")
        holdAndRelease("е")
    }

    @Test
    fun longPressClickHelper() {
        launch()
        key("ж").performTouchInput { longClick(durationMillis = 1000) }
        rule.mainClock.advanceTimeBy(1000)
        rule.onRoot().captureToImage()
    }
}
