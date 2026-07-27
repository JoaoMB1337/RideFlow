package com.rideflow.di

import com.rideflow.data.local.DatabaseDriverFactory
import org.koin.dsl.module

val platformModule = module {
    single { DatabaseDriverFactory(get()).createDriver() }
}
