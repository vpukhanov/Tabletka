package ru.pukhanov.tabletka.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.util.UUID
import javax.inject.Inject
import kotlin.math.absoluteValue

data class ScheduleUiState(
    val id: Long = -UUID.randomUUID().mostSignificantBits.absoluteValue,
    val hour: Int = 8,
    val minute: Int = 0,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val doses: Double = 1.0
)

data class AddEditUiState(
    val medicationId: Long? = null,
    val title: String = "",
    val brandName: String = "",
    val dosage: String = "",
    val schedules: List<ScheduleUiState> = emptyList(),
    val titleError: String? = null,
    val isSaving: Boolean = false,
    val isEditMode: Boolean = false,
    val hasChanges: Boolean = false
)

@HiltViewModel
class AddEditMedicationViewModel @Inject constructor(private val repository: MedicationRepository) : ViewModel() {

    private val _addEditUiState = MutableStateFlow(AddEditUiState())
    val addEditUiState: StateFlow<AddEditUiState> = _addEditUiState.asStateFlow()

    private var originalState: AddEditUiState = AddEditUiState()

    private fun checkHasChanges(state: AddEditUiState): Boolean {
        return state.title != originalState.title ||
                state.brandName != originalState.brandName ||
                state.dosage != originalState.dosage ||
                state.schedules != originalState.schedules
    }

    private fun updateState(update: (AddEditUiState) -> AddEditUiState) {
        _addEditUiState.update { current ->
            val next = update(current)
            next.copy(hasChanges = checkHasChanges(next))
        }
    }

    fun onTitleChanged(newTitle: String) {
        updateState { it.copy(title = newTitle, titleError = null) }
    }

    fun onBrandNameChanged(newBrandName: String) {
        updateState { it.copy(brandName = newBrandName) }
    }

    fun onDosageChanged(newDosage: String) {
        updateState { it.copy(dosage = newDosage) }
    }

    fun loadMedication(id: Long?) {
        if (id == null) {
            val initialState = AddEditUiState()
            originalState = initialState
            _addEditUiState.value = initialState
        } else {
            val loadingState = AddEditUiState(medicationId = id, isEditMode = true)
            originalState = loadingState
            _addEditUiState.value = loadingState
            viewModelScope.launch {
                val medicationWithSchedules = repository.getWithSchedules(id)
                if (medicationWithSchedules != null) {
                    val medication = medicationWithSchedules.medication
                    val loadedState = AddEditUiState(
                        medicationId = id,
                        title = medication.title,
                        brandName = medication.brandName ?: "",
                        dosage = medication.dosage ?: "",
                        schedules = medicationWithSchedules.schedules.map { schedule ->
                            ScheduleUiState(
                                id = schedule.id,
                                hour = schedule.hour,
                                minute = schedule.minute,
                                daysOfWeek = schedule.daysOfWeek,
                                doses = schedule.doses
                            )
                        },
                        isEditMode = true
                    )
                    originalState = loadedState
                    _addEditUiState.value = loadedState
                }
            }
        }
    }

    fun saveMedication(onSuccess: () -> Unit) {
        val currentState = _addEditUiState.value
        if (currentState.title.isBlank()) {
            updateState { it.copy(titleError = "Title cannot be empty") }
            return
        }

        updateState { it.copy(isSaving = true) }
        viewModelScope.launch {
            val id = currentState.medicationId ?: 0L

            val medication = Medication(
                id = id,
                title = currentState.title.trim(),
                brandName = currentState.brandName.trim().ifBlank { null },
                dosage = currentState.dosage.trim().ifBlank { null }
            )
            val schedules = currentState.schedules.map { scheduleState ->
                MedicationSchedule(
                    id = if (scheduleState.id < 0L) 0L else scheduleState.id,
                    medicationId = id,
                    hour = scheduleState.hour,
                    minute = scheduleState.minute,
                    daysOfWeek = scheduleState.daysOfWeek,
                    doses = scheduleState.doses
                )
            }
            repository.save(medication, schedules)
            updateState { it.copy(isSaving = false) }
            onSuccess()
        }
    }

    fun onAddSchedule() {
        updateState { state ->
            state.copy(
                schedules = state.schedules + ScheduleUiState()
            )
        }
    }

    fun onDeleteSchedule(index: Int) {
        updateState { state ->
            val updated = state.schedules.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            state.copy(schedules = updated)
        }
    }

    fun onScheduleTimeChanged(index: Int, hour: Int, minute: Int) {
        updateState { state ->
            val updated = state.schedules.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(hour = hour, minute = minute)
            }
            state.copy(schedules = updated)
        }
    }

    fun onScheduleDayToggled(index: Int, day: DayOfWeek) {
        updateState { state ->
            val updated = state.schedules.toMutableList()
            if (index in updated.indices) {
                val currentDays = updated[index].daysOfWeek
                val newDays = if (currentDays.contains(day)) {
                    currentDays - day
                } else {
                    currentDays + day
                }
                updated[index] = updated[index].copy(daysOfWeek = newDays)
            }
            state.copy(schedules = updated)
        }
    }

    fun onScheduleDosesChanged(index: Int, doses: Double) {
        updateState { state ->
            val updated = state.schedules.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(doses = doses)
            }
            state.copy(schedules = updated)
        }
    }
}
