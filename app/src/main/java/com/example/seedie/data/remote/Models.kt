package com.example.seedie.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val role: String,
    val agency_id: String?,
    val display_name: String?,
    val status: String?,
    val email: String? = null,
    val phone: String? = null,
    val phone_verified: Boolean = false,
    val phone_updated_at: String? = null
)

@Serializable
data class Student(
    val id: String,
    val agency_id: String,
    val name: String,
    val student_no: String?,
    val class_id: String?
)
