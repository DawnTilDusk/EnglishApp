package com.example.seedie.domain.repository

import com.example.seedie.domain.model.ShopOrder
import com.example.seedie.domain.model.ShopProduct

interface ShopRepository {
    suspend fun fetchAgencyProducts(agencyId: String, activeOnly: Boolean = true): List<ShopProduct>
    suspend fun fetchMyOrdersAsStudent(): List<ShopOrder>
    suspend fun submitOrder(productId: String): Result<String>
}
