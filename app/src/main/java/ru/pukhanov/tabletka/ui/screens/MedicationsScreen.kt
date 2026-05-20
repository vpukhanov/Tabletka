package ru.pukhanov.tabletka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.pukhanov.tabletka.data.model.Medication
import ru.pukhanov.tabletka.ui.viewmodel.AddEditUiState
import java.time.DayOfWeek

private const val NEW_MEDICATION_KEY: Long = -1L

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(
    medications: List<Medication>,
    onDeleteMedication: (Medication) -> Unit,
    addEditState: AddEditUiState,
    onLoadMedication: (Long?) -> Unit,
    onTitleChange: (String) -> Unit,
    onBrandNameChange: (String) -> Unit,
    onDosageChange: (String) -> Unit,
    onAddSchedule: () -> Unit,
    onDeleteSchedule: (Int) -> Unit,
    onScheduleTimeChange: (Int, Int, Int) -> Unit,
    onScheduleDayToggle: (Int, DayOfWeek) -> Unit,
    onScheduleDosesChange: (Int, Double) -> Unit,
    onSaveMedication: ((Long) -> Unit) -> Unit,
    openNew: Boolean,
    onOpenNewConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val scope = rememberCoroutineScope()

    val isListExpanded = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Expanded
    val isDetailExpanded = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Expanded
    val isTwoPaneVisible = isListExpanded && isDetailExpanded

    val currentKey = navigator.currentDestination?.contentKey
    val selectedId = currentKey?.takeIf { it != NEW_MEDICATION_KEY }

    var pendingNavigation by remember { mutableStateOf<Long?>(null) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }
    var justSavedId by remember { mutableStateOf<Long?>(null) }

    val performNavigate: (Long) -> Unit = { targetKey ->
        scope.launch {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, targetKey)
        }
    }

    val attemptNavigate: (Long) -> Unit = { targetKey ->
        val shouldConfirmDiscard = isTwoPaneVisible &&
            currentKey != null &&
            currentKey != targetKey &&
            addEditState.hasChanges
        if (shouldConfirmDiscard) {
            pendingNavigation = targetKey
            showDiscardConfirmation = true
        } else if (targetKey != currentKey) {
            performNavigate(targetKey)
        }
    }

    LaunchedEffect(openNew) {
        if (openNew) {
            onOpenNewConsumed()
            performNavigate(NEW_MEDICATION_KEY)
        }
    }

    LaunchedEffect(currentKey) {
        val key = currentKey ?: return@LaunchedEffect
        if (key == justSavedId) {
            justSavedId = null
            return@LaunchedEffect
        }
        onLoadMedication(if (key == NEW_MEDICATION_KEY) null else key)
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                MedicationListScreen(
                    medications = medications,
                    onAddClick = { attemptNavigate(NEW_MEDICATION_KEY) },
                    onMedicationClick = { id -> attemptNavigate(id) },
                    onDeleteMedication = { medication ->
                        if (selectedId == medication.id) {
                            scope.launch { navigator.navigateBack() }
                        }
                        onDeleteMedication(medication)
                    },
                    selectedId = if (isTwoPaneVisible) selectedId else null
                )
            }
        },
        detailPane = {
            AnimatedPane {
                if (currentKey != null) {
                    AddEditMedicationScreen(
                        state = addEditState,
                        showBackButton = !isTwoPaneVisible,
                        onTitleChange = onTitleChange,
                        onBrandNameChange = onBrandNameChange,
                        onDosageChange = onDosageChange,
                        onAddSchedule = onAddSchedule,
                        onDeleteSchedule = onDeleteSchedule,
                        onScheduleTimeChange = onScheduleTimeChange,
                        onScheduleDayToggle = onScheduleDayToggle,
                        onScheduleDosesChange = onScheduleDosesChange,
                        onSaveClick = {
                            val keyBeforeSave = currentKey
                            onSaveMedication { savedId ->
                                when {
                                    !isTwoPaneVisible -> {
                                        scope.launch { navigator.navigateBack() }
                                    }
                                    keyBeforeSave == NEW_MEDICATION_KEY -> {
                                        justSavedId = savedId
                                        scope.launch {
                                            navigator.navigateTo(
                                                ListDetailPaneScaffoldRole.Detail,
                                                savedId
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onBackClick = {
                            scope.launch { navigator.navigateBack() }
                        }
                    )
                } else {
                    EmptyDetailPane()
                }
            }
        },
        modifier = modifier
    )

    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showDiscardConfirmation = false
                pendingNavigation = null
            },
            title = {
                Text(
                    text = "Discard changes?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(text = "You have unsaved changes. Are you sure you want to discard them?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingNavigation
                        showDiscardConfirmation = false
                        pendingNavigation = null
                        if (target != null) {
                            performNavigate(target)
                        }
                    }
                ) {
                    Text(
                        text = "Discard",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        pendingNavigation = null
                    }
                ) {
                    Text("Keep Editing")
                }
            }
        )
    }
}

@Composable
private fun EmptyDetailPane(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Choose a medication from the list\nto view or edit.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(24.dp)
        )
    }
}
