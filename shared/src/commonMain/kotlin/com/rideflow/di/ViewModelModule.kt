package com.rideflow.di

import com.rideflow.domain.usecase.*
import com.rideflow.presentation.history.HistoryViewModel
import com.rideflow.presentation.history.detail.TrainDetailViewModel
import com.rideflow.presentation.settings.SettingsViewModel
import com.rideflow.presentation.workout.WorkoutViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    factoryOf(::StartWorkoutUseCase)
    factoryOf(::RecordSampleUseCase)
    factoryOf(::StopWorkoutUseCase)
    factoryOf(::AdjustResistanceUseCase)
    factoryOf(::GetWorkoutHistoryUseCase)
    factoryOf(::GetWorkoutDetailUseCase)
    
    viewModelOf(::WorkoutViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::TrainDetailViewModel)
    viewModelOf(::SettingsViewModel)
}
