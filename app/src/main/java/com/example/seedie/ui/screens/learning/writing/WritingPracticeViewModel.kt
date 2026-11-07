package com.example.seedie.ui.screens.learning.writing

import com.example.seedie.data.repository.PracticeDraftStore
import com.example.seedie.data.repository.StudySettlementManager
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    private val drafts: PracticeDraftStore,
    private val settlements: StudySettlementManager,
    @param:ApplicationContext private val appContext: Context
) : ViewModel() {
    private val ownerUserId = settlements.currentUserId()
    private val _uiState = MutableStateFlow(WritingPracticeUiState())
    val uiState = _uiState.asStateFlow()

    private val _studyResults = MutableSharedFlow<StudyResult>(extraBufferCapacity = 1)
    val studyResults = _studyResults.asSharedFlow()

    private var submissionId: String? = null
    private var assignmentId: String? = null
    private var draft: WritingFileDraft? = null
    private val draftJson = Json { ignoreUnknownKeys = true }
    private val draftKey: String get() = "writing:${submissionId.orEmpty()}"

    private suspend fun restoreFileDraft() {
        val saved = drafts.load(draftKey, ownerUserId)?.let {
            runCatching { draftJson.decodeFromString<WritingFileDraft>(it) }.getOrNull()
        } ?: return
        val file = File(appContext.filesDir, saved.localName)
        if (!file.isFile) return
        draft = saved
        selectedExt = saved.extension
        _uiState.update { it.copy(previewUri = Uri.fromFile(file).toString(), selectedFileName = saved.displayName,
            isPdfOriginal = saved.extension == "pdf") }
    }

    private fun discardDraft() {
        if (drafts.userId() != ownerUserId) return
        val previous = draft
        draft = null
        drafts.clear(draftKey)
        // Clear metadata first; a crash must not leave a live draft pointing at a removed file.
        viewModelScope.launch(Dispatchers.IO) {
            drafts.flush()
            previous?.let { File(appContext.filesDir, it.localName).delete() }
        }
    }

    private suspend fun recordSubmission(atMillis: Long = System.currentTimeMillis()) {
        val sid = submissionId ?: return
        settlements.enqueue(StudyResult(
            sessionId = "$sid:submitted", moduleId = "writing", isCompleted = true,
            completedQuestionCount = 1, correctCount = 0, wrongCount = 0, skippedCount = 0,
            accuracy = 0f, earnedTokens = 0, earnedDews = 5, studyDurationSec = 0,
            vocabularyDelta = 0, wrongWordIds = emptyList(), completedAtMillis = atMillis
        ), ownerUserId)
    }
    private var selectedExt: String = "jpg"
    private var tokensEmitted = false
    private var serverSubmittedAt: Long? = null

    fun initialize(args: PracticeAssignmentArgs) {
        if (args.moduleId != "writing") {
            _uiState.update { it.copy(isLoading = false, errorMessage = "不是写作作业") }
            return
        }
        submissionId = args.submissionId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            restoreFileDraft()
            runCatching {
                val detail = repository.getDetail(args.submissionId)
                serverSubmittedAt = detail.submittedAtEpochMs
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
                if (state.status in setOf("submitted", "returned")) {
                    if (draft?.submissionPending == true) recordSubmission(serverSubmittedAt ?: System.currentTimeMillis())
                    discardDraft()
                } else restoreFileDraft()
                maybeEmitTokens(state)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "加载失败")
                }
            }
        }
    }

    fun onFilePicked(uri: Uri, displayName: String?) {
        if (_uiState.value.isSubmitting || drafts.userId() != ownerUserId) return
        viewModelScope.launch {
            runCatching {
                val name = displayName ?: uri.lastPathSegment ?: "upload.jpg"
                val ext = extensionFromName(name) ?: extensionFromMime(appContext.contentResolver.getType(uri))
                    ?: error("请选择 JPG、PNG、WebP 或 PDF 文件")
                val saved = withContext(Dispatchers.IO) {
                    val file = File(appContext.filesDir, "writing_draft_${UUID.randomUUID()}.$ext")
                    try {
                        appContext.contentResolver.openInputStream(uri)?.use { input ->
                            file.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var total = 0
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    total += read
                                    require(total <= 10 * 1024 * 1024) { "文件不能超过 10MB" }
                                    output.write(buffer, 0, read)
                                }
                                require(total > 0) { "文件为空" }
                            }
                        } ?: throw IOException("无法读取文件")
                        WritingFileDraft(file.name, name, ext)
                    } catch (error: Exception) { file.delete(); throw error }
                }
                check(drafts.userId() == ownerUserId) { "账号已变化，请重新进入作业" }
                val previous = draft
                draft = saved
                selectedExt = ext
                drafts.save(draftKey, draftJson.encodeToString(saved))
                drafts.flush()
                withContext(Dispatchers.IO) { previous?.let { File(appContext.filesDir, it.localName).delete() } }
                _uiState.update { it.copy(previewUri = Uri.fromFile(File(appContext.filesDir, saved.localName)).toString(),
                    selectedFileName = name, errorMessage = null, isPdfOriginal = ext == "pdf") }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { it.copy(errorMessage = error.message ?: "读取文件失败") }
            }
        }
    }

    fun clearSelection() {
        if (_uiState.value.isSubmitting) return
        discardDraft()
        _uiState.update { it.copy(previewUri = null, selectedFileName = null, errorMessage = null) }
    }

    fun submit() {
        if (_uiState.value.isSubmitting || drafts.userId() != ownerUserId) return
        val sid = submissionId ?: return
        val aid = assignmentId ?: return
        val saved = draft ?: run {
            _uiState.update { it.copy(errorMessage = "请先拍照或选择文件") }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                draft = saved.copy(submissionPending = true)
                drafts.save(draftKey, draftJson.encodeToString(requireNotNull(draft)))
                drafts.flush()
                // A timed-out request may already have succeeded; query before uploading again.
                val detail = repository.getDetail(sid)
                serverSubmittedAt = detail.submittedAtEpochMs
                if (detail.status !in setOf("submitted", "returned")) {
                    val path = "$aid/$sid/original.${saved.extension}"
                    val bytes = withContext(Dispatchers.IO) { File(appContext.filesDir, saved.localName).readBytes() }
                    repository.uploadWritingOriginal(path, bytes)
                    repository.submitWriting(sid, path)
                    path
                } else detail.originalPath ?: error("作业已提交，请重新加载查看")
            }.onSuccess { path ->
                recordSubmission(serverSubmittedAt ?: System.currentTimeMillis())
                val url = runCatching { repository.createWritingSignedUrl(path) }.getOrNull()
                _uiState.update { it.copy(isSubmitting = false, status = "submitted", originalSignedUrl = url,
                    previewUri = null, selectedFileName = null, earnedDews = 5, isPdfOriginal = saved.extension == "pdf") }
                discardDraft()
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { it.copy(isSubmitting = false, errorMessage = "提交未确认，文件已保存，请重试或重新加载作业") }
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
        val result = StudyResult(
                sessionId = "$sid:returned",
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
                wrongWordIds = emptyList(),
                countsTowardTasks = false
            )
        viewModelScope.launch {
            settlements.enqueue(result, ownerUserId)
            _studyResults.emit(result)
        }
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

@Serializable
private data class WritingFileDraft(
    val localName: String,
    val displayName: String,
    val extension: String,
    val submissionPending: Boolean = false
)
