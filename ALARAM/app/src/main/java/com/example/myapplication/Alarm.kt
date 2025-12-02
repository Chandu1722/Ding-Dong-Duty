package com.example.alarmpuzzle

import java.util.Locale

enum class PuzzleType(val displayName: String) {
    MATH("Math Problem"),
    RETYPE("Retype Text"),
    SHAKE("Shake Phone"),
    MEMORY("Memory Pairs"),
    COLOR_MATCH("Color Match")
}

enum class DayOfWeek {
    SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY
}

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val recurringDays: Set<DayOfWeek> = emptySet(),
    val puzzleType: PuzzleType = PuzzleType.MATH,
    val label: String = "",
    val vibrate: Boolean = true,
    val ringtoneUri: String? = null // Added field for custom ringtone
) {
    fun getRecurringDaysText(): String {
        if (!isEnabled) return "Not scheduled"
        if (recurringDays.isEmpty()) return "Not scheduled"
        if (recurringDays.size == 7) return "Every day"

        return recurringDays.sortedBy { it.ordinal }.joinToString(", ") {
            it.name.take(3).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }
    }
}

