package com.rideflow.presentation.history.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideflow.domain.repository.WorkoutRepository
import com.rideflow.domain.usecase.GetWorkoutDetailUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TrainDetailViewModel(
    private val getWorkoutDetailUseCase: GetWorkoutDetailUseCase,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainDetailUiState())
    val uiState: StateFlow<TrainDetailUiState> = _uiState.asStateFlow()

    fun onIntent(intent: TrainDetailIntent) {
        when (intent) {
            is TrainDetailIntent.LoadDetail -> loadDetail(intent.trainId)
            TrainDetailIntent.DeleteTrain -> deleteTrain()
        }
    }

    private fun deleteTrain() {
        _uiState.value.train?.id?.let { id ->
            viewModelScope.launch {
                workoutRepository.deleteTrain(id)
            }
        }
    }

    private fun loadDetail(trainId: Long) {
        _uiState.update { it.copy(isLoading = true) }
        
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
