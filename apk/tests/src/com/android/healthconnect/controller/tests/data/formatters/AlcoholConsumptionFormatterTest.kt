/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.data.formatters

import android.health.connect.datatypes.AlcoholConsumptionRecord
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Volume
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.AlcoholConsumptionFormatter
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@RequiresFlagsEnabled(Flags.FLAG_ALCOHOL_CONSUMPTION)
class AlcoholConsumptionFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Inject lateinit var formatter: AlcoholConsumptionFormatter

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun formatValue_returnsFormattedString() = runBlocking {
        val record = getAlcoholConsumptionRecord(servingCount = 2)
        assertThat(formatter.formatValue(record)).isEqualTo("2 • Beer")
    }

    @Test
    fun formatA11yValue_returnsFormattedString() = runBlocking {
        val record = getAlcoholConsumptionRecord(servingCount = 2)
        assertThat(formatter.formatA11yValue(record)).isEqualTo("2 • Beer")
    }

    @Test
    fun formatRecordDetails_withVolume_returnsFormattedString() = runBlocking {
        val record = getAlcoholConsumptionRecord(servingCount = 2, volume = Volume.fromLiters(0.5))
        val details = formatter.formatRecordDetails(record)
        assertThat(details.size).isEqualTo(2)
        assertThat(details[0]).isInstanceOf(FormattedEntry.FormattedSectionTitle::class.java)
        val sectionTitle = details[0] as FormattedEntry.FormattedSectionTitle
        assertThat(sectionTitle.title).isEqualTo("Details")

        assertThat(details[1]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val sessionDetail = details[1] as FormattedEntry.ReverseSessionDetail
        assertThat(sessionDetail.header).isEqualTo("500 ml")
        assertThat(sessionDetail.headerA11y).isEqualTo("500 milliliters")
    }

    @Test
    fun formatRecordDetails_withPercentage_returnsFormattedString() = runBlocking {
        val record =
            getAlcoholConsumptionRecord(servingCount = 2, percentage = Percentage.fromValue(5.0))
        val details = formatter.formatRecordDetails(record)
        assertThat(details.size).isEqualTo(2)
        assertThat(details[0]).isInstanceOf(FormattedEntry.FormattedSectionTitle::class.java)
        val sectionTitle = details[0] as FormattedEntry.FormattedSectionTitle
        assertThat(sectionTitle.title).isEqualTo("Details")

        assertThat(details[1]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val sessionDetail = details[1] as FormattedEntry.ReverseSessionDetail
        assertThat(sessionDetail.header).isEqualTo("5%")
    }

    @Test
    fun formatRecordDetails_withNote_returnsFormattedString() = runBlocking {
        val record = getAlcoholConsumptionRecord(servingCount = 2, note = "note")
        val details = formatter.formatRecordDetails(record)
        assertThat(details.size).isEqualTo(2)
        assertThat(details[0]).isInstanceOf(FormattedEntry.FormattedSectionTitle::class.java)
        val sectionTitle = details[0] as FormattedEntry.FormattedSectionTitle
        assertThat(sectionTitle.title).isEqualTo("Details")

        assertThat(details[1]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val sessionDetail = details[1] as FormattedEntry.ReverseSessionDetail
        assertThat(sessionDetail.header).isEqualTo("note")
    }

    private fun getAlcoholConsumptionRecord(
        servingCount: Int,
        volume: Volume? = null,
        percentage: Percentage? = null,
        note: String? = null,
    ): AlcoholConsumptionRecord {
        return AlcoholConsumptionRecord.Builder(
                getMetaData(),
                NOW,
                NOW.plusSeconds(1),
                servingCount,
                AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER,
            )
            .setServingVolume(volume)
            .setAlcoholByVolume(percentage)
            .setNote(note)
            .build()
    }
}
