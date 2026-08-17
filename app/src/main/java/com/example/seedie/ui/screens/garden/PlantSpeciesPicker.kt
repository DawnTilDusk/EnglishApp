package com.example.seedie.ui.screens.garden

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.domain.model.GardenSpeciesCatalog
import com.example.seedie.domain.usecase.GardenSpeciesUi
import com.example.seedie.domain.usecase.GardenUnlockResult
import com.example.seedie.ui.theme.PrimaryGreen
import kotlinx.coroutines.launch

@Composable
fun PlantSessionGate(
    viewModel: PlantPickerViewModel = hiltViewModel(),
    onReady: @Composable (speciesId: String) -> Unit
) {
    var confirmedSpeciesId by remember { mutableStateOf<String?>(null) }
    if (confirmedSpeciesId == null) {
        PlantSpeciesPickerScreen(
            viewModel = viewModel,
            onConfirm = { confirmedSpeciesId = it }
        )
    } else {
        onReady(confirmedSpeciesId!!)
    }
}

@Composable
fun PlantSpeciesPickerScreen(
    viewModel: PlantPickerViewModel = hiltViewModel(),
    onConfirm: (speciesId: String) -> Unit,
    title: String = "选择要种的树",
    confirmLabel: String = "用这棵树开始"
) {
    val catalog by viewModel.catalog.collectAsState()
    val tokens by viewModel.tokenBalance.collectAsState()
    val dews by viewModel.dewBalance.collectAsState()
    val selectedId by viewModel.selectedSpeciesId.collectAsState()
    val unlockMessage by viewModel.unlockMessage.collectAsState()
    val convertMessage by viewModel.convertMessage.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingUnlock by remember { mutableStateOf<GardenSpeciesUi?>(null) }
    var showConvert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.bootstrap()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "完成练习后，这棵树会种进你的花园。开练约一分钟内退出不会留下记录；超时再退出会变成枯苗。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "露水：$dews",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryGreen
                )
                Text(
                    text = "代币：$tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            TextButton(onClick = { showConvert = true }) {
                Text("代币 → 露水")
            }
        }

        SpeciesCatalogGrid(
            catalog = catalog,
            selectedId = selectedId,
            onSelect = { viewModel.selectSpecies(it) },
            onLockedClick = {
                pendingUnlock = it
            }
        )

        unlockMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        convertMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = {
                val id = selectedId ?: return@Button
                scope.launch {
                    viewModel.confirmSelection()
                    onConfirm(id)
                }
            },
            enabled = selectedId != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(confirmLabel)
        }
    }

    pendingUnlock?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingUnlock = null },
            title = { Text("解锁 ${item.species.displayName}") },
            text = {
                Column {
                    Text(
                        text = "用露水解锁：${item.species.unlockCostDew} 滴",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "露水用于解锁花园树种；代币可在商城兑换奖励。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val result = viewModel.unlockSpecies(item.species.id)
                            pendingUnlock = null
                            if (result == GardenUnlockResult.Success ||
                                result == GardenUnlockResult.AlreadyUnlocked
                            ) {
                                viewModel.selectSpecies(item.species.id)
                            }
                        }
                    }
                ) { Text("花费露水解锁") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnlock = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showConvert) {
        AlertDialog(
            onDismissRequest = { showConvert = false },
            title = { Text("用代币兑换露水") },
            text = {
                Column {
                    Text("当前 1 代币 = 10 露水，每日最多兑换 10 代币 = 100 露水。")
                    Text(
                        "兑换 10 代币 → 100 露水？",
                        modifier = Modifier.padding(top = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        viewModel.convertTokensToDews(10)
                        showConvert = false
                    }
                }) { Text("兑换 10 代币") }
            },
            dismissButton = { TextButton(onClick = { showConvert = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun ColumnScope.SpeciesCatalogGrid(
    catalog: List<GardenSpeciesUi>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onLockedClick: (GardenSpeciesUi) -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        if (catalog.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(catalog, key = { it.species.id }) { item ->
                    SpeciesCard(
                        item = item,
                        selected = item.species.id == selectedId,
                        onClick = {
                            if (item.unlocked) {
                                onSelect(item.species.id)
                            } else {
                                onLockedClick(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeciesCard(
    item: GardenSpeciesUi,
    selected: Boolean,
    onClick: () -> Unit
) {
    val accent = Color(item.species.accentColorArgb)
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = shape
            )
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (item.unlocked) 0.25f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.unlocked) Icons.Default.Eco else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (item.unlocked) accent else MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = item.species.displayName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
            if (item.unlocked) {
                Text(
                    text = if (item.species.id == GardenSpeciesCatalog.DEFAULT_SPECIES_ID) "已解锁" else "可种植",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "${item.species.unlockCostDew} 滴露水",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGreen,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "花园专属解锁",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun GardenExitConfirmDialog(
    answeredQuestionCount: Int,
    withinAbandonGrace: Boolean,
    onConfirmExit: () -> Unit,
    onContinue: () -> Unit
) {
    // Grace path uses its own copy — do not reuse the withered-tree warning framing.
    if (withinAbandonGrace) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(onClick = onConfirmExit) {
                    Text("退出练习")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onContinue) {
                    Text("继续练习")
                }
            },
            title = { Text("确定要退出练习吗？") },
            text = {
                Text("现在还在开练后的短时容错里，退出不会在花园里留下记录。确定现在离开吗？")
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(onClick = onConfirmExit) {
                Text("仍然退出（种下枯苗）")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onContinue) {
                Text("继续学习")
            }
        },
        title = { Text("中途退出会种下枯苗") },
        text = {
            Text(
                if (answeredQuestionCount > 0) {
                    "中途退出会使正在生长的树变成枯苗，并留在你的森林里。确定要退出吗？"
                } else {
                    "即使还没作答，退出也会在森林里种下一棵枯苗。确定要退出吗？"
                }
            )
        }
    )
}

@Composable
fun GardenerHutSheetContent(
    viewModel: PlantPickerViewModel = hiltViewModel(),
    onClose: () -> Unit
) {
    val catalog by viewModel.catalog.collectAsState()
    val tokens by viewModel.tokenBalance.collectAsState()
    val dews by viewModel.dewBalance.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingUnlock by remember { mutableStateOf<GardenSpeciesUi?>(null) }
    val unlockMessage by viewModel.unlockMessage.collectAsState()
    val convertMessage by viewModel.convertMessage.collectAsState()
    var showConvert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.bootstrap() }

    Column(modifier = Modifier.padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "园丁小屋",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            TextButton(onClick = onClose) { Text("关闭") }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "露水：$dews",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryGreen
                )
                Text(
                    text = "代币：$tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            TextButton(onClick = { showConvert = true }) { Text("代币 → 露水") }
        }
        Text(
            text = "每日学习攒下的露水，就是花园最好的养分。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            modifier = Modifier.height(320.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(catalog, key = { it.species.id }) { item ->
                SpeciesCard(
                    item = item,
                    selected = false,
                    onClick = {
                        if (!item.unlocked) {
                            pendingUnlock = item
                        }
                    }
                )
            }
        }
        unlockMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
        convertMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 8.dp))
        }
    }

    pendingUnlock?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingUnlock = null },
            title = { Text("解锁 ${item.species.displayName}") },
            text = {
                Column {
                    Text(
                        text = "用露水解锁：${item.species.unlockCostDew} 滴",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "露水用于解锁花园树种；代币可在商城兑换奖励。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            viewModel.unlockSpecies(item.species.id)
                            pendingUnlock = null
                        }
                    }
                ) { Text("花费露水解锁") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnlock = null }) { Text("取消") }
            }
        )
    }

    if (showConvert) {
        AlertDialog(
            onDismissRequest = { showConvert = false },
            title = { Text("用代币兑换露水") },
            text = {
                Text("1 代币 = 10 露水，每日最多兑换 10 代币 = 100 露水。兑换 10 代币 → 100 露水？")
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        viewModel.convertTokensToDews(10)
                        showConvert = false
                    }
                }) { Text("兑换 10 代币") }
            },
            dismissButton = { TextButton(onClick = { showConvert = false }) { Text("取消") } }
        )
    }
}
