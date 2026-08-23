package com.example.seedie.ui.screens.community

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.domain.model.CommunityPost

@Composable
fun CommunityFeedScreen(
    onNavigateBack: (() -> Unit)? = null,
    enterKey: Int = 0,
    viewModel: CommunityFeedViewModel = hiltViewModel()
) {
    LaunchedEffect(enterKey) {
        if (enterKey > 0) {
            viewModel.refresh()
        }
    }
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val message by viewModel.message.collectAsState()
    val composerVisible by viewModel.composerVisible.collectAsState()

    var deleteTarget by remember { mutableStateOf<CommunityPost?>(null) }
    var composeTitle by remember { mutableStateOf("") }
    var composeBody by remember { mutableStateOf("") }

    deleteTarget?.let { post ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除帖子") },
            text = { Text("确定删除该帖？删除后不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePost(post.id)
                    deleteTarget = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
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

    if (composerVisible) {
        AlertDialog(
            onDismissRequest = {
                if (!isSubmitting) viewModel.dismissComposer()
            },
            title = { Text("发帖") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "默认仅本班同学可见；教师设为精华后全年级可见。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = composeTitle,
                        onValueChange = { if (it.length <= 80) composeTitle = it },
                        label = { Text("标题（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = composeBody,
                        onValueChange = { if (it.length <= 2000) composeBody = it },
                        label = { Text("正文") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPost(composeTitle, composeBody)
                    },
                    enabled = !isSubmitting && composeBody.isNotBlank()
                ) {
                    Text(if (isSubmitting) "发布中…" else "发布")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissComposer() },
                    enabled = !isSubmitting
                ) { Text("取消") }
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (onNavigateBack != null) {
                    TextButton(onClick = onNavigateBack) { Text("← 返回") }
                } else {
                    Text(
                        text = "社区",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::refresh) { Text("刷新") }
                    Button(onClick = {
                        composeTitle = ""
                        composeBody = ""
                        viewModel.openComposer()
                    }) { Text("发帖") }
                }
            }
        }

        if (onNavigateBack != null) {
            item {
                Text(
                    text = "社区",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "本班动态、教师年级公告与精华帖",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Text(
                    text = "本班动态、教师年级公告与精华帖",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isLoading && posts.isEmpty()) {
            item {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }
        }

        if (!isLoading && posts.isEmpty()) {
            item {
                Text(
                    text = "还没有可见帖子。发一条给本班同学吧。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(posts, key = { it.id }) { post ->
            CommunityPostCard(
                post = post,
                onDelete = { deleteTarget = post }
            )
        }
    }
}

@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    onDelete: () -> Unit
) {
    val roleLabel = if (post.authorRole == "teacher") "教师" else "学生"
    val meta = buildList {
        add(roleLabel)
        if (post.isFeatured) add("精华")
        if (post.authorRole == "teacher" && post.targetGrades.isNotEmpty()) {
            add("可见：${post.targetGrades.joinToString("、")}")
        }
        if (post.authorRole == "student") {
            post.classId?.let { add(it) }
            post.authorGrade?.let { add(it) }
        }
    }.joinToString(" · ")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = post.title?.takeIf { it.isNotBlank() } ?: "（无标题）",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                if (post.isMine) {
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
            Text(
                text = "${post.authorDisplayName} · $meta",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = post.body)
        }
    }
}
