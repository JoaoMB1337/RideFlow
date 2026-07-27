package com.rideflow

import android.app.Application
import com.rideflow.di.initKoin
import com.rideflow.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class RideFlowApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        initKoin {
            androidLogger()
            androidContext(this@RideFlowApp)
            modules(platformModule)
        }
    }
}
