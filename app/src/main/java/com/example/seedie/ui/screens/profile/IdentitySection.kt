package com.example.seedie.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
            ProfileInfoItem("注册手机号", profile.phone),
            ProfileInfoItem("注册邮箱", profile.email)
        )
    }
    val profileEntries = remember {
        listOf(
            ProfileActionEntry("编辑资料", "统一打开资料编辑浮层", Icons.Default.Edit),
            ProfileActionEntry("学习目标", "后续接入每日目标与学习计划", Icons.Default.Flag),
            ProfileActionEntry("账号安全", "后续接入密码与绑定管理", Icons.Default.Lock),
            ProfileActionEntry("帮助与反馈", "问题反馈、使用帮助与常见问题", Icons.Default.HelpOutline),
            ProfileActionEntry("关于 Seedie", "查看版本说明与产品介绍", Icons.Default.Info)
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .gardenShadow(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出登录")
            }
        }
    }
}

@Composable
fun ProfileEditOverlay(
    modifier: Modifier = Modifier,
    draft: ProfileIdentityUiState,
    onAvatarToneChange: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onGradeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "编辑资料",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "个人名片卡与“编辑资料”入口共用这一个编辑浮层。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                            text = draft.grade.ifBlank { "待补充年级" },
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
                OutlinedTextField(
                    value = draft.grade,
                    onValueChange = onGradeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("年级") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null
                        )
                    }
                )
            }

            ProfileGroup(title = "更多可编辑信息") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                ) {
                    Text(
                        text = "这里预留给后续扩展的资料字段，例如绑定方式、联系信息或学习偏好。",
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
                Button(onClick = onSave) {
                    Text("保存")
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = if (isPressed) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            Color.Transparent
        },
        tonalElevation = if (isPressed) 1.dp else 0.dp,
        shadowElevation = if (isPressed) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {}
                )
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
