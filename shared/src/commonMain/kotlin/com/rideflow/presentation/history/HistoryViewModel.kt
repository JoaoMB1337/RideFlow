package com.rideflow.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rideflow.domain.repository.WorkoutRepository
import com.rideflow.domain.usecase.GetWorkoutHistoryUseCase
import com.rideflow.util.SharingService
import com.rideflow.util.formatDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val getWorkoutHistoryUseCase: GetWorkoutHistoryUseCase,
    private val workoutRepository: WorkoutRepository,
    private val sharingService: SharingService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
        //seedIfEmpty()
    }

    private fun seedIfEmpty() {
        viewModelScope.launch {
            // Pequeno delay para garantir que o loadHistory tenha começado a emitir
            kotlinx.coroutines.delay(1000)
            if (_uiState.value.trains.isEmpty() && !_uiState.value.isLoading) {
                val seeder = com.rideflow.data.local.WorkoutSeeder(workoutRepository)
                seeder.seed(5)
            }
        }
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
            is HistoryIntent.DeleteTrains -> deleteTrains(intent.ids)
            is HistoryIntent.ToggleSelection -> toggleSelection(intent.trainId)
            HistoryIntent.ClearSelection -> clearSelection()
            HistoryIntent.EnterSelectionMode -> enterSelectionMode()
            is HistoryIntent.ShareWorkout -> shareWorkout(intent.trainId)
        }
    }

    private fun shareWorkout(trainId: Long) {
        val train = _uiState.value.trains.find { it.id == trainId } ?: return
        val summary = "Ride Flow Workout\n" +
                "Date: ${train.date.toString().substringBefore("T")}\n" +
                "Duration: ${formatDuration(train.duration)}\n" +
                "Avg HR: ${train.avgHeartRate} bpm"
        
        sharingService?.shareText(summary)
    }

    private fun enterSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = true) }
    }

    private fun clearSelection() {
        _uiState.update { it.copy(isSelectionMode = false, selectedTrainIds = emptySet()) }
    }

    private fun toggleSelection(trainId: Long) {
        _uiState.update { state ->
            val newSelection = if (state.selectedTrainIds.contains(trainId)) {
                state.selectedTrainIds - trainId
            } else {
                state.selectedTrainIds + trainId
            }
            state.copy(
                selectedTrainIds = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }

    private fun deleteTrains(ids: Set<Long>) {
        viewModelScope.launch {
            ids.forEach { id ->
                workoutRepository.deleteTrain(id)
            }
            clearSelection()
        }
    }
}
