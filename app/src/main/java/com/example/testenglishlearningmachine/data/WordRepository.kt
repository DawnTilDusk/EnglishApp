package com.example.testenglishlearningmachine.data

import com.example.testenglishlearningmachine.model.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WordRepository {

    private val words = listOf(
        Word(1, "Apple", "苹果", "/ˈæpl/", "I eat an apple every day."),
        Word(2, "Banana", "香蕉", "/bəˈnænə/", "Monkeys love bananas."),
        Word(3, "Cat", "猫", "/kæt/", "The cat is sleeping on the sofa."),
        Word(4, "Dog", "狗", "/dɔːɡ/", "My dog likes to play fetch."),
        Word(5, "Elephant", "大象", "/ˈelɪfənt/", "Elephants have long trunks.")
    )

    fun getAllWords(): Flow<List<Word>> = flow {
        emit(words)
    }

    fun getWordById(id: Int): Word? {
        return words.find { it.id == id }
    }
}
