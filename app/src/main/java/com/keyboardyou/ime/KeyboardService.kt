package com.keyboardyou.ime

import android.content.ClipboardManager
import android.content.Intent
import android.graphics.PixelFormat
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.keyboardyou.data.ClipboardRepository
import com.keyboardyou.data.CrashLog
import com.keyboardyou.data.SettingsRepository
import com.keyboardyou.data.WordRepository
import com.keyboardyou.data.ThemeStyle
import com.keyboardyou.keyboard.EnterAction
import com.keyboardyou.keyboard.FeedbackKind
import com.keyboardyou.keyboard.FieldKind
import com.keyboardyou.keyboard.InputSink
import com.keyboardyou.keyboard.KeyboardController
import com.keyboardyou.keyboard.ui.KeyboardRoot
import com.keyboardyou.keyboard.ui.LocalFeedback
import com.keyboardyou.ui.MainActivity
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

class KeyboardService : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner, InputSink {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private val scope = MainScope()
    private lateinit var controller: KeyboardController
    private var clipboard: ClipboardManager? = null
    private var vibrator: Vibrator? = null
    private var audio: AudioManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { captureClip() }

    override fun onCreate() {
        super.onCreate()
        SettingsRepository.init(this)
        ClipboardRepository.init(this)
        WordRepository.init(this)
        com.keyboardyou.data.EmojiStore.init(this)
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        controller = KeyboardController(scope, this)
        clipboard = getSystemService(ClipboardManager::class.java)
        clipboard?.addPrimaryClipChangedListener(clipListener)
        audio = getSystemService(AudioManager::class.java)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
        LocalFeedback.feedback = { feedback(FeedbackKind.SPECIAL) }
        CrashLog.startWatchdog(this)
    }

    override fun onCreateInputView(): View {
        window.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
            decor.setViewTreeViewModelStoreOwner(this)
        }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        return ComposeView(this).apply {
            setContent {
                val settings by SettingsRepository.state.collectAsState()
                val blur = settings.blurBehind && settings.themeStyle == ThemeStyle.LIQUID_GLASS
                LaunchedEffect(blur) { applyBlur(blur) }
                KeyboardRoot(
                    controller = controller,
                    settings = settings,
                    backgroundAlpha = if (blur && blurSupported()) 0.55f else 1f,
                )
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        val kind = when (cls) {
            InputType.TYPE_CLASS_NUMBER, InputType.TYPE_CLASS_DATETIME -> FieldKind.NUMBER
            InputType.TYPE_CLASS_PHONE -> FieldKind.PHONE
            else -> FieldKind.TEXT
        }
        val password = cls == InputType.TYPE_CLASS_TEXT && variation in setOf(
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        ) || cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        val incognito = info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
        val noSuggest = info.inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0 &&
            info.inputType and InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE == 0 && password
        val autoCaps = cls == InputType.TYPE_CLASS_TEXT && !password && variation !in setOf(
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_FILTER,
        )
        controller.onStartInput(kind, enterActionFor(info), password || incognito || noSuggest, autoCaps, animateIn = !restarting)
        captureClip()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        controller.onSelectionChanged()
    }

    override fun onDestroy() {
        clipboard?.removePrimaryClipChangedListener(clipListener)
        LocalFeedback.feedback = null
        CrashLog.stopWatchdog()
        scope.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDestroy()
    }

    private fun enterActionFor(info: EditorInfo): EnterAction {
        if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return EnterAction.NEWLINE
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> EnterAction.SEARCH
            EditorInfo.IME_ACTION_SEND -> EnterAction.SEND
            EditorInfo.IME_ACTION_GO -> EnterAction.GO
            EditorInfo.IME_ACTION_NEXT -> EnterAction.NEXT
            EditorInfo.IME_ACTION_DONE -> EnterAction.DONE
            EditorInfo.IME_ACTION_PREVIOUS -> EnterAction.PREVIOUS
            else -> EnterAction.NEWLINE
        }
    }

