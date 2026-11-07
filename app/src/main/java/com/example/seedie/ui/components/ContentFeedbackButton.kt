package com.example.seedie.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.BuildConfig
import com.example.seedie.data.repository.PracticeDraftStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.UUID
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@HiltViewModel
class ContentFeedbackViewModel @Inject constructor(private val drafts: PracticeDraftStore) : ViewModel() {
    fun save(report: String) {
        drafts.save("feedback:${UUID.randomUUID()}", report)
        viewModelScope.launch { runCatching { drafts.flush() } }
    }
}

/** Local-only feedback: students explicitly copy diagnostic identifiers to share with their teacher. */
@Composable
fun ContentFeedbackButton(moduleId: String, contentId: String, viewModel: ContentFeedbackViewModel = hiltViewModel()) {
    var open by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("答案或解析有误") }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    TextButton(onClick = { open = true; copied = false }, enabled = contentId.isNotBlank()) { Text("反馈此题") }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text("反馈内容问题") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("选择问题类型。信息仅保存在本机，复制后可交给老师；不会自动发送。")
                listOf("答案或解析有误", "音频或图片无法使用", "译文或例句有误", "其他内容问题").forEach { value ->
                    TextButton(onClick = { category = value }) { Text((if (category == value) "✓ " else "") + value) }
                }
                if (copied) Text("已复制问题信息")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val report = buildJsonObject {
                    put("module", moduleId); put("content_id", contentId); put("category", category)
                    put("app_version", BuildConfig.VERSION_NAME); put("reported_at", System.currentTimeMillis())
                    put("content_version", "未提供")
                }.toString()
                viewModel.save(report)
                clipboard.setText(AnnotatedString(report))
                copied = true
            }) { Text("复制问题信息") }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text("关闭") } }
    )
}
