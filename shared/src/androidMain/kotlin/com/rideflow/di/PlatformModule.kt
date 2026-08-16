package com.rideflow.di

import com.rideflow.data.bluetooth.AndroidPeripheralFactory
import com.rideflow.data.bluetooth.BlePermissionHandler
import com.rideflow.data.bluetooth.PeripheralFactory
import com.rideflow.data.local.DatabaseDriverFactory
import com.rideflow.domain.session.WorkoutKeepAlive
import com.rideflow.util.AndroidSharingService
import com.rideflow.util.AndroidWorkoutKeepAlive
import com.rideflow.util.SharingService
import org.koin.dsl.module

val platformModule = module {
    single { DatabaseDriverFactory(get()).createDriver() }
    single<PeripheralFactory> { AndroidPeripheralFactory() }
    single { BlePermissionHandler(get()) }
    single<SharingService> { AndroidSharingService(get()) }
    single<WorkoutKeepAlive> { AndroidWorkoutKeepAlive(get()) }
}
