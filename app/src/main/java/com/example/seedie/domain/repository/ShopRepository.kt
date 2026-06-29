package com.example.seedie.domain.repository

import com.example.seedie.domain.model.ShopOrder
import com.example.seedie.domain.model.ShopProduct

interface ShopRepository {
    suspend fun fetchTeacherProducts(teacherId: String): List<ShopProduct>
    suspend fun fetchMyProducts(): List<ShopProduct>
    suspend fun fetchMyOrdersAsStudent(): List<ShopOrder>
    suspend fun fetchPendingOrdersAsTeacher(): List<ShopOrder>
    suspend fun createProduct(
        name: String,
        description: String?,
        priceTokens: Int,
        stock: Int
    ): Result<Unit>
    suspend fun updateProduct(
        productId: String,
        name: String,
        description: String?,
        priceTokens: Int,
        stock: Int,
        isActive: Boolean
    ): Result<Unit>
    suspend fun submitOrder(productId: String): Result<String>
    suspend fun approveOrder(orderId: String): Result<Unit>
    suspend fun rejectOrder(orderId: String): Result<Unit>
}
