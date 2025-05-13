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
package com.android.healthconnect.testapps.toolbox.read.dataentries.formatters.shared

import android.content.Context
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FINGER
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_FOREHEAD
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_RECTUM
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TEMPORAL_ARTERY
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_TOE
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_VAGINA
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_WRIST
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BodyTemperatureMeasurementLocationFormatterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val formatter = MeasurementLocationFormatter()

    @Test
    fun formatMeasurementLocation_returnsArmpit() {
        val expectedLocation = "Armpit"

        val location = formatter.formatBodyTemperature(MEASUREMENT_LOCATION_ARMPIT, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsEar() {
        val expectedLocation = "Ear"

        val location = formatter.formatBodyTemperature(MEASUREMENT_LOCATION_EAR, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsFinger() {
        val expectedLocation = "Finger"

        val location = formatter.formatBodyTemperature(MEASUREMENT_LOCATION_FINGER, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsForehead() {
        val expectedLocation = "Forehead"

        val location = formatter.formatBodyTemperature(MEASUREMENT_LOCATION_FOREHEAD, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsMouth() {
        val expectedLocation = "Mouth"

        val location = formatter.formatBodyTemperature(MEASUREMENT_LOCATION_MOUTH, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsRectum() {
        val expectedLocation = "Rectum"

        val location = formatter.formatBodyTemperature(MEASUREMENT_LOCATION_RECTUM, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsTemporalArtery() {
        val expectedLocation = "Temporal Artery"

        val location =
            formatter.formatBodyTemperature(MEASUREMENT_LOCATION_TEMPORAL_ARTERY, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsToe() {
        val expectedLocation = "Toe"

        val location = formatter.formatBodyTemperature(MEASUREMENT_LOCATION_TOE, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsVagina() {
        val expectedLocation = "Vagina"

        val location = formatter.formatBodyTemperature(MEASUREMENT_LOCATION_VAGINA, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsWrist() {
        val expectedLocation = "Wrist"

        val location = formatter.formatBodyTemperature(MEASUREMENT_LOCATION_WRIST, context)

        assertThat(location).isEqualTo(expectedLocation)
    }

    @Test
    fun formatMeasurementLocation_returnsEmptyStringForUnknownLocation() {
        val expectedLocation = ""

        val location = formatter.formatBodyTemperature(111, context)

        assertThat(location).isEqualTo(expectedLocation)
    }
}
