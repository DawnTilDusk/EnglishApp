package com.example.seedie.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WordSense(
    @SerialName("part_of_speech") val partOfSpeech: String,
    val translation: String
)

object WordSenseFormat {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseSensesJson(raw: String?): List<WordSense> {
        if (raw.isNullOrBlank() || raw == "[]") return emptyList()
        return runCatching {
            json.decodeFromString<List<WordSense>>(raw)
        }.getOrDefault(emptyList())
    }

    fun encodeSensesJson(senses: List<WordSense>): String {
        return json.encodeToString(senses)
    }

    fun fromLegacy(partOfSpeech: String?, translation: String?): List<WordSense> {
        val zh = translation?.trim().orEmpty()
        val pos = partOfSpeech?.trim().orEmpty().ifBlank { "n." }
        if (zh.isBlank()) return emptyList()
        return listOf(WordSense(partOfSpeech = pos, translation = zh))
    }

    fun resolveSenses(
        sensesJson: String?,
        partOfSpeech: String?,
        translation: String?
    ): List<WordSense> {
        val parsed = parseSensesJson(sensesJson).filter {
            it.partOfSpeech.isNotBlank() && it.translation.isNotBlank()
        }
        if (parsed.isNotEmpty()) return parsed
        return fromLegacy(partOfSpeech, translation)
    }

    /** Single-line label: `n. 改变；v. 变化` */
    fun displayLabel(senses: List<WordSense>): String {
        return senses.joinToString("；") { sense ->
            val pos = sense.partOfSpeech.trim().let { if (it.endsWith(".")) it else "$it." }
            "$pos ${sense.translation.trim()}"
        }
    }
}
