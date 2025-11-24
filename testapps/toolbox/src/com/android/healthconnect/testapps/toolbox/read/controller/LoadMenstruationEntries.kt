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
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.readRecords
import java.time.Period.ofDays

class LoadMenstruationEntries(private val healthConnectManager: HealthConnectManager) :
    DataEntriesLoader {

    override suspend fun load(input: LoadEntriesInput): List<Record> {

        val timeFilter =
            TimeInstantRangeFilter.Builder()
                .setStartTime(input.endTime.minus(ofDays(30)))
                .setEndTime(input.endTime)

        // Return the latest 30 records
        return readRecords(
            recordType = input.dataType.recordClass!!.java,
            timeFilterRange = timeFilter.build(),
            numberOfRecordsPerBatch = 30L,
            manager = healthConnectManager,
            ascending = false,
        )
    }
}
