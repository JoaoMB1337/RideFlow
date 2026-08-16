package com.rideflow.data.bluetooth.bike

import com.juul.kable.Peripheral
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import io.github.aakira.napier.Napier
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class FtmsControlPoint(private val peripheral: Peripheral) {

    private var hasControl = false

    private val controlPoint = characteristicOf(
        service = Uuid.parse("00001826-0000-1000-8000-00805f9b34fb"),
        characteristic = Uuid.parse("00002ad9-0000-1000-8000-00805f9b34fb")
    )

    suspend fun requestControl(): Boolean {
        return try {
            peripheral.write(controlPoint, byteArrayOf(OP_REQUEST_CONTROL), WriteType.WithResponse)
            hasControl = true
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w(tag = "Ftms", throwable = e) { "Request control failed" }
            hasControl = false
            false
        }
    }

    suspend fun setResistance(level: Int) {
        try {
            if (!hasControl && !requestControl()) {
                Napier.w(tag = "Ftms") { "Skipping setResistance($level): no control of the bike" }
                return
            }
            val data = byteArrayOf(OP_SET_RESISTANCE, level.coerceIn(0, 255).toByte())
            peripheral.write(controlPoint, data, WriteType.WithResponse)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w(tag = "Ftms", throwable = e) { "setResistance($level) failed" }
            hasControl = false
        }
    }

    private companion object {
        const val OP_REQUEST_CONTROL: Byte = 0x00
        const val OP_SET_RESISTANCE: Byte = 0x04
    }
}
