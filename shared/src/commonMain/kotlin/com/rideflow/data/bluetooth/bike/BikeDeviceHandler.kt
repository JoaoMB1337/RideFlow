package com.rideflow.data.bluetooth.bike

import com.juul.kable.Peripheral
import com.juul.kable.characteristicOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BikeDeviceHandler(private val peripheral: Peripheral) {

    private val indoorBikeData = characteristicOf(
        service = "1826",
        characteristic = "2ad2"
    )

    data class BikeData(
        val speed: Double? = null,
        val power: Int? = null,
        val resistance: Int? = null
    )

    val bikeData: Flow<BikeData> = peripheral.observe(indoorBikeData).map { data ->
        decodeBikeData(data)
    }

    private fun decodeBikeData(data: ByteArray): BikeData {
        // FTMS Indoor Bike Data decoding is complex due to flags
        // Simplified version for now
        if (data.size < 4) return BikeData()
        
        val flags = (data[1].toInt() shl 8) or (data[0].toInt() and 0xFF)
        var offset = 2
        
        var speed: Double? = null
        if (flags and 0x01 == 0) { // More flags logic needed
            speed = ((data[offset+1].toInt() shl 8) or (data[offset].toInt() and 0xFF)) / 100.0
            offset += 2
        }

        // Skipping some fields to get to Power
        // This is a simplified placeholder. A real FTMS decoder is needed.
        
        return BikeData(speed = speed)
    }
}
