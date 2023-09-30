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

import android.health.connect.accesslog.AccessLog
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.ILoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadDataEntriesUseCase
import com.android.healthconnect.controller.data.entries.api.ILoadMenstruationDataUseCase
import com.android.healthconnect.controller.data.entries.api.LoadAggregationInput
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadMenstruationDataInput
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
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

    fun updateAggregation(aggregation: FormattedEntry.FormattedAggregation) {
        this.aggregation = aggregation
    }

    override suspend fun invoke(
        input: LoadAggregationInput
    ): UseCaseResults<FormattedEntry.FormattedAggregation> {
        return UseCaseResults.Success(aggregation)
    }

    override suspend fun execute(input: LoadAggregationInput): FormattedEntry.FormattedAggregation {
        return aggregation
    }
}
