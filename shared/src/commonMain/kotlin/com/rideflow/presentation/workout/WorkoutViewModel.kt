package com.rideflow.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideflow.domain.model.SensorConnectionState
import com.rideflow.domain.repository.SensorRepository
import com.rideflow.domain.session.WorkoutSessionManager
import com.rideflow.domain.usecase.GetWorkoutHistoryUseCase
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

private const val KEY_HR_ADDRESS = "hr_device_address"
private const val KEY_BIKE_ADDRESS = "bike_device_address"
private const val KEY_HR_NAME = "hr_device_name"
private const val KEY_BIKE_NAME = "bike_device_name"

/**
 * Casca fina: projeta o estado do [WorkoutSessionManager] e dos sensores para a UI e encaminha
 * intents. Não é dono de nenhum timer nem de nenhuma gravação — se este ViewModel morrer
 * (navegação, rotação, Activity destruída), o treino continua a decorrer.
 */
class WorkoutViewModel(
    private val sensorRepository: SensorRepository,
    private val sessionManager: WorkoutSessionManager,
    private val settings: Settings,
    private val getHistory: GetWorkoutHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        observeSensors()
        observeHistory()
        autoConnectDevices()
    }

    private fun observeHistory() {
        getHistory()
            .onEach { trains ->
                val since = Clock.System.now() - 7.days
                val week = trains.filter { it.date >= since }
                _uiState.update {
                    it.copy(
                        lastRide = trains.maxByOrNull { t -> t.date },
                        weekRides = week.size,
                        weekSeconds = week.sumOf { t -> t.duration },
                        weekKm = week.sumOf { t -> t.distance }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSession() {
        sessionManager.session
            .onEach { session ->
                _uiState.update {
                    it.copy(
                        isWorkoutActive = session != null,
                        currentTrainId = session?.trainId,
                        durationSeconds = session?.durationSeconds ?: 0,
                        distance = session?.distanceKm ?: 0.0
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun autoConnectDevices() {
        val hrAddr = settings.getStringOrNull(KEY_HR_ADDRESS)
        val bikeAddr = settings.getStringOrNull(KEY_BIKE_ADDRESS)

        viewModelScope.launch {
            hrAddr?.let { sensorRepository.connectHeartRateSensor(it) }
            bikeAddr?.let { sensorRepository.connectBikeSensor(it) }
        }
    }

    private fun observeSensors() {
        sensorRepository.heartRateState
            .onEach { state ->
                _uiState.update {
                    it.copy(
                        heartRateConnection = state,
                        heartRateDeviceName = settings.getStringOrNull(KEY_HR_NAME),
                        heartRate = if (state is SensorConnectionState.Connected) it.heartRate else null
                    )
                }
            }
            .launchIn(viewModelScope)

        sensorRepository.bikeState
            .onEach { state ->
                _uiState.update {
                    it.copy(
                        bikeConnection = state,
                        bikeDeviceName = settings.getStringOrNull(KEY_BIKE_NAME),
                        power = if (state is SensorConnectionState.Connected) it.power else null,
                        speed = if (state is SensorConnectionState.Connected) it.speed else null
                    )
                }
            }
            .launchIn(viewModelScope)

        sensorRepository.heartRate
            .onEach { hr -> _uiState.update { it.copy(heartRate = hr) } }
            .launchIn(viewModelScope)

        sensorRepository.power
            .onEach { pwr -> _uiState.update { it.copy(power = pwr) } }
            .launchIn(viewModelScope)

        sensorRepository.speed
            .onEach { spd -> _uiState.update { it.copy(speed = spd) } }
            .launchIn(viewModelScope)

        sensorRepository.resistance
            .onEach { res -> _uiState.update { it.copy(resistance = res ?: it.resistance) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: WorkoutIntent) {
        when (intent) {
            WorkoutIntent.StartWorkout -> viewModelScope.launch { startWorkout() }
            WorkoutIntent.StopWorkout -> viewModelScope.launch { sessionManager.stop() }
            is WorkoutIntent.AdjustResistance -> adjustResistance(intent.level)
        }
    }

    private suspend fun startWorkout() {
        try {
            sessionManager.start()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e(tag = "Workout", throwable = e) { "Failed to start workout" }
        }
    }

    private fun adjustResistance(level: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(resistance = level) }
            sensorRepository.setResistance(level)
        }
    }
}
