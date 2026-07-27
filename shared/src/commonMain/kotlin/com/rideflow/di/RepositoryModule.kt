package com.rideflow.di

import com.rideflow.data.bluetooth.BleScanner
import com.rideflow.data.bluetooth.SensorRepositoryImpl
import com.rideflow.data.local.WorkoutRepositoryImpl
import com.rideflow.domain.repository.SensorRepository
import com.rideflow.domain.repository.WorkoutRepository
import com.russhwolf.settings.Settings
import org.koin.dsl.module

val repositoryModule = module {
    single<WorkoutRepository> { WorkoutRepositoryImpl(get()) }
    single<SensorRepository> { SensorRepositoryImpl(get(), get()) }
    single { BleScanner() }
    single { Settings() }
}