    private fun captureClip() {
        if (!SettingsRepository.current.clipboardHistory) return
        runCatching {
            val clip = clipboard?.primaryClip ?: return
            val sensitive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                clip.description?.extras?.getBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE) == true
            if (sensitive || clip.itemCount == 0) return
            val text = clip.getItemAt(0).coerceToText(this)?.toString() ?: return
            if (ClipboardRepository.items.value.firstOrNull()?.text == text.trimEnd()) return
            ClipboardRepository.add(text)
            if (::controller.isInitialized) controller.refresh()
        }
    }

    private fun blurSupported(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true

    /** Экспериментально: размытие того, что под клавиатурой (Android 12+). */
    private fun applyBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val w = window.window ?: return
        runCatching {
            if (enabled && blurSupported()) {
                w.setFormat(PixelFormat.TRANSLUCENT)
                w.setBackgroundBlurRadius(90)
            } else {
                w.setBackgroundBlurRadius(0)
            }
        }
    }

    // ---------------- InputSink ----------------

    override fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun deleteBackward() {
        val ic = currentInputConnection ?: return
        if (hasSelection()) ic.commitText("", 1) else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    override fun deleteBefore(count: Int) {
        currentInputConnection?.deleteSurroundingText(count, 0)
    }

    override fun textBefore(count: Int): String =
        currentInputConnection?.getTextBeforeCursor(count, 0)?.toString() ?: ""

    override fun allText(): String {
        val ic = currentInputConnection ?: return ""
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted?.text != null) return extracted.text.toString()
        val before = ic.getTextBeforeCursor(10_000, 0) ?: ""
        val after = ic.getTextAfterCursor(10_000, 0) ?: ""
        return before.toString() + after.toString()
    }

    override fun replaceAll(text: String) {
        val ic = currentInputConnection ?: return
        ic.beginBatchEdit()
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        if (extracted?.text != null) {
            ic.setSelection(extracted.startOffset, extracted.startOffset + extracted.text.length)
        } else {
            ic.performContextMenuAction(android.R.id.selectAll)
        }
        ic.commitText(text, 1)
        ic.endBatchEdit()
    }

    override fun enter() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val action = info?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        val noEnterAction = info != null && info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        if (noEnterAction || action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        } else {
            ic.performEditorAction(action)
        }
    }

    override fun moveCursor(keyCode: Int, select: Boolean) {
        val ic = currentInputConnection ?: return
        val meta = if (select) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }

    override fun contextAction(id: Int) {
        currentInputConnection?.performContextMenuAction(id)
    }

    override fun hasSelection(): Boolean =
        !currentInputConnection?.getSelectedText(0).isNullOrEmpty()

    override fun switchToVoice() {
        val imm = getSystemService(InputMethodManager::class.java) ?: return
        for (imi in imm.enabledInputMethodList) {
            for (subtype in imm.getEnabledInputMethodSubtypeList(imi, true)) {
                if (subtype.mode == "voice" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    switchInputMethod(imi.id, subtype)
                    return
                }
            }
        }
        Toast.makeText(this, "Голосовой ввод не найден", Toast.LENGTH_SHORT).show()
    }

    override fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun hideKeyboard() {
        requestHideSelf(0)
    }

    override fun feedback(kind: FeedbackKind) {
        val s = SettingsRepository.current
        if (s.haptics) vibrate(s.hapticStrength, kind)
        if (s.sound) {
            val fx = when (kind) {
                FeedbackKind.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                FeedbackKind.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                FeedbackKind.RETURN -> AudioManager.FX_KEYPRESS_RETURN
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            audio?.playSoundEffect(fx, s.soundVolume)
        }
    }

    private fun vibrate(strength: Int, kind: FeedbackKind) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val boost = if (kind == FeedbackKind.SPECIAL || kind == FeedbackKind.RETURN) 1.25f else 1f
        val duration = (6 + strength / 9f * boost).toLong()
        val amplitude = ((25 + strength * 2.3f) * boost).toInt().coerceIn(1, 255)
        runCatching {
            if (v.hasAmplitudeControl()) v.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            else v.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}
