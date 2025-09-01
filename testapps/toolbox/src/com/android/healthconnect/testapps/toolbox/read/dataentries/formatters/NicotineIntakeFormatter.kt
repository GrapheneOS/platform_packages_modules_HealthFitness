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
import android.health.connect.datatypes.NicotineIntakeRecord
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_CIGARETTE
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_VAPE
import android.health.connect.datatypes.units.Mass
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.Unit
import com.android.healthconnect.testapps.toolbox.read.dataentries.utils.UnitFormatter.Companion.formatMass
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class NicotineIntakeFormatter {

    fun format(record: NicotineIntakeRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(header = getHeader(record), value = formatValue(record, context))
    }

    private fun formatValue(record: NicotineIntakeRecord, context: Context): String {

        return "${record.quantity} ${formatNicotineIntakeType(record.nicotineIntakeType, context)} ${formatMass(record.nicotineIntake?: Mass.fromGrams(0.0), Unit.Mass.MILLIGRAMS, context)}"
    }

    private fun formatNicotineIntakeType(nicotineIntakeType: Int, context: Context): String {
        return when (nicotineIntakeType) {
            NICOTINE_INTAKE_TYPE_CIGARETTE ->
                context.getString(R.string.nicotine_intake_type_cigarette)
            NICOTINE_INTAKE_TYPE_VAPE -> context.getString(R.string.nicotine_intake_type_vape)
            else -> context.getString(R.string.nicotine_intake_type_unknown)
        }
    }
}
