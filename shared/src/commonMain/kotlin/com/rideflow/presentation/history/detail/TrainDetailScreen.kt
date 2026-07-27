package com.rideflow.presentation.history.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rideflow.domain.model.Train
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainDetailScreen(
    trainId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TrainDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(trainId) {
        viewModel.onIntent(TrainDetailIntent.LoadDetail(trainId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SUMMARY", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.train?.let { train ->
                    TrainSummaryCard(train)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ChartContainer(
                    label = "Heart Rate",
                    samples = uiState.samples,
                    valueSelector = { it.heartRate?.toDouble() },
                    color = MaterialTheme.colorScheme.error
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ChartContainer(
                    label = "Power",
                    samples = uiState.samples,
                    valueSelector = { it.power?.toDouble() },
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Workout") },
            text = { Text("Are you sure you want to delete this workout?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onIntent(TrainDetailIntent.DeleteTrain)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TrainSummaryCard(train: Train) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(Modifier.weight(1f), "Time", formatDuration(train.duration))
                SummaryItem(Modifier.weight(1f), "Avg HR", "${train.avgHeartRate}")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(Modifier.weight(1f), "Avg Pwr", "${train.avgPower}W")
                SummaryItem(Modifier.weight(1f), "Max HR", "${train.maxHeartRate}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem(Modifier.weight(1f), "Max Pwr", "${train.maxPower}W")
                SummaryItem(Modifier.weight(1f), "Date", train.date.toString().substringBefore("T"))
            }
        }
    }
}

@Composable
fun SummaryItem(modifier: Modifier, label: String, value: String) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
    }
}

@Composable
fun ChartContainer(
    label: String,
    samples: List<com.rideflow.domain.model.TrainingData>,
    valueSelector: (com.rideflow.domain.model.TrainingData) -> Double?,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxSize()) {
                val dataPoints = samples.mapNotNull { valueSelector(it) }
                if (dataPoints.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val maxVal = dataPoints.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                        val minVal = 0.0
                        
                        val path = androidx.compose.ui.graphics.Path()
                        dataPoints.forEachIndexed { index, value ->
                            val x = (index.toFloat() / (dataPoints.size - 1).coerceAtLeast(1)) * width
                            val y = height - ((value.toFloat() - minVal.toFloat()) / (maxVal.toFloat() - minVal.toFloat()) * height)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        
                        drawPath(
                            path = path,
                            color = color,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    
    val hh = h.toString().padStart(2, '0')
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')
    
    return if (h > 0) "$hh:$mm:$ss" else "$mm:$ss"
}
