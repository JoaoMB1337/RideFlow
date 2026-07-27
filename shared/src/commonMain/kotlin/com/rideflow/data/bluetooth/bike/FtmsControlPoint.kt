package com.rideflow.data.bluetooth.bike

import com.juul.kable.Peripheral
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf

class FtmsControlPoint(private val peripheral: Peripheral) {

    private val controlPoint = characteristicOf(
        service = "1826",
        characteristic = "2ad9"
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
