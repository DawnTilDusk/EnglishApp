package com.example.seedie.domain.model

data class CommunityPost(
    val id: String,
    val authorId: String,
    val authorRole: String,
    val authorDisplayName: String,
    val title: String?,
    val body: String,
    val classId: String?,
    val authorGrade: String?,
    val targetGrades: List<String>,
    val isFeatured: Boolean,
    val createdAt: String,
    val isMine: Boolean
)
