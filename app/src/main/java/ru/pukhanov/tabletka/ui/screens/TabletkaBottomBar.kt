package ru.pukhanov.tabletka.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TabletkaBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = currentScreen is Screen.Today,
            onClick = { onNavigate(Screen.Today) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Today's Schedule"
                )
            },
            label = { Text("Today") }
        )
        NavigationBarItem(
            selected = currentScreen is Screen.List,
            onClick = { onNavigate(Screen.List) },
            icon = {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "All Medications"
                )
            },
            label = { Text("Medications") }
        )
    }
}
