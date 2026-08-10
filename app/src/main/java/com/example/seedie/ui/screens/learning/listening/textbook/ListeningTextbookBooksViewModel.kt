package com.example.seedie.ui.screens.learning.listening.textbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.repository.ManagedWordBook
import com.example.seedie.domain.repository.WordBookDownloadStatus
import com.example.seedie.domain.repository.WordBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class TextbookBooksUiState(
    val books: List<ManagedWordBook> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val downloadingBookId: String? = null,
    val readyToOpenBookId: String? = null
)

@HiltViewModel
class ListeningTextbookBooksViewModel @Inject constructor(
    private val wordBookRepository: WordBookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextbookBooksUiState())
    val uiState: StateFlow<TextbookBooksUiState> = _uiState.asStateFlow()

    init {
        wordBookRepository.observeWordBooks()
            .map { list -> list.filter { it.bookId.startsWith("pep-") || it.bookId.startsWith("fltrp-") } }
            .onEach { books ->
                _uiState.value = _uiState.value.copy(books = books, loading = false)
            }
            .launchIn(viewModelScope)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val result = wordBookRepository.refreshWordBooks()
            _uiState.value = _uiState.value.copy(
                loading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun onBookClicked(book: ManagedWordBook) {
        if (book.downloadStatus == WordBookDownloadStatus.DOWNLOADED) {
            _uiState.value = _uiState.value.copy(readyToOpenBookId = book.bookId)
            return
        }
        if (_uiState.value.downloadingBookId != null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(downloadingBookId = book.bookId, error = null)
            val result = wordBookRepository.downloadWordBook(book.bookId)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    downloadingBookId = null,
                    readyToOpenBookId = book.bookId
                )
            } else {
                _uiState.value.copy(
                    downloadingBookId = null,
                    error = result.exceptionOrNull()?.message ?: "下载失败"
                )
            }
        }
    }

    fun consumeReadyToOpen() {
        _uiState.value = _uiState.value.copy(readyToOpenBookId = null)
    }
}
