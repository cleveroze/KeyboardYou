package com.keyboardyou

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.keyboardyou.data.BackgroundStyle
import com.keyboardyou.data.KeyboardSettings
import com.keyboardyou.keyboard.KeyboardController
import com.keyboardyou.keyboard.ui.KeyboardRoot
import com.keyboardyou.ui.PreviewSink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardGestureTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var sink: PreviewSink

    private fun launch(settings: KeyboardSettings = KeyboardSettings(backgroundStyle = BackgroundStyle.SOLID)) {
        sink = PreviewSink("")
        rule.setContent {
            val scope = rememberCoroutineScope()
            val controller = remember { KeyboardController(scope, sink) }
            KeyboardRoot(controller, settings, insetNavigationBar = false)
        }
        rule.waitForIdle()
    }

    private fun key(label: String) = rule.onAllNodesWithText(label, useUnmergedTree = true)[0]

    @Test
    fun tapTypesLetter() {
        launch()
        key("ф").performClick()
        rule.waitForIdle()
        assertEquals("ф", sink.text.lowercase())
    }

    @Test
    fun longPressLetterWithoutAlternates() {
        launch()
        key("ф").performTouchInput { longClick(durationMillis = 900) }
        rule.waitForIdle()
        key("ы").performClick()
        rule.waitForIdle()
        assertEquals("фы", sink.text.lowercase())
    }

    @Test
    fun longPressLetterWithAlternates() {
        launch()
        key("е").performTouchInput { longClick(durationMillis = 900) }
        rule.waitForIdle()
        assertEquals("ё", sink.text.lowercase())
        key("ы").performClick()
        rule.waitForIdle()
        assertTrue(sink.text.lowercase().endsWith("ы"))
    }

    @Test
    fun longPressAndSlideToAlternate() {
        launch()
        key("е").performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(width * 1.2f, 0f))
            advanceEventTime(100)
            moveBy(Offset(-width * 1.2f, 0f))
            advanceEventTime(100)
            up()
        }
        rule.waitForIdle()
        key("ы").performClick()
        rule.waitForIdle()
        assertTrue("got '${sink.text}'", sink.text.lowercase().endsWith("ы"))
    }

    @Test
    fun longPressWithDefaultSettings() {
        launch(KeyboardSettings())
        rule.mainClock.autoAdvance = false
        key("ц").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(1200)
        key("ц").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1000)
        key("в").performTouchInput { down(center); up() }
        rule.mainClock.advanceTimeBy(500)
        assertTrue("got '${sink.text}'", sink.text.lowercase().endsWith("в"))
    }
}
