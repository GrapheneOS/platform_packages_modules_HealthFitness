/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.dataentries.units

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.units.DistanceUnit.*
import com.android.healthconnect.controller.dataentries.units.EnergyUnit.*
import com.android.healthconnect.controller.dataentries.units.HeightUnit.*
import com.android.healthconnect.controller.dataentries.units.TemperatureUnit.CELSIUS
import com.android.healthconnect.controller.dataentries.units.TemperatureUnit.FAHRENHEIT
import com.android.healthconnect.controller.dataentries.units.TemperatureUnit.KELVIN
import com.android.healthconnect.controller.dataentries.units.UnitPreferences.Companion.DISTANCE_UNIT_PREF_KEY
import com.android.healthconnect.controller.dataentries.units.UnitPreferences.Companion.ENERGY_UNIT_PREF_KEY
import com.android.healthconnect.controller.dataentries.units.UnitPreferences.Companion.HEIGHT_UNIT_PREF_KEY
import com.android.healthconnect.controller.dataentries.units.UnitPreferences.Companion.TEMPERATURE_UNIT_PREF_KEY
import com.android.healthconnect.controller.dataentries.units.UnitPreferences.Companion.WEIGHT_UNIT_PREF_KEY
import com.android.healthconnect.controller.dataentries.units.UnitPreferencesStrings.getUnitLabel
import com.android.healthconnect.controller.dataentries.units.WeightUnit.KILOGRAM
import com.android.healthconnect.controller.dataentries.units.WeightUnit.POUND
import com.android.healthconnect.controller.dataentries.units.WeightUnit.STONE
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(PreferenceFragmentCompat::class)
class UnitsFragment : Hilt_UnitsFragment() {

    @Inject lateinit var unitsPreferences: UnitPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.units_screen, rootKey)

        val height =
            createUnitPreference(
                HEIGHT_UNIT_PREF_KEY,
                R.string.height_unit_title,
                unitsPreferences.getHeightUnit().toString()) { newUnit ->
                    unitsPreferences.setHeightUnit(HeightUnit.valueOf(newUnit))
                }
        val weight =
            createUnitPreference(
                WEIGHT_UNIT_PREF_KEY,
                R.string.weight_unit_title,
                unitsPreferences.getWeightUnit().toString()) { newUnit ->
                    unitsPreferences.setWeightUnit(WeightUnit.valueOf(newUnit))
                }
        val distance =
            createUnitPreference(
                DISTANCE_UNIT_PREF_KEY,
                R.string.distance_unit_title,
                unitsPreferences.getDistanceUnit().toString()) { newUnit ->
                    unitsPreferences.setDistanceUnit(DistanceUnit.valueOf(newUnit))
                }
        val energy =
            createUnitPreference(
                ENERGY_UNIT_PREF_KEY,
                R.string.energy_unit_title,
                unitsPreferences.getEnergyUnit().toString()) { newUnit ->
                    unitsPreferences.setEnergyUnit(EnergyUnit.valueOf(newUnit))
                }
        val temperature =
            createUnitPreference(
                TEMPERATURE_UNIT_PREF_KEY,
                R.string.temperature_unit_title,
                unitsPreferences.getTemperatureUnit().toString()) { newUnit ->
                    unitsPreferences.setTemperatureUnit(TemperatureUnit.valueOf(newUnit))
                }
        preferenceScreen.addPreference(height)
        preferenceScreen.addPreference(weight)
        preferenceScreen.addPreference(distance)
        preferenceScreen.addPreference(energy)
        preferenceScreen.addPreference(temperature)
    }

    private fun createUnitPreference(
        key: String,
        @StringRes title: Int,
        unitValue: String,
        onNewValue: (String) -> Unit
    ): ListPreference {
        val listPreference = ListPreference(context)

        with(listPreference) {
            isPersistent = false
            isIconSpaceReserved = false
            value = unitValue
            setKey(key)
            setTitle(title)
            setDialogTitle(title)
            setEntries(getEntries(key))
            setEntryValues(getEntriesValues(key))
            setSummary("%s")
            setNegativeButtonText(R.string.units_cancel)
        }
        listPreference.setOnPreferenceChangeListener { _, newValue ->
            onNewValue(newValue.toString())
            true
        }

        return listPreference
    }

    private fun getEntries(key: String): Array<String> {
        val entries = getUnits(key)
        return entries.map { getString(getUnitLabel(it)) }.toTypedArray()
    }

    private fun getEntriesValues(key: String): Array<String> {
        val entries = getUnits(key)
        return entries.map { it.toString() }.toTypedArray()
    }

    private fun getUnits(key: String): Array<UnitPreference> {
        return when (key) {
            DISTANCE_UNIT_PREF_KEY -> arrayOf(KILOMETERS, MILES)
            HEIGHT_UNIT_PREF_KEY -> arrayOf(CENTIMETERS, FEET)
            WEIGHT_UNIT_PREF_KEY -> arrayOf(POUND, KILOGRAM, STONE)
            ENERGY_UNIT_PREF_KEY -> arrayOf(CALORIE, KILOJOULE)
            TEMPERATURE_UNIT_PREF_KEY -> arrayOf(CELSIUS, FAHRENHEIT, KELVIN)
            else -> emptyArray()
        }
    }
}
