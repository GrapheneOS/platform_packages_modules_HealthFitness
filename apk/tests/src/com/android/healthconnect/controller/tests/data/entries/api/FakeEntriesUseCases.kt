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

package com.android.healthconnect.controller.tests.data.entries.api

import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadLatestEntryDateUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadLatestSymptomEntryDateUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMedicalEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadSymptomDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.LoadAggregationInput
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadLatestEntryDateInput
import com.android.healthconnect.controller.data.entries.api.LoadLatestSymptomEntryDateInput
import com.android.healthconnect.controller.data.entries.api.LoadMedicalEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataInput
import com.android.healthconnect.controller.data.entries.api.LoadSymptomDataEntriesInput
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import com.android.healthconnect.controller.utils.toInstant
import java.time.Instant
import kotlinx.coroutines.Dispatchers

class FakeLoadDataEntriesUseCase :
    FakeUseCase<LoadDataEntriesInput, List<FormattedEntry>>(dispatcher = Dispatchers.Unconfined),
    ILoadDataEntriesUseCase {
    private var formattedList = listOf<FormattedEntry>()

    fun updateList(list: List<FormattedEntry>) {
        formattedList = list
    }

    override suspend fun successValue(input: LoadDataEntriesInput): List<FormattedEntry> {
        return formattedList
    }

    override fun reset() {
        super.reset()
        formattedList = emptyList()
    }
}

class FakeLoadSymptomDataEntriesUseCase :
    FakeUseCase<LoadSymptomDataEntriesInput, List<FormattedEntry>>(
        dispatcher = Dispatchers.Unconfined
    ),
    ILoadSymptomDataEntriesUseCase {
    private var formattedList = listOf<FormattedEntry>()

    fun updateList(list: List<FormattedEntry>) {
        formattedList = list
    }

    override suspend fun successValue(input: LoadSymptomDataEntriesInput): List<FormattedEntry> {
        return formattedList
    }

    override fun reset() {
        super.reset()
        formattedList = emptyList()
    }
}

class FakeLoadLatestSymptomEntryDateUseCase :
    FakeUseCase<LoadLatestSymptomEntryDateInput, Instant>(dispatcher = Dispatchers.Unconfined),
    ILoadLatestSymptomEntryDateUseCase {

    private var instant = System.currentTimeMillis().toInstant()

    fun updateInstant(instant: Instant) {
        this.instant = instant
    }

    override suspend fun successValue(input: LoadLatestSymptomEntryDateInput): Instant {
        return instant
    }
}

class FakeLoadMenstruationDataUseCase :
    FakeUseCase<LoadMenstruationDataInput, List<FormattedEntry>>(
        dispatcher = Dispatchers.Unconfined
    ),
    ILoadMenstruationDataUseCase {
    private var list: List<FormattedEntry> = emptyList()

    fun updateList(list: List<FormattedEntry>) {
        this.list = list
    }

    override suspend fun successValue(input: LoadMenstruationDataInput): List<FormattedEntry> {
        return list
    }

    override fun reset() {
        super.reset()
        this.list = emptyList()
    }
}

class FakeLoadDataAggregationsUseCase :
    FakeUseCase<LoadAggregationInput, FormattedEntry.FormattedAggregation>(
        dispatcher = Dispatchers.Unconfined
    ),
    ILoadDataAggregationsUseCase {
    private var aggregation: FormattedEntry.FormattedAggregation =
        FormattedEntry.FormattedAggregation("100 steps", "100 steps", "Test App")

    private var aggregations: List<FormattedEntry.FormattedAggregation> = listOf(aggregation)

    fun updateAggregation(aggregation: FormattedEntry.FormattedAggregation) {
        this.aggregations = listOf(aggregation)
    }

    /** Used for subsequent invocations when we need different responses */
    fun updateAggregationResponses(aggregations: List<FormattedEntry.FormattedAggregation>) {
        this.aggregations = aggregations
    }

    override suspend fun successValue(
        input: LoadAggregationInput
    ): FormattedEntry.FormattedAggregation {
        val result =
            if (numberOfInvocations - 1 >= this.aggregations.size) {
                aggregations.last()
            } else {
                aggregations[numberOfInvocations - 1]
            }
        return result
    }

    override fun reset() {
        super.reset()
        this.aggregations = listOf(aggregation)
    }
}

class FakeLoadMedicalEntriesUseCase :
    FakeUseCase<LoadMedicalEntriesInput, List<FormattedEntry>>(dispatcher = Dispatchers.Unconfined),
    ILoadMedicalEntriesUseCase {
    private var formattedList = listOf<FormattedEntry>()

    fun updateList(list: List<FormattedEntry>) {
        formattedList = list
    }

    override suspend fun successValue(input: LoadMedicalEntriesInput): List<FormattedEntry> {
        return formattedList
    }

    override fun reset() {
        super.reset()
        formattedList = emptyList()
    }
}

class FakeLoadLatestEntryDateUseCase :
    FakeUseCase<LoadLatestEntryDateInput, Instant>(dispatcher = Dispatchers.Unconfined),
    ILoadLatestEntryDateUseCase {
    private var instant = System.currentTimeMillis().toInstant()

    fun updateInstant(instant: Instant) {
        this.instant = instant
    }

    override suspend fun successValue(input: LoadLatestEntryDateInput): Instant {
        return instant
    }
}
