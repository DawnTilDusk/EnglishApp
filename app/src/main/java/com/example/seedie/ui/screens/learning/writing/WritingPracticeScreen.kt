package com.example.seedie.ui.screens.learning.writing

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.screens.learning.assignments.PracticeAssignmentArgs
import java.io.File

@Composable
fun WritingPracticeRoute(
    assignmentArgs: PracticeAssignmentArgs,
    onNavigateBack: () -> Unit,
    onAwardResult: (StudyResult) -> Unit,
    viewModel: WritingPracticeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(assignmentArgs.submissionId) {
        viewModel.initialize(assignmentArgs)
    }

    LaunchedEffect(Unit) {
        viewModel.studyResults.collect { result ->
            onAwardResult(result)
        }
    }

    WritingPracticeScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onFilePicked = viewModel::onFilePicked,
        onClearSelection = viewModel::clearSelection,
        onSubmit = viewModel::submit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WritingPracticeScreen(
    uiState: WritingPracticeUiState,
    onNavigateBack: () -> Unit,
    onFilePicked: (Uri, String?) -> Unit,
    onClearSelection: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onFilePicked(uri, null)
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            onFilePicked(uri, "camera.jpg")
        }
    }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "writing_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraUri = uri
            takePicture.launch(uri)
        }
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val file = File(context.cacheDir, "writing_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraUri = uri
            takePicture.launch(uri)
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title.ifBlank { "写作作业" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null && uiState.prompt == null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.errorMessage ?: "加载失败")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onNavigateBack) { Text("返回") }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val prompt = uiState.prompt
                        if (prompt != null) {
                            Text(
                                prompt.titleZh ?: prompt.title,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = prompt.promptTextZh ?: prompt.promptText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "建议词数 ${prompt.wordCountMin}–${prompt.wordCountHint} · 满分 ${prompt.maxScore}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        uiState.errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }

                        when (uiState.status) {
                            "pending", "in_progress" -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(onClick = { launchCamera() }) {
                                        Text("拍照")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            pickFile.launch(
                                                arrayOf(
                                                    "image/jpeg",
                                                    "image/png",
                                                    "image/webp",
                                                    "application/pdf"
                                                )
                                            )
                                        }
                                    ) {
                                        Text("选择文件")
                                    }
                                    if (uiState.previewUri != null) {
                                        OutlinedButton(onClick = onClearSelection) {
                                            Text("清除")
                                        }
                                    }
                                }
                                uiState.selectedFileName?.let {
                                    Text("已选择：$it", style = MaterialTheme.typography.bodySmall)
                                }
                                val preview = uiState.previewUri
                                if (preview != null && !uiState.isPdfOriginal) {
                                    WritingImageThumbnail(
                                        url = preview,
                                        contentDescription = "预览",
                                        heightDp = 220
                                    )
                                } else if (preview != null && uiState.isPdfOriginal) {
                                    Text("已选择 PDF，提交后老师可查看。")
                                }
                                Button(
                                    onClick = onSubmit,
                                    enabled = !uiState.isSubmitting && uiState.previewUri != null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (uiState.isSubmitting) "提交中…" else "提交作业")
                                }
                            }
                            "submitted" -> {
                                Text("已提交，等待老师批改", style = MaterialTheme.typography.titleMedium)
                                FilePreview(
                                    url = uiState.originalSignedUrl,
                                    isPdf = uiState.isPdfOriginal,
                                    label = "我的提交"
                                )
                            }
                            "returned" -> {
                                Text("老师已返还", style = MaterialTheme.typography.titleMedium)
                                if (uiState.score != null && uiState.maxScore != null) {
                                    Text("得分 ${uiState.score}/${uiState.maxScore}")
                                }
                                uiState.feedbackText?.let {
                                    Text("评语：$it", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilePreview(
                                    url = uiState.annotatedSignedUrl ?: uiState.originalSignedUrl,
                                    isPdf = if (uiState.annotatedSignedUrl != null) {
                                        uiState.isPdfAnnotated
                                    } else {
                                        uiState.isPdfOriginal
                                    },
                                    label = "批改件"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilePreview(url: String?, isPdf: Boolean, label: String) {
    Text(label, style = MaterialTheme.typography.titleSmall)
    when {
        url == null -> Text("暂无预览", color = MaterialTheme.colorScheme.onSurfaceVariant)
        isPdf -> Text("PDF 文件已就绪（请在电脑端查看批改件）")
        else -> {
            WritingImageThumbnail(
                url = url,
                contentDescription = label,
                heightDp = 280
            )
        }
    }
}
