package com.rideflow.di

import com.rideflow.data.local.DatabaseDriverFactory
import com.rideflow.domain.session.WorkoutKeepAlive
import com.rideflow.util.IosSharingService
import com.rideflow.util.IosWorkoutKeepAlive
import com.rideflow.util.SharingService
import org.koin.dsl.module

val platformModule = module {
    single { DatabaseDriverFactory().createDriver() }
    single<SharingService> { IosSharingService() }
    single<WorkoutKeepAlive> { IosWorkoutKeepAlive() }
}
