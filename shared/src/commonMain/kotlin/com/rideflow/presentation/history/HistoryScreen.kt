package com.rideflow.presentation.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rideflow.domain.model.Train
import com.rideflow.util.formatDate
import com.rideflow.util.formatDistance
import com.rideflow.util.formatDuration
import com.rideflow.util.formatTimeOfDay
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Treinos à espera de confirmação para apagar (null = sem diálogo aberto).
    var pendingDelete by remember { mutableStateOf<Set<Long>?>(null) }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text("${uiState.selectedTrainIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onIntent(HistoryIntent.ClearSelection) }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { pendingDelete = uiState.selectedTrainIds }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected rides", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                )
            } else {
                TopAppBar(title = { Text("History") })
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.trains.isEmpty() -> EmptyHistoryPlaceholder()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxHeight().widthIn(max = 720.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { QuickHistorySummary(uiState.trains) }

                    items(uiState.trains, key = { it.id }) { train ->
                        TrainItem(
                            train = train,
                            isSelected = train.id in uiState.selectedTrainIds,
                            isSelectionMode = uiState.isSelectionMode,
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    viewModel.onIntent(HistoryIntent.ToggleSelection(train.id))
                                } else {
                                    onNavigateToDetail(train.id)
                                }
                            },
                            onLongClick = { viewModel.onIntent(HistoryIntent.ToggleSelection(train.id)) },
                            onShare = { viewModel.onIntent(HistoryIntent.ShareWorkout(train.id)) },
                            onDelete = { pendingDelete = setOf(train.id) }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { ids ->
        val count = ids.size
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(if (count == 1) "Delete ride?" else "Delete $count rides?") },
            text = { Text("This permanently removes the ride and all its recorded data.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onIntent(HistoryIntent.DeleteTrains(ids))
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun QuickHistorySummary(trains: List<Train>) {
    val heartRates = trains.map { it.avgHeartRate }.filter { it > 0 }
    val avgHeartRate = if (heartRates.isEmpty()) "--" else heartRates.average().toInt().toString()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 8.dp)) {
            SummaryStat("Rides", trains.size.toString(), Modifier.weight(1f))
            SummaryStat("Time", formatDuration(trains.sumOf { it.duration }), Modifier.weight(1f))
            SummaryStat("Distance", formatDistance(trains.sumOf { it.distance }), Modifier.weight(1f))
            SummaryStat("Avg bpm", avgHeartRate, Modifier.weight(1f))
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun EmptyHistoryPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsBike,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("No rides yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Start a ride from the Ride tab and it will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrainItem(
    train: Train,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val date = formatDate(train.date)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Select"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onClick() })
                } else {
                    Box(
                        Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.DirectionsBike,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(date, style = MaterialTheme.typography.titleMedium)
                    Text(
                        formatTimeOfDay(train.date),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isSelectionMode) {
                    RideOverflowMenu(date, onShare, onDelete)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Grelha 2x2: cabe em ecrãs estreitos sem cortar valores como "01:02:03".
            Row(Modifier.fillMaxWidth().padding(start = 52.dp, end = 12.dp)) {
                HistoryStat(Icons.Default.Timer, "Duration", formatDuration(train.duration), Modifier.weight(1f))
                HistoryStat(Icons.Default.Route, "Distance", formatDistance(train.distance), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(start = 52.dp, end = 12.dp)) {
                HistoryStat(
                    Icons.Default.Bolt, "Avg power",
                    if (train.avgPower > 0) "${train.avgPower} W" else "--",
                    Modifier.weight(1f), MaterialTheme.colorScheme.primary
                )
                HistoryStat(
                    Icons.Default.Favorite, "Avg heart rate",
                    if (train.avgHeartRate > 0) "${train.avgHeartRate} bpm" else "--",
                    Modifier.weight(1f), MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun RideOverflowMenu(date: String, onShare: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options for ride on $date")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Share") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = { expanded = false; onShare() }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}

@Composable
fun HistoryStat(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier.clearAndSetSemantics { contentDescription = "$label $value" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}
