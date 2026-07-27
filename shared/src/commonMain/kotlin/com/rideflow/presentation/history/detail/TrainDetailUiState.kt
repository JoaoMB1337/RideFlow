package com.rideflow.presentation.history.detail

import com.rideflow.domain.model.Train
import com.rideflow.domain.model.TrainingData

data class TrainDetailUiState(
    val train: Train? = null,
    val samples: List<TrainingData> = emptyList(),
    val isLoading: Boolean = false
)

sealed class TrainDetailIntent {
    data class LoadDetail(val trainId: Long) : TrainDetailIntent()
    object DeleteTrain : TrainDetailIntent()
}
