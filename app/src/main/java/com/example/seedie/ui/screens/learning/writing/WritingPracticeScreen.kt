package com.example.seedie.ui.screens.learning.writing

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.model.WritingPrompt
import com.example.seedie.ui.components.TabSectionSurface
import com.example.seedie.ui.screens.learning.assignments.PracticeAssignmentArgs
import com.example.seedie.ui.theme.gardenShadow
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    WritingPracticeHeader(
                        title = uiState.title.ifBlank { "写作实战" },
                        statusLabel = statusChipText(uiState.status),
                        onNavigateBack = onNavigateBack
                    )

                    val prompt = uiState.prompt
                    if (prompt != null) {
                        WritingPromptCard(prompt = prompt)
                    }

                    uiState.errorMessage?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                text = it,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    when (uiState.status) {
                        "pending", "in_progress" -> WritingSubmitSection(
                            uiState = uiState,
                            onPickFile = {
                                pickFile.launch(
                                    arrayOf(
                                        "image/jpeg",
                                        "image/png",
                                        "image/webp",
                                        "application/pdf"
                                    )
                                )
                            },
                            onLaunchCamera = { launchCamera() },
                            onClearSelection = onClearSelection,
                            onSubmit = onSubmit
                        )
                        "submitted" -> WritingReturnedSection(
                            title = "已提交，等待老师批改",
                            subtitle = null,
                            fileLabel = "我的提交",
                            fileUrl = uiState.originalSignedUrl,
                            isPdf = uiState.isPdfOriginal
                        )
                        "returned" -> {
                            val scoreLine =
                                if (uiState.score != null && uiState.maxScore != null) {
                                    "得分 ${uiState.score} / ${uiState.maxScore}"
                                } else null
                            WritingReturnedSection(
                                title = "老师已返还",
                                subtitle = scoreLine,
                                feedback = uiState.feedbackText,
                                fileLabel = "批改件",
                                fileUrl = uiState.annotatedSignedUrl ?: uiState.originalSignedUrl,
                                isPdf = if (uiState.annotatedSignedUrl != null) {
                                    uiState.isPdfAnnotated
                                } else {
                                    uiState.isPdfOriginal
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WritingPracticeHeader(
    title: String,
    statusLabel: String?,
    onNavigateBack: () -> Unit
) {
    TabSectionSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onNavigateBack)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            if (statusLabel != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun WritingPromptCard(prompt: WritingPrompt) {
    TabSectionSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "作文题目",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = prompt.titleZh ?: prompt.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = prompt.promptTextZh ?: prompt.promptText,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(text = "词数 ${prompt.wordCountMin}–${prompt.wordCountHint}")
                InfoChip(text = "满分 ${prompt.maxScore}")
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = CircleShape
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun WritingSubmitSection(
    uiState: WritingPracticeUiState,
    onPickFile: () -> Unit,
    onLaunchCamera: () -> Unit,
    onClearSelection: () -> Unit,
    onSubmit: () -> Unit
) {
    TabSectionSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "上传作文",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "支持拍照或选择本地图片、PDF，一次一份。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onLaunchCamera,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("拍照")
                }
                OutlinedButton(
                    onClick = onPickFile,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("选择文件")
                }
                if (uiState.previewUri != null) {
                    OutlinedButton(
                        onClick = onClearSelection,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("清除")
                    }
                }
            }

            uiState.selectedFileName?.let {
                Text(
                    text = "已选择：$it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            val preview = uiState.previewUri
            if (preview != null && !uiState.isPdfOriginal) {
                WritingImageThumbnail(
                    url = preview,
                    contentDescription = "预览",
                    heightDp = 220
                )
            } else if (preview != null && uiState.isPdfOriginal) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "已选择 PDF，提交后老师可查看。",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .gardenShadow(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Button(
                onClick = onSubmit,
                enabled = !uiState.isSubmitting && uiState.previewUri != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.isSubmitting) "提交中…" else "提交作业")
            }
        }
    }
}

@Composable
private fun WritingReturnedSection(
    title: String,
    subtitle: String?,
    feedback: String? = null,
    fileLabel: String,
    fileUrl: String?,
    isPdf: Boolean
) {
    TabSectionSurface(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!feedback.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "老师评语",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = feedback,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            FilePreview(url = fileUrl, isPdf = isPdf, label = fileLabel)
        }
    }
}

@Composable
private fun FilePreview(url: String?, isPdf: Boolean, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.SemiBold
        )
        when {
            url == null -> Text(
                text = "暂无预览",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            isPdf -> Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "PDF 文件已就绪，请在电脑端查看批改件。",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            else -> WritingImageThumbnail(
                url = url,
                contentDescription = label,
                heightDp = 280
            )
        }
    }
}

private fun statusChipText(status: String): String? = when (status) {
    "pending" -> "待提交"
    "in_progress" -> "进行中"
    "submitted" -> "已提交"
    "returned" -> "已批改"
    else -> null
}
