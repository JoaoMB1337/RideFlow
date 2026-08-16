package com.rideflow.presentation.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideflow.domain.model.SensorConnectionState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    onWorkoutActiveChanged: (Boolean) -> Unit = {},
    viewModel: WorkoutViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isWorkoutActive) {
        onWorkoutActiveChanged(uiState.isWorkoutActive)
    }

    Scaffold(
        topBar = {
            if (uiState.isWorkoutActive) {
                CenterAlignedTopAppBar(
                    title = { Text("LIVE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        Row(modifier = Modifier.padding(end = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ConnectionDot(uiState.heartRateConnection, "HR")
                            ConnectionDot(uiState.bikeConnection, "Bike")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!uiState.isWorkoutActive) {
                // Sensor Status Chips at the top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SensorChip("HR", uiState.heartRateConnection, Modifier.weight(1f))
                    SensorChip("Bike", uiState.bikeConnection, Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                StartWorkoutButton { viewModel.onIntent(WorkoutIntent.StartWorkout) }
                
                Spacer(modifier = Modifier.weight(1f))
            } else {
                ActiveWorkoutDisplay(uiState, viewModel)
            }
        }
    }
}

@Composable
fun ConnectionDot(state: SensorConnectionState, label: String) {
    val color = when (state) {
        is SensorConnectionState.Connected -> MaterialTheme.colorScheme.primary
        is SensorConnectionState.Connecting -> MaterialTheme.colorScheme.tertiary
        is SensorConnectionState.Error -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun SensorChip(label: String, state: SensorConnectionState, modifier: Modifier = Modifier) {
    val isConnected = state is SensorConnectionState.Connected
    val color = when (state) {
        is SensorConnectionState.Connected -> MaterialTheme.colorScheme.primaryContainer
        is SensorConnectionState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color,
        border = if (!isConnected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun StartWorkoutButton(onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.size(220.dp).clickable(onClick = onClick),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("START", style = MaterialTheme.typography.headlineLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                    Text("New Ride", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun ActiveWorkoutDisplay(uiState: WorkoutUiState, viewModel: WorkoutViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatDuration(uiState.durationSeconds),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val hrColor = if (uiState.heartRateConnection is SensorConnectionState.Connected) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
            
            DataCard("BPM", uiState.heartRate?.toString() ?: "--", hrColor, Modifier.weight(1f))
            DataCard("Watts", uiState.power?.toString() ?: "--", MaterialTheme.colorScheme.secondaryContainer, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DataCard("km/h", uiState.speed?.let { ((it * 10).toInt() / 10.0).toString() } ?: "--", MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
            DataCard("KM", formatDistance(uiState.distance), MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        ResistanceControl(uiState.resistance, { viewModel.onIntent(WorkoutIntent.AdjustResistance(it)) }, Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { viewModel.onIntent(WorkoutIntent.StopWorkout) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = MaterialTheme.shapes.large
        ) {
            Text("FINISH RIDE", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
    }
}

@Composable
fun DataCard(label: String, value: String, containerColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
        }
    }
}

@Composable
fun ResistanceControl(level: Int, onLevelChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "RESISTANCE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = { onLevelChange((level - 10).coerceAtLeast(0)) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("-", style = MaterialTheme.typography.displaySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
                
                Text(
                    text = level.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                
                Surface(
                    onClick = { onLevelChange((level + 10).coerceAtMost(100)) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", style = MaterialTheme.typography.displaySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun formatDistance(km: Double): String {
    return if (km < 1.0) {
        "${(km * 1000).toInt()}m"
    } else {
        "${((km * 100).toInt() / 100.0)}km"
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    
    val hh = h.toString().padStart(2, '0')
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')
    
    return "$hh:$mm:$ss"
}
