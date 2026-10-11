package com.rideflow.domain.repository

import com.rideflow.domain.model.Train
import com.rideflow.domain.model.TrainingData
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    suspend fun startTrain(date: Long): Long
    suspend fun insertTrainingData(data: TrainingData)
    suspend fun updateTrainSummary(train: Train)
    /** Nome e notas livres do treino; strings em branco são gravadas como `null`. */
    suspend fun updateTrainInfo(id: Long, name: String?, notes: String?)
    /** Treinos sem resumo escrito (ver `selectUnfinishedTrainIds`). */
    suspend fun getUnfinishedTrainIds(): List<Long>
    fun getAllTrains(): Flow<List<Train>>
    suspend fun getTrainById(id: Long): Train?
    fun getTrainingDataForTrain(trainId: Long): Flow<List<TrainingData>>
    suspend fun deleteTrain(id: Long)
}
