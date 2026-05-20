package ru.pukhanov.tabletka.ui.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import java.time.DayOfWeek

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditMedicationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeDao: FakeMedicationDao
    private lateinit var repository: MedicationRepository
    private lateinit var viewModel: AddEditMedicationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeMedicationDao()
        repository = MedicationRepository(fakeDao, testDispatcher)
        viewModel = AddEditMedicationViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isCorrect() = runTest {
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
    fun onAddSchedule_appendsDefaultSchedule() = runTest {
        viewModel.loadMedication(null)
        viewModel.onAddSchedule()

        val schedules = viewModel.addEditUiState.value.schedules
        assertEquals(1, schedules.size)
        assertEquals(8, schedules[0].hour)
        assertEquals(0, schedules[0].minute)
        assertEquals(DayOfWeek.entries.toSet(), schedules[0].daysOfWeek)
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
        assertEquals(DayOfWeek.entries.toSet() - DayOfWeek.MONDAY, viewModel.addEditUiState.value.schedules[0].daysOfWeek)

        // Toggle Monday back on
        viewModel.onScheduleDayToggled(index = 0, day = DayOfWeek.MONDAY)
        assertEquals(DayOfWeek.entries.toSet(), viewModel.addEditUiState.value.schedules[0].daysOfWeek)
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
        assertEquals(DayOfWeek.entries.toSet() - DayOfWeek.WEDNESDAY, retrieved?.schedules?.first()?.daysOfWeek)
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
    fun hasChanges_initiallyFalse() = runTest {
        viewModel.loadMedication(null)
        assertEquals(false, viewModel.addEditUiState.value.hasChanges)
    }

    @Test
    fun hasChanges_becomesTrueOnFieldChange() = runTest {
        viewModel.loadMedication(null)
        viewModel.onTitleChanged("Aspirin")
        assertEquals(true, viewModel.addEditUiState.value.hasChanges)

        // Reverting it back should set hasChanges back to false
        viewModel.onTitleChanged("")
        assertEquals(false, viewModel.addEditUiState.value.hasChanges)
    }

    @Test
    fun hasChanges_becomesTrueOnScheduleAdd() = runTest {
        viewModel.loadMedication(null)
        viewModel.onAddSchedule()
        assertEquals(true, viewModel.addEditUiState.value.hasChanges)

        // Deleting it should make it false again
        viewModel.onDeleteSchedule(0)
        assertEquals(false, viewModel.addEditUiState.value.hasChanges)
    }

    @Test
    fun hasChanges_existingMedication_startsFalseAndTracksChanges() = runTest {
        val medication = Medication(id = 42L, title = "Aspirin", brandName = "Bayer", dosage = "500mg")
        fakeDao.insert(medication)

        viewModel.loadMedication(42L)
        assertEquals(false, viewModel.addEditUiState.value.hasChanges)

        viewModel.onDosageChanged("1000mg")
        assertEquals(true, viewModel.addEditUiState.value.hasChanges)

        // Reverting it back to "500mg" should make hasChanges false
        viewModel.onDosageChanged("500mg")
        assertEquals(false, viewModel.addEditUiState.value.hasChanges)
    }
}
