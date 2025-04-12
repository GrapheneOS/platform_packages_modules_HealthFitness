/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *    http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.utiltests

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.PowerRecord
import android.health.connect.datatypes.PowerRecord.PowerRecordSample
import android.health.connect.datatypes.units.Power
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration.ofMinutes
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class AggregationUtilTest {

    private lateinit var context: Context
    private lateinit var manager: HealthConnectManager

    private lateinit var startTime: Instant
    private lateinit var endTime: Instant
    private lateinit var timeFilter: TimeInstantRangeFilter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(HealthConnectManager::class.java)

        startTime = Instant.now().minus(ofMinutes(60))
        endTime = Instant.now()

        timeFilter = TimeInstantRangeFilter.Builder()
            .setStartTime(startTime)
            .setEndTime(endTime)
            .build()
    }

    @After
    fun cleanUp() = runTest{
        GeneralUtils.deleteRecords(manager,PowerRecord::class.java,timeFilter)
    }

    @Test
    fun requestAggregationForPowerRecords_returnsCorrectStatisticalAggregation() = runTest {
        val expectedPowerAVG = 76.25
        val expectedPowerMIN = 70.0
        val expectedPowerMAX = 90.0
        val powerRecordSamples = listOf(
            PowerRecordSample(
                Power.fromWatts(70.0),
                startTime.plus(ofMinutes(0))
            ),
            PowerRecordSample(
                Power.fromWatts(70.0),
                startTime.plus(ofMinutes(5))
            ),
            PowerRecordSample(
                Power.fromWatts(75.0),
                startTime.plus(ofMinutes(10))
            ),
            PowerRecordSample(
                Power.fromWatts(90.0),
                startTime.plus(ofMinutes(15))
            )
        )
        val powerRecord = listOf(
            PowerRecord.Builder(
                GeneralUtils.getMetaData(context),
                startTime,
                endTime,
                powerRecordSamples)
                .build()
        )

        GeneralUtils.insertRecords(powerRecord,manager)
        val response = GeneralUtils.aggregate<Power>(
            manager = manager,
            timeRangeFilter = timeFilter,
            metrics = setOf(PowerRecord.POWER_AVG,PowerRecord.POWER_MIN,PowerRecord.POWER_MAX)
        )
        val aggregatedPowerAVG = response.get(PowerRecord.POWER_AVG)?.inWatts
        val aggregatedPowerMIN = response.get(PowerRecord.POWER_MIN)?.inWatts
        val aggregatedPowerMAX = response.get(PowerRecord.POWER_MAX)?.inWatts

        assertThat(aggregatedPowerAVG).isEqualTo(expectedPowerAVG)
        assertThat(aggregatedPowerMIN).isEqualTo(expectedPowerMIN)
        assertThat(aggregatedPowerMAX).isEqualTo(expectedPowerMAX)
    }
}