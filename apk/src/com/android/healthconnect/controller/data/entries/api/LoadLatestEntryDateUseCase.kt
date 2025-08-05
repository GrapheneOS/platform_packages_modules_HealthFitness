/*
 * Copyright (C) 2025 The Android Open Source Project
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

import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

@Singleton
class LoadLatestEntryDateUseCase
@Inject
constructor(
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val loadEntriesHelper: LoadEntriesHelper,
) : BaseUseCase<LoadLatestEntryDateInput, Instant>(dispatcher), ILoadLatestEntryDateUseCase {

    override suspend fun execute(input: LoadLatestEntryDateInput): Instant {
        return loadEntriesHelper.readLatestRecordDate(input) ?: input.displayedStartTime
    }
}

data class LoadLatestEntryDateInput(
    val permissionType: FitnessPermissionType,
    val displayedStartTime: Instant,
)

interface ILoadLatestEntryDateUseCase {
    suspend fun invoke(input: LoadLatestEntryDateInput): UseCaseResults<Instant>

    suspend fun execute(input: LoadLatestEntryDateInput): Instant
}
