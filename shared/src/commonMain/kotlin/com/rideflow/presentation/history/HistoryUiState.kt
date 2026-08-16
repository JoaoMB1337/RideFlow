package com.rideflow.presentation.history

import com.rideflow.domain.model.Train

data class HistoryUiState(
    val trains: List<Train> = emptyList(),
    val isLoading: Boolean = false,
    val selectedTrainIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

sealed class HistoryIntent {
    object LoadHistory : HistoryIntent()
    data class DeleteTrains(val ids: Set<Long>) : HistoryIntent()
    data class ToggleSelection(val trainId: Long) : HistoryIntent()
    object ClearSelection : HistoryIntent()
    object EnterSelectionMode : HistoryIntent()
    data class ShareWorkout(val trainId: Long) : HistoryIntent()
}
