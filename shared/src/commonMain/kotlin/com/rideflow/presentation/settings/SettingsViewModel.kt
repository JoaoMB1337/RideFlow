package com.rideflow.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideflow.domain.repository.SensorRepository
import com.rideflow.data.bluetooth.BleScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val sensorRepository: SensorRepository,
    private val bleScanner: BleScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.StartScan -> startScan(intent.target)
            SettingsIntent.StopScan -> stopScan()
            is SettingsIntent.ConnectDevice -> connectDevice(intent.address, intent.target)
        }
    }

    private fun startScan(target: ScannerTarget) {
        stopScan()
        _uiState.update { it.copy(isScanning = true, discoveredDevices = emptyList(), scanningFor = target) }
        
        scanJob = bleScanner.advertisements
            .onEach { advertisement ->
                _uiState.update { state ->
                    if (state.discoveredDevices.any { it.address == advertisement.address }) {
                        state
                    } else {
                        state.copy(discoveredDevices = state.discoveredDevices + advertisement)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false, scanningFor = null) }
    }

    private fun connectDevice(address: String, target: ScannerTarget) {
        viewModelScope.launch {
            stopScan()
            when (target) {
                ScannerTarget.HeartRate -> sensorRepository.connectHeartRateSensor(address)
                ScannerTarget.Bike -> sensorRepository.connectBikeSensor(address)
            }
        }
    }
}
