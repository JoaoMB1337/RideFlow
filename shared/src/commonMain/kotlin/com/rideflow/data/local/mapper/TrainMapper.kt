package com.rideflow.data.local.mapper

import com.rideflow.domain.model.Train
import com.rideflow.domain.model.TrainingData
import kotlinx.datetime.Instant

fun com.rideflow.db.Train.toDomain(): Train {
    return Train(
        id = id,
        date = Instant.fromEpochMilliseconds(date),
        duration = duration ?: 0,
        avgHeartRate = avgHeartRate?.toInt() ?: 0,
        avgPower = avgPower?.toInt() ?: 0,
        maxHeartRate = maxHeartRate?.toInt() ?: 0,
        maxPower = maxPower?.toInt() ?: 0
    )
}

fun com.rideflow.db.TrainingData.toDomain(): TrainingData {
    return TrainingData(
        id = id,
        idTrain = idTrain,
        timestampOffset = timestampOffset,
        heartRate = heartRate?.toInt(),
        power = power?.toInt(),
        speed = speed,
        resistance = resistance?.toInt()
    )
}
