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
import android.health.connect.MedicalResourceTypeInfo
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.fromMedicalResourceType
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine

/** Use case to load [AppMetadata]s that have data of this [MedicalPermissionType]. */
@Singleton
class LoadMedicalTypeContributorAppsUseCase
@Inject
constructor(
    private val appInfoReader: AppInfoReader,
    private val healthConnectManager: HealthConnectManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) :
    BaseUseCase<MedicalPermissionType, List<AppMetadata>>(dispatcher),
    ILoadMedicalTypeContributorAppsUseCase {

    override suspend fun execute(input: MedicalPermissionType): List<AppMetadata> {
        val recordTypeInfoMap: List<MedicalResourceTypeInfo> =
            suspendCancellableCoroutine { continuation ->
                healthConnectManager.queryAllMedicalResourceTypeInfos(
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        val packages =
            recordTypeInfoMap
                .filter {
                    fromMedicalResourceType(it.medicalResourceType) == input &&
                        it.contributingDataSources.isNotEmpty()
                }
                .map { it.contributingDataSources }
                .flatten()
        return packages
            .map { appInfoReader.getAppMetadata(it.packageName) }
            .distinct()
            .sortedBy { it.appName }
    }
}

interface ILoadMedicalTypeContributorAppsUseCase :
    UseCaseContract<MedicalPermissionType, List<AppMetadata>>
