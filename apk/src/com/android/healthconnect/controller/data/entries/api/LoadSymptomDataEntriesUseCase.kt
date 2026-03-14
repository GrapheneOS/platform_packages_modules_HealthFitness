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

import android.health.connect.datatypes.SymptomRecord
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/** Use case to load all symptom data entries. */
@Singleton
class LoadSymptomDataEntriesUseCase
@Inject
constructor(
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val loadEntriesHelper: LoadEntriesHelper,
) :
    BaseUseCase<LoadSymptomDataEntriesInput, List<FormattedEntry>>(dispatcher),
    ILoadSymptomDataEntriesUseCase {

    override suspend fun execute(input: LoadSymptomDataEntriesInput): List<FormattedEntry> {
        val timeFilterRange =
            loadEntriesHelper.getTimeFilter(
                input.displayedStartTime,
                input.period,
                endTimeExclusive = true,
            )
        val entryRecords =
            loadEntriesHelper.readDataType(
                SymptomRecord::class.java,
                timeFilterRange,
                input.packageName,
                ascending = false,
            )

        return loadEntriesHelper.maybeAddDateSectionHeaders(
            entryRecords,
            input.period,
            input.showDataOrigin,
        )
    }
}

data class LoadSymptomDataEntriesInput(
    val packageName: String?,
    val displayedStartTime: Instant,
    val period: DateNavigationPeriod,
    val showDataOrigin: Boolean,
)

interface ILoadSymptomDataEntriesUseCase :
    UseCaseContract<LoadSymptomDataEntriesInput, List<FormattedEntry>>
