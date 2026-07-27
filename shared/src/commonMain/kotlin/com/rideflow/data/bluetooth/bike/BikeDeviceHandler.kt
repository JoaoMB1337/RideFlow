package com.rideflow.data.bluetooth.bike

import com.juul.kable.Peripheral
import com.juul.kable.characteristicOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class BikeDeviceHandler(private val peripheral: Peripheral) {

    private val indoorBikeData = characteristicOf(
        service = Uuid.parse("00001826-0000-1000-8000-00805f9b34fb"),
        characteristic = Uuid.parse("00002ad2-0000-1000-8000-00805f9b34fb")
    )

    data class BikeData(
        val speed: Double? = null,
        val cadence: Double? = null,
        val power: Int? = null,
        val resistance: Int? = null,
        val heartRate: Int? = null,
        val distance: Int? = null
    )

    val bikeData: Flow<BikeData> = peripheral.observe(indoorBikeData).map { data ->
        decodeBikeData(data)
    }

    private fun decodeBikeData(data: ByteArray): BikeData {
        if (data.size < 2) return BikeData()
        
        val flags = (data[1].toInt() and 0xFF shl 8) or (data[0].toInt() and 0xFF)
        var offset = 2
        
        var speed: Double? = null
        if (flags and 0x01 == 0) { // Instantaneous Speed (0 is present in FTMS for this bit)
            speed = ((data[offset + 1].toInt() and 0xFF shl 8) or (data[offset].toInt() and 0xFF)) / 100.0
            offset += 2
        }

        // Average Speed (Bit 1)
        if (flags and 0x02 != 0) offset += 2

        var cadence: Double? = null
        if (flags and 0x04 != 0) { // Instantaneous Cadence
            cadence = ((data[offset + 1].toInt() and 0xFF shl 8) or (data[offset].toInt() and 0xFF)) / 2.0
            offset += 2
        }

        // Average Cadence (Bit 3)
        if (flags and 0x08 != 0) offset += 2

        var distance: Int? = null
        if (flags and 0x10 != 0) { // Total Distance
            distance = (data[offset + 2].toInt() and 0xFF shl 16) or 
                       (data[offset + 1].toInt() and 0xFF shl 8) or 
                       (data[offset].toInt() and 0xFF)
            offset += 3
        }

        var resistance: Int? = null
        if (flags and 0x20 != 0) { // Resistance Level
            resistance = (data[offset + 1].toInt() and 0xFF shl 8) or (data[offset].toInt() and 0xFF)
            offset += 2
        }

        var power: Int? = null
        if (flags and 0x40 != 0) { // Instantaneous Power
            power = (data[offset + 1].toInt() and 0xFF shl 8) or (data[offset].toInt() and 0xFF)
            offset += 2
        }

        // Average Power (Bit 7)
        if (flags and 0x80 != 0) offset += 2

        // Expended Energy (Bit 8)
        if (flags and 0x100 != 0) offset += 5

        var heartRate: Int? = null
        if (flags and 0x200 != 0) { // Heart Rate
            heartRate = data[offset].toInt() and 0xFF
            offset += 1
        }

        return BikeData(
            speed = speed,
            cadence = cadence,
            power = power,
            resistance = resistance,
            heartRate = heartRate,
            distance = distance
        )
    }
}
