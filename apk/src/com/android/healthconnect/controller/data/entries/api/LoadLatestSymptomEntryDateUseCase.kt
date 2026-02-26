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

import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.SymptomRecord
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/** Use case to fetch the latest date for which a [SymptomRecord] exists, up to a given end time. */
@Singleton
class LoadLatestSymptomEntryDateUseCase
@Inject
constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val loadEntriesHelper: LoadEntriesHelper,
) :
    BaseUseCase<LoadLatestSymptomEntryDateInput, Instant>(dispatcher),
    ILoadLatestSymptomEntryDateUseCase {

    /**
     * Returns the start time of the most recent [SymptomRecord] before or at the
     * [LoadLatestSymptomEntryDateInput.displayedStartTime]. If no record is found, it returns the
     * [LoadLatestSymptomEntryDateInput.displayedStartTime].
     */
    override suspend fun execute(input: LoadLatestSymptomEntryDateInput): Instant {
        val timeFilterRange =
            TimeInstantRangeFilter.Builder().setEndTime(input.displayedStartTime).build()
        val records =
            loadEntriesHelper.readDataType(
                SymptomRecord::class.java,
                timeFilterRange,
                input.packageName,
                ascending = false,
                pageSize = 1,
            )
        return if (records.isEmpty()) input.displayedStartTime
        else loadEntriesHelper.getStartTime(records.first())
    }
}

data class LoadLatestSymptomEntryDateInput(
    val displayedStartTime: Instant,
    val packageName: String? = null,
)

interface ILoadLatestSymptomEntryDateUseCase :
    UseCaseContract<LoadLatestSymptomEntryDateInput, Instant>
