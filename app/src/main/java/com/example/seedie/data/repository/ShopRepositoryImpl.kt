package com.example.seedie.data.repository

import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.ShopRemoteDataSource
import com.example.seedie.domain.model.ShopOrder
import com.example.seedie.domain.model.ShopProduct
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.ShopRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRepositoryImpl @Inject constructor(
    private val remote: ShopRemoteDataSource,
    private val authService: AuthService,
    private val economyManager: EconomyManager
) : ShopRepository {

    override suspend fun fetchTeacherProducts(teacherId: String): List<ShopProduct> =
        remote.fetchProductsByTeacher(teacherId, activeOnly = true)

    override suspend fun fetchMyProducts(): List<ShopProduct> {
        val teacherId = authService.currentSession.value?.userId
            ?: return emptyList()
        return remote.fetchMyProducts(teacherId)
    }

    override suspend fun fetchMyOrdersAsStudent(): List<ShopOrder> {
        val studentId = authService.currentSession.value?.userId ?: return emptyList()
        return remote.fetchOrdersForStudent(studentId)
    }

    override suspend fun fetchPendingOrdersAsTeacher(): List<ShopOrder> {
        val teacherId = authService.currentSession.value?.userId ?: return emptyList()
        return remote.fetchOrdersForTeacher(teacherId, status = "pending")
    }

    override suspend fun createProduct(
        name: String,
        description: String?,
        priceTokens: Int,
        stock: Int
    ): Result<Unit> = runCatching {
        val teacherId = authService.currentSession.value?.userId
            ?: throw IllegalStateException("Not logged in")
        remote.createProduct(teacherId, name, description, priceTokens, stock)
    }

    override suspend fun updateProduct(
        productId: String,
        name: String,
        description: String?,
        priceTokens: Int,
        stock: Int,
        isActive: Boolean
    ): Result<Unit> = runCatching {
        remote.updateProduct(productId, name, description, priceTokens, stock, isActive)
    }

    override suspend fun submitOrder(productId: String): Result<String> = runCatching {
        val orderId = remote.submitOrder(productId)
        economyManager.refreshBalanceFromCloud()
        orderId
    }

    override suspend fun approveOrder(orderId: String): Result<Unit> = runCatching {
        remote.approveOrder(orderId)
    }

    override suspend fun rejectOrder(orderId: String): Result<Unit> = runCatching {
        remote.rejectOrder(orderId)
    }
}
