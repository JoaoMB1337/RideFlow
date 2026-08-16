package com.rideflow.data.local

import com.rideflow.domain.model.Train
import com.rideflow.domain.model.TrainingData
import com.rideflow.domain.repository.WorkoutRepository
import kotlin.time.Instant
import kotlin.time.Clock
import kotlin.math.sin
import kotlin.random.Random

class WorkoutSeeder(private val repository: WorkoutRepository) {

    suspend fun seed(count: Int = 3) {
        val now = Clock.System.now().toEpochMilliseconds()
        
        repeat(count) { workoutIndex ->
            // Seeding data from the last few days
            val startTime = now - (workoutIndex * 86400000L) - (Random.nextLong(0, 3600000L))
            val trainId = repository.startTrain(startTime)
            
            val durationSeconds = Random.nextLong(1200, 3600) // 20 to 60 mins
            
            var totalDistance = 0.0
            var heartRateSum = 0
            var powerSum = 0
            var speedSum = 0.0
            var resistanceSum = 0
            
            var maxHeartRate = 0
            var maxPower = 0
            var maxSpeed = 0.0
            var maxResistance = 0

            for (i in 0 until durationSeconds) {
                // Realistic data generation
                val t = i.toDouble()
                val hr = (130 + 25 * sin(t / 100) + Random.nextInt(-3, 4)).toInt()
                val pwr = (180 + 120 * sin(t / 80) + Random.nextInt(-15, 16)).toInt().coerceAtLeast(0)
                val spd = (28 + 8 * sin(t / 150) + Random.nextDouble(-1.5, 1.5)).coerceAtLeast(0.0)
                val res = (12 + 6 * sin(t / 200) + Random.nextInt(-1, 2)).toInt().coerceIn(1, 24)
                
                val distIncrement = spd / 3600.0
                totalDistance += distIncrement
                
                val data = TrainingData(
                    id = 0,
                    idTrain = trainId,
                    timestampOffset = i,
                    heartRate = hr,
                    power = pwr,
                    speed = spd,
                    distance = totalDistance,
                    resistance = res
                )
                
                repository.insertTrainingData(data)
                
                heartRateSum += hr
                powerSum += pwr
                speedSum += spd
                resistanceSum += res
                
                maxHeartRate = maxOf(maxHeartRate, hr)
                maxPower = maxOf(maxPower, pwr)
                maxSpeed = maxOf(maxSpeed, spd)
                maxResistance = maxOf(maxResistance, res)
            }
            
            val summary = Train(
                id = trainId,
                date = Instant.fromEpochMilliseconds(startTime),
                duration = durationSeconds,
                distance = totalDistance,
                avgHeartRate = heartRateSum / durationSeconds.toInt(),
                avgPower = powerSum / durationSeconds.toInt(),
                avgSpeed = speedSum / durationSeconds.toDouble(),
                avgResistance = resistanceSum / durationSeconds.toInt(),
                maxHeartRate = maxHeartRate,
                maxPower = maxPower,
                maxSpeed = maxSpeed,
                maxResistance = maxResistance
            )
            
            repository.updateTrainSummary(summary)
        }
    }
}
