package ru.pukhanov.tabletka.data.local.database

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromDaysOfWeek(days: Set<DayOfWeek>?): String {
        if (days == null) return ""
        return days.map { it.name }.joinToString(",")
    }

    @TypeConverter
    fun toDaysOfWeek(value: String?): Set<DayOfWeek> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",")
            .filter { it.isNotBlank() }
            .map { DayOfWeek.valueOf(it) }
            .toSet()
    }
}
