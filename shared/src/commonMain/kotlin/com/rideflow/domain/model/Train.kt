package com.rideflow.domain.model

import kotlinx.datetime.Instant

data class Train(
    val id: Long = 0,
    val date: Instant,
    val duration: Long = 0,
    val distance: Double = 0.0,
    val avgHeartRate: Int = 0,
    val avgPower: Int = 0,
    val maxHeartRate: Int = 0,
    val maxPower: Int = 0
)
