package com.example.testenglishlearningmachine.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.testenglishlearningmachine.data.UserPreferencesRepository
import com.example.testenglishlearningmachine.data.WordRepository
import com.example.testenglishlearningmachine.model.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class LearningViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val wordRepository = WordRepository()
    private val userPreferencesRepository = UserPreferencesRepository(application)
    private var tts: TextToSpeech? = null

    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow()

    private val _currentWordIndex = MutableStateFlow(0)
    val currentWordIndex: StateFlow<Int> = _currentWordIndex.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)

    init {
        tts = TextToSpeech(application, this)
        viewModelScope.launch {
            wordRepository.getAllWords().collect {
                _words.value = it
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language specified is not supported!")
            } else {
                _isTtsReady.value = true
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    fun playAudio(text: String) {
        if (_isTtsReady.value) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    fun nextWord() {
        if (_currentWordIndex.value < _words.value.size - 1) {
            _currentWordIndex.value++
            viewModelScope.launch {
                userPreferencesRepository.incrementWordsLearned()
            }
        } else {
            // Loop back or finish
            _currentWordIndex.value = 0
        }
    }

    fun previousWord() {
        if (_currentWordIndex.value > 0) {
            _currentWordIndex.value--
        }
    }

    override fun onCleared() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onCleared()
    }
}
