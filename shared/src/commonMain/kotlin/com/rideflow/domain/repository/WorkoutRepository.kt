package com.rideflow.domain.repository

import com.rideflow.domain.model.Train
import com.rideflow.domain.model.TrainingData
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    suspend fun startTrain(date: Long): Long
    suspend fun insertTrainingData(data: TrainingData)
    suspend fun updateTrainSummary(train: Train)
    fun getAllTrains(): Flow<List<Train>>
    suspend fun getTrainById(id: Long): Train?
    fun getTrainingDataForTrain(trainId: Long): Flow<List<TrainingData>>
    suspend fun deleteTrain(id: Long)
}
