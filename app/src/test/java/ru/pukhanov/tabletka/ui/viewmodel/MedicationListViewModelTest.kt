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
import ru.pukhanov.tabletka.data.repository.MedicationRepository

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private lateinit var fakeDao: FakeMedicationDao
    private lateinit var repository: MedicationRepository
    private lateinit var viewModel: MedicationListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeMedicationDao()
        repository = MedicationRepository(fakeDao, testDispatcher)
        viewModel = MedicationListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isCorrect() = runTest {
        assertEquals(emptyList<Medication>(), viewModel.medications.value)
    }

    @Test
    fun deleteMedication_success() = runTest {
        val medication = Medication(id = 5L, title = "To Delete")
        fakeDao.insert(medication)

        viewModel.deleteMedication(medication)

        val list = fakeDao.getAll().first()
        assertEquals(0, list.size)
    }
}
