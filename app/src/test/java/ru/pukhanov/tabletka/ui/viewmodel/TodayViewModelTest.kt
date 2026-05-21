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
import org.junit.Before
import org.junit.Test
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeDao: FakeMedicationDao
    private lateinit var repository: MedicationRepository
    private lateinit var viewModel: TodayViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeMedicationDao()
        repository = MedicationRepository(fakeDao, testDispatcher)
        viewModel = TodayViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isCorrect() = runTest {
        assertEquals(emptyList<Medication>(), viewModel.medications.value)
        assertEquals(emptyList<TodayScheduleGroup>(), viewModel.todayScheduleGroups.value)
        assertEquals(LocalDate.now().toString(), viewModel.currentDate.value)
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

    @Test
    fun todayScheduleGroups_includesBrandName() = runTest {
        viewModel.setCurrentDate(LocalDate.of(2026, 5, 20)) // Wednesday
        
        val med = Medication(id = 1L, title = "Acetylsalicylic Acid", brandName = "Aspirin")
        val schedules = listOf(
            MedicationSchedule(id = 1L, medicationId = 1L, hour = 8, minute = 0, daysOfWeek = setOf(DayOfWeek.WEDNESDAY), doses = 1.5)
        )
        fakeDao.saveMedicationWithSchedules(med, schedules)

        val groups = viewModel.todayScheduleGroups.first()
        assertEquals(1, groups.size)
        assertEquals("Acetylsalicylic Acid", groups[0].medications[0].title)
        assertEquals("Aspirin", groups[0].medications[0].brandName)
        assertEquals(1.5, groups[0].medications[0].doses, 0.0)
    }
}
