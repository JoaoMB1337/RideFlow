package com.rideflow.domain.model

data class TrainingData(
    val id: Long = 0,
    val idTrain: Long,
    val timestampOffset: Long,
    val heartRate: Int?,
    val power: Int?,
    val speed: Double?,
    val distance: Double?,
    val resistance: Int?
)
