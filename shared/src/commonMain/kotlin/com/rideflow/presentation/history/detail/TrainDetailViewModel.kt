package com.rideflow.presentation.history.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideflow.domain.repository.WorkoutRepository
import com.rideflow.domain.usecase.GetWorkoutDetailUseCase
import com.rideflow.util.SharingService
import com.rideflow.util.formatDuration
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TrainDetailViewModel(
    private val getWorkoutDetailUseCase: GetWorkoutDetailUseCase,
    private val workoutRepository: WorkoutRepository,
    private val settings: Settings,
    private val sharingService: SharingService? = null // Optional for now
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainDetailUiState())
    val uiState: StateFlow<TrainDetailUiState> = _uiState.asStateFlow()

    fun onIntent(intent: TrainDetailIntent) {
        when (intent) {
            is TrainDetailIntent.LoadDetail -> loadDetail(intent.trainId)
            TrainDetailIntent.DeleteTrain -> deleteTrain()
            TrainDetailIntent.ShareSummary -> shareSummary()
        }
    }

    private fun shareSummary() {
        val train = uiState.value.train ?: return
        val summaryText = "Ride Flow Workout Summary\n" +
                "Date: ${train.date.toString().substringBefore("T")}\n" +
                "Duration: ${formatDuration(train.duration)}\n" +
                "Distance: ${train.distance} km\n" +
                "Avg HR: ${train.avgHeartRate} bpm\n" +
                "Avg Power: ${train.avgPower} W"
        
        sharingService?.shareText(summaryText)
        
        // Signal the UI to trigger image capture of the summary and graphs.
        // In the Screen, use graphicsLayer to record the view and convert to image.
        _uiState.update { it.copy(isSharing = true) }
    }

    fun onShareComplete() {
        _uiState.update { it.copy(isSharing = false) }
    }

    private fun deleteTrain() {
        _uiState.value.train?.id?.let { id ->
            viewModelScope.launch {
                workoutRepository.deleteTrain(id)
            }
        }
    }

    private fun loadDetail(trainId: Long) {
        val userMaxHr = settings.getIntOrNull("user_max_hr")
        _uiState.update { it.copy(isLoading = true, userMaxHeartRate = userMaxHr) }
        
        viewModelScope.launch {
            val train = getWorkoutDetailUseCase.getTrain(trainId)
            _uiState.update { it.copy(train = train) }
        }

        getWorkoutDetailUseCase.getSamples(trainId)
            .onEach { samples ->
                _uiState.update { it.copy(samples = samples, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }
}
