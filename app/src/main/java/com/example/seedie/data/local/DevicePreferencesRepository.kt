package com.example.seedie.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_prefs")

@Singleton
class DevicePreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

    suspend fun getOrCreateDeviceId(): String {
        val currentId = context.deviceDataStore.data.map { preferences ->
            preferences[DEVICE_ID_KEY]
        }.first()

        if (currentId != null) {
            return currentId
        }

        val newId = UUID.randomUUID().toString()
        context.deviceDataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = newId
        }
        return newId
    }
}
