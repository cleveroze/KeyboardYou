package com.keyboardyou.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class ClipItem(
    val id: Long,
    val text: String,
    val time: Long,
    val pinned: Boolean = false,
)

object ClipboardRepository {
    private lateinit var prefs: SharedPreferences
    private val _items = MutableStateFlow<List<ClipItem>>(emptyList())
    val items: StateFlow<List<ClipItem>> = _items.asStateFlow()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("clipboard", Context.MODE_PRIVATE)
        _items.value = load()
    }

    fun add(text: String) {
        val clean = text.trimEnd()
        if (clean.isBlank()) return
        val existing = _items.value.firstOrNull { it.text == clean }
        val rest = _items.value.filter { it.text != clean }
        val item = ClipItem(
            id = System.nanoTime(),
            text = clean,
            time = System.currentTimeMillis(),
            pinned = existing?.pinned ?: false,
        )
        val limit = SettingsRepository.current.clipboardLimit
        val pinned = rest.filter { it.pinned }
        val unpinned = rest.filter { !it.pinned }.take((limit - 1).coerceAtLeast(0))
        commit(listOf(item) + pinned + unpinned)
    }

    fun togglePin(id: Long) = commit(_items.value.map { if (it.id == id) it.copy(pinned = !it.pinned) else it })

    fun remove(id: Long) = commit(_items.value.filter { it.id != id })

    fun clearUnpinned() = commit(_items.value.filter { it.pinned })

    /** Последняя запись, если она скопирована недавно — для подсказки в строке предложений. */
    fun recent(windowMs: Long = 60_000): ClipItem? =
        _items.value.maxByOrNull { it.time }?.takeIf { System.currentTimeMillis() - it.time < windowMs }

    private fun commit(list: List<ClipItem>) {
        val sorted = list.sortedWith(compareByDescending<ClipItem> { it.pinned }.thenByDescending { it.time })
        _items.value = sorted
        if (!::prefs.isInitialized) return
        val arr = JSONArray()
        sorted.forEach {
            arr.put(JSONObject().put("id", it.id).put("text", it.text).put("time", it.time).put("pinned", it.pinned))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    private fun load(): List<ClipItem> = runCatching {
        val arr = JSONArray(prefs.getString("items", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ClipItem(o.getLong("id"), o.getString("text"), o.getLong("time"), o.optBoolean("pinned"))
        }
    }.getOrDefault(emptyList())
}
