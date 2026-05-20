package ru.pukhanov.tabletka.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.pukhanov.tabletka.ui.viewmodel.MedicationViewModel
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp

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

    val showBottomBar = currentRoute in listOf("today", "medications")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                TabletkaBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigate
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("add_edit?medicationId=-1") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    expanded = true,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    text = {
                        Text(text = "Add Medication")
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(
                route = "today",
                enterTransition = {
                    if (initialState.destination.route == "medications") {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        )
                    } else {
                        null
                    }
                },
                exitTransition = {
                    if (targetState.destination.route == "medications") {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        )
                    } else {
                        null
                    }
                }
            ) {
                TodayScreen(
                    groups = todayGroups,
                    onToggleTakeStatus = { hour, minute, isTaken ->
                        viewModel.toggleTakeStatus(hour, minute, isTaken)
                    },
                    onAddMedicationClick = { navController.navigate("add_edit?medicationId=-1") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
            composable(
                route = "medications",
                enterTransition = {
                    if (initialState.destination.route == "today") {
                        slideIntoContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        )
                    } else {
                        null
                    }
                },
                exitTransition = {
                    if (targetState.destination.route == "today") {
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        )
                    } else {
                        null
                    }
                }
            ) {
                MedicationListScreen(
                    medications = medications,
                    onAddClick = { navController.navigate("add_edit?medicationId=-1") },
                    onMedicationClick = { id -> navController.navigate("add_edit?medicationId=$id") },
                    onDeleteMedication = { medication -> viewModel.deleteMedication(medication) }
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
            composable(route = "settings") {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
