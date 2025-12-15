package com.kk.pokemonalert

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pokemon_alert_preferences")

class PreferencesManager(private val context: Context) {
    
    companion object {
        val QUEUE_STATUS_KEY = stringPreferencesKey("queue_status")
        val SNOOZE_UNTIL_KEY = longPreferencesKey("snooze_until")
    }
    
    suspend fun setQueueStatus(status: String) {
        context.dataStore.edit { preferences ->
            preferences[QUEUE_STATUS_KEY] = status
        }
    }
    
    fun getQueueStatus(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[QUEUE_STATUS_KEY]
        }
    }
    
    suspend fun setSnoozeUntil(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[SNOOZE_UNTIL_KEY] = timestamp
        }
    }
    
    fun getSnoozeUntil(): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[SNOOZE_UNTIL_KEY] ?: 0L
        }
    }
}
