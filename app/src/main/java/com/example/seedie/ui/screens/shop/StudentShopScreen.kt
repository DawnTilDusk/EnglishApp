package com.example.seedie.ui.screens.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StudentShopScreen(
    onNavigateBack: () -> Unit,
    onOpenOrders: () -> Unit,
    viewModel: StudentShopViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val teacherId by viewModel.teacherId.collectAsState()
    val availableTokens by viewModel.availableTokens.collectAsState()
    val localTokens by viewModel.localTokens.collectAsState()
    val syncWarning by viewModel.syncWarning.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    var confirmProductId by remember { mutableStateOf<String?>(null) }
    var confirmProductName by remember { mutableStateOf("") }
    var confirmPrice by remember { mutableStateOf(0) }

    confirmProductId?.let {
        AlertDialog(
            onDismissRequest = { confirmProductId = null },
            title = { Text("确认购买") },
            text = { Text("购买「$confirmProductName」将花费 $confirmPrice 代币，提交后等待教师审核。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.submitOrder(it)
                    confirmProductId = null
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { confirmProductId = null }) { Text("取消") }
            }
        )
    }

    message?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage() },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage() }) { Text("确定") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onNavigateBack) { Text("← 返回") }
                TextButton(onClick = onOpenOrders) { Text("我的订单") }
            }
        }

        item {
            Text(
                text = "教师商城",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(text = "我的代币：$availableTokens")
            if (localTokens != availableTokens) {
                Text(
                    text = "本地记录：$localTokens",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            syncWarning?.let { warning ->
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            OutlinedButton(
                onClick = { viewModel.refresh() },
                enabled = !isLoading,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(if (isLoading) "同步中…" else "同步代币")
            }
        }

        when {
            teacherId == null -> {
                item {
                    Text("暂未绑定教师，无法购物。请联系管理员在后台设置 students.teacher_id。")
                }
            }
            isLoading && products.isEmpty() -> {
                item { CircularProgressIndicator() }
            }
            products.isEmpty() -> {
                item { Text("教师暂未上架商品") }
            }
            else -> {
                items(products, key = { it.id }) { product ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = product.name, style = MaterialTheme.typography.titleLarge)
                            product.description?.let { Text(text = it) }
                            Text(text = "${product.priceTokens} 代币")
                            val outOfStock = product.stock == 0
                            val canAfford = availableTokens >= product.priceTokens
                            Button(
                                onClick = {
                                    confirmProductId = product.id
                                    confirmProductName = product.name
                                    confirmPrice = product.priceTokens
                                },
                                enabled = !outOfStock && canAfford
                            ) {
                                Text(
                                    when {
                                        outOfStock -> "已售罄"
                                        !canAfford -> "代币不足（需 ${product.priceTokens}）"
                                        else -> "购买"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MyOrdersScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentShopViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedButton(onClick = onNavigateBack) { Text("← 返回商城") }
            Text(
                text = "我的订单",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (orders.isEmpty()) {
            item { Text("暂无订单") }
        }

        items(orders, key = { it.id }) { order ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "订单 ${order.id.take(8)}...")
                    Text(text = "代币：${order.tokensAmount}")
                    Text(text = "状态：${orderStatusLabel(order.status)}")
                    order.createdAt?.let { Text(text = "时间：$it") }
                }
            }
        }
    }
}

private fun orderStatusLabel(status: String): String = when (status) {
    "pending" -> "待审核"
    "approved" -> "已批准"
    "completed" -> "已完成"
    "rejected" -> "已拒绝"
    else -> status
}
