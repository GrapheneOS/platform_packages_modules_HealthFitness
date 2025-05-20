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
import android.health.connect.datatypes.CervicalMucusRecord
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_CREAMY
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_DRY
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_EGG_WHITE
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_STICKY
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_UNUSUAL
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_WATERY
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusSensation.SENSATION_HEAVY
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusSensation.SENSATION_LIGHT
import android.health.connect.datatypes.CervicalMucusRecord.CervicalMucusSensation.SENSATION_MEDIUM
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class CervicalMucusFormatter {

    fun format(record: CervicalMucusRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(header = getHeader(record), value = formatValue(record, context))
    }

    private fun formatValue(record: CervicalMucusRecord, context: Context): String {
        return "${getAppearance(record.appearance, context)} ${getSensation(record.sensation, context)}"
    }

    // Do string res later
    private fun getAppearance(appearance: Int, context: Context): String {
        return when (appearance) {
            APPEARANCE_DRY -> context.getString(R.string.cervical_mucus_appearance_dry)
            APPEARANCE_STICKY -> context.getString(R.string.cervical_mucus_appearance_sticky)
            APPEARANCE_CREAMY -> context.getString(R.string.cervical_mucus_appearance_creamy)
            APPEARANCE_WATERY -> context.getString(R.string.cervical_mucus_appearance_watery)
            APPEARANCE_EGG_WHITE -> context.getString(R.string.cervical_mucus_appearance_egg_white)
            APPEARANCE_UNUSUAL -> context.getString(R.string.cervical_mucus_appearance_unusual)
            else -> ""
        }
    }

    private fun getSensation(sensation: Int, context: Context): String {
        return when (sensation) {
            SENSATION_LIGHT -> context.getString(R.string.cervical_mucus_sensation_light)
            SENSATION_MEDIUM -> context.getString(R.string.cervical_mucus_sensation_medium)
            SENSATION_HEAVY -> context.getString(R.string.cervical_mucus_sensation_heavy)
            else -> ""
        }
    }
}
