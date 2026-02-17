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

package com.android.healthconnect.controller.data.access.api

import android.health.connect.HealthConnectManager
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SymptomRecord
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

/** Use case to load [AppMetadata]s that contribute symptom data. */
@Singleton
class LoadSymptomContributorAppsUseCase
@Inject
constructor(
    private val appInfoReader: AppInfoReader,
    private val healthConnectManager: HealthConnectManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<Unit, List<AppMetadata>>(dispatcher), ILoadSymptomContributorAppsUseCase {

    override suspend fun execute(input: Unit): List<AppMetadata> {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.queryAllRecordTypesInfo(
                    dispatcher.asExecutor(),
                    continuation.asOutcomeReceiver(),
                )
            }
        val packages =
            recordTypeInfoMap[SymptomRecord::class.java]?.contributingPackages?.map {
                it.packageName
            } ?: emptyList()
        return packages
            .map { appInfoReader.getAppMetadata(it) }
            .distinctBy { it.packageName }
            .sortedBy { it.appName }
    }
}

interface ILoadSymptomContributorAppsUseCase : UseCaseContract<Unit, List<AppMetadata>>
