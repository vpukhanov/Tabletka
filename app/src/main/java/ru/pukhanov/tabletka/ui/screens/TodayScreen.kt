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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.pukhanov.tabletka.ui.viewmodel.TodayScheduleGroup
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import ru.pukhanov.tabletka.util.formatMedicationDescription


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    groups: List<TodayScheduleGroup>,
    showAddButton: Boolean,
    onToggleTakeStatus: (Int, Int, Boolean) -> Unit,
    onAddMedicationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeGroups = groups.filter { !it.isTaken }
    val takenGroups = groups.filter { it.isTaken }
    val allTaken = groups.isNotEmpty() && groups.all { it.isTaken }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (groups.isEmpty()) {
                EmptyTodayState(
                    showAddButton = showAddButton,
                    onAddClick = onAddMedicationClick,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (allTaken) {
                        item(
                            key = "all_taken_card",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            AllTakenCard(
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (activeGroups.isNotEmpty()) {
                        items(
                            items = activeGroups,
                            key = { "${it.hour}:${it.minute}" }
                        ) { group ->
                            TodayScheduleCard(
                                group = group,
                                onToggleTakeStatus = onToggleTakeStatus,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }

                    if (takenGroups.isNotEmpty()) {
                        item(
                            key = "taken_header",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            Text(
                                text = "Taken earlier",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .animateItem()
                                    .padding(top = 8.dp, start = 4.dp, bottom = 4.dp)
                            )
                        }

                        items(
                            items = takenGroups,
                            key = { "${it.hour}:${it.minute}" }
                        ) { group ->
                            TodayScheduleCard(
                                group = group,
                                onToggleTakeStatus = onToggleTakeStatus,
                                modifier = Modifier.animateItem()
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
    val containerColor by animateColorAsState(
        targetValue = if (group.isTaken) {
            MaterialTheme.colorScheme.surfaceContainerLowest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "containerColor"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (group.isTaken) 0.6f else 1.0f,
        label = "contentAlpha"
    )

    val checkIcon = if (group.isTaken) {
        Icons.Default.CheckCircle
    } else {
        Icons.Outlined.CheckCircle
    }

    val checkIconTint by animateColorAsState(
        targetValue = if (group.isTaken) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "checkIconTint"
    )

    val cardElevation by animateDpAsState(
        targetValue = if (group.isTaken) 0.dp else 2.dp,
        label = "cardElevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (group.isTaken) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )

    val view = LocalView.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format(LocalLocale.current.platformLocale, "%02d:%02d", group.hour, group.minute),
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                IconButton(
                    onClick = {
                        val hapticType = if (group.isTaken) {
                            HapticFeedbackConstants.CLOCK_TICK
                        } else {
                            HapticFeedbackConstants.CONFIRM
                        }
                        view.performHapticFeedback(hapticType)
                        onToggleTakeStatus(group.hour, group.minute, group.isTaken)
                    },
                ) {
                    Icon(
                        imageVector = checkIcon,
                        contentDescription = if (group.isTaken) "Mark as untaken" else "Mark as taken",
                        tint = checkIconTint.copy(alpha = contentAlpha),
                        modifier = Modifier
                            .size(48.dp)
                            .scale(scale)
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

                val isLast = index == group.medications.lastIndex
                val topPadding = if (group.isTaken) 4.dp else 10.dp
                val bottomPadding = if (isLast && group.isTaken) 10.dp else topPadding

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBgColor)
                        .padding(
                            start = 16.dp,
                            top = topPadding,
                            end = 16.dp,
                            bottom = bottomPadding
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMedicationDescription(
                            title = med.title,
                            brandName = med.brandName,
                            dosage = med.dosage,
                            doses = med.doses
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    )
                }
            }
        }
    }
}

@Composable
fun AllTakenCard(
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .scale(scale.value)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "All caught up!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "You've taken all your scheduled medications for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun EmptyTodayState(
    showAddButton: Boolean,
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
        if (showAddButton) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddClick,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Add Medication")
            }
        }
    }
}
