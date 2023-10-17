/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.utils.di

import android.health.connect.HealthDataCategory
import android.health.connect.accesslog.AccessLog
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.api.LoadAggregationInput
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataInput
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.datasources.api.ILoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.datasources.api.ILoadPotentialPriorityListUseCase
import com.android.healthconnect.controller.datasources.api.IUpdatePriorityListUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissiontypes.api.ILoadPriorityListUseCase
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults

class FakeRecentAccessUseCase : ILoadRecentAccessUseCase {
    private var list: List<AccessLog> = emptyList()

    fun updateList(list: List<AccessLog>) {
        this.list = list
    }

    override suspend fun invoke(): List<AccessLog> {
        return list
    }
}

class FakeHealthPermissionAppsUseCase : ILoadHealthPermissionApps {
    private var list: List<ConnectedAppMetadata> = emptyList()

    fun updateList(list: List<ConnectedAppMetadata>) {
        this.list = list
    }

    override suspend fun invoke(): List<ConnectedAppMetadata> {
        return list
    }
}

class FakeLoadDataEntriesUseCase : ILoadDataEntriesUseCase {
    private var list: List<FormattedEntry> = emptyList()

    fun updateList(list: List<FormattedEntry>) {
        this.list = list
    }

    override suspend fun invoke(input: LoadDataEntriesInput): UseCaseResults<List<FormattedEntry>> {
        return UseCaseResults.Success(list)
    }

    override suspend fun execute(input: LoadDataEntriesInput): List<FormattedEntry> {
        return list
    }
}

class FakeLoadMenstruationDataUseCase : ILoadMenstruationDataUseCase {
    private var list: List<FormattedEntry> = emptyList()

    fun updateList(list: List<FormattedEntry>) {
        this.list = list
    }

    override suspend fun invoke(
        input: LoadMenstruationDataInput
    ): UseCaseResults<List<FormattedEntry>> {
        return UseCaseResults.Success(list)
    }

    override suspend fun execute(input: LoadMenstruationDataInput): List<FormattedEntry> {
        return list
    }
}

class FakeLoadDataAggregationsUseCase : ILoadDataAggregationsUseCase {
    private var aggregation: FormattedEntry.FormattedAggregation =
        FormattedEntry.FormattedAggregation("100 steps", "100 steps", "Test App")

    private var aggregations: List<FormattedEntry.FormattedAggregation> = listOf(aggregation)
    private var invocationCount = 0
    private var shouldReturnFailed = false

    fun updateAggregation(aggregation: FormattedEntry.FormattedAggregation) {
        this.aggregations = listOf(aggregation)
    }

    /**
     * Used for subsequent invocations when we need different responses
     */
    fun updateAggregationResponses(aggregations: List<FormattedEntry.FormattedAggregation>) {
        this.aggregations = aggregations
    }

    fun updateErrorResponse() {
        this.shouldReturnFailed = true
    }

    override suspend fun invoke(
        input: LoadAggregationInput
    ): UseCaseResults<FormattedEntry.FormattedAggregation> {
        return if (invocationCount >= this.aggregations.size) {
            UseCaseResults.Failed(
                IllegalStateException("AggregationResponsesSize = ${this.aggregations.size}, " +
                        "invocationCount = $invocationCount. Please update aggregation responses before invoking."))
        }
         else if (shouldReturnFailed) {
            UseCaseResults.Failed(
                IllegalStateException("Custom failure"))
        } else {
            val result = UseCaseResults.Success(aggregations[invocationCount])
            invocationCount += 1
            result
        }
    }

    override suspend fun execute(input: LoadAggregationInput): FormattedEntry.FormattedAggregation {
        return aggregation
    }

    fun reset() {
        this.invocationCount = 0
        this.aggregations = listOf(aggregation)
        this.shouldReturnFailed = false
    }
}

class FakeLoadMostRecentAggregationsUseCase : ILoadMostRecentAggregationsUseCase {

    private var mostRecentAggregations = listOf<AggregationCardInfo>()
    override suspend fun invoke(): UseCaseResults<List<AggregationCardInfo>> {
        return UseCaseResults.Success(mostRecentAggregations)
    }

    fun updateMostRecentAggregations(aggregations: List<AggregationCardInfo>) {
        this.mostRecentAggregations = aggregations
    }

    fun reset() {
        this.mostRecentAggregations = listOf()
    }
}

class FakeLoadPotentialPriorityListUseCase : ILoadPotentialPriorityListUseCase {

    private var potentialPriorityList = listOf<AppMetadata>()
    override suspend fun invoke(category: @HealthDataCategoryInt Int): UseCaseResults<List<AppMetadata>> {
        return UseCaseResults.Success(potentialPriorityList)
    }

    fun updatePotentialPriorityList(potentialList: List<AppMetadata>) {
        this.potentialPriorityList = potentialList
    }

    fun reset() {
        this.potentialPriorityList = listOf()
    }
}

class FakeLoadPriorityListUseCase : ILoadPriorityListUseCase {

    private var priorityList = listOf<AppMetadata>()
    override suspend fun invoke(input: @HealthDataCategoryInt Int): UseCaseResults<List<AppMetadata>> {
        return UseCaseResults.Success(priorityList)
    }

    override suspend fun execute(input: Int): List<AppMetadata> {
        return priorityList
    }

    fun updatePriorityList(priorityList: List<AppMetadata>) {
        this.priorityList = priorityList
    }

    fun reset() {
        this.priorityList = listOf()
    }
}

class FakeUpdatePriorityListUseCase : IUpdatePriorityListUseCase {

    var priorityList = listOf<String>()
    var category = HealthDataCategory.UNKNOWN

    override suspend fun invoke(priorityList: List<String>, category: Int) {
        this.priorityList = priorityList
        this.category = category
    }

    fun reset() {
        this.priorityList = listOf()
        this.category = HealthDataCategory.UNKNOWN
    }
}
