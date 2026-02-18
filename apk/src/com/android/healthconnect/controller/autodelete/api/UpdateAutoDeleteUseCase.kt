/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.autodelete.api

import android.health.connect.HealthConnectManager
import androidx.core.os.asOutcomeReceiver
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class UpdateAutoDeleteUseCase
@Inject
constructor(
    private val healthConnectManager: HealthConnectManager,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseUseCase<Int, Unit>(dispatcher), IUpdateAutoDeleteUseCase {

    companion object {
        private const val DAYS_IN_MONTH = 30
    }

    /** Updates the stored auto-delete range. */
    override suspend fun execute(numberOfMonths: Int) {
        suspendCancellableCoroutine<Void?> { continuation ->
            healthConnectManager.setRecordRetentionPeriodInDays(
                numberOfMonths * DAYS_IN_MONTH,
                dispatcher.asExecutor(),
                continuation.asOutcomeReceiver(),
            )
        }
    }
}

interface IUpdateAutoDeleteUseCase : UseCaseContract<Int, Unit>
