package com.rideflow.presentation.history.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideflow.domain.usecase.GetWorkoutDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrainDetailViewModel(
    private val getWorkoutDetailUseCase: GetWorkoutDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrainDetailUiState())
    val uiState: StateFlow<TrainDetailUiState> = _uiState.asStateFlow()

    fun onIntent(intent: TrainDetailIntent) {
        when (intent) {
            is TrainDetailIntent.LoadDetail -> loadDetail(intent.trainId)
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
