package com.rideflow.domain.usecase

import com.rideflow.domain.repository.SensorRepository

class AdjustResistanceUseCase(
    private val sensorRepository: SensorRepository
) {
    suspend operator fun invoke(level: Int) {
        sensorRepository.setResistance(level)
    }
}
