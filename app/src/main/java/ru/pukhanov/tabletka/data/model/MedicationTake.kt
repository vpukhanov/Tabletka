package ru.pukhanov.tabletka.data.model

import androidx.room.Entity

@Entity(
    tableName = "medication_takes",
    primaryKeys = ["date", "hour", "minute"]
)
data class MedicationTake(
    val date: String, // Format: YYYY-MM-DD
    val hour: Int,
    val minute: Int
)
