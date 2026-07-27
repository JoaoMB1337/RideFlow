package com.rideflow.domain.repository

import com.rideflow.domain.model.SensorConnectionState
import kotlinx.coroutines.flow.Flow

interface SensorRepository {
    val heartRateState: Flow<SensorConnectionState>
    val bikeState: Flow<SensorConnectionState>
    
    val heartRate: Flow<Int?>
    val power: Flow<Int?>
    val speed: Flow<Double?>
    val resistance: Flow<Int?>

    suspend fun startScanning()
    suspend fun stopScanning()
    
    suspend fun connectHeartRateSensor(address: String)
    suspend fun connectBikeSensor(address: String)
    
    suspend fun disconnectHeartRateSensor()
    suspend fun disconnectBikeSensor()
    
    suspend fun setResistance(level: Int)
}
