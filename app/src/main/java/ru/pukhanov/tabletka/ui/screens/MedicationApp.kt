package ru.pukhanov.tabletka.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pukhanov.tabletka.ui.viewmodel.MedicationViewModel

@Composable
fun MedicationApp(
    viewModel: MedicationViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val addEditState by viewModel.addEditUiState.collectAsState()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn().togetherWith(fadeOut())
        },
        label = "ScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            is Screen.List -> {
                MedicationListScreen(
                    medications = medications,
                    onAddClick = { viewModel.navigateTo(Screen.AddEdit(null)) },
                    onMedicationClick = { id -> viewModel.navigateTo(Screen.AddEdit(id)) },
                    onDeleteMedication = { medication -> viewModel.deleteMedication(medication) }
                )
            }
            is Screen.AddEdit -> {
                AddEditMedicationScreen(
                    state = addEditState,
                    onTitleChange = { viewModel.onTitleChanged(it) },
                    onBrandNameChange = { viewModel.onBrandNameChanged(it) },
                    onDosageChange = { viewModel.onDosageChanged(it) },
                    onSaveClick = { viewModel.saveMedication() },
                    onBackClick = { viewModel.navigateTo(Screen.List) }
                )
            }
        }
    }
}
