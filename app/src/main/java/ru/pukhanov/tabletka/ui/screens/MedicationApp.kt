package ru.pukhanov.tabletka.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material3.Scaffold
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ru.pukhanov.tabletka.ui.viewmodel.TodayViewModel
import ru.pukhanov.tabletka.ui.viewmodel.MedicationListViewModel
import ru.pukhanov.tabletka.ui.viewmodel.AddEditMedicationViewModel
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import java.time.LocalDate

@Composable
fun MedicationApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

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
                val viewModel: TodayViewModel = hiltViewModel()
                val todayGroups by viewModel.todayScheduleGroups.collectAsState()
                val medications by viewModel.medications.collectAsState()

                val context = LocalContext.current
                DisposableEffect(context, viewModel) {
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_DATE_CHANGED)
                        addAction(Intent.ACTION_TIMEZONE_CHANGED)
                    }
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            viewModel.setCurrentDate(LocalDate.now())
                        }
                    }
                    context.registerReceiver(receiver, filter)
                    onDispose {
                        context.unregisterReceiver(receiver)
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, viewModel) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_START) {
                            viewModel.setCurrentDate(LocalDate.now())
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                TodayScreen(
                    groups = todayGroups,
                    showAddButton = medications.isEmpty(),
                    onToggleTakeStatus = { hour, minute, isTaken ->
                        viewModel.toggleTakeStatus(hour, minute, isTaken)
                    },
                    onAddMedicationClick = {
                        navController.navigate("medications") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                        navController.currentBackStackEntry?.savedStateHandle
                            ?.set("openNew", true)
                    },
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
            ) { backStackEntry ->
                val listViewModel: MedicationListViewModel = hiltViewModel()
                val medications by listViewModel.medications.collectAsState()

                val addEditViewModel: AddEditMedicationViewModel = hiltViewModel()
                val addEditState by addEditViewModel.addEditUiState.collectAsState()

                val openNew by backStackEntry.savedStateHandle
                    .getStateFlow("openNew", false)
                    .collectAsState()

                MedicationsScreen(
                    medications = medications,
                    onDeleteMedication = { medication ->
                        listViewModel.deleteMedication(medication)
                    },
                    addEditState = addEditState,
                    onLoadMedication = { id -> addEditViewModel.loadMedication(id) },
                    onTitleChange = { addEditViewModel.onTitleChanged(it) },
                    onBrandNameChange = { addEditViewModel.onBrandNameChanged(it) },
                    onDosageChange = { addEditViewModel.onDosageChanged(it) },
                    onAddSchedule = { addEditViewModel.onAddSchedule() },
                    onDeleteSchedule = { addEditViewModel.onDeleteSchedule(it) },
                    onScheduleTimeChange = { index, hour, minute ->
                        addEditViewModel.onScheduleTimeChanged(index, hour, minute)
                    },
                    onScheduleDayToggle = { index, day ->
                        addEditViewModel.onScheduleDayToggled(index, day)
                    },
                    onScheduleDosesChange = { index, doses ->
                        addEditViewModel.onScheduleDosesChanged(index, doses)
                    },
                    onSaveMedication = { onSuccess ->
                        addEditViewModel.saveMedication(onSuccess = onSuccess)
                    },
                    openNew = openNew,
                    onOpenNewConsumed = {
                        backStackEntry.savedStateHandle["openNew"] = false
                    }
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
