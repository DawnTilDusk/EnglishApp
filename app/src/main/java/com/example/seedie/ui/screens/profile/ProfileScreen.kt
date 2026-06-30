package com.example.seedie.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.MaterialTheme
import com.example.seedie.ui.components.LogoutConfirmDialog

@Composable
fun ProfileScreen(
    onOpenShop: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val totalTokens by viewModel.totalTokens.collectAsState()
    val badges by viewModel.badges.collectAsState()
    val profileEditorUiState by viewModel.profileEditorUiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LogoutConfirmDialog(
        visible = showLogoutDialog,
        onConfirm = {
            showLogoutDialog = false
            viewModel.logout()
        },
        onDismiss = { showLogoutDialog = false }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (profileEditorUiState.isEditOverlayVisible) 0.82f else 1f)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            IdentitySection(
                modifier = Modifier.weight(0.38f),
                profile = profileEditorUiState.profile,
                onOpenProfileEditor = viewModel::openProfileEditor,
                onProfileActionClick = { label ->
                    Toast.makeText(context, "$label 功能暂未开放", Toast.LENGTH_SHORT).show()
                },
                onLogoutClick = { showLogoutDialog = true }
            )

            AssetGallerySection(
                modifier = Modifier.weight(0.62f),
                totalTokens = totalTokens,
                badges = badges,
                onOpenShop = onOpenShop
            )
        }

        if (profileEditorUiState.isEditOverlayVisible) {
            val dismissInteractionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.34f))
                    .clickable(
                        interactionSource = dismissInteractionSource,
                        indication = null
                    ) { viewModel.dismissProfileEditor() }
            )

            ProfileEditOverlay(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.42f),
                draft = profileEditorUiState.draft,
                onAvatarToneChange = viewModel::cycleDraftAvatarTone,
                onDisplayNameChange = viewModel::updateDraftDisplayName,
                onGradeChange = viewModel::updateDraftGrade,
                onMoreActionClick = { label ->
                    Toast.makeText(context, "$label 功能暂未开放", Toast.LENGTH_SHORT).show()
                },
                onDismiss = viewModel::dismissProfileEditor,
                onSave = viewModel::saveProfileEdits
            )
        }
    }
}
