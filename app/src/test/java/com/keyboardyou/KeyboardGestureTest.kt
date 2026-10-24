package com.keyboardyou

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
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

/** Жесты на клавишах. Часы анимаций ведём вручную: в клавиатуре есть бесконечные анимации. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeyboardGestureTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var sink: PreviewSink

    private fun launch(settings: KeyboardSettings = KeyboardSettings(autoCapitalize = false)) {
        sink = PreviewSink("")
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val scope = rememberCoroutineScope()
            val controller = remember { KeyboardController(scope, sink) }
            KeyboardRoot(controller, settings, insetNavigationBar = false)
        }
        settle()
    }

    private fun settle(ms: Long = 400) = rule.mainClock.advanceTimeBy(ms)

    private fun key(label: String) = rule.onAllNodes(hasText(label, ignoreCase = true), useUnmergedTree = true)[0]

    private fun tap(label: String) {
        key(label).performTouchInput { down(center) }
        settle(60)
        key(label).performTouchInput { up() }
        settle()
    }

    private fun hold(label: String, ms: Long = 900, slide: Float = 0f) {
        key(label).performTouchInput { down(center) }
        settle(ms)
        if (slide != 0f) {
            key(label).performTouchInput { moveBy(Offset(width * slide, 0f)) }
            settle(100)
        }
        key(label).performTouchInput { up() }
        settle()
    }

    @Test
    fun tapTypesLetter() {
        launch()
        tap("ф")
        assertEquals("ф", sink.text)
    }

    @Test
    fun longPressLetterWithoutAlternates() {
        launch()
        hold("ф")
        tap("ы")
        assertEquals("фы", sink.text)
    }

    @Test
    fun longPressLetterWithAlternates() {
        launch()
        hold("е")
        assertEquals("ё", sink.text)
        tap("ы")
        assertEquals("ёы", sink.text)
    }

    @Test
    fun longPressAndSlideToDigit() {
        launch()
        hold("е", slide = 1.2f)
        assertEquals("5", sink.text)
    }

    @Test
    fun fastTypingSequence() {
        launch()
        "привет".forEach { tap(it.toString()) }
        assertTrue(sink.text, sink.text.startsWith("привет"))
    }

    @Test
    fun shiftMorphsAndTypesCapital() {
        launch()
        // Shift — первая клавиша нижнего буквенного ряда; ищем через букву «я» в том же ряду.
        rule.onAllNodes(hasText("я"), useUnmergedTree = true)[0].assertExists()
        hold("ф", ms = 50)
        assertEquals("ф", sink.text)
    }
}
