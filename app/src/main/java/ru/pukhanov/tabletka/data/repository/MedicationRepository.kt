package ru.pukhanov.tabletka.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import ru.pukhanov.tabletka.data.local.dao.MedicationDao
import ru.pukhanov.tabletka.data.model.Medication

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAll()

    suspend fun getById(id: Long): Medication? = withContext(ioDispatcher) {
        medicationDao.getById(id)
    }

    suspend fun insert(medication: Medication): Long = withContext(ioDispatcher) {
        medicationDao.insert(medication)
    }

    suspend fun delete(medication: Medication) = withContext(ioDispatcher) {
        medicationDao.delete(medication)
    }
}
