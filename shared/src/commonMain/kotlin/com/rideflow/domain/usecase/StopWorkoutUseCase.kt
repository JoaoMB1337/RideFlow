package com.rideflow.domain.usecase

import com.rideflow.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.first

class StopWorkoutUseCase(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(trainId: Long, duration: Long) {
        val samples = repository.getTrainingDataForTrain(trainId).first()
        
        if (samples.isEmpty()) return

        // average() sobre lista vazia devolve NaN — sem este guard, um sensor que nunca
        // reportou nada gravava NaN na coluna REAL e a UI mostrava "NaNkm/h".
        val avgHeartRate = samples.mapNotNull { it.heartRate }.averageOrZero().toInt()
        val avgPower = samples.mapNotNull { it.power }.averageOrZero().toInt()
        val avgSpeed = samples.mapNotNull { it.speed }.averageOrZero()
        val avgResistance = samples.mapNotNull { it.resistance }.averageOrZero().toInt()

        val maxHeartRate = samples.mapNotNull { it.heartRate }.maxOrNull() ?: 0
        val maxPower = samples.mapNotNull { it.power }.maxOrNull() ?: 0
        val maxSpeed = samples.mapNotNull { it.speed }.maxOrNull() ?: 0.0
        val maxResistance = samples.mapNotNull { it.resistance }.maxOrNull() ?: 0

        val distance = samples.lastOrNull()?.distance ?: 0.0

        val train = repository.getTrainById(trainId)?.copy(
            duration = duration,
            distance = distance,
            avgHeartRate = avgHeartRate,
            avgPower = avgPower,
            avgSpeed = avgSpeed,
            avgResistance = avgResistance,
            maxHeartRate = maxHeartRate,
            maxPower = maxPower,
            maxSpeed = maxSpeed,
            maxResistance = maxResistance
        )

        train?.let { repository.updateTrainSummary(it) }
    }
}

private fun List<Number>.averageOrZero(): Double =
    if (isEmpty()) 0.0 else sumOf { it.toDouble() } / size
