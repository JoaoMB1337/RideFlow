package com.rideflow.presentation.history.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrainDetailScreen(
    trainId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TrainDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(trainId) {
        viewModel.onIntent(TrainDetailIntent.LoadDetail(trainId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Treino #\${trainId}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("<") // Placeholder for back icon
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                uiState.train?.let { train ->
                    Text("Summary", style = MaterialTheme.typography.titleLarge)
                    Text("Duration: \${train.duration}s")
                    Text("Avg BPM: \${train.avgHeartRate}")
                    Text("Avg Watts: \${train.avgPower}")
                    Text("Max BPM: \${train.maxHeartRate}")
                    Text("Max Watts: \${train.maxPower}")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Samples Recorded: \${uiState.samples.size}")
                
                // Here you would add the Chart component
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    Text("Chart Placeholder")
                }
            }
        }
    }
}
