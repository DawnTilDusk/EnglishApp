package com.example.seedie.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.ui.components.LogoutConfirmDialog

@Composable
fun ProfileScreen(
    onOpenShop: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val totalTokens by viewModel.totalTokens.collectAsState()
    val badges by viewModel.badges.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LogoutConfirmDialog(
        visible = showLogoutDialog,
        onConfirm = {
            showLogoutDialog = false
            viewModel.logout()
        },
        onDismiss = { showLogoutDialog = false }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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

        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("退出登录")
        }
    }
}
