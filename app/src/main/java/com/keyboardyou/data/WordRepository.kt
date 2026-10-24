package com.keyboardyou.data

import android.content.Context
import android.content.SharedPreferences

/** Простой словарь подсказок: встроенные частотные слова + слова, выученные у пользователя. */
object WordRepository {
    private lateinit var prefs: SharedPreferences
    private val learned = HashMap<String, Int>()
    private val base: Map<String, List<String>> = mapOf(
        "ru" to Dictionaries.RU.split(' ').filter { it.isNotBlank() },
        "uk" to Dictionaries.UK.split(' ').filter { it.isNotBlank() },
        "en" to Dictionaries.EN.split(' ').filter { it.isNotBlank() },
    )

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("words", Context.MODE_PRIVATE)
        prefs.all.forEach { (k, v) -> if (v is Int) learned[k] = v }
    }

    fun learn(word: String) {
        val w = word.lowercase()
        if (w.length < 2 || w.any { !it.isLetter() && it != '-' && it != '\'' }) return
        val count = (learned[w] ?: 0) + 1
        learned[w] = count
        if (::prefs.isInitialized) prefs.edit().putInt(w, count).apply()
    }

    fun forget(word: String) {
        learned.remove(word.lowercase())
        if (::prefs.isInitialized) prefs.edit().remove(word.lowercase()).apply()
    }

    fun clearLearned() {
        learned.clear()
        if (::prefs.isInitialized) prefs.edit().clear().apply()
    }

    fun learnedCount(): Int = learned.size

    fun suggest(prefix: String, lang: String, limit: Int = 3): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val p = prefix.lowercase()
        val words = base[lang].orEmpty()
        val scored = HashMap<String, Int>()
        words.forEachIndexed { index, w ->
            if (w.startsWith(p) && w != p) scored[w] = 1000 - index.coerceAtMost(999)
        }
        learned.forEach { (w, c) ->
            if (w.startsWith(p) && w != p) scored[w] = (scored[w] ?: 0) + 400 * c
        }
        val upper = prefix.first().isUpperCase()
        val allUpper = prefix.length > 1 && prefix.all { !it.isLetter() || it.isUpperCase() }
        return scored.entries.sortedByDescending { it.value }.take(limit).map { (w, _) ->
            when {
                allUpper -> w.uppercase()
                upper -> w.replaceFirstChar { it.uppercase() }
                else -> w
            }
        }
    }
}
