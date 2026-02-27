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

package com.android.healthconnect.controller.data.alldata.api

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AllDataUseCaseModule {
    @Binds
    @Singleton
    abstract fun bindHasFitnessDataUseCase(useCase: HasFitnessDataUseCase): IHasFitnessDataUseCase

    @Binds
    @Singleton
    abstract fun bindHasMedicalDataUseCase(useCase: HasMedicalDataUseCase): IHasMedicalDataUseCase

    @Binds
    @Singleton
    abstract fun bindGetFitnessPermissionTypesWithDataUseCase(
        useCase: GetFitnessPermissionTypesWithDataUseCase
    ): IGetFitnessPermissionTypesWithDataUseCase

    @Binds
    @Singleton
    abstract fun bindGetMedicalPermissionTypesWithDataUseCase(
        useCase: GetMedicalPermissionTypesWithDataUseCase
    ): IGetMedicalPermissionTypesWithDataUseCase
}
