package com.rideflow.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.rideflow.data.local.mapper.toDomain
import com.rideflow.db.RideFlowDb
import com.rideflow.domain.model.Train
import com.rideflow.domain.model.TrainingData
import com.rideflow.domain.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorkoutRepositoryImpl(
    database: RideFlowDb
) : WorkoutRepository {
    private val queries = database.rideFlowDbQueries

    override suspend fun startTrain(date: Long): Long {
        queries.insertTrain(date)
        return queries.lastInsertRowId().executeAsOne()
    }

    override suspend fun insertTrainingData(data: TrainingData) {
        queries.insertTrainingData(
            idTrain = data.idTrain,
            timestampOffset = data.timestampOffset,
            heartRate = data.heartRate?.toLong(),
            power = data.power?.toLong(),
            speed = data.speed,
            resistance = data.resistance?.toLong()
        )
    }

    override suspend fun updateTrainSummary(train: Train) {
        queries.updateTrainSummary(
            duration = train.duration,
            avgHeartRate = train.avgHeartRate.toLong(),
            avgPower = train.avgPower.toLong(),
            maxHeartRate = train.maxHeartRate.toLong(),
            maxPower = train.maxPower.toLong(),
            id = train.id
        )
    }

    override fun getAllTrains(): Flow<List<Train>> {
        return queries.selectAllTrains()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTrainById(id: Long): Train? {
        return queries.selectTrainById(id).executeAsOneOrNull()?.toDomain()
    }

    override fun getTrainingDataForTrain(trainId: Long): Flow<List<TrainingData>> {
        return queries.selectDataForTrain(trainId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun deleteTrain(id: Long) {
        queries.deleteTrain(id)
    }
}
