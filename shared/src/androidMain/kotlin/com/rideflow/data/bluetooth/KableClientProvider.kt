package com.rideflow.data.bluetooth

import android.content.Context
import com.juul.kable.Peripheral

class KableClientProvider(private val context: Context) {
    fun createPeripheral(address: String): Peripheral {
        return Peripheral(address)
    }
}
