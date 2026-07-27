package com.rideflow.di

import com.rideflow.db.RideFlowDb
import org.koin.dsl.module

val databaseModule = module {
    single { RideFlowDb(get()) }
}
