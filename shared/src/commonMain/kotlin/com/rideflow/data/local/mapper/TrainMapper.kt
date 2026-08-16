package com.rideflow.data.local.mapper

import com.rideflow.domain.model.Train
import com.rideflow.domain.model.TrainingData
import kotlinx.datetime.Instant

fun com.rideflow.db.Train.toDomain(): Train {
    return Train(
        id = id,
        date = Instant.fromEpochMilliseconds(date),
        duration = duration ?: 0,
        distance = distance ?: 0.0,
        avgHeartRate = avgHeartRate?.toInt() ?: 0,
        avgPower = avgPower?.toInt() ?: 0,
        avgSpeed = avgSpeed ?: 0.0,
        avgResistance = avgResistance?.toInt() ?: 0,
        maxHeartRate = maxHeartRate?.toInt() ?: 0,
        maxPower = maxPower?.toInt() ?: 0,
        maxSpeed = maxSpeed ?: 0.0,
        maxResistance = maxResistance?.toInt() ?: 0
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
        distance = distance,
        resistance = resistance?.toInt()
    )
}
