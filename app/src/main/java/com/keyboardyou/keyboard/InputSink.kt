package com.keyboardyou.keyboard

enum class EnterAction { NEWLINE, SEARCH, SEND, GO, NEXT, DONE, PREVIOUS }

enum class FieldKind { TEXT, NUMBER, PHONE }

enum class FeedbackKind { KEY, DELETE, SPACE, RETURN, SPECIAL }

/** Абстракция над InputConnection — позволяет использовать клавиатуру и в превью настроек. */
interface InputSink {
    fun commit(text: String)
    fun deleteBackward()
    fun deleteBefore(count: Int)
    fun textBefore(count: Int): String
    fun allText(): String
    fun replaceAll(text: String)
    fun enter()
    fun moveCursor(keyCode: Int, select: Boolean)
    fun contextAction(id: Int)
    fun hasSelection(): Boolean
    fun switchToVoice() {}
    fun openSettings() {}
    fun hideKeyboard() {}
    fun feedback(kind: FeedbackKind) {}
}
