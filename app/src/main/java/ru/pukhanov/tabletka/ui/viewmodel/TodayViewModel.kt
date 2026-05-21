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
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.data.model.MedicationTake
import ru.pukhanov.tabletka.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

data class TodayMedicationItem(
    val title: String,
    val dosage: String?,
    val doses: Double,
    val brandName: String? = null
)

data class TodayScheduleGroup(
    val hour: Int,
    val minute: Int,
    val medications: List<TodayMedicationItem>,
    val isTaken: Boolean
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel @Inject constructor(private val repository: MedicationRepository) : ViewModel() {

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
                            doses = schedule.doses,
                            brandName = medication.brandName
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
}
