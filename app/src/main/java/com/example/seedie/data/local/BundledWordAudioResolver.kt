package com.example.seedie.data.local

import com.example.seedie.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BundledWordAudioResolver @Inject constructor() {
    fun rawResId(wordId: String): Int = rawByWordId[wordId] ?: 0

    fun hasBundledAudio(wordId: String): Boolean = rawResId(wordId) != 0

    companion object {
        private val rawByWordId: Map<String, Int> = mapOf(
            "w1" to R.raw.word_w1,
            "w2" to R.raw.word_w2,
            "w3" to R.raw.word_w3,
            "w4" to R.raw.word_w4,
            "w5" to R.raw.word_w5,
            "w6" to R.raw.word_w6,
            "w7" to R.raw.word_w7,
            "w8" to R.raw.word_w8,
            "w9" to R.raw.word_w9,
            "w10" to R.raw.word_w10,
            "w11" to R.raw.word_w11,
            "w12" to R.raw.word_w12,
            "w13" to R.raw.word_w13,
            "w14" to R.raw.word_w14,
            "w15" to R.raw.word_w15,
            "w16" to R.raw.word_w16,
            "w17" to R.raw.word_w17,
            "w18" to R.raw.word_w18,
            "w19" to R.raw.word_w19,
            "w20" to R.raw.word_w20,
            "w21" to R.raw.word_w21,
            "w22" to R.raw.word_w22,
            "w23" to R.raw.word_w23,
            "w24" to R.raw.word_w24
        )
    }
}
