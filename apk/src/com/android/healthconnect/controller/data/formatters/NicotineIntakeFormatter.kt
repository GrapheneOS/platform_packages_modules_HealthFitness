/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.data.formatters

import android.content.Context
import android.health.connect.datatypes.NicotineIntakeRecord
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_CIGARETTE
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_VAPE
import android.icu.number.IntegerWidth
import android.icu.number.NumberFormatter
import android.icu.number.NumberFormatter.UnitWidth
import android.icu.number.Precision
import android.icu.text.MessageFormat
import android.icu.util.MeasureUnit.MILLIGRAM
import androidx.annotation.StringRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.formatters.shared.EntryFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject

class NicotineIntakeFormatter
@Inject
constructor(
    @ApplicationContext context: Context,
    timeFormatter: LocalDateTimeFormatter,
    unitPreferences: UnitPreferences,
) : EntryFormatter<NicotineIntakeRecord>(context, timeFormatter, unitPreferences) {

    override suspend fun formatValue(record: NicotineIntakeRecord): String {
        return formatNicotineIntake(R.string.nicotine_intake_title, record)
    }

    override suspend fun formatA11yValue(record: NicotineIntakeRecord): String {
        return formatA11yNicotineIntake(R.string.nicotine_intake_title_long, record)
    }

    private fun formatNicotineIntake(
        @StringRes nicotineIntakeRes: Int,
        record: NicotineIntakeRecord,
    ): String {
        val nicotineIntakeType = getNicotineIntakeType(record.nicotineIntakeType, record.quantity)
        if (record.nicotineIntake == null) {
            return nicotineIntakeType
        }

        val formattedIntake =
            NumberFormatter.withLocale(Locale.getDefault())
                .unit(MILLIGRAM)
                .unitWidth(UnitWidth.SHORT)
                .precision(Precision.maxFraction(2))
                .integerWidth(IntegerWidth.zeroFillTo(1))
                .format(record.nicotineIntake!!.inGrams * 1000)

        return context.getString(nicotineIntakeRes, nicotineIntakeType, formattedIntake)
    }

    private fun formatA11yNicotineIntake(
        @StringRes nicotineIntakeRes: Int,
        record: NicotineIntakeRecord,
    ): String {
        val nicotineIntakeType = getNicotineIntakeType(record.nicotineIntakeType, record.quantity)
        if (record.nicotineIntake == null) {
            return nicotineIntakeType
        }

        val formattedIntake =
            NumberFormatter.withLocale(Locale.getDefault())
                .unit(MILLIGRAM)
                .unitWidth(UnitWidth.FULL_NAME)
                .precision(Precision.maxFraction(2))
                .integerWidth(IntegerWidth.zeroFillTo(1))
                .format(record.nicotineIntake!!.inGrams * 1000)

        return context.getString(nicotineIntakeRes, nicotineIntakeType, formattedIntake)
    }

    private fun getNicotineIntakeType(type: Int, quantity: Int): String {
        return when (type) {
            NICOTINE_INTAKE_TYPE_VAPE -> {
                MessageFormat.format(
                    context.getString(R.string.nicotine_intake_type_vape),
                    mapOf("quantity" to quantity),
                )
            }

            NICOTINE_INTAKE_TYPE_CIGARETTE -> {
                MessageFormat.format(
                    context.getString(R.string.nicotine_intake_type_cigarette),
                    mapOf("quantity" to quantity),
                )
            }

            else ->
                MessageFormat.format(
                    context.getString(R.string.nicotine_intake_type_unknown),
                    mapOf("quantity" to quantity),
                )
        }
    }
}
