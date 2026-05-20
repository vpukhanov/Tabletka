package ru.pukhanov.tabletka.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationSchedule
import ru.pukhanov.tabletka.data.model.MedicationWithSchedules
import ru.pukhanov.tabletka.data.model.MedicationTake
import ru.pukhanov.tabletka.data.repository.MedicationRepository

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import kotlin.math.absoluteValue

data class ScheduleUiState(
    val id: Long = -UUID.randomUUID().mostSignificantBits.absoluteValue,
    val hour: Int = 8,
    val minute: Int = 0,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.values().toSet(),
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
    val isEditMode: Boolean = false
)

data class TodayMedicationItem(
    val title: String,
    val dosage: String?,
    val doses: Double
)

data class TodayScheduleGroup(
    val hour: Int,
    val minute: Int,
    val medications: List<TodayMedicationItem>,
    val isTaken: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationViewModel(private val repository: MedicationRepository) : ViewModel() {



    private val _currentDate = MutableStateFlow(LocalDate.now().toString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    val medications: StateFlow<List<Medication>> = repository.allMedications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayScheduleGroups: StateFlow<List<TodayScheduleGroup>> = combine(
        repository.allMedicationsWithSchedules,
        _currentDate.flatMapLatest { date -> repository.getTakesForDate(date) }
    ) { medicationsWithSchedules, takes ->
        val date = LocalDate.parse(_currentDate.value)
        val dayOfWeek = date.dayOfWeek
        val takenTimes = takes.map { it.hour to it.minute }.toSet()

        val scheduledItems = medicationsWithSchedules.flatMap { medWithSchedules ->
            medWithSchedules.schedules
                .filter { it.daysOfWeek.contains(dayOfWeek) }
                .map { schedule ->
                    Pair(medWithSchedules.medication, schedule)
                }
        }

        scheduledItems.groupBy { it.second.hour to it.second.minute }
            .map { (time, items) ->
                val (hour, minute) = time
                val isTaken = takenTimes.contains(hour to minute)
                TodayScheduleGroup(
                    hour = hour,
                    minute = minute,
                    medications = items.map { (medication, schedule) ->
                        TodayMedicationItem(
                            title = medication.title,
                            dosage = medication.dosage,
                            doses = schedule.doses
                        )
                    },
                    isTaken = isTaken
                )
            }
            .sortedWith(compareBy({ it.hour }, { it.minute }))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleTakeStatus(hour: Int, minute: Int, isCurrentlyTaken: Boolean) {
        viewModelScope.launch {
            val dateStr = _currentDate.value
            if (isCurrentlyTaken) {
                repository.deleteTake(dateStr, hour, minute)
            } else {
                repository.insertTake(MedicationTake(date = dateStr, hour = hour, minute = minute))
            }
        }
    }

    fun setCurrentDate(date: LocalDate) {
        _currentDate.value = date.toString()
    }

    private val _addEditUiState = MutableStateFlow(AddEditUiState())
    val addEditUiState: StateFlow<AddEditUiState> = _addEditUiState.asStateFlow()

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
            _addEditUiState.value = AddEditUiState(medicationId = id, isEditMode = true)
            viewModelScope.launch {
                val medicationWithSchedules = repository.getWithSchedules(id)
                if (medicationWithSchedules != null) {
                    val medication = medicationWithSchedules.medication
                    _addEditUiState.update {
                        it.copy(
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
                    }
                }
            }
        }
    }

    fun saveMedication(onSuccess: () -> Unit) {
        val currentState = _addEditUiState.value
        if (currentState.title.isBlank()) {
            _addEditUiState.update { it.copy(titleError = "Title cannot be empty") }
            return
        }

        _addEditUiState.update { it.copy(isSaving = true) }
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
            _addEditUiState.update { it.copy(isSaving = false) }
            onSuccess()
        }
    }

    fun onAddSchedule() {
        _addEditUiState.update { state ->
            state.copy(
                schedules = state.schedules + ScheduleUiState()
            )
        }
    }

    fun onDeleteSchedule(index: Int) {
        _addEditUiState.update { state ->
            val updated = state.schedules.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            state.copy(schedules = updated)
        }
    }

    fun onScheduleTimeChanged(index: Int, hour: Int, minute: Int) {
        _addEditUiState.update { state ->
            val updated = state.schedules.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(hour = hour, minute = minute)
            }
            state.copy(schedules = updated)
        }
    }

    fun onScheduleDayToggled(index: Int, day: DayOfWeek) {
        _addEditUiState.update { state ->
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
        _addEditUiState.update { state ->
            val updated = state.schedules.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(doses = doses)
            }
            state.copy(schedules = updated)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.delete(medication)
        }
    }
}
