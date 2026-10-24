package com.keyboardyou.keyboard

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import com.keyboardyou.data.ClipboardRepository
import com.keyboardyou.data.SettingsRepository
import com.keyboardyou.data.ThemeStyle
import com.keyboardyou.data.WordRepository
import com.keyboardyou.translate.TranslateResult
import com.keyboardyou.translate.TranslatorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Mode { ALPHA, SYMBOLS, MORE_SYMBOLS, NUMPAD }
enum class ShiftState { OFF, ONCE, LOCKED }
enum class Panel { KEYBOARD, EMOJI, CLIPBOARD, EDIT, LANG_PICKER }
enum class PickerTarget { SOURCE, TARGET }

data class KeyPreview(val id: Int, val label: String, val bounds: Rect)
data class AltPopup(val key: Key, val bounds: Rect, val options: List<String>, val selected: Int)

sealed interface Suggestion {
    val text: String
    data class Word(override val text: String, val primary: Boolean = false) : Suggestion
    data class Typed(override val text: String) : Suggestion
    data class Clip(override val text: String) : Suggestion
    /** Отмена последнего автоисправления. */
    data class Revert(override val text: String) : Suggestion
}

/** Что было исправлено: исходное слово, исправление и разделитель после него. */
data class Correction(val original: String, val corrected: String, val separator: String)

