package ru.pukhanov.tabletka.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import javax.inject.Inject

@HiltViewModel
class MedicationListViewModel @Inject constructor(private val repository: MedicationRepository) : ViewModel() {

    val medications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.delete(medication)
        }
    }
}
