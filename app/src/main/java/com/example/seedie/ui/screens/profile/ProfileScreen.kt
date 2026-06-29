package com.example.seedie.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ProfileScreen(
    onOpenShop: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val totalTokens by viewModel.totalTokens.collectAsState()
    val badges by viewModel.badges.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        IdentitySection(modifier = Modifier.weight(0.35f))

        AssetGallerySection(
            modifier = Modifier.weight(0.65f),
            totalTokens = totalTokens,
            badges = badges,
            onOpenShop = onOpenShop
        )
    }
}
