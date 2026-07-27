package com.rideflow.data.bluetooth

import com.rideflow.domain.model.SensorConnectionState
import com.rideflow.domain.repository.SensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SensorRepositoryImpl : SensorRepository {
    private val _heartRateState = MutableStateFlow<SensorConnectionState>(SensorConnectionState.Disconnected)
    override val heartRateState: Flow<SensorConnectionState> = _heartRateState.asStateFlow()

    private val _bikeState = MutableStateFlow<SensorConnectionState>(SensorConnectionState.Disconnected)
    override val bikeState: Flow<SensorConnectionState> = _bikeState.asStateFlow()

    private val _heartRate = MutableStateFlow<Int?>(null)
    override val heartRate: Flow<Int?> = _heartRate.asStateFlow()

    private val _power = MutableStateFlow<Int?>(null)
    override val power: Flow<Int?> = _power.asStateFlow()

    private val _speed = MutableStateFlow<Double?>(null)
    override val speed: Flow<Double?> = _speed.asStateFlow()

    private val _resistance = MutableStateFlow<Int?>(null)
    override val resistance: Flow<Int?> = _resistance.asStateFlow()

    override suspend fun startScanning() {
        // TODO: Implement with BleScanner
    }

    override suspend fun stopScanning() {
        // TODO: Implement
    }

    override suspend fun connectHeartRateSensor(address: String) {
        // TODO: Implement with HeartRateDeviceHandler
    }

    override suspend fun connectBikeSensor(address: String) {
        // TODO: Implement with BikeDeviceHandler
    }

    override suspend fun setResistance(level: Int) {
        // TODO: Implement with FtmsControlPoint
    }
}
