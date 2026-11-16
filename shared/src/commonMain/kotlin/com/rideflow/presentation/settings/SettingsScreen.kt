package com.rideflow.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rideflow.domain.model.SensorConnectionState
import com.rideflow.domain.repository.SensorRepository
import com.rideflow.presentation.workout.statusColor
import com.rideflow.presentation.workout.statusLabel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    // Só leitura do estado de ligação, para mostrar "Ligado / A ligar / Erro" junto de cada sensor.
    sensorRepository: SensorRepository = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hrState by sensorRepository.heartRateState.collectAsState()
    val bikeState by sensorRepository.bikeState.collectAsState()
    // Alvo do diálogo de pesquisa (null = fechado). Guardado localmente porque o ViewModel
    // limpa `scanningFor` quando a pesquisa pára.
    var scanTarget by remember { mutableStateOf<ScannerTarget?>(null) }

    fun startScan(target: ScannerTarget) {
        viewModel.onIntent(SettingsIntent.StartScan(target))
        scanTarget = target
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                SectionHeader("Sensors")
                Column {
                    SensorSettingRow(
                        label = "Heart rate sensor",
                        deviceName = uiState.heartRateDeviceName,
                        connection = hrState,
                        onPair = { startScan(ScannerTarget.HeartRate) },
                        onReconnect = uiState.heartRateDeviceAddress?.let { address ->
                            { viewModel.onIntent(SettingsIntent.ConnectDevice(uiState.heartRateDeviceName, address, ScannerTarget.HeartRate)) }
                        },
                        onUnpair = { viewModel.onIntent(SettingsIntent.DisconnectDevice(ScannerTarget.HeartRate)) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SensorSettingRow(
                        label = "Smart bike (FTMS)",
                        deviceName = uiState.bikeDeviceName,
                        connection = bikeState,
                        onPair = { startScan(ScannerTarget.Bike) },
                        onReconnect = uiState.bikeDeviceAddress?.let { address ->
                            { viewModel.onIntent(SettingsIntent.ConnectDevice(uiState.bikeDeviceName, address, ScannerTarget.Bike)) }
                        },
                        onUnpair = { viewModel.onIntent(SettingsIntent.DisconnectDevice(ScannerTarget.Bike)) }
                    )
                }
                Text(
                    "Paired sensors reconnect automatically when the app starts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    scanTarget?.let { target ->
        ScanDialog(
            target = target,
            uiState = uiState,
            onSelect = { name, address ->
                viewModel.onIntent(SettingsIntent.ConnectDevice(name, address, target))
                scanTarget = null
            },
            onDismiss = {
                viewModel.onIntent(SettingsIntent.StopScan)
                scanTarget = null
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).semantics { heading() }
    )
}

@Composable
private fun SensorSettingRow(
    label: String,
    deviceName: String?,
    connection: SensorConnectionState,
    onPair: () -> Unit,
    onReconnect: (() -> Unit)?,
    onUnpair: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    deviceName ?: "Not paired",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (deviceName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Box(Modifier.size(8.dp).background(connection.statusColor(), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(connection.statusLabel(), style = MaterialTheme.typography.labelMedium, color = connection.statusColor())
                    }
                }
            }
            if (deviceName == null) {
                Button(onClick = onPair) { Text("Pair") }
            }
        }

        if (deviceName != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                val disconnected = connection is SensorConnectionState.Disconnected || connection is SensorConnectionState.Error
                if (onReconnect != null && disconnected) {
                    FilledTonalButton(onClick = onReconnect) { Text("Reconnect") }
                }
                OutlinedButton(
                    onClick = onUnpair,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Unpair") }
            }
        }
    }
}

@Composable
private fun ScanDialog(
    target: ScannerTarget,
    uiState: SettingsUiState,
    onSelect: (name: String, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    val targetText = if (target is ScannerTarget.HeartRate) "heart rate sensor" else "smart bike"
    val devices = uiState.discoveredDevices.filter { it.name != null || it.peripheralName != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pair $targetText") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (uiState.isScanning) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Searching for nearby devices. Make sure the $targetText is awake.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (devices.isEmpty()) {
                    Text(
                        "Scanning stopped. Check that Bluetooth is on and the app has permission to use it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (devices.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        items(devices, key = { it.identifier.toString() }) { device ->
                            val name = device.name ?: device.peripheralName.orEmpty()
                            val address = device.identifier.toString()
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = { Text(address) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onSelect(name, address) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
