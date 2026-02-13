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

import android.health.connect.datatypes.Record
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate

@Module
@InstallIn(SingletonComponent::class)
class DataSourcesUseCaseModule {
    @Provides
    fun providesLoadLastDateWithPriorityDataUseCase(
        useCase: LoadLastDateWithPriorityDataUseCase
    ): BaseUseCase<FitnessPermissionType, LocalDate?> {
        return useCase
    }

    @Provides
    @com.android.healthconnect.controller.shared.usecase.LoadPriorityListUseCase
    fun providesPriorityListUseCase(
        useCase: LoadPriorityListUseCase
    ): BaseUseCase<@HealthDataCategoryInt Int, List<AppMetadata>> {
        return useCase
    }

    @Provides
    fun updatePriorityListUseCase(
        useCase: UpdatePriorityListUseCase
    ): BaseUseCase<UpdatePriorityListInput, Unit> {
        return useCase
    }

    @Provides
    fun providesSleepSessionHelper(
        useCase: SleepSessionHelper
    ): BaseUseCase<LocalDate, Pair<Instant, Instant>?> {
        return useCase
    }

    @Provides
    fun providesLoadPriorityEntriesUseCase(
        useCase: LoadPriorityEntriesUseCase
    ): BaseUseCase<LoadPriorityEntriesInput, List<Record>> {
        return useCase
    }

    @Provides
    @com.android.healthconnect.controller.shared.usecase.LoadPotentialPriorityListUseCase
    fun providesLoadPotentialPriorityListUseCase(
        useCase: LoadPotentialPriorityListUseCase
    ): BaseUseCase<@HealthDataCategoryInt Int, List<AppMetadata>> {
        return useCase
    }

    @Provides
    fun providesMostRecentAggregationsUseCase(
        useCase: LoadMostRecentAggregationsUseCase
    ): BaseUseCase<Int, List<AggregationCardInfo>> {
        return useCase
    }
}
