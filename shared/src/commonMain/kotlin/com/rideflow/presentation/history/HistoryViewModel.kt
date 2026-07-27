package com.rideflow.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideflow.domain.repository.WorkoutRepository
import com.rideflow.domain.usecase.GetWorkoutHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val getWorkoutHistoryUseCase: GetWorkoutHistoryUseCase,
    private val workoutRepository: WorkoutRepository // For delete, could also be a UseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        _uiState.update { it.copy(isLoading = true) }
        getWorkoutHistoryUseCase()
            .onEach { trains ->
                _uiState.update { it.copy(trains = trains, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: HistoryIntent) {
        when (intent) {
            HistoryIntent.LoadHistory -> loadHistory()
            is HistoryIntent.DeleteTrain -> deleteTrain(intent.id)
        }
    }

    private fun deleteTrain(id: Long) {
        viewModelScope.launch {
            workoutRepository.deleteTrain(id)
        }
    }
}
