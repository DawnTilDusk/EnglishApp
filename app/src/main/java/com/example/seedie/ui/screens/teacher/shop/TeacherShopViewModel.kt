package com.example.seedie.ui.screens.teacher.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.ShopOrder
import com.example.seedie.domain.model.ShopProduct
import com.example.seedie.domain.repository.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherShopViewModel @Inject constructor(
    private val shopRepository: ShopRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<ShopProduct>>(emptyList())
    val products: StateFlow<List<ShopProduct>> = _products.asStateFlow()

    private val _pendingOrders = MutableStateFlow<List<ShopOrder>>(emptyList())
    val pendingOrders: StateFlow<List<ShopOrder>> = _pendingOrders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _products.value = shopRepository.fetchMyProducts()
                _pendingOrders.value = shopRepository.fetchPendingOrdersAsTeacher()
            } catch (e: Exception) {
                _message.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createProduct(name: String, description: String, priceTokens: Int, stock: Int) {
        viewModelScope.launch {
            shopRepository.createProduct(name, description.ifBlank { null }, priceTokens, stock)
                .onSuccess {
                    _message.value = "商品已上架"
                    refresh()
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun approveOrder(orderId: String) {
        viewModelScope.launch {
            shopRepository.approveOrder(orderId)
                .onSuccess {
                    _message.value = "已批准订单"
                    refresh()
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun rejectOrder(orderId: String) {
        viewModelScope.launch {
            shopRepository.rejectOrder(orderId)
                .onSuccess {
                    _message.value = "已拒绝并退款"
                    refresh()
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
