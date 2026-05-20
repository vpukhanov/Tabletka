package ru.pukhanov.tabletka.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.pukhanov.tabletka.data.local.dao.MedicationDao
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.model.MedicationWithSchedules
import ru.pukhanov.tabletka.data.model.MedicationTake

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAll()

    val allMedicationsWithSchedules: Flow<List<MedicationWithSchedules>> = medicationDao.getAllWithSchedules()

    fun getTakesForDate(date: String): Flow<List<MedicationTake>> {
        return medicationDao.getTakesForDate(date)
    }

    suspend fun insertTake(take: MedicationTake) = withContext(ioDispatcher) {
        medicationDao.insertTake(take)
    }

    suspend fun deleteTake(date: String, hour: Int, minute: Int) = withContext(ioDispatcher) {
        medicationDao.deleteTake(date, hour, minute)
    }

    suspend fun getById(id: Long): Medication? = withContext(ioDispatcher) {
        medicationDao.getById(id)
    }

    suspend fun insert(medication: Medication): Long = withContext(ioDispatcher) {
        medicationDao.insert(medication)
    }

    suspend fun delete(medication: Medication) = withContext(ioDispatcher) {
        medicationDao.delete(medication)
    }

    suspend fun getWithSchedules(id: Long): MedicationWithSchedules? = withContext(ioDispatcher) {
        medicationDao.getMedicationWithSchedules(id)
    }

    suspend fun save(medication: Medication, schedules: List<MedicationSchedule>): Long = withContext(ioDispatcher) {
        medicationDao.saveMedicationWithSchedules(medication, schedules)
    }
}

