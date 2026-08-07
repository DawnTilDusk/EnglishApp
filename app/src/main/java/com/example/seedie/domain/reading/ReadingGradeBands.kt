package com.example.seedie.domain.reading

/**
 * Maps student profile grade labels to reading catalog grade bands (七上–九下).
 * Profile uses 初一/初二/初三; content uses g7a…g9b.
 */
object ReadingGradeBands {
    const val G7A = "g7a"
    const val G7B = "g7b"
    const val G8A = "g8a"
    const val G8B = "g8b"
    const val G9A = "g9a"
    const val G9B = "g9b"

    val ALL: Set<String> = setOf(G7A, G7B, G8A, G8B, G9A, G9B)

    private val displayNames = mapOf(
        G7A to "七上",
        G7B to "七下",
        G8A to "八上",
        G8B to "八下",
        G9A to "九上",
        G9B to "九下"
    )

    fun bandsForProfileGrade(grade: String?): Set<String> {
        return when (grade?.trim()) {
            "初一" -> setOf(G7A, G7B)
            "初二" -> setOf(G8A, G8B)
            "初三" -> setOf(G9A, G9B)
            else -> emptySet()
        }
    }

    fun displayName(band: String?): String? {
        if (band.isNullOrBlank()) return null
        return displayNames[band]
    }

    /** Empty-catalog copy when profile grade is outside junior-middle bands. */
    const val EMPTY_NON_JUNIOR_MESSAGE =
        "阅读练习面向初中，请在个人资料中将年级设为初一、初二或初三"

    const val EMPTY_CATALOG_MESSAGE = "题库暂无内容"
}
