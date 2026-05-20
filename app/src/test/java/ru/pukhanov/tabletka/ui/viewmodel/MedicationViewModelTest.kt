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
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import ru.pukhanov.tabletka.ui.screens.Screen

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
        assertEquals(Screen.List, viewModel.currentScreen.value)
        assertEquals(emptyList<Medication>(), viewModel.medications.value)
        assertEquals(AddEditUiState(), viewModel.addEditUiState.value)
    }

    @Test
    fun navigateTo_updatesCurrentScreen() = runTest {
        viewModel.navigateTo(Screen.AddEdit(null))
        assertEquals(Screen.AddEdit(null), viewModel.currentScreen.value)

        viewModel.navigateTo(Screen.List)
        assertEquals(Screen.List, viewModel.currentScreen.value)
    }

    @Test
    fun navigateTo_withMedicationId_loadsMedicationDetails() = runTest {
        val medication = Medication(id = 42L, title = "Aspirin", brandName = "Bayer")
        fakeDao.insert(medication)

        viewModel.navigateTo(Screen.AddEdit(42L))
        assertEquals(Screen.AddEdit(42L), viewModel.currentScreen.value)
        assertEquals("Aspirin", viewModel.addEditUiState.value.title)
        assertEquals("Bayer", viewModel.addEditUiState.value.brandName)
        assertEquals(true, viewModel.addEditUiState.value.isEditMode)
    }

    @Test
    fun saveMedication_emptyTitle_setsError() = runTest {
        viewModel.navigateTo(Screen.AddEdit(null))
        viewModel.onTitleChanged("")
        viewModel.saveMedication()

        assertEquals("Title cannot be empty", viewModel.addEditUiState.value.titleError)
        assertEquals(0, fakeDao.getAll().first().size)
    }

    @Test
    fun saveNewMedication_success() = runTest {
        viewModel.navigateTo(Screen.AddEdit(null))
        viewModel.onTitleChanged("Paracetamol")
        viewModel.onBrandNameChanged("Panadol")
        viewModel.saveMedication()

        val list = fakeDao.getAll().first()
        assertEquals(1, list.size)
        assertEquals("Paracetamol", list[0].title)
        assertEquals("Panadol", list[0].brandName)
        
        // Navigation should go back to list
        assertEquals(Screen.List, viewModel.currentScreen.value)
    }

    @Test
    fun saveExistingMedication_success() = runTest {
        val original = Medication(id = 10L, title = "Ibuprofen", brandName = "Nurofen")
        fakeDao.insert(original)

        viewModel.navigateTo(Screen.AddEdit(10L))
        viewModel.onTitleChanged("Ibuprofen Forte")
        viewModel.onBrandNameChanged("Advil")
        viewModel.saveMedication()

        val list = fakeDao.getAll().first()
        assertEquals(1, list.size)
        assertEquals(10L, list[0].id)
        assertEquals("Ibuprofen Forte", list[0].title)
        assertEquals("Advil", list[0].brandName)
        
        // Navigation should go back to list
        assertEquals(Screen.List, viewModel.currentScreen.value)
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

class FakeMedicationDao : MedicationDao {
    private val medicationsMap = mutableMapOf<Long, Medication>()
    private val _flow = MutableStateFlow<List<Medication>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(medication: Medication): Long {
        val id = if (medication.id == 0L) nextId++ else medication.id
        val saved = medication.copy(id = id)
        medicationsMap[id] = saved
        _flow.value = medicationsMap.values.toList().reversed()
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
        _flow.value = medicationsMap.values.toList().reversed()
    }
}
