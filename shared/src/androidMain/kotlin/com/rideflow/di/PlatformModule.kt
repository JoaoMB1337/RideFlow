package com.rideflow.di

import com.rideflow.data.bluetooth.AndroidPeripheralFactory
import com.rideflow.data.bluetooth.BlePermissionHandler
import com.rideflow.data.bluetooth.PeripheralFactory
import com.rideflow.data.local.DatabaseDriverFactory
import org.koin.dsl.module

val platformModule = module {
    single { DatabaseDriverFactory(get()).createDriver() }
    single<PeripheralFactory> { AndroidPeripheralFactory() }
    single { BlePermissionHandler(get()) }
}
