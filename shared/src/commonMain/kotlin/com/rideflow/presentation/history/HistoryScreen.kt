package com.rideflow.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rideflow.domain.model.Train
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HistoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    viewModel: HistoryViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Workout History") })
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(uiState.trains) { train ->
                    TrainItem(train = train, onClick = { onNavigateToDetail(train.id) })
                }
            }
        }
    }
}

@Composable
fun TrainItem(train: Train, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Treino #\${train.id}", style = MaterialTheme.typography.titleMedium)
            Text(text = "Data: \${train.date}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Duration: \${train.duration}s")
                Text(text = "Avg HR: \${train.avgHeartRate}")
                Text(text = "Avg Pwr: \${train.avgPower}")
            }
        }
    }
}
