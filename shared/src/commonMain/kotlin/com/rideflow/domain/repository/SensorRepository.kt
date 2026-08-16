package com.rideflow.domain.repository

import com.rideflow.domain.model.SensorConnectionState
import kotlinx.coroutines.flow.StateFlow

interface SensorRepository {
    // StateFlow (e não Flow) para que a camada de sessão possa ler o valor corrente
    // no seu tick de amostragem sem ter de manter um coletor por métrica.
    val heartRateState: StateFlow<SensorConnectionState>
    val bikeState: StateFlow<SensorConnectionState>

    val heartRate: StateFlow<Int?>
    val power: StateFlow<Int?>
    val speed: StateFlow<Double?>
    val resistance: StateFlow<Int?>

    suspend fun startScanning()
    suspend fun stopScanning()
    
    suspend fun connectHeartRateSensor(address: String)
    suspend fun connectBikeSensor(address: String)
    
    suspend fun disconnectHeartRateSensor()
    suspend fun disconnectBikeSensor()
    
    suspend fun setResistance(level: Int)
}
