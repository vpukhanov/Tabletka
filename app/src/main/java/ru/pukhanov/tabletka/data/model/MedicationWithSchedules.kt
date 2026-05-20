package ru.pukhanov.tabletka.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class MedicationWithSchedules(
    @Embedded val medication: Medication,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicationId"
    )
    val schedules: List<MedicationSchedule>
)
