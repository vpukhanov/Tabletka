package ru.pukhanov.tabletka.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(
    tableName = "medication_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["medicationId"])]
)
data class MedicationSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long = 0L,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<DayOfWeek>,
    val doses: Double = 1.0
)
