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
package com.android.healthconnect.testapps.toolbox.utiltests

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import androidx.test.core.app.ApplicationProvider
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Duration.ofMinutes
import java.time.Instant

class DeletionUtilTest {

    private lateinit var context: Context
    private lateinit var manager: HealthConnectManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(HealthConnectManager::class.java)
    }

    @Test
    fun deleteRecords_newRecordsAreAddedThenDeleted_resultsInNoRecordSizeChange() = runTest {
        val startTime = Instant.now().minus(ofMinutes(60))
        val endTime = Instant.now()
        val timeFilter = TimeInstantRangeFilter.Builder()
            .setStartTime(startTime)
            .setEndTime(endTime)
            .build()
        val recordsToInsert = listOf(
            StepsRecord.Builder(
                getMetaData(context),
                timeFilter.startTime!!,
                timeFilter.startTime!!.plus(ofMinutes(1)),
                10L
            ).build(),
            StepsRecord.Builder(
                getMetaData(context),
                timeFilter.startTime!!.plus(ofMinutes(5)),
                timeFilter.startTime!!.plus(ofMinutes(10)),
                10L
            ).build()
        )

        val recordsBeforeInsertion = readStepsRecord(timeFilter)
        GeneralUtils.insertRecords(recordsToInsert, manager)
        val recordsAfterInsertion = readStepsRecord(timeFilter)
        GeneralUtils.deleteRecords(manager, StepsRecord::class.java, timeFilter)
        val recordsAfterDeletion = readStepsRecord(timeFilter)

        assertThat(recordsAfterInsertion.size).isEqualTo(recordsBeforeInsertion.size + 2)
        assertThat(recordsAfterDeletion.size).isEqualTo(recordsBeforeInsertion.size)
    }

    private suspend fun readStepsRecord(timeFilter: TimeInstantRangeFilter) : List<Record>{
        return GeneralUtils.readRecords(
            StepsRecord::class.java,
            timeFilter,
            10L,
            manager
        )
    }
}