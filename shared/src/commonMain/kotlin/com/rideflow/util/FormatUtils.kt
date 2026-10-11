package com.rideflow.util

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60

    val hh = h.toString().padStart(2, '0')
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')

    return if (h > 0) "$hh:$mm:$ss" else "$mm:$ss"
}

/** Valor e unidade em separado, para a UI mostrar o número em grande e a unidade em pequeno. */
fun distanceParts(km: Double): Pair<String, String> {
    return if (km < 1.0) {
        "${(km * 1000).toInt()}" to "m"
    } else {
        "${((km * 100).toInt() / 100.0)}" to "km"
    }
}

fun formatDistance(km: Double): String = distanceParts(km).let { (value, unit) -> "$value$unit" }

fun formatSpeedValue(kmh: Double): String = "${((kmh * 10).toInt() / 10.0)}"

fun formatSpeed(kmh: Double): String = "${formatSpeedValue(kmh)}km/h"

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** Ex.: "11 Oct 2026", no fuso horário do dispositivo. */
fun formatDate(instant: Instant): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.day} ${MONTHS[dt.month.ordinal]} ${dt.year}"
}

/** Ex.: "18:05", no fuso horário do dispositivo. */
fun formatTimeOfDay(instant: Instant): String {
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
}
