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
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType.FLOW_HEAVY
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType.FLOW_LIGHT
import android.health.connect.datatypes.MenstruationFlowRecord.MenstruationFlowType.FLOW_MEDIUM
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class MenstruationFlowFormatter {

    fun format(record: MenstruationFlowRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(
            header = getHeader(record),
            value = formatFlow(record.flow, context),
        )
    }

    private fun formatFlow(flow: Int, context: Context): String {
        return when (flow) {
            FLOW_HEAVY -> context.getString(R.string.menstruation_flow_heavy)
            FLOW_MEDIUM -> context.getString(R.string.menstruation_flow_Medium)
            FLOW_LIGHT -> context.getString(R.string.menstruation_flow_light)
            else -> context.getString(R.string.menstruation_flow_unknown)
        }
    }
}
