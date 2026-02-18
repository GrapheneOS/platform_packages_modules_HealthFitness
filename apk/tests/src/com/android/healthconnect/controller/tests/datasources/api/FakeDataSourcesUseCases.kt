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

package com.android.healthconnect.controller.tests.datasources.api

import android.health.connect.HealthDataCategory
import android.health.connect.datatypes.Record
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.datasources.api.ILoadLastDateWithPriorityDataUseCase
import com.android.healthconnect.controller.datasources.api.ILoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPriorityEntriesUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.ISleepSessionHelper
import com.android.healthconnect.controller.datasources.api.IUpdatePriorityListUseCase
import com.android.healthconnect.controller.datasources.api.LoadPriorityEntriesInput
import com.android.healthconnect.controller.datasources.api.UpdatePriorityListInput
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers

class FakeLoadMostRecentAggregationsUseCase :
    FakeUseCase<Int, List<AggregationCardInfo>>(dispatcher = Dispatchers.Unconfined),
    ILoadMostRecentAggregationsUseCase {

    private var mostRecentAggregations = listOf<AggregationCardInfo>()

    override suspend fun successValue(input: Int): List<AggregationCardInfo> {
        return mostRecentAggregations
    }

    fun setMostRecentAggregations(aggregations: List<AggregationCardInfo>) {
        this.mostRecentAggregations = aggregations
    }

    override fun reset() {
        super.reset()
        this.mostRecentAggregations = listOf()
    }
}

class FakeLoadLastDateWithPriorityDataUseCase :
    FakeUseCase<FitnessPermissionType, LocalDate?>(dispatcher = Dispatchers.Unconfined),
    ILoadLastDateWithPriorityDataUseCase {

    private var lastDateWithPriorityDataMap = mutableMapOf<FitnessPermissionType, LocalDate?>()

    fun setLastDateWithPriorityDataForHealthPermissionType(
        fitnessPermissionType: FitnessPermissionType,
        localDate: LocalDate?,
    ) {
        lastDateWithPriorityDataMap[fitnessPermissionType] = localDate
    }

    override suspend fun successValue(input: FitnessPermissionType): LocalDate? {
        // TODO what if this is not here?
        return lastDateWithPriorityDataMap[input]
    }

    override fun reset() {
        lastDateWithPriorityDataMap.clear()
    }
}

class FakeLoadPriorityListUseCase :
    FakeUseCase<@HealthDataCategoryInt Int, List<AppMetadata>>(dispatcher = Dispatchers.Unconfined),
    ILoadPriorityListUseCase {

    private var priorityList = listOf<AppMetadata>()

    override suspend fun successValue(input: @HealthDataCategoryInt Int): List<AppMetadata> {
        return priorityList
    }

    fun setPriorityList(priorityList: List<AppMetadata>) {
        this.priorityList = priorityList
    }

    override fun reset() {
        this.priorityList = listOf()
    }
}

class FakeLoadPotentialPriorityListUseCase :
    FakeUseCase<@HealthDataCategoryInt Int, List<AppMetadata>>(dispatcher = Dispatchers.Unconfined),
    ILoadPotentialPriorityListUseCase {

    private var potentialPriorityList = listOf<AppMetadata>()

    override suspend fun successValue(input: @HealthDataCategoryInt Int): List<AppMetadata> {
        return potentialPriorityList
    }

    fun setPotentialPriorityList(potentialList: List<AppMetadata>) {
        this.potentialPriorityList = potentialList
    }

    override fun reset() {
        this.potentialPriorityList = listOf()
    }
}

class FakeLoadPriorityEntriesUseCase :
    FakeUseCase<LoadPriorityEntriesInput, List<Record>>(dispatcher = Dispatchers.Unconfined),
    ILoadPriorityEntriesUseCase {

    private var priorityEntries = mutableMapOf<LocalDate, List<Record>>()

    override suspend fun successValue(input: LoadPriorityEntriesInput): List<Record> {
        return priorityEntries.getOrDefault(input.localDate, listOf())
    }

    fun setEntriesListForDate(localDate: LocalDate, list: List<Record>) {
        priorityEntries[localDate] = list
    }

    override fun reset() {
        priorityEntries.clear()
    }
}

class FakeSleepSessionHelper :
    FakeUseCase<LocalDate, Pair<Instant, Instant>?>(dispatcher = Dispatchers.Unconfined),
    ISleepSessionHelper {

    private var datePair = Pair(Instant.EPOCH, Instant.EPOCH)

    fun setDatePair(minDate: Instant, maxDate: Instant) {
        datePair = Pair(minDate, maxDate)
    }

    override suspend fun successValue(input: LocalDate): Pair<Instant, Instant>? {
        return datePair
    }

    override fun reset() {
        datePair = Pair(Instant.EPOCH, Instant.EPOCH)
    }
}

class FakeUpdatePriorityListUseCase :
    FakeUseCase<UpdatePriorityListInput, Unit>(dispatcher = Dispatchers.Unconfined),
    IUpdatePriorityListUseCase {

    var priorityList = listOf<String>()
    var category = HealthDataCategory.UNKNOWN

    override suspend fun successValue(input: UpdatePriorityListInput) {
        priorityList = input.priorityList
        category = input.category
    }

    override fun reset() {
        this.priorityList = listOf()
        this.category = HealthDataCategory.UNKNOWN
    }
}
