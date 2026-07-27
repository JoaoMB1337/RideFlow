package com.rideflow.data.bluetooth

import com.juul.kable.Scanner
import com.juul.kable.Advertisement
import kotlinx.coroutines.flow.Flow

class BleScanner {
    private val scanner = Scanner {
        // Configure filters if needed
    }

    val advertisements: Flow<Advertisement> = scanner.advertisements
}
