package com.rideflow.data.bluetooth

import com.juul.kable.Peripheral
import com.juul.kable.State
import com.rideflow.data.bluetooth.bike.BikeDeviceHandler
import com.rideflow.data.bluetooth.bike.FtmsControlPoint
import com.rideflow.data.bluetooth.heartrate.HeartRateDeviceHandler
import com.rideflow.domain.model.SensorConnectionState
import com.rideflow.domain.repository.SensorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SensorRepositoryImpl(
    private val bleScanner: BleScanner,
    private val peripheralFactory: PeripheralFactory
) : SensorRepository {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var heartRatePeripheral: Peripheral? = null
    private var bikePeripheral: Peripheral? = null
    
    private var hrConnectionJob: Job? = null
    private var bikeConnectionJob: Job? = null
    
    private var hrObservationJob: Job? = null
    private var bikeObservationJob: Job? = null
    
    private var ftmsControlPoint: FtmsControlPoint? = null

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
        // Handled by UI observing bleScanner.advertisements for now
    }

    override suspend fun stopScanning() {
        // Implementation
    }

    override suspend fun connectHeartRateSensor(address: String) {
        hrConnectionJob?.cancel()
        heartRatePeripheral?.disconnect()
        
        val peripheral = peripheralFactory.createPeripheral(address)
        heartRatePeripheral = peripheral
        observeHeartRate(peripheral)

        hrConnectionJob = scope.launch {
            while (isActive) {
                try {
                    peripheral.connect()
                } catch (e: Exception) {
                    delay(5000) // Wait 5s before retrying
                }
            }
        }
    }

    @OptIn(com.juul.kable.ExperimentalKableApi::class)
    private fun observeHeartRate(peripheral: Peripheral) {
        hrObservationJob?.cancel()
        hrObservationJob = scope.launch {
            val handler = HeartRateDeviceHandler(peripheral)
            
            launch {
                handler.heartRate
                    .onEach { hr -> _heartRate.value = hr }
                    .collect {}
            }

            launch {
                peripheral.state
                    .onEach { state ->
                        _heartRateState.value = when(state) {
                            is State.Connected -> SensorConnectionState.Connected(peripheral.name)
                            is State.Connecting -> SensorConnectionState.Connecting
                            is State.Disconnecting -> SensorConnectionState.Connecting
                            is State.Disconnected -> SensorConnectionState.Disconnected
                        }
                    }
                    .collect {}
            }
        }
    }

    override suspend fun connectBikeSensor(address: String) {
        bikeConnectionJob?.cancel()
        bikePeripheral?.disconnect()
        
        val peripheral = peripheralFactory.createPeripheral(address)
        bikePeripheral = peripheral
        observeBike(peripheral)

        bikeConnectionJob = scope.launch {
            while (isActive) {
                try {
                    peripheral.connect()
                } catch (e: Exception) {
                    delay(5000)
                }
            }
        }
    }

    override suspend fun disconnectHeartRateSensor() {
        hrConnectionJob?.cancel()
        hrObservationJob?.cancel()
        heartRatePeripheral?.disconnect()
        heartRatePeripheral = null
        _heartRateState.value = SensorConnectionState.Disconnected
        _heartRate.value = null
    }

    override suspend fun disconnectBikeSensor() {
        bikeConnectionJob?.cancel()
        bikeObservationJob?.cancel()
        bikePeripheral?.disconnect()
        bikePeripheral = null
        ftmsControlPoint = null
        _bikeState.value = SensorConnectionState.Disconnected
        _power.value = null
        _speed.value = null
        _resistance.value = null
    }

    @OptIn(com.juul.kable.ExperimentalKableApi::class)
    private fun observeBike(peripheral: Peripheral) {
        bikeObservationJob?.cancel()
        bikeObservationJob = scope.launch {
            val handler = BikeDeviceHandler(peripheral)
            ftmsControlPoint = FtmsControlPoint(peripheral)
            
            launch {
                handler.bikeData
                    .onEach { data ->
                        _power.value = data.power
                        _speed.value = data.speed
                        _resistance.value = data.resistance
                    }
                    .collect {}
            }

            launch {
                peripheral.state
                    .onEach { state ->
                        _bikeState.value = when(state) {
                            is State.Connected -> SensorConnectionState.Connected(peripheral.name)
                            is State.Connecting -> SensorConnectionState.Connecting
                            is State.Disconnecting -> SensorConnectionState.Connecting
                            is State.Disconnected -> SensorConnectionState.Disconnected
                        }
                    }
                    .collect {}
            }
        }
    }

    override suspend fun setResistance(level: Int) {
        ftmsControlPoint?.setResistance(level)
    }
}
