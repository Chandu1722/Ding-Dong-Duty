package com.example.alarmpuzzle

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object AlarmRepository {
    private const val PREFS_NAME = "AlarmPuzzlePrefs"
    private const val ALARMS_KEY = "alarms_list"
    private val gson = Gson()

    fun saveAlarms(context: Context, alarms: List<Alarm>) {
        val jsonString = gson.toJson(alarms)
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putString(ALARMS_KEY, jsonString).apply()
    }

    fun loadAlarms(context: Context): List<Alarm> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = sharedPrefs.getString(ALARMS_KEY, null)
        return if (jsonString != null) {
            val type = object : TypeToken<List<Alarm>>() {}.type
            gson.fromJson(jsonString, type)
        } else {
            emptyList()
        }
    }
}

