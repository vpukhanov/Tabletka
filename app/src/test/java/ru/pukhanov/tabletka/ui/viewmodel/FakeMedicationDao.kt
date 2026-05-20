package ru.pukhanov.tabletka.ui.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ru.pukhanov.tabletka.data.local.dao.MedicationDao
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.model.MedicationTake
import ru.pukhanov.tabletka.data.model.MedicationWithSchedules

class FakeMedicationDao : MedicationDao {
    private val medicationsMap = mutableMapOf<Long, Medication>()
    private val schedulesMap = mutableMapOf<Long, List<MedicationSchedule>>()
    private val _flow = MutableStateFlow<List<Medication>>(emptyList())
    private val _allWithSchedulesFlow = MutableStateFlow<List<MedicationWithSchedules>>(emptyList())
    private val takesFlowsMap = mutableMapOf<String, MutableStateFlow<List<MedicationTake>>>()
    private var nextId = 1L

    private fun updateAllWithSchedules() {
        _allWithSchedulesFlow.value = medicationsMap.map { (id, medication) ->
            MedicationWithSchedules(medication, schedulesMap[id] ?: emptyList())
        }
    }

    override suspend fun insert(medication: Medication): Long {
        val id = if (medication.id == 0L) nextId++ else medication.id
        val saved = medication.copy(id = id)
        medicationsMap[id] = saved
        _flow.value = medicationsMap.values.toList().reversed()
        updateAllWithSchedules()
        return id
    }

    override suspend fun getById(id: Long): Medication? {
        return medicationsMap[id]
    }

    override fun getAll(): Flow<List<Medication>> {
        return _flow
    }

    override suspend fun delete(medication: Medication) {
        medicationsMap.remove(medication.id)
        schedulesMap.remove(medication.id)
        _flow.value = medicationsMap.values.toList().reversed()
        updateAllWithSchedules()
    }

    override suspend fun insertSchedules(schedules: List<MedicationSchedule>) {
        // Not directly called by repo under fake mapping
    }

    override suspend fun deleteSchedulesForMedication(medicationId: Long) {
        schedulesMap.remove(medicationId)
        updateAllWithSchedules()
    }

    override suspend fun getMedicationWithSchedules(id: Long): MedicationWithSchedules? {
        val medication = medicationsMap[id] ?: return null
        val schedules = schedulesMap[id] ?: emptyList()
        return MedicationWithSchedules(medication, schedules)
    }

    override fun getAllWithSchedules(): Flow<List<MedicationWithSchedules>> {
        return _allWithSchedulesFlow
    }

    override fun getTakesForDate(date: String): Flow<List<MedicationTake>> {
        return takesFlowsMap.getOrPut(date) { MutableStateFlow(emptyList()) }
    }

    override suspend fun insertTake(take: MedicationTake) {
        val flow = takesFlowsMap.getOrPut(take.date) { MutableStateFlow(emptyList()) }
        flow.value = (flow.value.filterNot { it.hour == take.hour && it.minute == take.minute } + take)
    }

    override suspend fun deleteTake(date: String, hour: Int, minute: Int) {
        val flow = takesFlowsMap[date] ?: return
        flow.value = flow.value.filterNot { it.hour == hour && it.minute == minute }
    }

    override suspend fun saveMedicationWithSchedules(
        medication: Medication,
        schedules: List<MedicationSchedule>
    ): Long {
        val id = insert(medication)
        val targetId = if (medication.id == 0L) id else medication.id
        schedulesMap[targetId] = schedules.map { it.copy(medicationId = targetId) }
        updateAllWithSchedules()
        return targetId
    }
}
