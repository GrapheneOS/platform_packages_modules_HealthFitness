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
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MOVEMENT
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MUSIC
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNGUIDED
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataDetails
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class MindfulnessSessionFormatter {

    fun format(record: MindfulnessSessionRecord, context: Context): FormattedDataDetails {
        return FormattedDataDetails(
            dataEntry = formatDataEntry(record, context),
            dataDetails = formatDataDetails(record, context),
        )
    }

    private fun formatDataDetails(
        record: MindfulnessSessionRecord,
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

        formattedDataDetails.add(
            FormattedDataEntry(
                header = context.getString(R.string.type),
                value = getMindfulnessSessionType(record.mindfulnessSessionType, context),
            )
        )

        return formattedDataDetails
    }

    private fun formatDataEntry(
        record: MindfulnessSessionRecord,
        context: Context,
    ): FormattedDataEntry {
        var title = context.getString(R.string.mindfulness_session_header)
        if (!record.title.isNullOrEmpty()) {
            title = record.title.toString()
        }

        return FormattedDataEntry(header = getHeader(record), value = title)
    }

    private fun getMindfulnessSessionType(type: Int, context: Context): String {
        return when (type) {
            MINDFULNESS_SESSION_TYPE_BREATHING ->
                context.getString(R.string.mindfulness_session_type_breathing)
            MINDFULNESS_SESSION_TYPE_MEDITATION ->
                context.getString(R.string.mindfulness_session_type_meditation)
            MINDFULNESS_SESSION_TYPE_MOVEMENT ->
                context.getString(R.string.mindfulness_session_type_movement)
            MINDFULNESS_SESSION_TYPE_MUSIC ->
                context.getString(R.string.mindfulness_session_type_music)
            MINDFULNESS_SESSION_TYPE_OTHER ->
                context.getString(R.string.mindfulness_session_type_other)
            MINDFULNESS_SESSION_TYPE_UNGUIDED ->
                context.getString(R.string.mindfulness_session_type_unguided)
            else -> context.getString(R.string.mindfulness_session_type_unknown)
        }
    }
}
