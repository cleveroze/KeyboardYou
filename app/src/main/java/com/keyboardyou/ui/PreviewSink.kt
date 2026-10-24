package com.keyboardyou.ui

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.keyboardyou.keyboard.InputSink

/** Поле ввода «понарошку» для живого превью клавиатуры в настройках. */
class PreviewSink(initial: String = "") : InputSink {
    var text by mutableStateOf(initial)
    private var cursor = initial.length

    override fun commit(text: String) {
        this.text = this.text.substring(0, cursor) + text + this.text.substring(cursor)
        cursor += text.length
        trim()
    }

    override fun deleteBackward() {
        if (cursor == 0) return
        val cut = Character.offsetByCodePoints(text, cursor, -1)
        text = text.substring(0, cut) + text.substring(cursor)
        cursor = cut
    }

    override fun deleteBefore(count: Int) {
        val start = (cursor - count).coerceAtLeast(0)
        text = text.substring(0, start) + text.substring(cursor)
        cursor = start
    }

    override fun textBefore(count: Int): String = text.substring((cursor - count).coerceAtLeast(0), cursor)
    override fun allText(): String = text
    override fun replaceAll(text: String) {
        this.text = text
        cursor = text.length
    }

    override fun enter() = commit("\n")

    override fun moveCursor(keyCode: Int, select: Boolean) {
        cursor = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> (cursor - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_RIGHT -> (cursor + 1).coerceAtMost(text.length)
            KeyEvent.KEYCODE_MOVE_HOME, KeyEvent.KEYCODE_DPAD_UP -> 0
            else -> text.length
        }
    }

    override fun contextAction(id: Int) {
        if (id == android.R.id.selectAll) cursor = text.length
    }

    override fun hasSelection() = false

    private fun trim() {
        if (text.length > 200) {
            val drop = text.length - 200
            text = text.drop(drop)
            cursor = (cursor - drop).coerceAtLeast(0)
        }
    }

    fun clear() {
        text = ""
        cursor = 0
    }
}
