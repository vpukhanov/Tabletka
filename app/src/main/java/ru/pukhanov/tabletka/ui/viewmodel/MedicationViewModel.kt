package ru.pukhanov.tabletka.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import ru.pukhanov.tabletka.ui.screens.Screen

data class AddEditUiState(
    val title: String = "",
    val brandName: String = "",
    val dosage: String = "",
    val titleError: String? = null,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false
)

class MedicationViewModel(private val repository: MedicationRepository) : ViewModel() {

    private val _currentScreen = MutableStateFlow<Screen>(Screen.List)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    val medications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _addEditUiState = MutableStateFlow(AddEditUiState())
    val addEditUiState: StateFlow<AddEditUiState> = _addEditUiState.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen is Screen.AddEdit) {
            loadMedication(screen.medicationId)
        }
    }

    fun onTitleChanged(newTitle: String) {
        _addEditUiState.update { it.copy(title = newTitle, titleError = null) }
    }

    fun onBrandNameChanged(newBrandName: String) {
        _addEditUiState.update { it.copy(brandName = newBrandName) }
    }

    fun onDosageChanged(newDosage: String) {
        _addEditUiState.update { it.copy(dosage = newDosage) }
    }

    fun loadMedication(id: Long?) {
        if (id == null) {
            _addEditUiState.value = AddEditUiState()
        } else {
            _addEditUiState.value = AddEditUiState(isEditMode = true)
            viewModelScope.launch {
                val medication = repository.getById(id)
                if (medication != null) {
                    _addEditUiState.update {
                        it.copy(
                            title = medication.title,
                            brandName = medication.brandName ?: "",
                            dosage = medication.dosage ?: "",
                            isEditMode = true
                        )
                    }
                }
            }
        }
    }

    fun saveMedication() {
        val currentState = _addEditUiState.value
        if (currentState.title.isBlank()) {
            _addEditUiState.update { it.copy(titleError = "Title cannot be empty") }
            return
        }

        _addEditUiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val currentScreenState = _currentScreen.value
            val id = if (currentScreenState is Screen.AddEdit) currentScreenState.medicationId ?: 0L else 0L

            val medication = Medication(
                id = id,
                title = currentState.title.trim(),
                brandName = currentState.brandName.trim().ifBlank { null },
                dosage = currentState.dosage.trim().ifBlank { null }
            )
            repository.insert(medication)
            _addEditUiState.update { it.copy(isSaving = false) }
            _currentScreen.value = Screen.List
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.delete(medication)
            val currentScreenState = _currentScreen.value
            if (currentScreenState is Screen.AddEdit && currentScreenState.medicationId == medication.id) {
                _currentScreen.value = Screen.List
            }
        }
    }
}
