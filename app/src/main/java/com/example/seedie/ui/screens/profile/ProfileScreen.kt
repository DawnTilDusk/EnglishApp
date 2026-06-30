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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val message by viewModel.message.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

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
            val contentInteractionSource = remember { MutableInteractionSource() }
            Dialog(
                onDismissRequest = viewModel::dismissProfileEditor,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false,
                    dismissOnClickOutside = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.34f))
                        .clickable(
                            interactionSource = dismissInteractionSource,
                            indication = null
                        ) { viewModel.dismissProfileEditor() },
                    contentAlignment = Alignment.Center
                ) {
                    ProfileEditOverlay(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .widthIn(max = 520.dp)
                            .clickable(
                                interactionSource = contentInteractionSource,
                                indication = null
                            ) {},
                        draft = profileEditorUiState.draft,
                        isSaving = profileEditorUiState.isSaving,
                        onAvatarToneChange = viewModel::cycleDraftAvatarTone,
                        onDisplayNameChange = viewModel::updateDraftDisplayName,
                        onGradeChange = viewModel::updateDraftGrade,
                        onBindPhoneClick = viewModel::openPhoneBindingDialog,
                        onDismiss = viewModel::dismissProfileEditor,
                        onSave = viewModel::saveProfileEdits
                    )
                }
            }
        }

        ProfilePhoneBindingDialog(
            visible = profileEditorUiState.isPhoneDialogVisible,
            phone = profileEditorUiState.phoneDraft,
            phoneError = profileEditorUiState.phoneInputError,
            isSubmitting = profileEditorUiState.isBindingPhone,
            onPhoneChange = viewModel::updatePhoneDraft,
            onConfirm = viewModel::bindPhone,
            onDismiss = viewModel::dismissPhoneBindingDialog
        )
    }
}
