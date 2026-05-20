package ru.pukhanov.tabletka.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import ru.pukhanov.tabletka.data.local.dao.MedicationDao
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.model.MedicationWithSchedules
import ru.pukhanov.tabletka.data.model.MedicationTake
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeDao: FakeMedicationDao
    private lateinit var repository: MedicationRepository
    private lateinit var viewModel: MedicationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeMedicationDao()
        repository = MedicationRepository(fakeDao, testDispatcher)
        viewModel = MedicationViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isCorrect() = runTest {
        assertEquals(emptyList<Medication>(), viewModel.medications.value)
        assertEquals(AddEditUiState(), viewModel.addEditUiState.value)
    }

    @Test
    fun loadMedication_withMedicationId_loadsMedicationDetails() = runTest {
        val medication = Medication(id = 42L, title = "Aspirin", brandName = "Bayer", dosage = "500mg")
        fakeDao.insert(medication)

        viewModel.loadMedication(42L)
        assertEquals("Aspirin", viewModel.addEditUiState.value.title)
        assertEquals("Bayer", viewModel.addEditUiState.value.brandName)
        assertEquals("500mg", viewModel.addEditUiState.value.dosage)
        assertEquals(true, viewModel.addEditUiState.value.isEditMode)
        assertEquals(42L, viewModel.addEditUiState.value.medicationId)
    }

    @Test
    fun saveMedication_emptyTitle_setsError() = runTest {
        viewModel.loadMedication(null)
        viewModel.onTitleChanged("")
        viewModel.saveMedication(onSuccess = {})

        assertEquals("Title cannot be empty", viewModel.addEditUiState.value.titleError)
        assertEquals(0, fakeDao.getAll().first().size)
    }

    @Test
    fun saveNewMedication_success() = runTest {
        viewModel.loadMedication(null)
        viewModel.onTitleChanged("Paracetamol")
        viewModel.onBrandNameChanged("Panadol")
        viewModel.onDosageChanged("500mg")
        var wasSuccess = false
        viewModel.saveMedication(onSuccess = { wasSuccess = true })

        val list = fakeDao.getAll().first()
        assertEquals(1, list.size)
        assertEquals("Paracetamol", list[0].title)
        assertEquals("Panadol", list[0].brandName)
        assertEquals("500mg", list[0].dosage)
        assertEquals(true, wasSuccess)
    }

    @Test
    fun saveExistingMedication_success() = runTest {
        val original = Medication(id = 10L, title = "Ibuprofen", brandName = "Nurofen", dosage = "200mg")
        fakeDao.insert(original)

        viewModel.loadMedication(10L)
        viewModel.onTitleChanged("Ibuprofen Forte")
        viewModel.onBrandNameChanged("Advil")
        viewModel.onDosageChanged("400mg")
        var wasSuccess = false
        viewModel.saveMedication(onSuccess = { wasSuccess = true })

        val list = fakeDao.getAll().first()
        assertEquals(1, list.size)
        assertEquals(10L, list[0].id)
        assertEquals("Ibuprofen Forte", list[0].title)
        assertEquals("Advil", list[0].brandName)
        assertEquals("400mg", list[0].dosage)
        assertEquals(true, wasSuccess)
    }

    @Test
    fun deleteMedication_success() = runTest {
        val medication = Medication(id = 5L, title = "To Delete")
        fakeDao.insert(medication)

        viewModel.deleteMedication(medication)

        val list = fakeDao.getAll().first()
        assertEquals(0, list.size)
    }

    @Test
    fun onAddSchedule_appendsDefaultSchedule() = runTest {
        viewModel.loadMedication(null)
        viewModel.onAddSchedule()

        val schedules = viewModel.addEditUiState.value.schedules
        assertEquals(1, schedules.size)
        assertEquals(8, schedules[0].hour)
        assertEquals(0, schedules[0].minute)
        assertEquals(DayOfWeek.values().toSet(), schedules[0].daysOfWeek)
    }

    @Test
    fun onScheduleTimeChanged_updatesTimeCorrectly() = runTest {
        viewModel.loadMedication(null)
        viewModel.onAddSchedule()
        viewModel.onScheduleTimeChanged(index = 0, hour = 14, minute = 45)

        val schedules = viewModel.addEditUiState.value.schedules
        assertEquals(14, schedules[0].hour)
        assertEquals(45, schedules[0].minute)
    }

    @Test
    fun onScheduleDayToggled_togglesDayCorrectly() = runTest {
        viewModel.loadMedication(null)
        viewModel.onAddSchedule()

        // Toggle Monday off
        viewModel.onScheduleDayToggled(index = 0, day = DayOfWeek.MONDAY)
        assertEquals(DayOfWeek.values().toSet() - DayOfWeek.MONDAY, viewModel.addEditUiState.value.schedules[0].daysOfWeek)

        // Toggle Monday back on
        viewModel.onScheduleDayToggled(index = 0, day = DayOfWeek.MONDAY)
        assertEquals(DayOfWeek.values().toSet(), viewModel.addEditUiState.value.schedules[0].daysOfWeek)
    }

    @Test
    fun onDeleteSchedule_removesScheduleCorrectly() = runTest {
        viewModel.loadMedication(null)
        viewModel.onAddSchedule() // index 0
        viewModel.onAddSchedule() // index 1

        viewModel.onScheduleTimeChanged(index = 1, hour = 18, minute = 0)
        viewModel.onDeleteSchedule(index = 0)

        val schedules = viewModel.addEditUiState.value.schedules
        assertEquals(1, schedules.size)
        assertEquals(18, schedules[0].hour)
    }

    @Test
    fun loadMedication_withMedicationId_loadsSchedules() = runTest {
        val medication = Medication(id = 42L, title = "Aspirin")
        val schedules = listOf(
            MedicationSchedule(id = 1L, medicationId = 42L, hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY))
        )
        fakeDao.saveMedicationWithSchedules(medication, schedules)

        viewModel.loadMedication(42L)
        val state = viewModel.addEditUiState.value
        assertEquals("Aspirin", state.title)
        assertEquals(1, state.schedules.size)
        assertEquals(8, state.schedules[0].hour)
        assertEquals(setOf(DayOfWeek.MONDAY), state.schedules[0].daysOfWeek)
    }

    @Test
    fun saveNewMedication_savesSchedules() = runTest {
        viewModel.loadMedication(null)
        viewModel.onTitleChanged("Ibuprofen")
        viewModel.onAddSchedule()
        viewModel.onScheduleTimeChanged(index = 0, hour = 12, minute = 30)
        viewModel.onScheduleDayToggled(index = 0, day = DayOfWeek.WEDNESDAY)
        viewModel.saveMedication(onSuccess = {})

        val all = fakeDao.getAll().first()
        assertEquals(1, all.size)
        val medId = all[0].id

        val retrieved = fakeDao.getMedicationWithSchedules(medId)
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.schedules?.size)
        assertEquals(12, retrieved?.schedules?.first()?.hour)
        assertEquals(30, retrieved?.schedules?.first()?.minute)
        assertEquals(DayOfWeek.values().toSet() - DayOfWeek.WEDNESDAY, retrieved?.schedules?.first()?.daysOfWeek)
    }

    @Test
    fun onScheduleDosesChanged_updatesDosesCorrectly() = runTest {
        viewModel.loadMedication(null)
        viewModel.onAddSchedule()
        viewModel.onScheduleDosesChanged(index = 0, doses = 2.5)

        val schedules = viewModel.addEditUiState.value.schedules
        assertEquals(2.5, schedules[0].doses, 0.0)
    }

    @Test
    fun loadMedication_withMedicationId_loadsSchedulesWithCustomDoses() = runTest {
        val medication = Medication(id = 42L, title = "Aspirin")
        val schedules = listOf(
            MedicationSchedule(id = 1L, medicationId = 42L, hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.MONDAY), doses = 1.5)
        )
        fakeDao.saveMedicationWithSchedules(medication, schedules)

        viewModel.loadMedication(42L)
        val state = viewModel.addEditUiState.value
        assertEquals("Aspirin", state.title)
        assertEquals(1, state.schedules.size)
        assertEquals(8, state.schedules[0].hour)
        assertEquals(setOf(DayOfWeek.MONDAY), state.schedules[0].daysOfWeek)
        assertEquals(1.5, state.schedules[0].doses, 0.0)
    }

    @Test
    fun saveNewMedication_savesSchedulesWithCustomDoses() = runTest {
        viewModel.loadMedication(null)
        viewModel.onTitleChanged("Ibuprofen")
        viewModel.onAddSchedule()
        viewModel.onScheduleTimeChanged(index = 0, hour = 12, minute = 30)
        viewModel.onScheduleDosesChanged(index = 0, doses = 3.0)
        viewModel.saveMedication(onSuccess = {})

        val all = fakeDao.getAll().first()
        assertEquals(1, all.size)
        val medId = all[0].id

        val retrieved = fakeDao.getMedicationWithSchedules(medId)
        assertNotNull(retrieved)
        assertEquals(1, retrieved?.schedules?.size)
        assertEquals(12, retrieved?.schedules?.first()?.hour)
        assertEquals(30, retrieved?.schedules?.first()?.minute)
        assertEquals(3.0, retrieved?.schedules?.first()?.doses ?: 0.0, 0.0)
    }

    @Test
    fun todayScheduleGroups_filtersByDayOfWeek() = runTest {
        viewModel.setCurrentDate(LocalDate.of(2026, 5, 20)) // Wednesday
        
        val med1 = Medication(id = 1L, title = "Aspirin")
        val schedules1 = listOf(
            MedicationSchedule(id = 1L, medicationId = 1L, hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.WEDNESDAY), doses = 1.0)
        )
        fakeDao.saveMedicationWithSchedules(med1, schedules1)
        
        val med2 = Medication(id = 2L, title = "Ibuprofen")
        val schedules2 = listOf(
            MedicationSchedule(id = 2L, medicationId = 2L, hour = 12, minute = 0, daysOfWeek = setOf(DayOfWeek.SUNDAY), doses = 2.0)
        )
        fakeDao.saveMedicationWithSchedules(med2, schedules2)

        val groups = viewModel.todayScheduleGroups.first()
        assertEquals(1, groups.size)
        assertEquals(8, groups[0].hour)
        assertEquals(0, groups[0].minute)
        assertEquals(1, groups[0].medications.size)
        assertEquals("Aspirin", groups[0].medications[0].title)
    }

    @Test
    fun todayScheduleGroups_groupsByTimeAndSorts() = runTest {
        viewModel.setCurrentDate(LocalDate.of(2026, 5, 20)) // Wednesday
        
        val med1 = Medication(id = 1L, title = "Aspirin")
        val schedules1 = listOf(
            MedicationSchedule(id = 1L, medicationId = 1L, hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.WEDNESDAY), doses = 1.0),
            MedicationSchedule(id = 2L, medicationId = 1L, hour = 20, minute = 0, daysOfWeek = setOf(DayOfWeek.WEDNESDAY), doses = 1.5)
        )
        fakeDao.saveMedicationWithSchedules(med1, schedules1)
        
        val med2 = Medication(id = 2L, title = "Ibuprofen")
        val schedules2 = listOf(
            MedicationSchedule(id = 3L, medicationId = 2L, hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.WEDNESDAY), doses = 2.0)
        )
        fakeDao.saveMedicationWithSchedules(med2, schedules2)

        val groups = viewModel.todayScheduleGroups.first()
        assertEquals(2, groups.size)
        
        // First group should be 08:00
        assertEquals(8, groups[0].hour)
        assertEquals(0, groups[0].minute)
        assertEquals(2, groups[0].medications.size)
        
        // Second group should be 20:00
        assertEquals(20, groups[1].hour)
        assertEquals(0, groups[1].minute)
        assertEquals(1, groups[1].medications.size)
    }

    @Test
    fun toggleTakeStatus_updatesTakenState() = runTest {
        viewModel.setCurrentDate(LocalDate.of(2026, 5, 20)) // Wednesday
        
        val med = Medication(id = 1L, title = "Aspirin")
        val schedules = listOf(
            MedicationSchedule(id = 1L, medicationId = 1L, hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.WEDNESDAY), doses = 1.0)
        )
        fakeDao.saveMedicationWithSchedules(med, schedules)

        // Initially not taken
        assertEquals(false, viewModel.todayScheduleGroups.first()[0].isTaken)

        // Toggle to taken
        viewModel.toggleTakeStatus(8, 0, isCurrentlyTaken = false)
        assertEquals(true, viewModel.todayScheduleGroups.first()[0].isTaken)

        // Toggle back to not taken
        viewModel.toggleTakeStatus(8, 0, isCurrentlyTaken = true)
        assertEquals(false, viewModel.todayScheduleGroups.first()[0].isTaken)
    }
}

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
        // Not directly called by repo under fake mapping, but required to implement MedicationDao
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

    override suspend fun saveMedicationWithSchedules(medication: Medication, schedules: List<MedicationSchedule>) {
        val id = insert(medication)
        val targetId = if (medication.id == 0L) id else medication.id
        schedulesMap[targetId] = schedules.map { it.copy(medicationId = targetId) }
        updateAllWithSchedules()
    }
}
