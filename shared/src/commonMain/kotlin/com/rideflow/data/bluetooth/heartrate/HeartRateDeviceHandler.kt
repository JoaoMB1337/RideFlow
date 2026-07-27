package com.rideflow.data.bluetooth.heartrate

import com.juul.kable.Peripheral
import com.juul.kable.characteristicOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HeartRateDeviceHandler(private val peripheral: Peripheral) {
    
    private val heartRateMeasurement = characteristicOf(
        service = "180d",
        characteristic = "2a37"
    )

    val heartRate: Flow<Int> = peripheral.observe(heartRateMeasurement).map { data ->
        decodeHeartRate(data)
    }

    private fun decodeHeartRate(data: ByteArray): Int {
        if (data.isEmpty()) return 0
        val flags = data[0].toInt()
        return if (flags and 0x01 == 0) {
            data[1].toInt() and 0xFF
        } else {
            (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        }
    }
}
