package com.rideflow.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideflow.domain.repository.SensorRepository
import com.rideflow.data.bluetooth.BleScanner
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val sensorRepository: SensorRepository,
    private val bleScanner: BleScanner,
    private val settings: Settings
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    companion object {
        private const val KEY_HR_NAME = "hr_device_name"
        private const val KEY_HR_ADDR = "hr_device_address"
        private const val KEY_BIKE_NAME = "bike_device_name"
        private const val KEY_BIKE_ADDR = "bike_device_address"
    }

    init {
        loadSavedDevices()
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.StartScan -> startScan(intent.target)
            SettingsIntent.StopScan -> stopScan()
            is SettingsIntent.ConnectDevice -> connectDevice(intent.name, intent.address, intent.target)
            is SettingsIntent.DisconnectDevice -> disconnectDevice(intent.target)
            SettingsIntent.LoadSavedDevices -> loadSavedDevices()
        }
    }

    private fun loadSavedDevices() {
        val hrName = settings.getStringOrNull(KEY_HR_NAME)
        val hrAddr = settings.getStringOrNull(KEY_HR_ADDR)
        val bikeName = settings.getStringOrNull(KEY_BIKE_NAME)
        val bikeAddr = settings.getStringOrNull(KEY_BIKE_ADDR)

        _uiState.update { it.copy(
            heartRateDeviceName = hrName,
            heartRateDeviceAddress = hrAddr,
            bikeDeviceName = bikeName,
            bikeDeviceAddress = bikeAddr
        ) }
        
        // Auto-connect if addresses exist
        viewModelScope.launch {
            hrAddr?.let { sensorRepository.connectHeartRateSensor(it) }
            bikeAddr?.let { sensorRepository.connectBikeSensor(it) }
        }
    }

    private fun startScan(target: ScannerTarget) {
        stopScan()
        _uiState.update { it.copy(isScanning = true, discoveredDevices = emptyList(), scanningFor = target) }
        
        scanJob = bleScanner.advertisements
            .onEach { advertisement ->
                _uiState.update { state ->
                    if (state.discoveredDevices.any { it.identifier == advertisement.identifier }) {
                        state
                    } else {
                        state.copy(discoveredDevices = state.discoveredDevices + advertisement)
                    }
                }
            }
            .catch { e ->
                _uiState.update { it.copy(isScanning = false, scanningFor = null) }
                // Here you could emit a side effect to show a message about permissions
            }
            .launchIn(viewModelScope)
    }

    private fun stopScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false, scanningFor = null) }
    }

    private fun connectDevice(name: String?, address: String, target: ScannerTarget) {
        viewModelScope.launch {
            stopScan()
            when (target) {
                ScannerTarget.HeartRate -> {
                    settings.putString(KEY_HR_NAME, name ?: "Unknown Device")
                    settings.putString(KEY_HR_ADDR, address)
                    _uiState.update { it.copy(heartRateDeviceName = name ?: "Unknown Device", heartRateDeviceAddress = address) }
                    sensorRepository.connectHeartRateSensor(address)
                }
                ScannerTarget.Bike -> {
                    settings.putString(KEY_BIKE_NAME, name ?: "Unknown Device")
                    settings.putString(KEY_BIKE_ADDR, address)
                    _uiState.update { it.copy(bikeDeviceName = name ?: "Unknown Device", bikeDeviceAddress = address) }
                    sensorRepository.connectBikeSensor(address)
                }
            }
        }
    }

    private fun disconnectDevice(target: ScannerTarget) {
        viewModelScope.launch {
            when (target) {
                ScannerTarget.HeartRate -> {
                    settings.remove(KEY_HR_NAME)
                    settings.remove(KEY_HR_ADDR)
                    _uiState.update { it.copy(heartRateDeviceName = null, heartRateDeviceAddress = null) }
                    sensorRepository.disconnectHeartRateSensor()
                }
                ScannerTarget.Bike -> {
                    settings.remove(KEY_BIKE_NAME)
                    settings.remove(KEY_BIKE_ADDR)
                    _uiState.update { it.copy(bikeDeviceName = null, bikeDeviceAddress = null) }
                    sensorRepository.disconnectBikeSensor()
                }
            }
        }
    }
}
