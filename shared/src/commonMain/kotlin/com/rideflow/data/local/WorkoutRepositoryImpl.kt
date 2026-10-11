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
import kotlinx.coroutines.withContext

class WorkoutRepositoryImpl(
    database: RideFlowDb
) : WorkoutRepository {
    private val queries = database.rideFlowDbQueries

    // O INSERT e o last_insert_rowid() têm de partilhar a mesma transação, senão um insert
    // concorrente pode fazer com que devolvamos o id errado.
    override suspend fun startTrain(date: Long): Long = withContext(Dispatchers.IO) {
        queries.transactionWithResult {
            queries.insertTrain(date)
            queries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun insertTrainingData(data: TrainingData): Unit = withContext(Dispatchers.IO) {
        queries.insertTrainingData(
            idTrain = data.idTrain,
            timestampOffset = data.timestampOffset,
            heartRate = data.heartRate?.toLong(),
            power = data.power?.toLong(),
            speed = data.speed,
            distance = data.distance,
            resistance = data.resistance?.toLong()
        )
    }

    override suspend fun updateTrainSummary(train: Train): Unit = withContext(Dispatchers.IO) {
        queries.updateTrainSummary(
            duration = train.duration,
            distance = train.distance,
            avgHeartRate = train.avgHeartRate.toLong(),
            avgPower = train.avgPower.toLong(),
            avgSpeed = train.avgSpeed,
            avgResistance = train.avgResistance.toLong(),
            maxHeartRate = train.maxHeartRate.toLong(),
            maxPower = train.maxPower.toLong(),
            maxSpeed = train.maxSpeed,
            maxResistance = train.maxResistance.toLong(),
            calories = train.calories.toLong(),
            id = train.id
        )
    }

    override suspend fun updateTrainInfo(id: Long, name: String?, notes: String?): Unit =
        withContext(Dispatchers.IO) {
            queries.updateTrainInfo(name = name?.ifBlank { null }, notes = notes?.ifBlank { null }, id = id)
        }

    override suspend fun getUnfinishedTrainIds(): List<Long> = withContext(Dispatchers.IO) {
        queries.selectUnfinishedTrainIds().executeAsList()
    }

    override fun getAllTrains(): Flow<List<Train>> {
        return queries.selectAllTrains()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTrainById(id: Long): Train? = withContext(Dispatchers.IO) {
        queries.selectTrainById(id).executeAsOneOrNull()?.toDomain()
    }

    override fun getTrainingDataForTrain(trainId: Long): Flow<List<TrainingData>> {
        return queries.selectDataForTrain(trainId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun deleteTrain(id: Long): Unit = withContext(Dispatchers.IO) {
        queries.deleteTrain(id)
    }
}
