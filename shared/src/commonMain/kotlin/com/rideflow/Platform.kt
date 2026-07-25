package com.rideflow

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform