package ru.pukhanov.tabletka.ui.screens

sealed interface Screen {
    object List : Screen
    data class AddEdit(val medicationId: Long? = null) : Screen
}
