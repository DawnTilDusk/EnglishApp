package com.example.seedie.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.seedie.domain.profile.ProfileGradeOptions
import com.example.seedie.domain.repository.ManagedWordBook
import com.example.seedie.domain.repository.WordBookDownloadStatus
import com.example.seedie.ui.components.TabSectionSurface
import com.example.seedie.ui.theme.gardenPressable
import com.example.seedie.ui.theme.gardenShadow

private data class ProfileInfoItem(
    val label: String,
    val value: String
)

private data class ProfileActionEntry(
    val label: String,
    val supporting: String,
    val icon: ImageVector
)

@Composable
fun IdentitySection(
    modifier: Modifier = Modifier,
    profile: ProfileIdentityUiState,
    onOpenProfileEditor: () -> Unit = {},
    onProfileActionClick: (String) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val profileFields = remember(profile) {
        listOf(
            ProfileInfoItem("名称", profile.displayName),
            ProfileInfoItem("年级", profile.grade),
            ProfileInfoItem("词汇量", profile.vocabularySizeLabel),
            ProfileInfoItem("注册手机号", profile.phone),
            ProfileInfoItem("注册邮箱", profile.email)
        )
    }
    val profileEntries = remember {
        listOf(
            ProfileActionEntry("编辑资料", "更新头像、昵称和年级信息", Icons.Default.Edit),
            ProfileActionEntry("学习目标", "查看并调整你的学习计划", Icons.Default.Flag),
            ProfileActionEntry("账号安全", "管理登录方式与账号保护", Icons.Default.Lock),
            ProfileActionEntry("帮助与反馈", "获取使用帮助或提交问题反馈", Icons.AutoMirrored.Filled.HelpOutline),
            ProfileActionEntry("关于 Seedie", "了解版本信息与产品介绍", Icons.Default.Info)
        )
    }

    TabSectionSurface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            ProfileHeroCard(
                profile = profile,
                onClick = onOpenProfileEditor
            )

            ProfileGroup(title = "资料信息") {
                profileFields.forEachIndexed { index, field ->
                    ProfileInfoRow(
                        label = field.label,
                        value = field.value
                    )
                    if (index != profileFields.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    }
                }
            }

            ProfileGroup(title = "成长进度") {
                Text(
                    text = "当前等级: LV 5",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已保留原有等级与升级文案，后续可继续接入真实经验值。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { 0.7f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "距离升级还需 300 经验",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            ProfileGroup(title = "更多功能") {
                profileEntries.forEachIndexed { index, entry ->
                    ProfileActionRow(
                        icon = entry.icon,
                        label = entry.label,
                        supporting = entry.supporting,
                        onClick = {
                            if (entry.label == "编辑资料") {
                                onOpenProfileEditor()
                            } else {
                                onProfileActionClick(entry.label)
                            }
                        }
                    )
                    if (index != profileEntries.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                    }
                }
            }

            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                )
            ) {
                Text("退出登录")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditOverlay(
    modifier: Modifier = Modifier,
    draft: ProfileIdentityUiState,
    isSaving: Boolean,
    onAvatarToneChange: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onBindPhoneClick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var isGradeMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.gardenShadow(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "编辑资料",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatar(
                        profile = draft,
                        modifier = Modifier.size(88.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = draft.displayName.ifBlank { "未命名用户" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = draft.grade,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            ProfileGroup(title = "基础资料") {
                Button(
                    onClick = onAvatarToneChange,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("切换头像样式")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.displayName,
                    onValueChange = onDisplayNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("用户名") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = isGradeMenuExpanded,
                    onExpandedChange = { isGradeMenuExpanded = !isGradeMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = draft.grade,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        label = { Text("年级") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGradeMenuExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = isGradeMenuExpanded,
                        onDismissRequest = { isGradeMenuExpanded = false }
                    ) {
                        ProfileGradeOptions.values.forEach { grade ->
                            DropdownMenuItem(
                                text = { Text(grade) },
                                onClick = {
                                    onGradeChange(grade)
                                    isGradeMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            ProfileGroup(title = "更多") {
                ProfileActionRow(
                    icon = Icons.Default.Phone,
                    label = "绑定手机号",
                    supporting = "绑定常用手机号，便于登录验证与账号找回",
                    onClick = onBindPhoneClick
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                ) {
                    Text(
                        text = "更多资料设置将陆续开放，当前可先完成手机号绑定。",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = onSave,
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "保存中..." else "保存")
                }
            }
        }
    }
}

@Composable
fun ProfilePhoneBindingDialog(
    visible: Boolean,
    phone: String,
    phoneError: String?,
    isSubmitting: Boolean,
    onPhoneChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("绑定手机号") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "请输入常用手机号，保存后会同步到个人资料。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("手机号") },
                    singleLine = true,
                    isError = phoneError != null,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null
                        )
                    }
                )
                if (phoneError != null) {
                    Text(
                        text = phoneError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting
            ) {
                Text(if (isSubmitting) "提交中..." else "保存")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun LearningTargetOverlay(
    modifier: Modifier = Modifier,
    state: LearningTargetUiState,
    onRefresh: () -> Unit,
    onDownload: (String) -> Unit,
    onActivate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = modifier.gardenShadow(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "学习目标",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = state.books.firstOrNull { it.isActive }?.title ?: "当前使用默认词书",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.books.firstOrNull { it.isActive }?.description
                            ?.takeIf { it.isNotBlank() }
                            ?: "你可以在这里下载新的词书，并把已下载词书设为当前学习内容。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = state.errorMessage,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            ProfileGroup(title = "可选词书") {
                if (state.isLoading && state.books.isEmpty()) {
                    Text(
                        text = "词书列表加载中...",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (state.books.isEmpty()) {
                    Text(
                        text = "暂时还没有可显示的词书，请稍后刷新。",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.books.forEachIndexed { index, book ->
                        WordBookRow(
                            book = book,
                            isOperating = state.activeOperationBookId == book.bookId,
                            onDownload = { onDownload(book.bookId) },
                            onActivate = { onActivate(book.bookId) }
                        )
                        if (index != state.books.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRefresh, enabled = !state.isLoading) {
                    Text(if (state.isLoading) "刷新中..." else "刷新列表")
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun ProfileHeroCard(
    profile: ProfileIdentityUiState,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(
            profile = profile,
            modifier = Modifier.size(84.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 84.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = profile.displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = profile.grade,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "编辑资料",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProfileAvatar(
    profile: ProfileIdentityUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(avatarBackgroundColor(profile.avatarToneIndex)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = profile.avatarMonogram,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun WordBookRow(
    book: ManagedWordBook,
    isOperating: Boolean,
    onDownload: () -> Unit,
    onActivate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = book.description.ifBlank { "适合加入当前学习计划的词书。" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "词量 ${book.wordCount}  |  难度 ${book.difficulty}  |  ${bookStatusText(book)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (book.downloadStatus != WordBookDownloadStatus.DOWNLOADED) {
                OutlinedButton(
                    onClick = onDownload,
                    enabled = !isOperating
                ) {
                    Text(if (isOperating) "下载中..." else "下载")
                }
            }
            Button(
                onClick = onActivate,
                enabled = book.downloadStatus == WordBookDownloadStatus.DOWNLOADED &&
                    !book.isActive &&
                    !isOperating
            ) {
                Text(
                    when {
                        book.isActive -> "当前使用中"
                        isOperating -> "切换中..."
                        else -> "使用这本"
                    }
                )
            }
        }
    }
}

private fun bookStatusText(book: ManagedWordBook): String {
    return when {
        book.isActive -> "当前使用"
        book.downloadStatus == WordBookDownloadStatus.DOWNLOADED -> "已下载"
        book.downloadStatus == WordBookDownloadStatus.DOWNLOADING -> "下载中"
        book.downloadStatus == WordBookDownloadStatus.FAILED -> "下载失败"
        else -> "未下载"
    }
}

@Composable
private fun avatarBackgroundColor(avatarToneIndex: Int): Color {
    return when (avatarToneIndex % 3) {
        1 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        2 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    }
}

@Composable
private fun ProfileGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ProfileInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .gardenPressable(shape = MaterialTheme.shapes.small, onClick = {})
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    label: String,
    supporting: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
