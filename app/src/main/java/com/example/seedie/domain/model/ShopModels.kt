package com.example.seedie.domain.model

data class ShopProduct(
    val id: String,
    val agencyId: String,
    val name: String,
    val description: String?,
    val priceTokens: Int,
    val stock: Int,
    val imageUrl: String?,
    val isActive: Boolean
)

data class ShopOrder(
    val id: String,
    val studentId: String,
    val agencyId: String,
    val productId: String,
    val productName: String?,
    val tokensAmount: Int,
    val status: String,
    val createdAt: String?
)

data class StudentSummary(
    val id: String,
    val name: String,
    val studentNo: String?,
    val classId: String?
)

data class StudentStats(
    val studentId: String,
    val name: String,
    val studentNo: String?,
    val classId: String?,
    val totalCheckIns: Int,
    val totalStudyMinutes: Int,
    val learnedWordCount: Int,
    val tokenBalance: Int
)
