package com.rideflow.di

import com.rideflow.domain.usecase.*
import com.rideflow.presentation.workout.WorkoutViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    factoryOf(::StartWorkoutUseCase)
    factoryOf(::RecordSampleUseCase)
    factoryOf(::StopWorkoutUseCase)
    
    viewModelOf(::WorkoutViewModel)
}
