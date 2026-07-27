package com.rideflow.presentation.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideflow.domain.repository.SensorRepository
import com.rideflow.domain.usecase.RecordSampleUseCase
import com.rideflow.domain.usecase.StartWorkoutUseCase
import com.rideflow.domain.usecase.StopWorkoutUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val sensorRepository: SensorRepository,
    private val startWorkoutUseCase: StartWorkoutUseCase,
    private val recordSampleUseCase: RecordSampleUseCase,
    private val stopWorkoutUseCase: StopWorkoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var samplingJob: Job? = null

    init {
        observeSensors()
    }

    private fun observeSensors() {
        sensorRepository.heartRateState
            .onEach { state -> _uiState.update { it.copy(heartRateConnection = state) } }
            .launchIn(viewModelScope)

        sensorRepository.bikeState
            .onEach { state -> _uiState.update { it.copy(bikeConnection = state) } }
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
            WorkoutIntent.StartWorkout -> startWorkout()
            WorkoutIntent.StopWorkout -> stopWorkout()
            is WorkoutIntent.AdjustResistance -> adjustResistance(intent.level)
        }
    }

    private fun startWorkout() {
        viewModelScope.launch {
            val trainId = startWorkoutUseCase()
            _uiState.update { it.copy(isWorkoutActive = true, currentTrainId = trainId, durationSeconds = 0) }
            startTimer()
            startSampling(trainId)
        }
    }

    private fun stopWorkout() {
        viewModelScope.launch {
            val state = _uiState.value
            state.currentTrainId?.let { id ->
                stopWorkoutUseCase(id, state.durationSeconds)
            }
            timerJob?.cancel()
            samplingJob?.cancel()
            _uiState.update { it.copy(isWorkoutActive = false, currentTrainId = null) }
        }
    }

    private fun adjustResistance(level: Int) {
        viewModelScope.launch {
            sensorRepository.setResistance(level)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    private fun startSampling(trainId: Long) {
        samplingJob?.cancel()
        samplingJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                recordSampleUseCase(
                    trainId = trainId,
                    offset = state.durationSeconds,
                    heartRate = state.heartRate,
                    power = state.power,
                    speed = state.speed,
                    resistance = state.resistance
                )
            }
        }
    }
}
