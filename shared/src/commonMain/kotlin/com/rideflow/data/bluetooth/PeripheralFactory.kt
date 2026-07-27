package com.rideflow.data.bluetooth

import com.juul.kable.Peripheral

interface PeripheralFactory {
    fun createPeripheral(address: String): Peripheral
}
