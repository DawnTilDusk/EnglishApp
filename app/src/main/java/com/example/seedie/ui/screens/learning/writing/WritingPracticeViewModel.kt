package com.example.seedie.ui.screens.learning.writing

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.PracticeAssignmentRepository
import com.example.seedie.ui.screens.learning.assignments.PracticeAssignmentArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WritingPracticeViewModel @Inject constructor(
    private val repository: PracticeAssignmentRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(WritingPracticeUiState())
    val uiState = _uiState.asStateFlow()

    private val _studyResults = MutableSharedFlow<StudyResult>(extraBufferCapacity = 1)
    val studyResults = _studyResults.asSharedFlow()

    private var submissionId: String? = null
    private var assignmentId: String? = null
    private var selectedBytes: ByteArray? = null
    private var selectedExt: String = "jpg"
    private var tokensEmitted = false

    fun initialize(args: PracticeAssignmentArgs) {
        if (args.moduleId != "writing") {
            _uiState.update { it.copy(isLoading = false, errorMessage = "不是写作作业") }
            return
        }
        submissionId = args.submissionId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val detail = repository.getDetail(args.submissionId)
                assignmentId = detail.assignmentId
                if (detail.status == "pending" || detail.status == "in_progress") {
                    runCatching { repository.start(args.submissionId) }
                }
                val promptId = detail.itemRefs.firstOrNull()
                    ?: error("作业没有作文题目")
                val prompt = repository.fetchWritingPrompt(promptId)
                    ?: error("作文题目不存在")
                val originalUrl = detail.originalPath?.let { path ->
                    runCatching { repository.createWritingSignedUrl(path) }.getOrNull()
                }
                val annotatedUrl = detail.annotatedPath?.let { path ->
                    runCatching { repository.createWritingSignedUrl(path) }.getOrNull()
                }
                val baseDews = 5
                val bonusDews = if (detail.score != null && detail.score >= 12) 5 else 0
                WritingPracticeUiState(
                    isLoading = false,
                    title = detail.title,
                    status = detail.status,
                    prompt = prompt,
                    originalSignedUrl = originalUrl,
                    annotatedSignedUrl = annotatedUrl,
                    score = detail.score,
                    maxScore = detail.maxScore ?: prompt.maxScore,
                    feedbackText = detail.feedbackText,
                    earnedTokens = detail.earnedTokens,
                    earnedDews = if (detail.status == "returned") baseDews + bonusDews else 0,
                    isPdfOriginal = detail.originalPath?.lowercase()?.endsWith(".pdf") == true,
                    isPdfAnnotated = detail.annotatedPath?.lowercase()?.endsWith(".pdf") == true
                )
            }.onSuccess { state ->
                _uiState.value = state
                maybeEmitTokens(state)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "加载失败")
                }
            }
        }
    }

    fun onFilePicked(uri: Uri, displayName: String?) {
        viewModelScope.launch {
            runCatching {
                val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("无法读取文件")
                if (bytes.size > 10 * 1024 * 1024) {
                    error("文件不能超过 10MB")
                }
                val name = displayName ?: uri.lastPathSegment ?: "upload.jpg"
                val ext = extensionFromName(name) ?: extensionFromMime(
                    appContext.contentResolver.getType(uri)
                ) ?: "jpg"
                selectedBytes = bytes
                selectedExt = ext
                _uiState.update {
                    it.copy(
                        previewUri = uri.toString(),
                        selectedFileName = name,
                        errorMessage = null,
                        isPdfOriginal = ext == "pdf"
                    )
                }
            }.onFailure { error ->
                selectedBytes = null
                _uiState.update {
                    it.copy(errorMessage = error.message ?: "读取文件失败")
                }
            }
        }
    }

    fun clearSelection() {
        selectedBytes = null
        _uiState.update {
            it.copy(previewUri = null, selectedFileName = null, errorMessage = null)
        }
    }

    fun submit() {
        val sid = submissionId ?: return
        val aid = assignmentId ?: return
        val bytes = selectedBytes
        if (bytes == null) {
            _uiState.update { it.copy(errorMessage = "请先拍照或选择文件") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            runCatching {
                val path = "$aid/$sid/original.$selectedExt"
                repository.uploadWritingOriginal(path, bytes)
                repository.submitWriting(sid, path)
                path
            }.onSuccess { path ->
                val url = runCatching { repository.createWritingSignedUrl(path) }.getOrNull()
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        status = "submitted",
                        originalSignedUrl = url,
                        previewUri = null,
                        selectedFileName = null,
                        earnedDews = it.earnedDews + 5,
                        isPdfOriginal = selectedExt == "pdf"
                    )
                }
                selectedBytes = null
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "提交失败"
                    )
                }
            }
        }
    }

    private fun maybeEmitTokens(state: WritingPracticeUiState) {
        if (tokensEmitted) return
        if (state.status != "returned") return
        val sid = submissionId ?: return
        tokensEmitted = true
        val baseDews = 5
        val bonusDews = if (state.score != null && state.score >= 12) 5 else 0
        _studyResults.tryEmit(
            StudyResult(
                sessionId = sid,
                moduleId = "writing",
                isCompleted = true,
                completedQuestionCount = 1,
                correctCount = 0,
                wrongCount = 0,
                skippedCount = 0,
                accuracy = 0f,
                earnedTokens = 0,
                earnedDews = baseDews + bonusDews,
                studyDurationSec = 0,
                vocabularyDelta = 0,
                wrongWordIds = emptyList()
            )
        )
    }

    private fun extensionFromName(name: String): String? {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "jpg"
            "png" -> "png"
            "webp" -> "webp"
            "pdf" -> "pdf"
            else -> null
        }
    }

    private fun extensionFromMime(mime: String?): String? {
        return when (mime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> null
        }
    }
}
