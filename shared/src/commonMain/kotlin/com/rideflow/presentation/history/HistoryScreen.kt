package com.rideflow.presentation.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                    contentPadding = PaddingValues(vertical = 8.dp)
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
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
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

    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            SummaryStat("Rides", trains.size.toString(), Modifier.weight(1f))
            SummaryStat("Time", formatDuration(trains.sumOf { it.duration }), Modifier.weight(1f))
            SummaryStat("Distance", formatDistance(trains.sumOf { it.distance }), Modifier.weight(1f))
            SummaryStat("Avg bpm", avgHeartRate, Modifier.weight(1f))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyHistoryPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

/** Ex.: "45:12  ·  18.3km  ·  182 W  ·  141 bpm" (métricas sem dados ficam de fora). */
fun Train.statsLine(): String = listOfNotNull(
    formatDuration(duration),
    formatDistance(distance),
    avgPower.takeIf { it > 0 }?.let { "$it W" },
    avgHeartRate.takeIf { it > 0 }?.let { "$it bpm" },
).joinToString("  ·  ")

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Select")
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(checked = isSelected, onCheckedChange = { onClick() })
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(train.name ?: date, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                if (train.name != null) "$date · ${formatTimeOfDay(train.date)}" else formatTimeOfDay(train.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(train.statsLine(), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        if (!isSelectionMode) {
            RideOverflowMenu(date, onShare, onDelete)
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
                onClick = { expanded = false; onShare() }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { expanded = false; onDelete() }
            )
        }
    }
}
