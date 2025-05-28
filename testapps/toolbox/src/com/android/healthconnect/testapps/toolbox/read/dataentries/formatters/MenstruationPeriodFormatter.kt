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
import android.health.connect.datatypes.MenstruationPeriodRecord
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit.DAYS

class MenstruationPeriodFormatter {

    fun format(record: MenstruationPeriodRecord, context: Context): FormattedDataEntry {

        return FormattedDataEntry(
            header = getHeader(record),
            value =
                context.getString(
                    R.string.menstruation_flow_description,
                    dayOfPeriod(record, Instant.now()).toString(),
                    totalDaysOfPeriod(record).toString(),
                ),
        )
    }

    private fun dayOfPeriod(record: MenstruationPeriodRecord, day: Instant): Int {
        return (Period.between(record.startTime.toLocalDate(), day.toLocalDate()).days + 1)
    }

    private fun totalDaysOfPeriod(record: MenstruationPeriodRecord): Int {
        return (DAYS.between(record.startTime.toLocalDate(), record.endTime.toLocalDate()).toInt() +
            1)
    }

    private fun Instant.toLocalDate(): LocalDate {
        return this.atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
