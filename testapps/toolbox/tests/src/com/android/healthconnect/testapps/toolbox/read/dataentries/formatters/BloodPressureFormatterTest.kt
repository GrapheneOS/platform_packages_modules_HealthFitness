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
import android.health.connect.datatypes.BloodPressureRecord
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_UPPER_ARM
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_UPPER_ARM
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST
import android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_UNKNOWN
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_LYING_DOWN
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_RECLINING
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_SITTING_DOWN
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_STANDING_UP
import android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_UNKNOWN
import android.health.connect.datatypes.units.Pressure
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BloodPressureFormatterTest {
    private val formatter = BloodPressureFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatBloodPressureValue_returnsFormattedEntry_empty() {
        val record = getBloodPressureRecordBuilder(110.1, 79.5).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("110.1/79.5 mmHg")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedEntry.Header(value = "No details")))
    }

    @Test
    fun formatBloodPressureValue_returnsFormattedEntry_withMeasurementLocation_leftUpperArm() {
        val record =
            getBloodPressureRecordBuilder(
                    110.1,
                    79.5,
                    measurementLocation = BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
                )
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("110.1/79.5 mmHg")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedDataEntry("Measurement Location", "Left upper arm")))
    }

    @Test
    fun formatBloodPressureValue_returnsFormattedEntry_withMeasurementLocation_leftWrist() {
        val record =
            getBloodPressureRecordBuilder(
                    110.1,
                    79.5,
                    measurementLocation = BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST,
                )
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("110.1/79.5 mmHg")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedDataEntry("Measurement Location", "Left wrist")))
    }

    @Test
    fun formatBloodPressureValue_returnsFormattedEntry_withMeasurementLocation_rightUpperArm() {
        val record =
            getBloodPressureRecordBuilder(
                    110.1,
                    79.5,
                    measurementLocation = BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_UPPER_ARM,
                )
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("110.1/79.5 mmHg")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedDataEntry("Measurement Location", "Right upper arm")))
    }

    @Test
    fun formatBloodPressureValue_returnsFormattedEntry_withMeasurementLocation_rightWrist() {
        val record =
            getBloodPressureRecordBuilder(
                    110.1,
                    79.5,
                    measurementLocation = BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST,
                )
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("110.1/79.5 mmHg")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedDataEntry("Measurement Location", "Right wrist")))
    }

    @Test
    fun formatBloodPressureValue_returnsFormattedEntry_withBodyPosition_standingUp() {
        val record =
            getBloodPressureRecordBuilder(110.1, 79.5, bodyPosition = BODY_POSITION_STANDING_UP)
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("110.1/79.5 mmHg")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedDataEntry("Body Position", "Standing up")))
    }

    @Test
    fun formatBloodPressureValue_returnsFormattedEntry_withBodyPosition_sittingDown() {
        val record =
            getBloodPressureRecordBuilder(110.1, 79.5, bodyPosition = BODY_POSITION_SITTING_DOWN)
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("110.1/79.5 mmHg")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedDataEntry("Body Position", "Sitting down")))
    }

    @Test
    fun formatBloodPressureValue_returnsFormattedEntry_withBodyPosition_lyingDown() {
        val record =
            getBloodPressureRecordBuilder(110.1, 79.5, bodyPosition = BODY_POSITION_LYING_DOWN)
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("110.1/79.5 mmHg")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedDataEntry("Body Position", "Lying down")))
    }

    @Test
    fun formatBloodPressureValue_returnsFormattedEntry_withBodyPosition_reclining() {
        val record =
            getBloodPressureRecordBuilder(110.1, 79.5, bodyPosition = BODY_POSITION_RECLINING)
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.dataEntry.value).isEqualTo("110.1/79.5 mmHg")
        assertThat(formattedEntry.dataDetails)
            .isEqualTo(listOf(FormattedDataEntry("Body Position", "Reclining")))
    }

    private fun getBloodPressureRecordBuilder(
        systolic: Double,
        diastolic: Double,
        measurementLocation: Int = BLOOD_PRESSURE_MEASUREMENT_LOCATION_UNKNOWN,
        bodyPosition: Int = BODY_POSITION_UNKNOWN,
    ): BloodPressureRecord.Builder {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return BloodPressureRecord.Builder(
            getMetaData(context),
            NOW,
            measurementLocation,
            Pressure.fromMillimetersOfMercury(systolic),
            Pressure.fromMillimetersOfMercury(diastolic),
            bodyPosition,
        )
    }
}
