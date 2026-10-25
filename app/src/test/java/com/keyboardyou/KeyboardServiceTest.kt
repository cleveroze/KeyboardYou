package com.keyboardyou

import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
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

    private fun launch(style: ThemeStyle = ThemeStyle.MATERIAL_YOU, name: String = style.name) {
        testName = name
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

    private var shot = 0
    private var testName = "kb"

    /** Рисуем окно в картинку (настоящая отрисовка) и сохраняем PNG для артефакта CI. */
    private fun snapshot(tag: String = "") {
        rule.runOnUiThread {
            val root = rule.activity.window.decorView
            if (root.width == 0 || root.height == 0) return@runOnUiThread
            val bmp = android.graphics.Bitmap.createBitmap(root.width, root.height, android.graphics.Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(bmp))
            val dir = java.io.File("build/screens").apply { mkdirs() }
            java.io.File(dir, "%s_%02d%s.png".format(testName, shot++, tag)).outputStream().use {
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun key(label: String) =
        rule.onAllNodes(hasText(label, ignoreCase = true), useUnmergedTree = true)[0]

    private fun holdAndRelease(label: String) {
        key(label).performTouchInput { down(center) }
        repeat(12) {
            rule.mainClock.advanceTimeBy(100)
            snapshot()
        }
        key(label).performTouchInput { up() }
        repeat(5) {
            rule.mainClock.advanceTimeBy(100)
            snapshot()
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
        snapshot()
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
        snapshot()
    }

    @Test
    fun allBackgroundEffectsRender() {
        launch(name = "effects")
        com.keyboardyou.data.BackgroundEffect.entries.forEach { effect ->
            listOf(false, true).forEach { onTop ->
                SettingsRepository.update { it.copy(effect = effect, effectOnTop = onTop) }
                rule.mainClock.advanceTimeBy(700)
                snapshot("_" + effect.name + if (onTop) "_top" else "")
            }
        }
        SettingsRepository.update { it.copy(effect = com.keyboardyou.data.BackgroundEffect.NONE, effectOnTop = false) }
        rule.mainClock.advanceTimeBy(300)
        key("в").performClick()
    }

    @Test
    fun emojiSearchFavoritesAndRecents() {
        val store = com.keyboardyou.data.EmojiStore
        store.init(org.robolectric.RuntimeEnvironment.getApplication())
        var found = emptyList<String>()
        repeat(50) {
            if (found.isEmpty()) {
                Thread.sleep(100)
                found = store.search("огонь")
            }
        }
        org.junit.Assert.assertTrue("search: $found", "🔥" in found)
        org.junit.Assert.assertTrue("🐱" in store.search("кот") || "😺" in store.search("кот"))
        repeat(20) { store.use("😀"); store.use("🔥") }
        (0 until 20).forEach { store.use(String(Character.toChars(0x1F600 + it))) }
        org.junit.Assert.assertEquals(15, store.recents.value.size)
        if (!store.isFavorite("🔥")) store.toggleFavorite("🔥")
        org.junit.Assert.assertTrue(store.isFavorite("🔥"))

        launch(name = "emoji")
        key("ж").performClick() // клавиатура живая
        rule.mainClock.advanceTimeBy(300)
    }
}
