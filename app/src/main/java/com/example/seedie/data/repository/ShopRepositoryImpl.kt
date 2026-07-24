package com.example.seedie.data.repository

import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.ShopRemoteDataSource
import com.example.seedie.domain.model.ShopOrder
import com.example.seedie.domain.model.ShopProduct
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.ShopRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShopRepositoryImpl @Inject constructor(
    private val remote: ShopRemoteDataSource,
    private val authService: AuthService,
    private val economyManager: EconomyManager
) : ShopRepository {

    override suspend fun fetchAgencyProducts(agencyId: String, activeOnly: Boolean): List<ShopProduct> =
        remote.fetchProductsByAgency(agencyId, activeOnly = activeOnly)

    override suspend fun fetchMyOrdersAsStudent(): List<ShopOrder> {
        val studentId = authService.currentSession.value?.userId ?: return emptyList()
        return remote.fetchOrdersForStudent(studentId)
    }

    override suspend fun submitOrder(productId: String): Result<String> = runCatching {
        val orderId = remote.submitOrder(productId)
        economyManager.refreshBalanceFromCloud()
        orderId
    }
}
