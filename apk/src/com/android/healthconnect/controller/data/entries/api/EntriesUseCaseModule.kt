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

package com.android.healthconnect.controller.data.entries.api

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface EntriesUseCaseModule {

    @Binds
    @Singleton
    fun bindsLoadDataEntriesUseCase(useCase: LoadDataEntriesUseCase): ILoadDataEntriesUseCase

    @Binds
    @Singleton
    fun bindsLoadLatestEntryDateUseCase(
        useCase: LoadLatestEntryDateUseCase
    ): ILoadLatestEntryDateUseCase

    @Binds
    @Singleton
    fun bindsLoadSymptomDataEntriesUseCase(
        useCase: LoadSymptomDataEntriesUseCase
    ): ILoadSymptomDataEntriesUseCase

    @Binds
    @Singleton
    fun bindsLoadLatestSymptomEntryDateUseCase(
        useCase: LoadLatestSymptomEntryDateUseCase
    ): ILoadLatestSymptomEntryDateUseCase

    @Binds
    @Singleton
    fun bindsLoadMedicalEntriesUseCase(
        useCase: LoadMedicalEntriesUseCase
    ): ILoadMedicalEntriesUseCase

    @Binds
    @Singleton
    fun bindsLoadDataAggregationsUseCase(
        useCase: LoadDataAggregationsUseCase
    ): ILoadDataAggregationsUseCase

    @Binds
    @Singleton
    fun bindsLoadMenstruationDataUseCase(
        useCase: LoadMenstruationDataUseCase
    ): ILoadMenstruationDataUseCase
}
