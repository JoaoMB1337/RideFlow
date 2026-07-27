package com.rideflow.domain.usecase

import com.rideflow.domain.model.Train
import com.rideflow.domain.model.TrainingData
import com.rideflow.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class WorkoutDetail(
    val train: Train,
    val samples: List<TrainingData>
)

class GetWorkoutDetailUseCase(
    private val repository: WorkoutRepository
) {
    suspend fun getTrain(id: Long): Train? = repository.getTrainById(id)
    
    fun getSamples(id: Long): Flow<List<TrainingData>> = repository.getTrainingDataForTrain(id)
}
