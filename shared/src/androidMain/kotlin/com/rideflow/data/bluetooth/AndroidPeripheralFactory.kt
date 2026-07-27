package com.rideflow.data.bluetooth

import com.juul.kable.Peripheral

class AndroidPeripheralFactory : PeripheralFactory {
    override fun createPeripheral(address: String): Peripheral {
        // According to Kable documentation for newer versions
        return Peripheral(address)
    }
}
