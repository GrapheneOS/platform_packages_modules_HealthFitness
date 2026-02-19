/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.healthconnect.controller.datasources.api

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataSourcesUseCaseModule {
    @Binds
    @Singleton
    abstract fun providesLoadLastDateWithPriorityDataUseCase(
        useCase: LoadLastDateWithPriorityDataUseCase
    ): ILoadLastDateWithPriorityDataUseCase

    @Binds
    @Singleton
    abstract fun providesPriorityListUseCase(
        useCase: LoadPriorityListUseCase
    ): ILoadPriorityListUseCase

    @Binds
    @Singleton
    abstract fun updatePriorityListUseCase(
        useCase: UpdatePriorityListUseCase
    ): IUpdatePriorityListUseCase

    @Binds
    @Singleton
    abstract fun providesSleepSessionHelper(useCase: SleepSessionHelper): ISleepSessionHelper

    @Binds
    @Singleton
    abstract fun providesLoadPriorityEntriesUseCase(
        useCase: LoadPriorityEntriesUseCase
    ): ILoadPriorityEntriesUseCase

    @Binds
    @Singleton
    abstract fun providesLoadPotentialPriorityListUseCase(
        useCase: LoadPotentialPriorityListUseCase
    ): ILoadPotentialPriorityListUseCase

    @Binds
    @Singleton
    abstract fun providesMostRecentAggregationsUseCase(
        useCase: LoadMostRecentAggregationsUseCase
    ): ILoadMostRecentAggregationsUseCase
}
