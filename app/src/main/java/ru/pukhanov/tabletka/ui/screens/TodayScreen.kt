package ru.pukhanov.tabletka.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.pukhanov.tabletka.ui.viewmodel.TodayScheduleGroup
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    groups: List<TodayScheduleGroup>,
    onToggleTakeStatus: (Int, Int, Boolean) -> Unit,
    onAddMedicationClick: () -> Unit,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeGroups = groups.filter { !it.isTaken }
    val takenGroups = groups.filter { it.isTaken }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            TabletkaBottomBar(
                currentScreen = Screen.Today,
                onNavigate = onNavigate
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddMedicationClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medication",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (groups.isEmpty()) {
                EmptyTodayState(
                    onAddClick = onAddMedicationClick,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (activeGroups.isNotEmpty()) {
                        items(
                            items = activeGroups,
                            key = { "${it.hour}:${it.minute}_active" }
                        ) { group ->
                            TodayScheduleCard(
                                group = group,
                                onToggleTakeStatus = onToggleTakeStatus
                            )
                        }
                    }

                    if (takenGroups.isNotEmpty()) {
                        item(key = "taken_header") {
                            Text(
                                text = "Taken earlier",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(
                            items = takenGroups,
                            key = { "${it.hour}:${it.minute}_taken" }
                        ) { group ->
                            TodayScheduleCard(
                                group = group,
                                onToggleTakeStatus = onToggleTakeStatus
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodayScheduleCard(
    group: TodayScheduleGroup,
    onToggleTakeStatus: (Int, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (group.isTaken) {
        MaterialTheme.colorScheme.surfaceContainerLowest
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentAlpha = if (group.isTaken) 0.6f else 1.0f

    val checkIcon = if (group.isTaken) {
        Icons.Default.CheckCircle
    } else {
        Icons.Outlined.CheckCircle
    }

    val checkIconTint = if (group.isTaken) {
        Color(0xFF2E7D32) // Soft forest green
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (group.isTaken) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", group.hour, group.minute),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                IconButton(
                    onClick = { onToggleTakeStatus(group.hour, group.minute, group.isTaken) }
                ) {
                    Icon(
                        imageVector = checkIcon,
                        contentDescription = if (group.isTaken) "Mark as untaken" else "Mark as taken",
                        tint = checkIconTint.copy(alpha = contentAlpha),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            group.medications.forEachIndexed { index, med ->
                val rowBgColor = if (group.isTaken) {
                    Color.Transparent
                } else if (index % 2 == 0) {
                    MaterialTheme.colorScheme.surfaceContainerLow
                } else {
                    MaterialTheme.colorScheme.surface
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBgColor)
                        .padding(
                            start = 16.dp,
                            top = 10.dp,
                            end = 16.dp,
                            bottom = 10.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dosageText = if (!med.dosage.isNullOrBlank()) " • ${med.dosage}" else ""
                    val dosesText = if (med.doses == 1.0) "1 dose" else {
                        val dosesInt = med.doses.toInt()
                        if (med.doses == dosesInt.toDouble()) "$dosesInt doses" else "${med.doses} doses"
                    }

                    Text(
                        text = "${med.title}$dosageText × $dosesText",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTodayState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No medications for today",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You have no medications scheduled for today. You can add one or check your medications list.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddClick,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Medication")
        }
    }
}
