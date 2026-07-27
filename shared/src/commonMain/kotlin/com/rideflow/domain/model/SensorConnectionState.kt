package com.rideflow.domain.model

sealed class SensorConnectionState {
    object Disconnected : SensorConnectionState()
    object Connecting : SensorConnectionState()
    data class Connected(val deviceName: String?) : SensorConnectionState()
    data class Error(val message: String) : SensorConnectionState()
}
