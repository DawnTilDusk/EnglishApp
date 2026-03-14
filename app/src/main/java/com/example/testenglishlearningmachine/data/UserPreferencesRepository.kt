package com.example.testenglishlearningmachine.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val USER_NAME = stringPreferencesKey("user_name")
        val WORDS_LEARNED = intPreferencesKey("words_learned")
    }

    val userName: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USER_NAME] ?: "Student"
        }

    val wordsLearned: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WORDS_LEARNED] ?: 0
        }

    suspend fun saveUserName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
        }
    }

    suspend fun incrementWordsLearned() {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.WORDS_LEARNED] ?: 0
            preferences[PreferencesKeys.WORDS_LEARNED] = current + 1
        }
    }
}
