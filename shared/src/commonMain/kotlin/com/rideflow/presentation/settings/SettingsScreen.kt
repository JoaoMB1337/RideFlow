package com.rideflow.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showScanDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Saved Devices Info
            DeviceSettingRow(
                label = "Heart Rate Sensor",
                deviceName = uiState.heartRateDeviceName,
                onScan = { 
                    viewModel.onIntent(SettingsIntent.StartScan(ScannerTarget.HeartRate))
                    showScanDialog = true
                },
                onDisconnect = { viewModel.onIntent(SettingsIntent.DisconnectDevice(ScannerTarget.HeartRate)) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DeviceSettingRow(
                label = "Bike / FTMS",
                deviceName = uiState.bikeDeviceName,
                onScan = { 
                    viewModel.onIntent(SettingsIntent.StartScan(ScannerTarget.Bike))
                    showScanDialog = true
                },
                onDisconnect = { viewModel.onIntent(SettingsIntent.DisconnectDevice(ScannerTarget.Bike)) }
            )
        }
    }

    if (showScanDialog) {
        val targetText = if (uiState.scanningFor is ScannerTarget.HeartRate) "Heart Rate Sensor" else "Bike / FTMS"
        AlertDialog(
            onDismissRequest = { 
                viewModel.onIntent(SettingsIntent.StopScan)
                showScanDialog = false 
            },
            title = { Text("Scanning for $targetText") },
            text = {
                val devicesWithName = uiState.discoveredDevices.filter { it.name != null || it.peripheralName != null }
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    if (devicesWithName.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(devicesWithName) { device ->
                                val name = device.name ?: device.peripheralName!!
                                ListItem(
                                    headlineContent = { Text(name) },
                                    supportingContent = { Text(device.identifier.toString()) },
                                    modifier = Modifier.clickable {
                                        uiState.scanningFor?.let { target ->
                                            viewModel.onIntent(SettingsIntent.ConnectDevice(name, device.identifier.toString(), target))
                                            showScanDialog = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.onIntent(SettingsIntent.StopScan)
                    showScanDialog = false 
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DeviceSettingRow(
    label: String,
    deviceName: String?,
    onScan: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = deviceName ?: "Not paired", 
                    style = MaterialTheme.typography.bodyLarge, 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = if (deviceName == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                )
            }
            if (deviceName == null) {
                Button(onClick = onScan, shape = MaterialTheme.shapes.medium) { Text("Pair") }
            } else {
                TextButton(onClick = onDisconnect) { Text("Unpair", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
