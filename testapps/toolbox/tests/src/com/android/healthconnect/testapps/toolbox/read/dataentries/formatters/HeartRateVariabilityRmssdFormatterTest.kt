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
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeartRateVariabilityRmssdFormatterTest {
    private val formatter = HeartRateVariabilityRmssdFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun formatHeartRateVariabilityRmssdValue_returnsFormattedEntry() {
        val record = getHeartRateVariabilityRmssdBuilder(55.2).build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("55.2 ms")
    }

    private fun getHeartRateVariabilityRmssdBuilder(
        heartRateVariability: Double
    ): HeartRateVariabilityRmssdRecord.Builder {
        val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")
        return HeartRateVariabilityRmssdRecord.Builder(
            getMetaData(context),
            NOW,
            heartRateVariability,
        )
    }
}
