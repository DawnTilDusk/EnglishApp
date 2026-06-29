package com.example.seedie.ui.screens.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.ShopOrder
import com.example.seedie.domain.model.ShopProduct
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentShopViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val authService: AuthService,
    private val economyManager: EconomyManager
) : ViewModel() {

    private val _products = MutableStateFlow<List<ShopProduct>>(emptyList())
    val products: StateFlow<List<ShopProduct>> = _products.asStateFlow()

    private val _orders = MutableStateFlow<List<ShopOrder>>(emptyList())
    val orders: StateFlow<List<ShopOrder>> = _orders.asStateFlow()

    private val _teacherId = MutableStateFlow<String?>(null)
    val teacherId: StateFlow<String?> = _teacherId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val totalTokens = economyManager.totalTokens

    init {
        _teacherId.value = authService.currentSession.value?.teacherId
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _teacherId.value = authService.currentSession.value?.teacherId
            val teacherId = _teacherId.value
            if (teacherId == null) {
                _products.value = emptyList()
            } else {
                try {
                    economyManager.refreshBalanceFromCloud()
                    _products.value = shopRepository.fetchTeacherProducts(teacherId)
                    _orders.value = shopRepository.fetchMyOrdersAsStudent()
                } catch (e: Exception) {
                    _message.value = e.message
                }
            }
            _isLoading.value = false
        }
    }

    fun submitOrder(productId: String) {
        viewModelScope.launch {
            shopRepository.submitOrder(productId)
                .onSuccess {
                    _message.value = "下单成功，等待教师审核"
                    refresh()
                }
                .onFailure { _message.value = it.message ?: "下单失败" }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
