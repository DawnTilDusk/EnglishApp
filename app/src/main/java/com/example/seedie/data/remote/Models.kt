package com.example.seedie.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val role: String,
    @SerialName("agency_id") val agency_id: String?,
    @SerialName("display_name") val display_name: String?,
    val grade: String? = null,
    val status: String?,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("phone_verified") val phone_verified: Boolean = false,
    @SerialName("phone_updated_at") val phone_updated_at: String? = null,
    @SerialName("current_device_id") val current_device_id: String? = null,
    @SerialName("avatar_tone") val avatar_tone: Int = 0
)

@Serializable
data class Student(
    val id: String,
    @SerialName("agency_id") val agency_id: String,
    val name: String,
    @SerialName("student_no") val student_no: String?,
    @SerialName("class_id") val class_id: String?,
    @SerialName("teacher_id") val teacher_id: String? = null
)

@Serializable
data class Teacher(
    val id: String,
    @SerialName("display_name") val display_name: String,
    @SerialName("agency_id") val agency_id: String? = null
)

@Serializable
data class SupabaseShopProduct(
    val id: String,
    @SerialName("agency_id") val agency_id: String,
    val name: String,
    val description: String? = null,
    @SerialName("price_tokens") val price_tokens: Int,
    val stock: Int = -1,
    @SerialName("image_url") val image_url: String? = null,
    @SerialName("is_active") val is_active: Boolean = true
)

@Serializable
data class SupabaseShopOrder(
    val id: String,
    @SerialName("student_id") val student_id: String,
    @SerialName("agency_id") val agency_id: String,
    @SerialName("product_id") val product_id: String,
    @SerialName("tokens_amount") val tokens_amount: Int,
    val status: String,
    @SerialName("created_at") val created_at: String? = null
)

@Serializable
data class UserEconomyTransactionDto(
    val id: String,
    @SerialName("user_id") val user_id: String,
    val amount: Int,
    val reason: String,
    @SerialName("ref_id") val ref_id: String? = null
)

@Serializable
data class SupabaseWordBook(
    @SerialName("book_id") val book_id: String,
    val title: String,
    val description: String? = null,
    val language: String? = null,
    val difficulty: String? = null,
    val version: Int = 1,
    @SerialName("word_count") val word_count: Int = 0,
    @SerialName("cover_url") val cover_url: String? = null,
    @SerialName("updated_at") val updated_at: Long = 0L
)

@Serializable
data class SupabaseWordBookModule(
    @SerialName("module_id") val module_id: String,
    @SerialName("book_id") val book_id: String,
    val title: String,
    @SerialName("sort_order") val sort_order: Int,
    @SerialName("word_count") val word_count: Int = 0
)

@Serializable
data class SupabaseVocabularyWord(
    @SerialName("word_id") val word_id: String,
    @SerialName("book_id") val book_id: String,
    @SerialName("module_id") val module_id: String? = null,
    val english: String,
    val phonetic: String? = null,
    @SerialName("part_of_speech") val part_of_speech: String? = null,
    val translation: String? = null,
    @SerialName("example_sentence") val example_sentence: String? = null,
    @SerialName("difficulty_level") val difficulty_level: String? = null,
    @SerialName("reward_token") val reward_token: Int = 0,
    @SerialName("estimated_duration_sec") val estimated_duration_sec: Int = 0,
    @SerialName("sort_order") val sort_order: Int = 0,
    @SerialName("audio_url") val audio_url: String? = null
)
