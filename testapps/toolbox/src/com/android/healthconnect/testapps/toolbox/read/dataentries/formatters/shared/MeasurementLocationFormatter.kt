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
import com.android.healthconnect.testapps.toolbox.R

class MeasurementLocationFormatter {

    fun formatBodyTemperature(location: Int, context: Context): String {
        return when (location) {
            MEASUREMENT_LOCATION_ARMPIT -> context.getString(R.string.measurement_location_armpit)
            MEASUREMENT_LOCATION_EAR -> context.getString(R.string.measurement_location_ear)
            MEASUREMENT_LOCATION_FINGER -> context.getString(R.string.measurement_location_finger)
            MEASUREMENT_LOCATION_FOREHEAD ->
                context.getString(R.string.measurement_location_forehead)
            MEASUREMENT_LOCATION_MOUTH -> context.getString(R.string.measurement_location_mouth)
            MEASUREMENT_LOCATION_RECTUM -> context.getString(R.string.measurement_location_rectum)
            MEASUREMENT_LOCATION_TEMPORAL_ARTERY ->
                context.getString(R.string.measurement_location_temporal_artery)
            MEASUREMENT_LOCATION_TOE -> context.getString(R.string.measurement_location_toe)
            MEASUREMENT_LOCATION_VAGINA -> context.getString(R.string.measurement_location_vagina)
            MEASUREMENT_LOCATION_WRIST -> context.getString(R.string.measurement_location_wrist)
            else -> ""
        }
    }
}
