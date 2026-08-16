package com.rideflow.util

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60

    val hh = h.toString().padStart(2, '0')
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')

    return if (h > 0) "$hh:$mm:$ss" else "$mm:$ss"
}

fun formatDistance(km: Double): String {
    return if (km < 1.0) {
        "${(km * 1000).toInt()}m"
    } else {
        "${((km * 100).toInt() / 100.0)}km"
    }
}

fun formatSpeed(kmh: Double): String {
    return "${((kmh * 10).toInt() / 10.0)}km/h"
}
