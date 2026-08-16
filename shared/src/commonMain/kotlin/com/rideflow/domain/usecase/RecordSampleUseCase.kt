package com.rideflow.domain.usecase

import com.rideflow.domain.model.TrainingData
import com.rideflow.domain.repository.WorkoutRepository

class RecordSampleUseCase(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(
        trainId: Long,
        offset: Long,
        heartRate: Int?,
        power: Int?,
        speed: Double?,
        distance: Double?,
        resistance: Int?
    ) {
        val data = TrainingData(
            idTrain = trainId,
            timestampOffset = offset,
            heartRate = heartRate,
            power = power,
            speed = speed,
            distance = distance,
            resistance = resistance
        )
        repository.insertTrainingData(data)
    }
}
