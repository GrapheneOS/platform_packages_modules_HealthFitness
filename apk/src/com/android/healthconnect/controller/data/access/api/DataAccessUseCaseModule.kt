/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.data.access.api

import com.android.healthconnect.controller.data.access.ILoadAccessUseCase
import com.android.healthconnect.controller.data.access.ILoadFitnessTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.ILoadMedicalTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.ILoadSymptomAccessUseCase
import com.android.healthconnect.controller.data.access.ILoadSymptomContributorAppsUseCase
import com.android.healthconnect.controller.data.access.LoadAccessUseCase
import com.android.healthconnect.controller.data.access.LoadFitnessTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.LoadMedicalTypeContributorAppsUseCase
import com.android.healthconnect.controller.data.access.LoadSymptomAccessUseCase
import com.android.healthconnect.controller.data.access.LoadSymptomContributorAppsUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataAccessUseCaseModule {
    @Binds
    @Singleton
    abstract fun providesLoadFitnessTypeContributorAppsUseCase(
        useCase: LoadFitnessTypeContributorAppsUseCase
    ): ILoadFitnessTypeContributorAppsUseCase

    @Binds
    @Singleton
    abstract fun providesLoadMedicalTypeContributorAppsUseCase(
        useCase: LoadMedicalTypeContributorAppsUseCase
    ): ILoadMedicalTypeContributorAppsUseCase

    @Binds
    @Singleton
    abstract fun providesLoadAccessUseCase(useCase: LoadAccessUseCase): ILoadAccessUseCase

    @Binds
    @Singleton
    abstract fun providesLoadSymptomAccessUseCase(
        useCase: LoadSymptomAccessUseCase
    ): ILoadSymptomAccessUseCase

    @Binds
    @Singleton
    abstract fun providesLoadSymptomTypeContributorAppsUseCase(
        useCase: LoadSymptomContributorAppsUseCase
    ): ILoadSymptomContributorAppsUseCase
}
