package com.rideflow.presentation.settings

import com.juul.kable.Advertisement

data class SettingsUiState(
    val isScanning: Boolean = false,
    val discoveredDevices: List<Advertisement> = emptyList(),
    val scanningFor: ScannerTarget? = null,
    val heartRateDeviceName: String? = null,
    val heartRateDeviceAddress: String? = null,
    val bikeDeviceName: String? = null,
    val bikeDeviceAddress: String? = null
)

sealed class ScannerTarget {
    object HeartRate : ScannerTarget()
    object Bike : ScannerTarget()
}

sealed class SettingsIntent {
    data class StartScan(val target: ScannerTarget) : SettingsIntent()
    object StopScan : SettingsIntent()
    data class ConnectDevice(val name: String?, val address: String, val target: ScannerTarget) : SettingsIntent()
    data class DisconnectDevice(val target: ScannerTarget) : SettingsIntent()
    object LoadSavedDevices : SettingsIntent()
}
