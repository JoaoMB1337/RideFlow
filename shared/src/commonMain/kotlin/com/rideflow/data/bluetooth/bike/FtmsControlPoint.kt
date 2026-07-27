package com.rideflow.data.bluetooth.bike

import com.juul.kable.Peripheral
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FtmsControlPoint(private val peripheral: Peripheral) {

    private val controlPoint = characteristicOf(
        service = Uuid.parse("00001826-0000-1000-8000-00805f9b34fb"),
        characteristic = Uuid.parse("00002ad9-0000-1000-8000-00805f9b34fb")
    )

    suspend fun requestControl() {
        peripheral.write(controlPoint, byteArrayOf(0x00), WriteType.WithResponse)
    }

    suspend fun setResistance(level: Int) {
        // level should be 0-100 or specific to machine
        val data = byteArrayOf(0x04, level.toByte())
        peripheral.write(controlPoint, data, WriteType.WithResponse)
    }
}
