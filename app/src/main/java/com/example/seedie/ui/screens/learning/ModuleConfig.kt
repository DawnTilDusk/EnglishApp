package com.example.seedie.ui.screens.learning

import androidx.compose.ui.graphics.vector.ImageVector

data class ModuleConfig(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isAvailable: Boolean = false,
    val hasNewContent: Boolean = false
)