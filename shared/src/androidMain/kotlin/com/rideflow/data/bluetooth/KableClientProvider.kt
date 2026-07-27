package com.rideflow.data.bluetooth

import android.content.Context
import com.juul.kable.Peripheral
import com.juul.kable.peripheral
import kotlinx.coroutines.CoroutineScope

class KableClientProvider(private val context: Context) {
    fun createPeripheral(address: String, scope: CoroutineScope): Peripheral {
        return scope.peripheral(address)
    }
}
