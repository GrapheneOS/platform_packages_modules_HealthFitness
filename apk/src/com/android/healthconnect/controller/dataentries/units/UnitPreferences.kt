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

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import com.android.healthconnect.controller.dataentries.units.LengthUnit.IMPERIAL_LENGTH_UNIT
import com.android.healthconnect.controller.utils.Constants.UNIT_PREFERENCES_FILE
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UnitPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        const val DISTANCE_UNIT_PREF_KEY = "DISTANCE_UNIT_KEY"
        val DEFAULT_DISTANCE_UNIT = IMPERIAL_LENGTH_UNIT
    }

    private val unitSharedPreference: SharedPreferences by lazy {
        context.getSharedPreferences(UNIT_PREFERENCES_FILE, MODE_PRIVATE)
    }

    fun getDistanceUnit(): LengthUnit {
        if (!unitSharedPreference.contains(DISTANCE_UNIT_PREF_KEY)) {
            setDistanceUnit(DEFAULT_DISTANCE_UNIT)
            return DEFAULT_DISTANCE_UNIT
        }
        val unitString =
            unitSharedPreference.getString(
                DISTANCE_UNIT_PREF_KEY, DEFAULT_DISTANCE_UNIT.toString())!!
        return LengthUnit.valueOf(unitString)
    }

    fun setDistanceUnit(distanceUnit: LengthUnit) {
        with(unitSharedPreference.edit()) {
            putString(DISTANCE_UNIT_PREF_KEY, distanceUnit.toString())
            apply()
        }
    }
}

enum class LengthUnit {
    IMPERIAL_LENGTH_UNIT,
    METRIC_LENGTH_UNIT
}
