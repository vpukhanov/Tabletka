package ru.pukhanov.tabletka.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.pukhanov.tabletka.ui.viewmodel.MedicationViewModel

@Composable
fun MedicationApp(
    viewModel: MedicationViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val medications by viewModel.medications.collectAsState()
    val addEditState by viewModel.addEditUiState.collectAsState()
    val todayGroups by viewModel.todayScheduleGroups.collectAsState()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val onNavigate: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = "today",
        modifier = modifier.fillMaxSize()
    ) {
        composable("today") {
            TodayScreen(
                groups = todayGroups,
                onToggleTakeStatus = { hour, minute, isTaken ->
                    viewModel.toggleTakeStatus(hour, minute, isTaken)
                },
                onAddMedicationClick = { navController.navigate("add_edit?medicationId=-1") },
                onNavigate = onNavigate,
                currentRoute = currentRoute
            )
        }
        composable("medications") {
            MedicationListScreen(
                medications = medications,
                onAddClick = { navController.navigate("add_edit?medicationId=-1") },
                onMedicationClick = { id -> navController.navigate("add_edit?medicationId=$id") },
                onDeleteMedication = { medication -> viewModel.deleteMedication(medication) },
                onNavigate = onNavigate,
                currentRoute = currentRoute
            )
        }
        composable(
            route = "add_edit?medicationId={medicationId}",
            arguments = listOf(
                navArgument("medicationId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val medicationIdArg = backStackEntry.arguments?.getLong("medicationId")
            val medicationId = if (medicationIdArg == -1L || medicationIdArg == null) null else medicationIdArg

            LaunchedEffect(medicationId) {
                viewModel.loadMedication(medicationId)
            }

            AddEditMedicationScreen(
                state = addEditState,
                onTitleChange = { viewModel.onTitleChanged(it) },
                onBrandNameChange = { viewModel.onBrandNameChanged(it) },
                onDosageChange = { viewModel.onDosageChanged(it) },
                onAddSchedule = { viewModel.onAddSchedule() },
                onDeleteSchedule = { viewModel.onDeleteSchedule(it) },
                onScheduleTimeChange = { index, hour, minute ->
                    viewModel.onScheduleTimeChanged(index, hour, minute)
                },
                onScheduleDayToggle = { index, day ->
                    viewModel.onScheduleDayToggled(index, day)
                },
                onScheduleDosesChange = { index, doses ->
                    viewModel.onScheduleDosesChanged(index, doses)
                },
                onSaveClick = {
                    viewModel.saveMedication(
                        onSuccess = { navController.popBackStack() }
                    )
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
