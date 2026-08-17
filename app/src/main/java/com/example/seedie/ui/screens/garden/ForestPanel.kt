package com.example.seedie.ui.screens.garden

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.seedie.data.local.entity.GardenPlantEntity
import com.example.seedie.domain.model.GardenSpeciesCatalog
import com.example.seedie.domain.model.activityModuleLabel
import com.example.seedie.domain.usecase.GardenForestRules
import com.example.seedie.ui.components.TabSectionSurface
import com.example.seedie.ui.theme.PrimaryGreen

@Composable
fun ForestPanel(
    state: ForestPanelUiState,
    onRangeModeChange: (ForestRangeMode) -> Unit,
    onShiftRange: (Int) -> Unit,
    onTreeClick: (com.example.seedie.domain.usecase.PlacedTree) -> Unit,
    onOpenGardenerHut: () -> Unit,
    onCloseGardenerHut: () -> Unit,
    onRemoveWithered: () -> Unit = {},
    removeMessage: String? = null,
    onClearRemoveMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(28.dp)
    TabSectionSurface(
        modifier = modifier.fillMaxSize(),
        shape = cardShape,
        accentColor = PrimaryGreen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的森林",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onOpenGardenerHut) {
                    Text("园丁小屋")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.rangeMode == ForestRangeMode.Day,
                    onClick = { onRangeModeChange(ForestRangeMode.Day) },
                    label = { Text("日") }
                )
                FilterChip(
                    selected = state.rangeMode == ForestRangeMode.Week,
                    onClick = { onRangeModeChange(ForestRangeMode.Week) },
                    label = { Text("周") }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onShiftRange(-1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一期")
                }
                Text(
                    text = state.rangeLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                IconButton(onClick = { onShiftRange(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一期")
                }
            }

            Text(
                text = "所选时段森林 · 存活树苗 ${state.aliveCount} · 枯萎树苗 ${state.witheredCount}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            ) {
                ForestScene(
                    cells = state.cells,
                    selectedPlantId = state.selectedTree?.plantId,
                    onTreeClick = onTreeClick,
                    modifier = Modifier.fillMaxSize()
                )
                if (state.trees.isEmpty()) {
                    Text(
                        text = "花园土地已备好，完成练习种下第一棵树吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = state.selectedTree != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.selectedTree?.let { tree ->
                    val species = GardenSpeciesCatalog.requireById(tree.speciesId)
                    val statusLabel =
                        if (tree.status == GardenPlantEntity.STATUS_ALIVE) "活苗" else "枯苗"
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "${species.displayName} · $statusLabel",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${activityModuleLabel(tree.moduleId)} · ${tree.completedQuestionCount} 题 · ${tree.studyDurationSec}s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            if (tree.status == GardenPlantEntity.STATUS_WITHERED) {
                                TextButton(
                                    onClick = { showRemoveConfirm = true },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("铲除（${GardenForestRules.REMOVE_WITHERED_COST} 代币）")
                                }
                            }
                            if (!removeMessage.isNullOrBlank()) {
                                Text(
                                    text = removeMessage,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("用代币铲除这棵枯苗？") },
            text = {
                Text(
                    "花费 ${GardenForestRules.REMOVE_WITHERED_COST} 代币后，这棵枯苗会从森林里移除，格子会空出来。确定铲除吗？"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirm = false
                        onClearRemoveMessage()
                        onRemoveWithered()
                    }
                ) {
                    Text("花费 ${GardenForestRules.REMOVE_WITHERED_COST} 代币铲除")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveConfirm = false }) {
                    Text("先留着")
                }
            }
        )
    }

    if (state.showGardenerHut) {
        Dialog(
            onDismissRequest = onCloseGardenerHut,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(480.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                GardenerHutSheetContent(onClose = onCloseGardenerHut)
            }
        }
    }
}
