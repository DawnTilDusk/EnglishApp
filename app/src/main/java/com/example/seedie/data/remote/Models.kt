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
    @SerialName("avatar_tone") val avatar_tone: Int = 0,
    @SerialName("vocabulary_size") val vocabulary_size: Int = 0,
    @SerialName("vocabulary_estimated_at") val vocabulary_estimated_at: String? = null
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
    @SerialName("updated_at") val updated_at: Long = 0L,
    @SerialName("grade_level") val grade_level: Int? = null
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
    @SerialName("difficulty_value") val difficulty_value: Int? = null,
    @SerialName("reward_token") val reward_token: Int = 0,
    @SerialName("estimated_duration_sec") val estimated_duration_sec: Int = 0,
    @SerialName("sort_order") val sort_order: Int = 0,
    @SerialName("audio_url") val audio_url: String? = null,
    val senses: List<SupabaseWordSense>? = null,
    @SerialName("master_id") val master_id: String? = null
)

@Serializable
data class SupabaseWordSense(
    @SerialName("part_of_speech") val part_of_speech: String? = null,
    val translation: String? = null
)

@Serializable
data class SupabaseReadingSet(
    @SerialName("set_id") val set_id: String,
    val title: String,
    @SerialName("title_zh") val title_zh: String? = null,
    val grade: Int = 8,
    @SerialName("grade_band") val grade_band: String = "",
    /** Provenance for audit only; do not surface in UI. */
    @SerialName("content_origin") val content_origin: String = "ai_generated",
    val difficulty: String = "medium",
    val topic: String? = null,
    val passage: String,
    @SerialName("word_count") val word_count: Int = 0,
    @SerialName("estimated_minutes") val estimated_minutes: Int = 8,
    @SerialName("sort_order") val sort_order: Int = 0,
    val version: Int = 1,
    @SerialName("updated_at") val updated_at: Long = 0L
)

@Serializable
data class SupabaseReadingQuestion(
    @SerialName("question_id") val question_id: String,
    @SerialName("set_id") val set_id: String,
    @SerialName("question_type") val question_type: String? = null,
    val stem: String,
    @SerialName("sort_order") val sort_order: Int = 0,
    @SerialName("correct_option_id") val correct_option_id: String,
    val explanation: String,
    @SerialName("highlight_word") val highlight_word: String? = null,
    @SerialName("reward_token") val reward_token: Int = 2
)

@Serializable
data class SupabaseReadingOption(
    @SerialName("question_id") val question_id: String,
    @SerialName("option_id") val option_id: String,
    @SerialName("option_text") val option_text: String,
    @SerialName("sort_order") val sort_order: Int = 0
)

@Serializable
data class SupabaseListeningMaterial(
    @SerialName("material_id") val material_id: String,
    val title: String? = null,
    @SerialName("title_zh") val title_zh: String? = null,
    @SerialName("material_type") val material_type: String? = null,
    @SerialName("prompt_text") val prompt_text: String? = null,
    val transcript: String? = null,
    @SerialName("audio_url") val audio_url: String? = null,
    @SerialName("estimated_seconds") val estimated_seconds: Int = 0,
    @SerialName("sort_order") val sort_order: Int = 0,
    val version: Int = 1,
    @SerialName("updated_at") val updated_at: Long = 0L,
    @SerialName("book_id") val book_id: String? = null,
    @SerialName("grade_level") val grade_level: Int? = null,
    @SerialName("unit_ref") val unit_ref: String? = null,
    @SerialName("section_ref") val section_ref: String? = null
)

@Serializable
data class SupabaseListeningQuestion(
    @SerialName("question_id") val question_id: String,
    @SerialName("material_id") val material_id: String,
    @SerialName("question_type") val question_type: String? = null,
    val stem: String? = null,
    @SerialName("sort_order") val sort_order: Int = 0,
    @SerialName("correct_option_id") val correct_option_id: String? = null,
    val explanation: String? = null,
    @SerialName("reward_token") val reward_token: Int = 2
)

@Serializable
data class SupabaseListeningOption(
    @SerialName("question_id") val question_id: String,
    @SerialName("option_id") val option_id: String,
    @SerialName("option_text") val option_text: String? = null,
    @SerialName("sort_order") val sort_order: Int = 0
)

@Serializable
data class SupabasePracticeAssignment(
    val id: String,
    @SerialName("teacher_id") val teacher_id: String,
    @SerialName("agency_id") val agency_id: String,
    @SerialName("module_id") val module_id: String,
    val title: String,
    @SerialName("due_at") val due_at: String,
    @SerialName("allow_late") val allow_late: Boolean = false,
    @SerialName("created_at") val created_at: String? = null
)

@Serializable
data class SupabasePracticeAssignmentItem(
    @SerialName("assignment_id") val assignment_id: String,
    @SerialName("item_ref") val item_ref: String,
    @SerialName("sort_order") val sort_order: Int = 0
)

@Serializable
data class SupabasePracticeAssignmentSubmission(
    val id: String,
    @SerialName("assignment_id") val assignment_id: String,
    @SerialName("student_id") val student_id: String,
    val status: String,
    @SerialName("started_at") val started_at: String? = null,
    @SerialName("submitted_at") val submitted_at: String? = null,
    @SerialName("correct_count") val correct_count: Int = 0,
    @SerialName("total_count") val total_count: Int = 0,
    @SerialName("earned_tokens") val earned_tokens: Int = 0,
    @SerialName("answer_payload") val answer_payload: kotlinx.serialization.json.JsonElement? = null,
    val score: Int? = null,
    @SerialName("max_score") val max_score: Int? = null,
    @SerialName("feedback_text") val feedback_text: String? = null,
    @SerialName("returned_at") val returned_at: String? = null,
    @SerialName("original_path") val original_path: String? = null,
    @SerialName("annotated_path") val annotated_path: String? = null
)

@Serializable
data class SupabaseWritingPrompt(
    @SerialName("prompt_id") val prompt_id: String,
    val title: String,
    @SerialName("title_zh") val title_zh: String? = null,
    val grade: Int = 9,
    val topic: String? = null,
    @SerialName("prompt_text") val prompt_text: String,
    @SerialName("prompt_text_zh") val prompt_text_zh: String? = null,
    @SerialName("word_count_min") val word_count_min: Int = 80,
    @SerialName("word_count_hint") val word_count_hint: Int = 100,
    @SerialName("max_score") val max_score: Int = 15,
    @SerialName("reward_token") val reward_token: Int = 5,
    @SerialName("sort_order") val sort_order: Int = 0
)
