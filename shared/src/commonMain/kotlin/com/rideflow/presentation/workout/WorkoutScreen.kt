package com.rideflow.presentation.workout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideflow.domain.model.SensorConnectionState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = formatDuration(uiState.durationSeconds), fontSize = 48.sp)
        
        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SensorInfo(label = "BPM", value = uiState.heartRate?.toString() ?: "--", connection = uiState.heartRateConnection)
            SensorInfo(label = "Watts", value = uiState.power?.toString() ?: "--", connection = uiState.bikeConnection)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SensorInfo(label = "km/h", value = uiState.speed?.let { "%.1f".format(it) } ?: "--", connection = uiState.bikeConnection)
            SensorInfo(label = "Res", value = uiState.resistance.toString(), connection = uiState.bikeConnection)
        }

        Spacer(modifier = Modifier.height(48.dp))

        if (!uiState.isWorkoutActive) {
            Button(onClick = { viewModel.onIntent(WorkoutIntent.StartWorkout) }) {
                Text("Start Workout")
            }
        } else {
            Button(
                onClick = { viewModel.onIntent(WorkoutIntent.StopWorkout) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Stop Workout")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.onIntent(WorkoutIntent.AdjustResistance(uiState.resistance - 1)) }) {
                    Text("-")
                }
                Text("Resistance: ${uiState.resistance}")
                IconButton(onClick = { viewModel.onIntent(WorkoutIntent.AdjustResistance(uiState.resistance + 1)) }) {
                    Text("+")
                }
            }
        }
    }
}

@Composable
fun SensorInfo(label: String, value: String, connection: SensorConnectionState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.headlineMedium)
        val color = when(connection) {
            is SensorConnectionState.Connected -> MaterialTheme.colorScheme.primary
            is SensorConnectionState.Connecting -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.error
        }
        Box(modifier = Modifier.size(8.dp).padding(top = 4.dp).align(Alignment.CenterHorizontally)) // Indicator
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
