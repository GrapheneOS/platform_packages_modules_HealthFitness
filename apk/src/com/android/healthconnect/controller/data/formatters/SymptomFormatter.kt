/*
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
import android.health.connect.datatypes.SymptomRecord
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.shared.BaseFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for printing SymptomRecord data. */
class SymptomFormatter
@Inject
constructor(
    @ApplicationContext context: Context,
    timeFormatter: LocalDateTimeFormatter,
    unitPreferences: UnitPreferences,
) : BaseFormatter<SymptomRecord>(context, timeFormatter, unitPreferences) {

    override suspend fun formatRecord(
        record: SymptomRecord,
        header: String,
        headerA11y: String,
    ): FormattedEntry {
        return FormattedEntry.SymptomEntry(
            uuid = record.metadata.id,
            header = header,
            headerA11y = headerA11y,
            title = formatValue(record),
            titleA11y = formatA11yValue(record),
            dataType = record::class,
            notes = record.notes,
        )
    }

    private fun formatValue(record: SymptomRecord): String {
        val severity = formatSeverity(record.severity)
        return if (severity.isNotEmpty()) {
            val type = formatSymptomType(record.symptomType)
            context.getString(R.string.symptom_entry_title, severity, type)
        } else {
            formatSymptomTypeTitleCase(record.symptomType)
        }
    }

    private fun formatA11yValue(record: SymptomRecord): String {
        return formatValue(record)
    }

    private fun formatSymptomType(type: Int): String {
        return when (type) {
            SymptomRecord.SYMPTOM_TYPE_COUGH ->
                context.getString(R.string.symptom_cough_lowercase_label)
            else -> context.getString(R.string.symptom_unknown)
        }
    }

    private fun formatSymptomTypeTitleCase(type: Int): String {
        return when (type) {
            SymptomRecord.SYMPTOM_TYPE_COUGH ->
                context.getString(R.string.symptom_cough_uppercase_label)
            else -> context.getString(R.string.symptom_unknown)
        }
    }

    private fun formatSeverity(severity: Int): String {
        return when (severity) {
            SymptomRecord.SEVERITY_MILD -> context.getString(R.string.symptom_severity_mild)
            SymptomRecord.SEVERITY_MODERATE -> context.getString(R.string.symptom_severity_moderate)
            SymptomRecord.SEVERITY_SEVERE -> context.getString(R.string.symptom_severity_severe)
            else -> ""
        }
    }
}