class KeyboardController(
    private val scope: CoroutineScope,
    var sink: InputSink,
) {
    private val settings get() = SettingsRepository.current

    var mode by mutableStateOf(Mode.ALPHA)
    var shift by mutableStateOf(ShiftState.OFF)
    var languageIndex by mutableIntStateOf(0)
    var panel by mutableStateOf(Panel.KEYBOARD)
    var toolbarOpen by mutableStateOf(false)
    var selectMode by mutableStateOf(false)

    var enterAction by mutableStateOf(EnterAction.NEWLINE)
    var fieldKind by mutableStateOf(FieldKind.TEXT)
    var privateField by mutableStateOf(false)
    var allowAutoCaps by mutableStateOf(true)

    var currentWord by mutableStateOf("")
    val suggestions = mutableStateListOf<Suggestion>()

    /** Какая клавиша сейчас нажата в каждом ряду — для анимации соседей. */
    val pressedInRow = mutableStateMapOf<Int, Int>()

    var preview by mutableStateOf<KeyPreview?>(null)
    var altPopup by mutableStateOf<AltPopup?>(null)
    /** Счётчик для «вспышки» названия языка на пробеле. */
    var languageFlash by mutableIntStateOf(0)

    // Переводчик
    var translatorOpen by mutableStateOf(false)
    var translatorInput by mutableStateOf("")
    var translatorOutput by mutableStateOf("")
    var translatorBusy by mutableStateOf(false)
    var translatorError by mutableStateOf<String?>(null)
    var translatorDetected by mutableStateOf<String?>(null)
    var pickerTarget by mutableStateOf(PickerTarget.TARGET)
    private var translateJob: Job? = null

    var rootCoordinates: LayoutCoordinates? = null
    private var previewCounter = 0
    private var lastShiftTap = 0L
    private var lastSpaceTap = 0L
    var lastCorrection by mutableStateOf<Correction?>(null)
        private set

    val languages: List<Language> get() = settings.languages.map { Layouts.language(it) }.ifEmpty { listOf(Layouts.languages.first()) }
    val language: Language get() = languages[languageIndex.coerceIn(0, languages.lastIndex)]

    fun layout(): KeyboardLayout {
        val s = settings
        return when (mode) {
            Mode.ALPHA -> Layouts.alpha(language, s.showNumberRow, s.showHints, s.showEmojiKey)
            Mode.SYMBOLS -> Layouts.symbols()
            Mode.MORE_SYMBOLS -> Layouts.moreSymbols()
            Mode.NUMPAD -> Layouts.numpad()
        }
    }

    fun boundsOf(coords: LayoutCoordinates?): Rect {
        val root = rootCoordinates
        if (coords == null || root == null || !coords.isAttached || !root.isAttached) return Rect.Zero
        return root.localBoundingBoxOf(coords, clipBounds = false)
    }

    // ---------- Жизненный цикл поля ввода ----------

    fun onStartInput(kind: FieldKind, action: EnterAction, privateField: Boolean, autoCaps: Boolean) {
        fieldKind = kind
        enterAction = action
        this.privateField = privateField
        allowAutoCaps = autoCaps
        mode = when (kind) {
            FieldKind.NUMBER, FieldKind.PHONE -> Mode.NUMPAD
            FieldKind.TEXT -> Mode.ALPHA
        }
        panel = Panel.KEYBOARD
        selectMode = false
        altPopup = null
        preview = null
        shift = ShiftState.OFF
        refresh()
    }

    /** Курсор сдвинулся извне (тап по тексту и т.п.). */
    fun onSelectionChanged() {
        if (!translatorOpen) refresh()
    }

    // ---------- Нажатия ----------

    fun onKeyDown(key: Key, bounds: Rect) {
        sink.feedback(
            when (key.type) {
                KeyType.BACKSPACE -> FeedbackKind.DELETE
                KeyType.SPACE -> FeedbackKind.SPACE
                KeyType.ENTER -> FeedbackKind.RETURN
                KeyType.CHAR, KeyType.COMMA, KeyType.PERIOD -> FeedbackKind.KEY
                else -> FeedbackKind.SPECIAL
            }
        )
        if (settings.keyPopup && key.type in previewTypes) {
            preview = KeyPreview(++previewCounter, displayLabel(key), bounds)
        }
    }

    fun onKeyUp() {
        preview = null
    }

    fun displayLabel(key: Key): String =
        if (mode == Mode.ALPHA && shift != ShiftState.OFF && key.type == KeyType.CHAR) key.label.uppercase() else key.label

    fun onKeyTap(key: Key) {
        when (key.type) {
            KeyType.CHAR, KeyType.COMMA, KeyType.PERIOD -> typeChar(displayLabel(key))
            KeyType.SPACE -> typeSpace()
            KeyType.BACKSPACE -> backspace()
            KeyType.ENTER -> enter()
            KeyType.SHIFT -> toggleShift()
            KeyType.SYMBOLS -> mode = Mode.SYMBOLS
            KeyType.MORE_SYMBOLS -> mode = Mode.MORE_SYMBOLS
            KeyType.ALPHA -> {
                mode = Mode.ALPHA
                updateAutoCaps()
            }
            KeyType.EMOJI -> openPanel(Panel.EMOJI)
            KeyType.LANGUAGE -> nextLanguage()
        }
    }

    fun onKeyLongPress(key: Key, bounds: Rect): Boolean {
        when (key.type) {
            KeyType.SHIFT -> {
                shift = ShiftState.LOCKED
                return true
            }
            KeyType.SPACE -> {
                if (languages.size > 1) {
                    nextLanguage()
                    sink.feedback(FeedbackKind.SPECIAL)
                }
                return true
            }
            KeyType.EMOJI -> {
                openPanel(Panel.CLIPBOARD)
                return true
            }
            else -> Unit
        }
        if (bounds.width <= 1f || bounds.height <= 1f) return false
        val options = key.alternates.map { if (shift != ShiftState.OFF && mode == Mode.ALPHA) it.uppercase() else it }
        if (options.isEmpty()) return false
        preview = null
        altPopup = AltPopup(key, bounds, listOf(displayLabel(key)) + options, 1.coerceAtMost(options.size))
        sink.feedback(FeedbackKind.SPECIAL)
        return true
    }

    fun onAltMove(x: Float) {
        val popup = altPopup ?: return
        val cell = popupCellWidth(popup)
        val start = popupStart(popup)
        val index = ((x - start) / cell).toInt().coerceIn(0, popup.options.lastIndex)
        if (index != popup.selected) {
            altPopup = popup.copy(selected = index)
            sink.feedback(FeedbackKind.KEY)
        }
    }

    fun onAltRelease() {
        val popup = altPopup ?: return
        altPopup = null
        typeChar(popup.options[popup.selected])
    }

    fun popupCellWidth(p: AltPopup): Float = minOf(p.bounds.width, p.bounds.height * 1.1f).coerceAtLeast(1f)

    /** Левая граница всплывающего ряда альтернатив (в координатах корня клавиатуры). */
    fun popupStart(p: AltPopup): Float {
        val total = popupCellWidth(p) * p.options.size
        val rootWidth = rootCoordinates?.size?.width?.toFloat() ?: Float.MAX_VALUE
        return p.bounds.left.coerceAtMost(rootWidth - total - 4f).coerceAtLeast(0f)
    }

    fun onSpaceSwipe(steps: Int) {
        if (steps == 0) return
        val code = if (steps > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        repeat(kotlin.math.abs(steps)) { sink.moveCursor(code, false) }
        sink.feedback(FeedbackKind.KEY)
    }

    fun onBackspaceRepeat() = backspace()

    // ---------- Логика ввода ----------

    private fun typeChar(text: String) {
        if (translatorOpen) {
            translatorInput += text
            scheduleTranslate()
        } else {
            val separator = text.length == 1 && text[0] in wordSeparators
            val correction = if (separator) finishWord() else null
            lastCorrection = null
            sink.commit(text)
            if (correction != null) lastCorrection = correction.copy(separator = text)
        }
        if (shift == ShiftState.ONCE) shift = ShiftState.OFF
        if (mode == Mode.SYMBOLS && text == "'") mode = Mode.ALPHA
        if (text.length == 1 && text[0].isLetterOrDigit()) lastCorrection = null
        refresh()
    }

    private fun typeSpace() {
        if (translatorOpen) {
            translatorInput += " "
            scheduleTranslate()
            return
        }
        val now = System.currentTimeMillis()
        val before = sink.textBefore(2)
        if (settings.doubleSpacePeriod && now - lastSpaceTap < 600 && before.length == 2 &&
            before[1] == ' ' && before[0].isLetterOrDigit()
        ) {
            sink.deleteBefore(1)
            sink.commit(". ")
            lastSpaceTap = 0
        } else {
            val correction = finishWord()
            lastCorrection = null
            sink.commit(" ")
            if (correction != null) lastCorrection = correction.copy(separator = " ")
            lastSpaceTap = now
        }
        if (mode == Mode.SYMBOLS || mode == Mode.MORE_SYMBOLS) {
            if (fieldKind == FieldKind.TEXT) mode = Mode.ALPHA
        }
        refresh()
    }

    private fun backspace() {
        if (translatorOpen) {
            if (translatorInput.isNotEmpty()) {
                translatorInput = translatorInput.dropLast(1)
                scheduleTranslate()
            }
            return
        }
        // ⌫ сразу после автоисправления возвращает исходное слово.
        if (revertCorrection()) return
        lastCorrection = null
        sink.deleteBackward()
        refresh()
    }

    /**
     * Слово закончено (пробел или знак препинания): автоисправляем и учимся.
     * Возвращает сделанное исправление, если оно было.
     */
    private fun finishWord(): Correction? {
        val word = currentWord
        if (word.isEmpty()) return null
        val s = settings
        if (privateField || fieldKind != FieldKind.TEXT) return null
        val fixed = if (s.autocorrect) WordRepository.correct(word, language.code) else null
        return if (fixed != null && fixed != word) {
            sink.deleteBefore(word.length)
            sink.commit(fixed)
            WordRepository.learn(fixed)
            Correction(word, fixed, "")
        } else {
            WordRepository.learn(word)
            null
        }
    }

    fun revertCorrection(): Boolean {
        val c = lastCorrection ?: return false
        lastCorrection = null
        val tail = c.corrected + c.separator
        if (!sink.textBefore(tail.length).equals(tail)) return false
        sink.deleteBefore(tail.length)
        sink.commit(c.original + c.separator)
        WordRepository.forgetCorrection(c.original)
        refresh()
        return true
    }

    private fun enter() {
        if (translatorOpen) {
            insertTranslation()
            return
        }
        sink.enter()
        refresh()
    }

    private fun toggleShift() {
        val now = System.currentTimeMillis()
        shift = when {
            now - lastShiftTap < 320 && shift == ShiftState.ONCE -> ShiftState.LOCKED
            shift == ShiftState.OFF -> ShiftState.ONCE
            else -> ShiftState.OFF
        }
        lastShiftTap = now
    }

    fun nextLanguage() {
        if (languages.size <= 1) return
        languageIndex = (languageIndex + 1) % languages.size
        languageFlash++
        refresh()
    }

    fun openPanel(p: Panel) {
        panel = if (panel == p) Panel.KEYBOARD else p
        toolbarOpen = false
        altPopup = null
        preview = null
    }

    fun closePanel() {
        panel = Panel.KEYBOARD
    }

    fun commitEmoji(emoji: String) {
        if (translatorOpen) {
            translatorInput += emoji
            scheduleTranslate()
        } else sink.commit(emoji)
    }

    fun commitClip(text: String) {
        sink.commit(text)
        if (panel == Panel.CLIPBOARD) panel = Panel.KEYBOARD
        refresh()
    }

    fun pickSuggestion(s: Suggestion) {
        if (s is Suggestion.Revert) {
            revertCorrection()
            sink.feedback(FeedbackKind.SPECIAL)
            return
        }
        lastCorrection = null
        when (s) {
            is Suggestion.Revert -> Unit
            is Suggestion.Clip -> {
                sink.commit(s.text)
            }
            is Suggestion.Word, is Suggestion.Typed -> {
                if (currentWord.isNotEmpty()) sink.deleteBefore(currentWord.length)
                sink.commit(s.text + " ")
                if (!privateField) WordRepository.learn(s.text, if (s is Suggestion.Typed) 2 else 1)
            }
        }
        sink.feedback(FeedbackKind.SPECIAL)
        refresh()
    }

    // ---------- Подсказки и авто-заглавные ----------

    fun refresh() {
        updateCurrentWord()
        updateSuggestions()
        updateAutoCaps()
    }

    private fun updateCurrentWord() {
        if (translatorOpen) {
            currentWord = ""
            return
        }
        val before = sink.textBefore(48)
        currentWord = before.takeLastWhile { it.isLetter() || it == '\'' || it == '-' }
    }

    private fun updateSuggestions() {
        suggestions.clear()
        val s = settings
        if (!s.suggestions || privateField || fieldKind != FieldKind.TEXT || translatorOpen) return
        if (currentWord.isEmpty()) {
            lastCorrection?.let { suggestions += Suggestion.Revert(it.original) }
            if (s.clipboardSuggest) {
                ClipboardRepository.recent()?.let { suggestions += Suggestion.Clip(it.text) }
            }
            return
        }
        val code = language.code
        // Если слово будет исправлено — показываем исправление главной подсказкой.
        val completions = WordRepository.suggest(currentWord, code, 3)
        val fix = if (s.autocorrect && completions.isEmpty()) WordRepository.correct(currentWord, code) else null
        val words = (listOfNotNull(fix) + completions).distinct()
        suggestions += Suggestion.Typed(currentWord)
        words.take(2).forEachIndexed { i, w -> suggestions += Suggestion.Word(w, primary = i == 0) }
    }

    private fun updateAutoCaps() {
        if (mode != Mode.ALPHA || shift == ShiftState.LOCKED) return
        if (!settings.autoCapitalize) {
            if (shift == ShiftState.ONCE) shift = ShiftState.OFF
            return
        }
        if (!allowAutoCaps || privateField || translatorOpen) {
            if (translatorOpen && translatorInput.isEmpty() && settings.autoCapitalize) shift = ShiftState.ONCE
            return
        }
        val before = sink.textBefore(3)
        val trimmed = before.trimEnd(' ')
        val shouldCap = before.isEmpty() || trimmed.isEmpty() && before.isNotEmpty() ||
            (before.endsWith(" ") && trimmed.lastOrNull() in sentenceEnds) || before.endsWith("\n")
        shift = if (shouldCap) ShiftState.ONCE else ShiftState.OFF
    }

    // ---------- Переводчик ----------

    fun toggleTranslator() {
        translatorOpen = !translatorOpen
        toolbarOpen = false
        panel = Panel.KEYBOARD
        if (translatorOpen) {
            translatorInput = ""
            translatorOutput = ""
            translatorError = null
            translatorDetected = null
            if (mode != Mode.ALPHA) mode = Mode.ALPHA
        }
        refresh()
    }

    fun openLanguagePicker(target: PickerTarget) {
        pickerTarget = target
        panel = if (panel == Panel.LANG_PICKER && pickerTarget == target) Panel.KEYBOARD else Panel.LANG_PICKER
    }

    fun pickTranslateLanguage(code: String) {
        SettingsRepository.update {
            if (pickerTarget == PickerTarget.SOURCE) it.copy(translateSource = code) else it.copy(translateTarget = code)
        }
        panel = Panel.KEYBOARD
        scheduleTranslate(0)
    }

    fun swapTranslateLanguages() {
        val s = settings
        val src = if (s.translateSource == "auto") translatorDetected ?: language.code else s.translateSource
        SettingsRepository.update { it.copy(translateSource = s.translateTarget, translateTarget = src) }
        if (translatorOutput.isNotBlank()) translatorInput = translatorOutput
        scheduleTranslate(0)
    }

    private fun scheduleTranslate(delayMs: Long = 380) {
        translateJob?.cancel()
        val text = translatorInput
        if (text.isBlank()) {
            translatorOutput = ""
            translatorBusy = false
            translatorError = null
            return
        }
        translateJob = scope.launch {
            delay(delayMs)
            translatorBusy = true
            translatorError = null
            when (val r = TranslatorEngine.translate(text, settings.translateSource, settings.translateTarget)) {
                is TranslateResult.Success -> {
                    translatorOutput = r.text
                    translatorDetected = r.detected
                }
                is TranslateResult.Error -> translatorError = r.message
            }
            translatorBusy = false
        }
    }

    fun insertTranslation() {
        val text = translatorOutput.ifBlank { translatorInput }
        if (text.isBlank()) return
        sink.commit(text)
        translatorInput = ""
        translatorOutput = ""
        translatorOpen = false
        refresh()
    }

    /** Перевести весь текст в поле ввода и заменить его переводом. */
    fun translateWholeField() {
        val original = sink.allText()
        if (original.isBlank()) return
        translateJob?.cancel()
        translateJob = scope.launch {
            translatorBusy = true
            translatorError = null
            when (val r = TranslatorEngine.translate(original, settings.translateSource, settings.translateTarget)) {
                is TranslateResult.Success -> {
                    sink.replaceAll(r.text)
                    translatorDetected = r.detected
                }
                is TranslateResult.Error -> translatorError = r.message
            }
            translatorBusy = false
        }
    }

    // ---------- Тулбар ----------

    fun cycleTheme() {
        SettingsRepository.update {
            val values = ThemeStyle.entries
            it.copy(themeStyle = values[(it.themeStyle.ordinal + 1) % values.size])
        }
    }

    fun editKey(code: Int) = sink.moveCursor(code, selectMode)

    companion object {
        private val previewTypes = setOf(KeyType.CHAR, KeyType.COMMA, KeyType.PERIOD)
        private val sentenceEnds = setOf('.', '!', '?', '…')
        private val wordSeparators = setOf('.', ',', '!', '?', ';', ':', '…', ')', '"', '»')
    }
}
