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
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_AWAKE
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_IN_BED
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_AWAKE_OUT_OF_BED
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT
import android.health.connect.datatypes.SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_REM
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataDetails
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedSample
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class SleepSessionFormatter {

    fun format(record: SleepSessionRecord, context: Context): FormattedDataDetails {
        return FormattedDataDetails(
            dataEntry = formatDataEntry(record, context),
            dataDetails = formatDataDetails(record, context),
        )
    }

    private fun formatDataDetails(
        record: SleepSessionRecord,
        context: Context,
    ): List<FormattedEntry> {
        val formattedDataDetails = mutableListOf<FormattedEntry>()

        if (!record.notes.isNullOrEmpty()) {
            formattedDataDetails.add(
                FormattedDataEntry(
                    header = context.getString(R.string.notes),
                    value = record.notes.toString(),
                )
            )
        }

        val formattedStages = mutableListOf<FormattedEntry>()
        record.stages.forEach { stage -> formattedStages.add(formatStage(stage, context)) }

        if (formattedStages.isEmpty()) {
            formattedDataDetails.add(
                FormattedEntry.Header(context.getString(R.string.sleep_no_stages))
            )
        } else {
            formattedDataDetails.addAll(formattedStages)
        }

        return formattedDataDetails
    }

    private fun formatStage(stage: SleepSessionRecord.Stage, context: Context): FormattedSample {
        return FormattedSample(
            header = getHeader(stage.startTime, stage.endTime),
            value = getStageType(stage.type, context),
        )
    }

    private fun getStageType(stageType: Int, context: Context): String {
        return when (stageType) {
            STAGE_TYPE_AWAKE -> context.getString(R.string.sleep_awake)
            STAGE_TYPE_AWAKE_IN_BED -> context.getString(R.string.sleep_awake_in_bed)
            STAGE_TYPE_AWAKE_OUT_OF_BED -> context.getString(R.string.sleep_awake_out_of_bed)
            STAGE_TYPE_SLEEPING -> context.getString(R.string.sleep_sleeping)
            STAGE_TYPE_SLEEPING_DEEP -> context.getString(R.string.sleep_deep_sleep)
            STAGE_TYPE_SLEEPING_LIGHT -> context.getString(R.string.sleep_light_sleep)
            STAGE_TYPE_SLEEPING_REM -> context.getString(R.string.sleep_rem_sleep)
            else -> context.getString(R.string.sleep_unknown)
        }
    }

    private fun formatDataEntry(record: SleepSessionRecord, context: Context): FormattedDataEntry {
        var title = context.getString(R.string.sleep_sleep_session)
        if (!record.title.isNullOrEmpty()) {
            title = record.title.toString()
        }

        return FormattedDataEntry(header = getHeader(record), value = title)
    }
}
