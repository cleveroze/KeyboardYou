package com.keyboardyou.keyboard

/** Тип клавиши определяет её поведение и цвет. */
enum class KeyType { CHAR, SHIFT, BACKSPACE, ENTER, SPACE, SYMBOLS, ALPHA, MORE_SYMBOLS, EMOJI, LANGUAGE, COMMA, PERIOD }

data class Key(
    val label: String,
    val type: KeyType = KeyType.CHAR,
    val width: Float = 1f,
    val alternates: List<String> = emptyList(),
    /** Маленькая подсказка в углу (обычно — первая альтернатива). */
    val hint: String? = alternates.firstOrNull(),
)

data class KeyboardLayout(val rows: List<List<Key>>)

data class Language(val code: String, val name: String, val short: String, val rows: List<String>, val extra: Map<String, List<String>>)

object Layouts {
    val languages = listOf(
        Language(
            "ru", "Русский", "RU",
            listOf("йцукенгшщзх", "фывапролджэ", "ячсмитьбю"),
            mapOf("е" to listOf("ё"), "ь" to listOf("ъ"), "х" to listOf("ъ"), "и" to listOf("й"), "г" to listOf("ґ")),
        ),
        Language(
            "en", "English", "EN",
            listOf("qwertyuiop", "asdfghjkl", "zxcvbnm"),
            mapOf(
                "a" to listOf("á", "à", "â", "ä", "ã", "å", "æ"),
                "e" to listOf("é", "è", "ê", "ë", "ē"),
                "i" to listOf("í", "ì", "î", "ï"),
                "o" to listOf("ó", "ò", "ô", "ö", "õ", "ø", "œ"),
                "u" to listOf("ú", "ù", "û", "ü"),
                "c" to listOf("ç", "ć", "č"),
                "n" to listOf("ñ", "ń"),
                "s" to listOf("ß", "ś", "š"),
                "y" to listOf("ý", "ÿ"),
                "z" to listOf("ž", "ź", "ż"),
            ),
        ),
        Language(
            "uk", "Українська", "UK",
            listOf("йцукенгшщзхї", "фівапролджє", "ячсмитьбю"),
            mapOf("г" to listOf("ґ"), "і" to listOf("ї", "ы"), "ь" to listOf("ъ"), "е" to listOf("ё", "є")),
        ),
    )

    fun language(code: String) = languages.firstOrNull { it.code == code } ?: languages.first()

    private val digits = "1234567890"
    private val numberRowAlternates = listOf(
        listOf("¹", "½", "⅓", "¼"), listOf("²", "⅔"), listOf("³", "¾"), listOf("⁴"), listOf("⁵"),
        listOf("⁶"), listOf("⁷"), listOf("⁸", "∞"), listOf("⁹"), listOf("⁰", "∅"),
    )

    fun numberRow(): List<Key> = digits.mapIndexed { i, c -> Key(c.toString(), alternates = numberRowAlternates[i], hint = null) }

    fun alpha(lang: Language, numberRow: Boolean, hints: Boolean): KeyboardLayout {
        val rows = mutableListOf<List<Key>>()
        if (numberRow) rows += numberRow()
        lang.rows.forEachIndexed { rowIndex, row ->
            val keys = row.mapIndexed { i, c ->
                val s = c.toString()
                val alts = buildList {
                    if (rowIndex == 0 && !numberRow && i < 10) add(digits[i].toString())
                    addAll(lang.extra[s].orEmpty())
                }
                Key(s, alternates = alts, hint = if (hints) alts.firstOrNull() else null)
            }
            rows += when (rowIndex) {
                lang.rows.lastIndex -> buildList {
                    // Shift и ⌫ заполняют ряд до ширины самого длинного ряда букв,
                    // чтобы буквы в остальных рядах были максимально широкими.
                    val widest = lang.rows.maxOf { it.length }
                    val side = ((widest - row.length) / 2f).coerceIn(1f, 1.5f)
                    add(Key("shift", KeyType.SHIFT, side))
                    addAll(keys)
                    add(Key("del", KeyType.BACKSPACE, side))
                }
                else -> keys
            }
        }
        rows += bottomRow(alpha = true)
        return KeyboardLayout(rows)
    }

    private fun bottomRow(alpha: Boolean): List<Key> = listOf(
        if (alpha) Key("?123", KeyType.SYMBOLS, 1.5f) else Key("ABC", KeyType.ALPHA, 1.5f),
        Key(",", KeyType.COMMA, 1f, alternates = listOf(";", ":", "\"", "'", "/"), hint = null),
        Key("emoji", KeyType.EMOJI, 1f),
        Key("space", KeyType.SPACE, 4f),
        Key(".", KeyType.PERIOD, 1f, alternates = listOf("!", "?", "…", ",", "-", "@", "#"), hint = null),
        Key("enter", KeyType.ENTER, 1.5f),
    )

    private fun charRow(chars: String, alts: Map<String, List<String>> = emptyMap()) =
        chars.split(' ').filter { it.isNotEmpty() }.map { Key(it, alternates = alts[it].orEmpty(), hint = null) }

    private val symbolAlternates = mapOf(
        "$" to listOf("€", "£", "¥", "₽", "₴", "¢"),
        "-" to listOf("—", "–", "_", "·"),
        "\"" to listOf("«", "»", "„", "“", "”"),
        "'" to listOf("‘", "’", "‚", "`"),
        "!" to listOf("¡"), "?" to listOf("¿"),
        "%" to listOf("‰", "℅"),
        "*" to listOf("★", "†", "‡"),
        "+" to listOf("±"),
        "(" to listOf("[", "{", "<"), ")" to listOf("]", "}", ">"),
        "1" to listOf("¹", "½", "⅓", "¼"), "2" to listOf("²", "⅔"), "3" to listOf("³", "¾"),
        "0" to listOf("⁰", "∅"),
    )

    fun symbols(): KeyboardLayout = KeyboardLayout(
        listOf(
            charRow("1 2 3 4 5 6 7 8 9 0", symbolAlternates),
            charRow("@ # $ _ & - + ( ) /", symbolAlternates),
            listOf(Key("=\\<", KeyType.MORE_SYMBOLS, 1.5f)) + charRow("* \" ' : ; ! ?", symbolAlternates) +
                listOf(Key("del", KeyType.BACKSPACE, 1.5f)),
            bottomRow(alpha = false),
        )
    )

    fun moreSymbols(): KeyboardLayout = KeyboardLayout(
        listOf(
            charRow("~ ` | • √ π ÷ × ¶ ∆"),
            charRow("£ € ₽ ¥ ^ ° = { } \\"),
            listOf(Key("?123", KeyType.SYMBOLS, 1.5f)) + charRow("% © ® ™ ✓ [ ]") +
                listOf(Key("del", KeyType.BACKSPACE, 1.5f)),
            bottomRow(alpha = false),
        )
    )

    fun numpad(): KeyboardLayout = KeyboardLayout(
        listOf(
            charRow("1 2 3 -"),
            charRow("4 5 6 ,"),
            charRow("7 8 9") + listOf(Key("del", KeyType.BACKSPACE, 1f)),
            listOf(
                Key("ABC", KeyType.ALPHA, 1f),
                Key(".", KeyType.PERIOD, 0.75f),
                Key("0"),
                Key("space", KeyType.SPACE, 1.25f),
                Key("enter", KeyType.ENTER, 1f),
            ),
        )
    )
}
