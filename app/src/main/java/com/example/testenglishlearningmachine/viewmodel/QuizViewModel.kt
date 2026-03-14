package com.example.testenglishlearningmachine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testenglishlearningmachine.data.WordRepository
import com.example.testenglishlearningmachine.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizViewModel : ViewModel() {
    private val repository = WordRepository()

    private val _currentQuestion = MutableStateFlow<Word?>(null)
    val currentQuestion: StateFlow<Word?> = _currentQuestion.asStateFlow()

    private val _options = MutableStateFlow<List<Word>>(emptyList())
    val options: StateFlow<List<Word>> = _options.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _isCorrect = MutableStateFlow<Boolean?>(null)
    val isCorrect: StateFlow<Boolean?> = _isCorrect.asStateFlow()

    private var allWords: List<Word> = emptyList()

    init {
        viewModelScope.launch {
            repository.getAllWords().collect { words ->
                allWords = words
                generateNewQuestion()
            }
        }
    }

    fun generateNewQuestion() {
        if (allWords.size < 4) return // Need at least 4 words

        val question = allWords.random()
        _currentQuestion.value = question

        val distractors = allWords.filter { it.id != question.id }.shuffled().take(3)
        _options.value = (distractors + question).shuffled()
        _isCorrect.value = null
    }

    fun checkAnswer(selectedWord: Word) {
        if (_isCorrect.value != null) return // Already answered

        val correct = selectedWord.id == _currentQuestion.value?.id
        _isCorrect.value = correct
        if (correct) {
            _score.value += 1
        }
    }
}
