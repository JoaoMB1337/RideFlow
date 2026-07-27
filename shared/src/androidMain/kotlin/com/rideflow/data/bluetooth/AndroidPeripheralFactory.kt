package com.rideflow.data.bluetooth

import com.juul.kable.Peripheral
import com.juul.kable.peripheral

class AndroidPeripheralFactory : PeripheralFactory {
    override fun createPeripheral(address: String): Peripheral {
        // In Kable, the top level peripheral function works on Android with address
        return peripheral(address)
    }
}
