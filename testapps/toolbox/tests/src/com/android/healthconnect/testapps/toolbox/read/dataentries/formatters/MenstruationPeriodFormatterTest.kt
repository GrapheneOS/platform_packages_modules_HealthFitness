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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.Period.ofDays
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenstruationPeriodFormatterTest {

    private val formatter = MenstruationPeriodFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatMenstruationPeriodValue_returnsFormattedEntry() {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        val periodRecord = buildMenstruationPeriodRecord(NOW).build()

        val formattedRecord = formatter.format(periodRecord, context)

        assertThat(formattedRecord.value).containsMatch("Period Day \\d+ of 6")
    }

    private fun buildMenstruationPeriodRecord(
        startTime: Instant
    ): MenstruationPeriodRecord.Builder {
        return MenstruationPeriodRecord.Builder(
            getMetaData(context),
            startTime.minus(ofDays(5)).plusMillis(1),
            startTime,
        )
    }
}
