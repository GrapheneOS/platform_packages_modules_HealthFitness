/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.dataentries.units

import com.android.healthconnect.controller.dataentries.units.LengthUnit.IMPERIAL_LENGTH_UNIT
import com.android.healthconnect.controller.dataentries.units.LengthUnit.METRIC_LENGTH_UNIT

/** Length conversion utilities. */
object LengthConverter {
    private const val METERS_PER_MILE = 1609.3444978925634
    private const val METERS_PER_KM = 1000.0

    /**
     * Converts from meters to the provided distance units (miles or km)
     *
     * @param unit the units type to convert the passed in sourceCMeters (m) to
     * @param source the m length to convert to toUnit length
     * @return the sourceMeters in toUnit length units
     */
    fun convertDistanceFromMeters(unit: LengthUnit, source: Double): Double {
        return when (unit) {
            IMPERIAL_LENGTH_UNIT -> return source / METERS_PER_MILE
            METRIC_LENGTH_UNIT -> return source / METERS_PER_KM
        }
    }
}
