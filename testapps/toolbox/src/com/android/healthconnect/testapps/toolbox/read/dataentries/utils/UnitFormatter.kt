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
package com.android.healthconnect.testapps.toolbox.read.dataentries.utils

import android.content.Context
import android.health.connect.datatypes.units.BloodGlucose
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Mass
import android.health.connect.datatypes.units.Power
import android.health.connect.datatypes.units.Pressure
import android.health.connect.datatypes.units.Velocity
import android.health.connect.datatypes.units.Volume
import com.android.healthconnect.testapps.toolbox.R
import java.math.RoundingMode

class UnitFormatter {
    companion object {

        // Capture trailing zeros
        private val regex = Regex("(\\.\\d+?)0+\\b")

        fun formatSteps(count: Long, context: Context): String {
            return "$count ${context.getString(R.string.steps_label)}"
        }

        fun formatLength(length: Length, unit: Unit.Length, context: Context): String {

            val roundedLength =
                when (unit) {
                    Unit.Length.KILOMETERS -> round(length.inMeters / 1000, 2)
                    Unit.Length.METERS -> round(length.inMeters, 2)
                }
            return "${roundedLength.replace(regex,"$1")} ${context.getString(unit.label)}"
        }

        fun formatMass(mass: Mass, unit: Unit.Mass, context: Context): String {

            val roundedMass =
                when (unit) {
                    Unit.Mass.KILOGRAMS -> round(mass.inGrams / 1000, 1)
                    Unit.Mass.MILLIGRAMS -> round(mass.inGrams * 1000, 3)
                    Unit.Mass.GRAMS -> round(mass.inGrams, 1)
                }
            return "${roundedMass.replace(regex,"1$")} ${context.getString(unit.label)}"
        }

        fun formatEnergy(energy: Energy, context: Context): String {
            val roundedMass = round(energy.inCalories / 1000, 0)
            return "${roundedMass.replace(regex,"1$")} ${context.getString(R.string.calories_label)}"
        }

        fun formatPower(power: Power, context: Context): String {
            val roundedPower = round(power.inWatts, 1)
            return "${roundedPower.replace(regex,"$1")} ${context.getString(R.string.watts_label)}"
        }

        fun formatPower(power: Double, context: Context): String {
            val roundedPower = round(power, 1)
            return "${roundedPower.replace(regex,"$1")} ${context.getString(R.string.watts_label)}"
        }

        fun formatVelocity(velocity: Velocity, context: Context): String {
            val roundedVelocity = round(velocity.inMetersPerSecond * 3.6, 2)
            return "${roundedVelocity.replace(regex,"$1")} ${context.getString(R.string.kilometers_per_hour_label)}"
        }

        fun formatBloodGlucose(bloodGlucose: BloodGlucose, context: Context): String {
            val roundedBloodGlucose = round(bloodGlucose.inMillimolesPerLiter, 3)
            return "${roundedBloodGlucose.replace(regex,"$1")} ${context.getString(R.string.millimoles_per_liter_label)}"
        }

        fun formatPressure(pressure: Pressure): String {
            val roundedPressure = round(pressure.inMillimetersOfMercury, 2)
            return roundedPressure.replace(regex, "$1")
        }

        fun formatVolume(volume: Volume, context: Context): String {
            val roundedVolume = round(volume.inLiters, 3)
            return "${roundedVolume.replace(regex, "$1")} ${context.getString(R.string.liter_label)}"
        }

        fun round(value: Double, scale: Int): String {
            return value.toBigDecimal().setScale(scale, RoundingMode.UP).toString()
        }
    }
}
