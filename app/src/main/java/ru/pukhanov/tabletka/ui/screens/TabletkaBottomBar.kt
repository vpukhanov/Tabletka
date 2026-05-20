package ru.pukhanov.tabletka.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TabletkaBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = currentRoute == "today",
            onClick = { onNavigate("today") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "Today's Schedule"
                )
            },
            label = { Text("Today") }
        )
        NavigationBarItem(
            selected = currentRoute == "medications",
            onClick = { onNavigate("medications") },
            icon = {
                Icon(
                    imageVector = Icons.Default.Medication,
                    contentDescription = "All Medications"
                )
            },
            label = { Text("Medications") }
        )
    }
}
