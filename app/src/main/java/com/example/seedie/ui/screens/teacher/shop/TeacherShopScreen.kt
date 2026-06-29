package com.example.seedie.ui.screens.teacher.shop

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TeacherShopScreen(
    viewModel: TeacherShopViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val pendingOrders by viewModel.pendingOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("-1") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("上架新商品") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") })
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("描述") })
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("代币价格") })
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("库存 (-1=无限)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val priceInt = price.toIntOrNull() ?: return@TextButton
                    val stockInt = stock.toIntOrNull() ?: -1
                    viewModel.createProduct(name, description, priceInt, stockInt)
                    showCreateDialog = false
                    name = ""
                    description = ""
                    price = ""
                    stock = "-1"
                }) { Text("上架") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "商品管理",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.refresh() }) { Text("刷新") }
                    Button(onClick = { showCreateDialog = true }) { Text("上架商品") }
                }
            }
        }

        if (isLoading && products.isEmpty() && pendingOrders.isEmpty()) {
            item { CircularProgressIndicator() }
        }

        if (!isLoading && products.isEmpty() && pendingOrders.isEmpty() && message == null) {
            item { Text("暂无商品，点击「上架商品」添加") }
        }

        items(products, key = { it.id }) { product ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = product.name, style = MaterialTheme.typography.titleLarge)
                    product.description?.let { Text(text = it) }
                    Text(text = "${product.priceTokens} 代币 | 库存: ${if (product.stock < 0) "无限" else product.stock}")
                    Text(text = if (product.isActive) "上架中" else "已下架")
                }
            }
        }

        item {
            Text(
                text = "待审核订单",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (pendingOrders.isEmpty()) {
            item { Text("暂无待审核订单") }
        }

        items(pendingOrders, key = { it.id }) { order ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "订单 ${order.id.take(8)}...")
                    Text(text = "代币：${order.tokensAmount}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.approveOrder(order.id) }) { Text("批准") }
                        OutlinedButton(onClick = { viewModel.rejectOrder(order.id) }) { Text("拒绝") }
                    }
                }
            }
        }
    }
}
