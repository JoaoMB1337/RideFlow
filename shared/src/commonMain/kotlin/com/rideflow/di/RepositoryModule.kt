package com.rideflow.di

import com.rideflow.data.local.WorkoutRepositoryImpl
import com.rideflow.domain.repository.WorkoutRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<WorkoutRepository> { WorkoutRepositoryImpl(get()) }
    // SensorRepository will be added when implemented
}
