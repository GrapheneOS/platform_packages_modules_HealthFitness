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
package com.android.healthconnect.controller.devices.api

import com.android.healthconnect.controller.matchmaking.api.GetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.matchmaking.api.IGetDeviceDataSourcesInfoUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DevicesUseCaseModule {

    @Binds
    @Singleton
    abstract fun provideLoadDeviceDataSourcesUseCase(
        useCase: LoadDeviceDataSourcesUseCase
    ): ILoadDeviceDataSourcesUseCase

    @Binds
    @Singleton
    abstract fun provideSetTrackingEnabledUseCase(
        useCase: SetTrackingEnabledUseCase
    ): ISetTrackingEnabledUseCase

    @Binds
    @Singleton
    abstract fun provideLoadSensorListUseCase(
        useCase: LoadSensorListUseCase
    ): ILoadSensorListUseCase

    @Binds
    @Singleton
    abstract fun provideGetDeviceDataSourcesInfoUseCase(
        useCase: GetDeviceDataSourcesInfoUseCase
    ): IGetDeviceDataSourcesInfoUseCase
}
