package com.keyboardyou.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

data class TranslateLang(val code: String, val name: String, val flag: String)

sealed interface TranslateResult {
    data class Success(val text: String, val detected: String?) : TranslateResult
    data class Error(val message: String) : TranslateResult
}

/** Офлайн-переводчик на ML Kit: модели (~30 МБ на язык) скачиваются один раз. */
object TranslatorEngine {
    val languages = listOf(
        TranslateLang(TranslateLanguage.RUSSIAN, "Русский", "🇷🇺"),
        TranslateLang(TranslateLanguage.ENGLISH, "English", "🇬🇧"),
        TranslateLang(TranslateLanguage.UKRAINIAN, "Українська", "🇺🇦"),
        TranslateLang(TranslateLanguage.GERMAN, "Deutsch", "🇩🇪"),
        TranslateLang(TranslateLanguage.FRENCH, "Français", "🇫🇷"),
        TranslateLang(TranslateLanguage.SPANISH, "Español", "🇪🇸"),
        TranslateLang(TranslateLanguage.ITALIAN, "Italiano", "🇮🇹"),
        TranslateLang(TranslateLanguage.PORTUGUESE, "Português", "🇵🇹"),
        TranslateLang(TranslateLanguage.POLISH, "Polski", "🇵🇱"),
        TranslateLang(TranslateLanguage.TURKISH, "Türkçe", "🇹🇷"),
        TranslateLang(TranslateLanguage.CHINESE, "中文", "🇨🇳"),
        TranslateLang(TranslateLanguage.JAPANESE, "日本語", "🇯🇵"),
        TranslateLang(TranslateLanguage.KOREAN, "한국어", "🇰🇷"),
        TranslateLang(TranslateLanguage.ARABIC, "العربية", "🇸🇦"),
        TranslateLang(TranslateLanguage.HINDI, "हिन्दी", "🇮🇳"),
        TranslateLang(TranslateLanguage.CZECH, "Čeština", "🇨🇿"),
        TranslateLang(TranslateLanguage.DUTCH, "Nederlands", "🇳🇱"),
        TranslateLang(TranslateLanguage.SWEDISH, "Svenska", "🇸🇪"),
        TranslateLang(TranslateLanguage.GEORGIAN, "ქართული", "🇬🇪"),
        TranslateLang(TranslateLanguage.BELARUSIAN, "Беларуская", "🇧🇾"),
    )

    val auto = TranslateLang("auto", "Авто", "✨")

    fun lang(code: String): TranslateLang = if (code == "auto") auto else languages.firstOrNull { it.code == code } ?: languages[1]

    private val translators = HashMap<Pair<String, String>, Translator>()
    private val identifier by lazy { LanguageIdentification.getClient() }
    private val modelManager by lazy { RemoteModelManager.getInstance() }

    private fun client(source: String, target: String): Translator = translators.getOrPut(source to target) {
        Translation.getClient(
            TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build()
        )
    }

    suspend fun detect(text: String): String? = runCatching {
        val tag = identifier.identifyLanguage(text).await()
        if (tag == "und") null else TranslateLanguage.fromLanguageTag(tag)
    }.getOrNull()

    suspend fun translate(text: String, source: String, target: String): TranslateResult {
        if (text.isBlank()) return TranslateResult.Success("", null)
        return try {
            val detected = if (source == "auto") detect(text) ?: fallbackDetect(text) else source
            if (detected == target) return TranslateResult.Success(text, detected)
            val translator = client(detected, target)
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
            TranslateResult.Success(translator.translate(text).await(), detected)
        } catch (e: Exception) {
            TranslateResult.Error(e.message ?: "Не удалось перевести")
        }
    }

    /** Если ML Kit не уверен (короткий текст) — смотрим на алфавит. */
    private fun fallbackDetect(text: String): String =
        if (text.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' }) TranslateLanguage.RUSSIAN else TranslateLanguage.ENGLISH

    suspend fun downloadedModels(): Set<String> = runCatching {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await().map { it.language }.toSet()
    }.getOrDefault(emptySet())

    suspend fun download(code: String): Boolean = runCatching {
        modelManager.download(TranslateRemoteModel.Builder(code).build(), DownloadConditions.Builder().build()).await()
        true
    }.getOrDefault(false)

    suspend fun delete(code: String): Boolean = runCatching {
        modelManager.deleteDownloadedModel(TranslateRemoteModel.Builder(code).build()).await()
        true
    }.getOrDefault(false)
}
