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
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.WeightRecord
import android.health.connect.datatypes.units.Mass
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataEntryUtilsTest {

    private val dataEntryUtils = DataEntryUtils.Companion
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getHeader_withIntervalRecord_returnsCorrectValue() {
        val NOW: Instant = Instant.parse("2024-11-11T16:36:18.000Z")
        val intervalRecord =
            StepsRecord.Builder(getMetaData(context), NOW, NOW.plusSeconds(3600), 10).build()
        val expectedHeader = "16:36 - 17:36"

        val header = dataEntryUtils.getHeader(intervalRecord)

        assertThat(header).isEqualTo(expectedHeader)
    }

    @Test
    fun getHeader_withInstantRecord_returnsCorrectValue() {
        val NOW: Instant = Instant.parse("2024-11-11T17:20:18.000Z")
        val intervalRecord =
            WeightRecord.Builder(getMetaData(context), NOW, Mass.fromGrams(6000.0)).build()
        val expectedHeader = "17:20"

        val header = dataEntryUtils.getHeader(intervalRecord)

        assertThat(header).isEqualTo(expectedHeader)
    }

    @Test
    fun getHeader_withInstant_returnsCorrectValue() {
        val instant = Instant.parse("2024-11-11T10:12:18.000Z")
        val expectedHeader = "10:12"

        val header = dataEntryUtils.getHeader(instant)

        assertThat(header).isEqualTo(expectedHeader)
    }

    @Test
    fun getHeader_withIntervalInstant_returnsCorrectValue() {
        val NOW: Instant = Instant.parse("2024-11-11T11:11:18.000Z")
        val startTime = NOW
        val endTime = NOW.plusSeconds(60)
        val expectedHeader = "11:11 - 11:12"

        val header = dataEntryUtils.getHeader(startTime, endTime)

        assertThat(header).isEqualTo(expectedHeader)
    }
}
