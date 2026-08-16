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

    /**
     * Cursor sobre o pacote Indoor Bike Data (0x2AD2).
     *
     * Todos os `read*` devolvem `null` — e param o parsing — quando não há bytes suficientes,
     * para que uma trama truncada de uma bicicleta menos conforme não rebente com
     * IndexOutOfBoundsException dentro do Flow de observação.
     */
    private class FtmsCursor(private val data: ByteArray) {
        var offset = 0
            private set

        fun has(bytes: Int): Boolean = offset + bytes <= data.size

        fun skip(bytes: Int) {
            offset += bytes
        }

        private fun byteAt(i: Int): Int = data[i].toInt() and 0xFF

        fun readUInt8(): Int? = if (has(1)) byteAt(offset).also { offset += 1 } else null

        fun readUInt16(): Int? =
            if (has(2)) ((byteAt(offset + 1) shl 8) or byteAt(offset)).also { offset += 2 } else null

        /** sint16 little-endian — negativos são válidos (ex.: potência em roda livre). */
        fun readSInt16(): Int? = readUInt16()?.toShort()?.toInt()

        fun readUInt24(): Int? =
            if (has(3)) {
                ((byteAt(offset + 2) shl 16) or (byteAt(offset + 1) shl 8) or byteAt(offset))
                    .also { offset += 3 }
            } else {
                null
            }
    }

    private fun decodeBikeData(data: ByteArray): BikeData {
        if (data.size < 2) return BikeData()

        val cursor = FtmsCursor(data)
        val flags = cursor.readUInt16() ?: return BikeData()

        // Bit 0 é "More Data": quando está a 0, a velocidade instantânea está presente.
        val speed = if (flags and FLAG_MORE_DATA == 0) {
            cursor.readUInt16()?.let { it / 100.0 }
        } else {
            null
        }

        if (flags and FLAG_AVG_SPEED != 0) cursor.skip(2)

        val cadence = if (flags and FLAG_INST_CADENCE != 0) {
            cursor.readUInt16()?.let { it / 2.0 }
        } else {
            null
        }

        if (flags and FLAG_AVG_CADENCE != 0) cursor.skip(2)

        val distance = if (flags and FLAG_TOTAL_DISTANCE != 0) cursor.readUInt24() else null

        // Resistance Level e Instantaneous Power são sint16 na spec FTMS. Lidos como unsigned,
        // um valor negativo (-5 W em roda livre) virava 65531 W e envenenava médias e máximos.
        val resistance = if (flags and FLAG_RESISTANCE != 0) cursor.readSInt16() else null
        val power = if (flags and FLAG_INST_POWER != 0) cursor.readSInt16() else null

        if (flags and FLAG_AVG_POWER != 0) cursor.skip(2)
        if (flags and FLAG_EXPENDED_ENERGY != 0) cursor.skip(5)

        val heartRate = if (flags and FLAG_HEART_RATE != 0) cursor.readUInt8() else null

        return BikeData(
            speed = speed,
            cadence = cadence,
            power = power,
            resistance = resistance,
            heartRate = heartRate,
            distance = distance
        )
    }

    private companion object {
        const val FLAG_MORE_DATA = 0x001
        const val FLAG_AVG_SPEED = 0x002
        const val FLAG_INST_CADENCE = 0x004
        const val FLAG_AVG_CADENCE = 0x008
        const val FLAG_TOTAL_DISTANCE = 0x010
        const val FLAG_RESISTANCE = 0x020
        const val FLAG_INST_POWER = 0x040
        const val FLAG_AVG_POWER = 0x080
        const val FLAG_EXPENDED_ENERGY = 0x100
        const val FLAG_HEART_RATE = 0x200
    }
}
