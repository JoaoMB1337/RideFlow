package com.rideflow.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sensor Configuration", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Here you will add BLE Scanning and device selection logic
            Button(onClick = { /* Open Scanner */ }) {
                Text("Scan for Sensors")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("App Version: 1.0.0", style = MaterialTheme.typography.bodySmall)
        }
    }
}
