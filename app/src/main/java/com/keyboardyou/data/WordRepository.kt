package com.keyboardyou.data

import android.content.Context
import android.content.SharedPreferences
import com.keyboardyou.keyboard.Layouts
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.min

/**
 * Словарь для подсказок и автоисправления: частотные словари (assets/dict) +
 * слова, которые клавиатура выучила у пользователя.
 */
object WordRepository {
    private class Dict(val freq: HashMap<String, Int>, val sorted: Array<String>)

    private lateinit var prefs: SharedPreferences
    private lateinit var appContext: Context
    private val learned = ConcurrentHashMap<String, Int>()
    private val dicts = ConcurrentHashMap<String, Dict>()
    private val loading = ConcurrentHashMap.newKeySet<String>()
    private val loader = Executors.newSingleThreadExecutor()

    private val alphabets = mapOf(
        "ru" to "абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
        "uk" to "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя'",
        "en" to "abcdefghijklmnopqrstuvwxyz'",
    )
    private val neighbours = HashMap<String, Map<Char, Set<Char>>>()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences("words", Context.MODE_PRIVATE)
        prefs.all.forEach { (k, v) -> if (v is Int) learned[k] = v }
        SettingsRepository.current.languages.forEach { ensureLoaded(it) }
    }

    fun ensureLoaded(lang: String) {
        if (!::appContext.isInitialized || dicts.containsKey(lang) || !loading.add(lang)) return
        loader.execute {
            runCatching {
                val freq = HashMap<String, Int>(60_000)
                appContext.assets.open("dict/$lang.txt").bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val space = line.indexOf(' ')
                        if (space > 0) freq[line.substring(0, space)] = line.substring(space + 1).toIntOrNull() ?: 1
                    }
                }
                val sorted = freq.keys.toTypedArray().also { it.sort() }
                dicts[lang] = Dict(freq, sorted)
            }
            loading.remove(lang)
        }
    }

    // ---------------- Обучение ----------------

    fun learn(word: String, weight: Int = 1) {
        val w = word.lowercase()
        if (w.length < 2 || w.any { !it.isLetter() && it != '-' && it != '\'' }) return
        val count = (learned[w] ?: 0) + weight
        learned[w] = count
        if (::prefs.isInitialized) prefs.edit().putInt(w, count).apply()
    }

    /** Пользователь отменил исправление — значит, слово настоящее, запоминаем его сильнее. */
    fun forgetCorrection(original: String) = learn(original, 3)

    fun clearLearned() {
        learned.clear()
        if (::prefs.isInitialized) prefs.edit().clear().apply()
    }

    fun learnedCount(): Int = learned.size

    private fun score(w: String, lang: String): Long =
        (dicts[lang]?.freq?.get(w) ?: 0).toLong() + (learned[w] ?: 0) * 4000L

    fun isKnown(word: String, lang: String): Boolean {
        val w = word.lowercase()
        return dicts[lang]?.freq?.containsKey(w) == true || (learned[w] ?: 0) > 0
    }

    // ---------------- Подсказки ----------------

    fun suggest(prefix: String, lang: String, limit: Int = 3): List<String> {
        if (prefix.isEmpty()) return emptyList()
        ensureLoaded(lang)
        val p = prefix.lowercase()
        val scored = HashMap<String, Long>()
        dicts[lang]?.let { d ->
            var i = lowerBound(d.sorted, p)
            var scanned = 0
            while (i < d.sorted.size && d.sorted[i].startsWith(p) && scanned < 6000) {
                val w = d.sorted[i]
                if (w != p) scored[w] = score(w, lang)
                i++
                scanned++
            }
        }
        learned.keys.forEach { w -> if (w.startsWith(p) && w != p) scored[w] = score(w, lang) }
        return scored.entries.sortedByDescending { it.value }.take(limit).map { matchCase(prefix, it.key) }
    }

    // ---------------- Автоисправление ----------------

    /** Возвращает исправленное слово или null, если исправлять не нужно. */
    fun correct(word: String, lang: String): String? {
        val dict = dicts[lang] ?: run { ensureLoaded(lang); return null }
        val w = word.lowercase()
        if (w.length < 2 || w.any { !it.isLetter() && it != '\'' && it != '-' }) return null
        if (word.drop(1).any { it.isUpperCase() }) return null // АББРЕВИАТУРЫ и ИмЕнА не трогаем
        if (isKnown(w, lang)) return null
        val alphabet = alphabets[lang] ?: return null
        val near = neighboursFor(lang)

        var best: String? = null
        var bestScore = 0.0
        fun consider(candidate: String, bonus: Double) {
            if (candidate == w) return
            val s = score(candidate, lang)
            if (s <= 0) return
            val value = s * bonus
            if (value > bestScore) {
                bestScore = value
                best = candidate
            }
        }

        // Расстояние 1: пропуск, лишняя буква, перестановка, замена (соседние клавиши — вероятнее).
        for (i in 0..w.length) {
            if (i < w.length) consider(w.removeRange(i, i + 1), 1.0)
            if (i < w.length - 1) consider(w.substring(0, i) + w[i + 1] + w[i] + w.substring(i + 2), 2.5)
            for (c in alphabet) {
                if (i < w.length && c != w[i]) {
                    val adjacent = near[w[i]]?.contains(c) == true
                    val easy = (w[i] == 'е' && c == 'ё') || (w[i] == 'ь' && c == 'ъ') || (w[i] == 'и' && c == 'й')
                    consider(w.substring(0, i) + c + w.substring(i + 1), if (easy) 8.0 else if (adjacent) 3.0 else 0.6)
                }
                consider(w.substring(0, i) + c + w.substring(i), 1.0)
            }
        }
        // Расстояние 2 — только для длинных слов и только среди частых.
        if (best == null && w.length >= 5) {
            for (candidate in dict.sorted) {
                if (abs(candidate.length - w.length) > 2) continue
                if (candidate[0] != w[0] && candidate.getOrNull(1) != w.getOrNull(1)) continue
                if (score(candidate, lang) < 200) continue
                if (distance(w, candidate, 2) <= 2) consider(candidate, 0.15)
            }
        }
        // Слишком редкие кандидаты не стоят того, чтобы менять слово пользователя.
        if (bestScore < 30) return null
        return best?.let { matchCase(word, it) }
    }

    private fun neighboursFor(lang: String): Map<Char, Set<Char>> = neighbours.getOrPut(lang) {
        val rows = Layouts.language(lang).rows
        val map = HashMap<Char, MutableSet<Char>>()
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { i, c ->
                val set = map.getOrPut(c) { HashSet() }
                row.getOrNull(i - 1)?.let(set::add)
                row.getOrNull(i + 1)?.let(set::add)
                for (dr in listOf(-1, 1)) {
                    val other = rows.getOrNull(r + dr) ?: continue
                    for (di in -1..1) other.getOrNull(i + di)?.let(set::add)
                }
            }
        }
        map
    }

    /** Расстояние Дамерау–Левенштейна с ранним выходом, если оно больше [max]. */
    private fun distance(a: String, b: String, max: Int): Int {
        val n = a.length
        val m = b.length
        var prev2 = IntArray(m + 1)
        var prev = IntArray(m + 1) { it }
        var cur = IntArray(m + 1)
        for (i in 1..n) {
            cur[0] = i
            var rowMin = cur[0]
            for (j in 1..m) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = min(min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = min(v, prev2[j - 2] + 1)
                cur[j] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > max) return max + 1
            val t = prev2
            prev2 = prev
            prev = cur
            cur = t
        }
        return prev[m]
    }

    private fun lowerBound(arr: Array<String>, key: String): Int {
        var lo = 0
        var hi = arr.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (arr[mid] < key) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun matchCase(pattern: String, word: String): String = when {
        pattern.length > 1 && pattern.all { !it.isLetter() || it.isUpperCase() } -> word.uppercase()
        pattern.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercase() }
        word == "i" -> "I"
        else -> word
    }
}
