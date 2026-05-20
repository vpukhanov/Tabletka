package ru.pukhanov.tabletka.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.model.MedicationWithSchedules

@Dao
interface MedicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication): Long

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Query("SELECT * FROM medications ORDER BY id DESC")
    fun getAll(): Flow<List<Medication>>

    @Delete
    suspend fun delete(medication: Medication)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<MedicationSchedule>)

    @Query("DELETE FROM medication_schedules WHERE medicationId = :medicationId")
    suspend fun deleteSchedulesForMedication(medicationId: Long)

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationWithSchedules(id: Long): MedicationWithSchedules?

    @Transaction
    suspend fun saveMedicationWithSchedules(medication: Medication, schedules: List<MedicationSchedule>) {
        val medicationId = insert(medication)
        val targetMedicationId = if (medication.id == 0L) medicationId else medication.id
        deleteSchedulesForMedication(targetMedicationId)
        val schedulesToInsert = schedules.map { it.copy(medicationId = targetMedicationId) }
        insertSchedules(schedulesToInsert)
    }
}
