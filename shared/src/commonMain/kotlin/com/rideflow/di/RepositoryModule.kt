package com.rideflow.di

import com.rideflow.data.bluetooth.BleScanner
import com.rideflow.data.bluetooth.SensorRepositoryImpl
import com.rideflow.data.local.WorkoutRepositoryImpl
import com.rideflow.domain.repository.SensorRepository
import com.rideflow.domain.repository.WorkoutRepository
import com.rideflow.domain.session.WorkoutSessionManager
import com.russhwolf.settings.Settings
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    single<WorkoutRepository> { WorkoutRepositoryImpl(get()) }
    single<SensorRepository> { SensorRepositoryImpl(get(), get()) }
    single { BleScanner() }
    single { Settings() }

    // `single`, não `factory`: só pode existir uma sessão de treino, e tem de sobreviver
    // aos ViewModels que a observam.
    singleOf(::WorkoutSessionManager)
}
