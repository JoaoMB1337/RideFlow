package com.rideflow.data.bluetooth

import com.juul.kable.Peripheral
import com.juul.kable.State
import com.rideflow.data.bluetooth.bike.BikeDeviceHandler
import com.rideflow.data.bluetooth.bike.FtmsControlPoint
import com.rideflow.data.bluetooth.heartrate.HeartRateDeviceHandler
import com.rideflow.domain.model.SensorConnectionState
import com.rideflow.domain.repository.SensorRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

private const val RECONNECT_DELAY_MS = 5_000L

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
    override val heartRateState: StateFlow<SensorConnectionState> = _heartRateState.asStateFlow()

    private val _bikeState = MutableStateFlow<SensorConnectionState>(SensorConnectionState.Disconnected)
    override val bikeState: StateFlow<SensorConnectionState> = _bikeState.asStateFlow()

    private val _heartRate = MutableStateFlow<Int?>(null)
    override val heartRate: StateFlow<Int?> = _heartRate.asStateFlow()

    private val _power = MutableStateFlow<Int?>(null)
    override val power: StateFlow<Int?> = _power.asStateFlow()

    private val _speed = MutableStateFlow<Double?>(null)
    override val speed: StateFlow<Double?> = _speed.asStateFlow()

    private val _resistance = MutableStateFlow<Int?>(null)
    override val resistance: StateFlow<Int?> = _resistance.asStateFlow()

    override suspend fun startScanning() {
        // Handled by UI observing bleScanner.advertisements for now
    }

    override suspend fun stopScanning() {
        // Implementation
    }

    /**
     * Mantém a ligação viva com retry.
     *
     * [Peripheral.connect] suspende só até ficar ligado e depois **retorna** (e retorna de
     * imediato se já estiver ligado). Sem o `first { Disconnected }` a seguir, o `while` voltava
     * a chamar `connect()` num busy-loop a 100% de CPU durante todo o tempo em que o sensor
     * estivesse ligado.
     */
    private fun maintainConnection(
        peripheral: Peripheral,
        stateFlow: MutableStateFlow<SensorConnectionState>,
        tag: String
    ): Job = scope.launch {
        while (isActive) {
            try {
                peripheral.connect()
                peripheral.state.first { it is State.Disconnected }
                Napier.i(tag = tag) { "Connection dropped, retrying in ${RECONNECT_DELAY_MS}ms" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.w(tag = tag, throwable = e) { "Connection attempt failed" }
                stateFlow.value = SensorConnectionState.Error(e.message ?: "Connection failed")
            }
            delay(RECONNECT_DELAY_MS)
        }
    }

    override suspend fun connectHeartRateSensor(address: String) {
        hrConnectionJob?.cancelAndJoin()
        heartRatePeripheral?.let {
            it.disconnect()
            it.close()
        }

        val peripheral = peripheralFactory.createPeripheral(address)
        heartRatePeripheral = peripheral
        observeHeartRate(peripheral)

        hrConnectionJob = maintainConnection(peripheral, _heartRateState, "HeartRate")
    }

    @OptIn(com.juul.kable.ExperimentalKableApi::class)
    private fun observeHeartRate(peripheral: Peripheral) {
        hrObservationJob?.cancel()
        hrObservationJob = scope.launch {
            var observationJob: Job? = null
            peripheral.state.collect { state ->
                _heartRateState.value = when (state) {
                    is State.Connected -> SensorConnectionState.Connected(peripheral.name)
                    is State.Connecting -> SensorConnectionState.Connecting
                    is State.Disconnecting -> SensorConnectionState.Connecting
                    is State.Disconnected -> SensorConnectionState.Disconnected
                }

                if (state is State.Connected) {
                    observationJob?.cancel()
                    observationJob = launch {
                        try {
                            val handler = HeartRateDeviceHandler(peripheral)
                            handler.heartRate
                                .onEach { hr -> _heartRate.value = hr }
                                .collect()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Napier.w(tag = "HeartRate", throwable = e) { "Observation failed" }
                        }
                    }
                } else if (state is State.Disconnected) {
                    observationJob?.cancel()
                }
            }
        }
    }

    override suspend fun connectBikeSensor(address: String) {
        bikeConnectionJob?.cancelAndJoin()
        bikePeripheral?.let {
            it.disconnect()
            it.close()
        }

        val peripheral = peripheralFactory.createPeripheral(address)
        bikePeripheral = peripheral
        observeBike(peripheral)

        bikeConnectionJob = maintainConnection(peripheral, _bikeState, "Bike")
    }

    override suspend fun disconnectHeartRateSensor() {
        // Cancelar o retry antes de desligar, senão volta a reconectar sozinho.
        hrConnectionJob?.cancelAndJoin()
        hrConnectionJob = null
        hrObservationJob?.cancelAndJoin()
        hrObservationJob = null
        heartRatePeripheral?.let {
            it.disconnect()
            it.close()
        }
        heartRatePeripheral = null
        _heartRateState.value = SensorConnectionState.Disconnected
        _heartRate.value = null
    }

    override suspend fun disconnectBikeSensor() {
        bikeConnectionJob?.cancelAndJoin()
        bikeConnectionJob = null
        bikeObservationJob?.cancelAndJoin()
        bikeObservationJob = null
        bikePeripheral?.let {
            it.disconnect()
            it.close()
        }
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
            var observationJob: Job? = null
            peripheral.state.collect { state ->
                _bikeState.value = when (state) {
                    is State.Connected -> SensorConnectionState.Connected(peripheral.name)
                    is State.Connecting -> SensorConnectionState.Connecting
                    is State.Disconnecting -> SensorConnectionState.Connecting
                    is State.Disconnected -> SensorConnectionState.Disconnected
                }

                if (state is State.Connected) {
                    observationJob?.cancel()
                    observationJob = launch {
                        try {
                            val handler = BikeDeviceHandler(peripheral)
                            ftmsControlPoint = FtmsControlPoint(peripheral)
                            
                            handler.bikeData
                                .onEach { data ->
                                    _power.value = data.power
                                    _speed.value = data.speed
                                    _resistance.value = data.resistance
                                }
                                .collect()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Napier.w(tag = "Bike", throwable = e) { "Observation failed" }
                        }
                    }
                } else if (state is State.Disconnected) {
                    observationJob?.cancel()
                    ftmsControlPoint = null
                }
            }
        }
    }

    override suspend fun setResistance(level: Int) {
        ftmsControlPoint?.setResistance(level)
    }
}
