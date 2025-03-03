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
package com.android.healthconnect.testapps.toolbox.read.controller

import android.health.connect.HealthConnectManager
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.Record
import com.android.healthconnect.testapps.toolbox.Constants.HealthPermissionType
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils
import java.time.Instant

class LoadDataEntries(private val healthConnectManager: HealthConnectManager): DataEntriesLoader {

    override suspend fun load(input: LoadEntriesInput): List<Record> {

        val timeFilter = TimeInstantRangeFilter.Builder()
            .setStartTime(input.startTime)
            .setEndTime(input.endTime)
            .build()
        return GeneralUtils.readRecords(
            recordType =  input.dataType.recordClass!!.java,
            timeFilterRange =  timeFilter,
            numberOfRecordsPerBatch = 10L,
            manager = healthConnectManager
        )
    }
}

data class LoadEntriesInput(
    val dataType: HealthPermissionType,
    val startTime: Instant,
    val endTime: Instant
)

interface DataEntriesLoader{
    suspend fun load(input: LoadEntriesInput): List<Record>
}