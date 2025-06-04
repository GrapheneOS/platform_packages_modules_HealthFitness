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
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class Vo2MaxFormatter {

    fun format(record: Vo2MaxRecord, context: Context): FormattedDataEntry {

        return FormattedDataEntry(
            header = getHeader(record),
            value =
                "${record.vo2MillilitersPerMinuteKilogram} ${context.getString(R.string.vo2_milliliters_per_minute_kilogram_label)} ${formatMeasurementMethod(record.measurementMethod, context)}",
        )
    }

    private fun formatMeasurementMethod(measurementMethod: Int, context: Context): String {
        return when (measurementMethod) {
            MEASUREMENT_METHOD_COOPER_TEST ->
                context.getString(R.string.vo2Max_measurement_method_cooper_test)
            MEASUREMENT_METHOD_HEART_RATE_RATIO ->
                context.getString(R.string.vo2Max_measurement_method_heart_rate_ratio)
            MEASUREMENT_METHOD_METABOLIC_CART ->
                context.getString(R.string.vo2Max_measurement_method_metabolic_cart)
            MEASUREMENT_METHOD_MULTISTAGE_FITNESS_TEST ->
                context.getString(R.string.vo2Max_measurement_method_multistage_fitness_test)
            MEASUREMENT_METHOD_OTHER -> context.getString(R.string.vo2Max_measurement_method_other)
            MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST ->
                context.getString(R.string.vo2Max_measurement_method_rockport_fitness_test)
            else -> context.getString(R.string.vo2Max_measurement_method_unknown)
        }
    }
}
