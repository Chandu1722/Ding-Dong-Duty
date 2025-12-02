package com.example.myapplication

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// top-level property (use a distinct name to avoid clash)
private val Context.alarmDataStore by preferencesDataStore(name = "alarm_prefs")

object AlarmDataStore {
    private val ALARM_HOUR = intPreferencesKey("alarm_hour")
    private val ALARM_MINUTE = intPreferencesKey("alarm_minute")

    // Save - use applicationContext to be safe
    suspend fun saveAlarm(context: Context, hour: Int, minute: Int) {
        // edit is suspend; caller should call from coroutine (preferably on IO)
        context.applicationContext.alarmDataStore.edit { prefs ->
            prefs[ALARM_HOUR] = hour
            prefs[ALARM_MINUTE] = minute
        }
    }

    // Read as Flow, but recover from IO errors so the consumer doesn't crash
    fun getSavedAlarm(context: Context): Flow<Pair<Int, Int>?> {
        return context.applicationContext.alarmDataStore.data
            .catch { exception ->
                // DataStore emits IOException on read problems — recover to empty preferences
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                val hour = prefs[ALARM_HOUR]
                val minute = prefs[ALARM_MINUTE]
                if (hour != null && minute != null) Pair(hour, minute) else null
            }
    }
}
 















/* package com.example.myapplication

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "alarm_prefs")

object AlarmDataStore {
    val ALARM_HOUR = intPreferencesKey("alarm_hour")
    val ALARM_MINUTE = intPreferencesKey("alarm_minute")

    suspend fun saveAlarm(context: Context, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[ALARM_HOUR] = hour
            prefs[ALARM_MINUTE] = minute
        }
    }

    fun getSavedAlarm(context: Context): Flow<Pair<Int, Int>?> {
        return context.dataStore.data.map { prefs ->
            val hour = prefs[ALARM_HOUR]
            val minute = prefs[ALARM_MINUTE]
            if (hour != null && minute != null) Pair(hour, minute) else null
        }
    }
}

*/