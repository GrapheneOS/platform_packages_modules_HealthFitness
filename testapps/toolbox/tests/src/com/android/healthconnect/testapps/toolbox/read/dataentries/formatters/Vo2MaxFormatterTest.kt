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
package com.android.healthconnect.testapps.toolbox.read.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.Vo2MaxRecord
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_COOPER_TEST
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_HEART_RATE_RATIO
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_METABOLIC_CART
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_OTHER
import android.health.connect.datatypes.Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Vo2MaxFormatterTest {
    private val formatter = Vo2MaxFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatVo2MaxValue_returnsFormattedEntry_zero() {
        val record = getVo2MaxRecord(0.0)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("0.0 mL/(kg·min) Other")
    }

    @Test
    fun formatVo2MaxValue_returnsFormattedEntry_withCooperTest() {
        val record = getVo2MaxRecord(1.1, MEASUREMENT_METHOD_COOPER_TEST)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("1.1 mL/(kg·min) Cooper Test")
    }

    @Test
    fun formatVo2MaxValue_returnsFormattedEntry_withHeartRateRatio() {
        val record = getVo2MaxRecord(1.2, MEASUREMENT_METHOD_HEART_RATE_RATIO)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("1.2 mL/(kg·min) Heart Rate Ratio")
    }

    @Test
    fun formatVo2MaxValue_returnsFormattedEntry_withMetabolicCart() {
        val record = getVo2MaxRecord(1.3, MEASUREMENT_METHOD_METABOLIC_CART)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("1.3 mL/(kg·min) Metabolic Cart")
    }

    @Test
    fun formatVo2MaxValue_returnsFormattedEntry_withMultistageFitnessTest() {
        val record = getVo2MaxRecord(1.4, MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("1.4 mL/(kg·min) Multistage Fitness Test")
    }

    @Test
    fun formatVo2MaxValue_returnsFormattedEntry_withRockportFitnessTest() {
        val record = getVo2MaxRecord(1.5, MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST)

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("1.5 mL/(kg·min) Rockport Fitness Test")
    }

    private fun getVo2MaxRecord(value: Double, type: Int = MEASUREMENT_METHOD_OTHER): Vo2MaxRecord {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return Vo2MaxRecord.Builder(getMetaData(context), NOW, type, value).build()
    }
}
