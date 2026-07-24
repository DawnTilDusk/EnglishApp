package com.example.seedie.domain.profile

object ProfileGradeOptions {
    const val DEFAULT_GRADE = "一年级"

    val values: List<String> = listOf(
        "一年级",
        "二年级",
        "三年级",
        "四年级",
        "五年级",
        "六年级",
        "初一",
        "初二",
        "初三",
        "高一",
        "高二",
        "高三",
        "其他"
    )

    fun normalize(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        return values.firstOrNull { it == trimmed } ?: DEFAULT_GRADE
    }
}
