package com.rideflow.presentation.history

import com.rideflow.domain.model.Train

data class HistoryUiState(
    val trains: List<Train> = emptyList(),
    val isLoading: Boolean = false
)

sealed class HistoryIntent {
    object LoadHistory : HistoryIntent()
    data class DeleteTrain(val id: Long) : HistoryIntent()
}
