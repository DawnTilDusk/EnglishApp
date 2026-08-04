package com.example.seedie.data.local.entity

import com.example.seedie.domain.model.WordSense
import com.example.seedie.domain.model.WordSenseFormat

fun VocabularyWordEntity.resolvedSenses(): List<WordSense> {
    return WordSenseFormat.resolveSenses(
        sensesJson = sensesJson,
        partOfSpeech = partOfSpeech,
        translation = translation
    )
}

fun VocabularyWordEntity.senseDisplayLabel(): String {
    val senses = resolvedSenses()
    if (senses.isEmpty()) {
        return listOfNotNull(
            partOfSpeech.takeIf { it.isNotBlank() },
            translation.takeIf { it.isNotBlank() }
        ).joinToString(" ").trim()
    }
    return WordSenseFormat.displayLabel(senses)
}
