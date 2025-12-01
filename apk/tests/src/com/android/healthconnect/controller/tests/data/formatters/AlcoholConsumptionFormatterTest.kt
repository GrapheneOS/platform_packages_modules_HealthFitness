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
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER
import android.health.connect.datatypes.AlcoholConsumptionRecord.AlcoholConsumptionBeverageType
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Volume
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.AlcoholConsumptionFormatter
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
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
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        InstrumentationRegistry.getInstrumentation().context.setLocale(Locale.US)
        hiltRule.inject()
    }

    @Test
    fun formatValue_returnsFormattedString() = runBlocking {
        val record =
            getAlcoholConsumptionRecord(beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER)
        assertThat(formatter.formatValue(record)).isEqualTo("Beer")
    }

    @Test
    fun formatA11yValue_returnsFormattedString() = runBlocking {
        val record =
            getAlcoholConsumptionRecord(beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER)
        assertThat(formatter.formatA11yValue(record)).isEqualTo("Beer")
    }

    @Test
    fun formatRecordDetails_withAllFieldsSet_returnsFormattedString() = runBlocking {
        val record =
            getAlcoholConsumptionRecord(
                volume = Volume.fromLiters(0.123),
                percentage = Percentage.fromValue(12.0),
                notes = "This is a note.",
            )
        val details = formatter.formatRecordDetails(record)
        assertThat(details.size).isEqualTo(4)
        assertThat(details[0]).isInstanceOf(FormattedEntry.FormattedSectionTitle::class.java)
        val sectionTitle = details[0] as FormattedEntry.FormattedSectionTitle
        assertThat(sectionTitle.title).isEqualTo("Details")

        assertThat(details[1]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val servingVolumeEntry = details[1] as FormattedEntry.ReverseSessionDetail
        assertThat(details[2]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val alcoholByVolumeEntry = details[2] as FormattedEntry.ReverseSessionDetail
        assertThat(details[3]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val notesEntry = details[3] as FormattedEntry.ReverseSessionDetail

        assertThat(servingVolumeEntry.title).isEqualTo("Serving Volume")
        assertThat(servingVolumeEntry.titleA11y).isEqualTo("Serving Volume")
        assertThat(servingVolumeEntry.header).isEqualTo("123 mL")
        assertThat(servingVolumeEntry.headerA11y).isEqualTo("123 milliliters")
        assertThat(alcoholByVolumeEntry.title).isEqualTo("Alcohol by Volume")
        assertThat(alcoholByVolumeEntry.titleA11y).isEqualTo("Alcohol by Volume")
        assertThat(alcoholByVolumeEntry.header).isEqualTo("12%")
        assertThat(alcoholByVolumeEntry.headerA11y).isEqualTo("12 percent")
        assertThat(notesEntry.title).isEqualTo("Notes")
        assertThat(notesEntry.titleA11y).isEqualTo("Notes")
        assertThat(notesEntry.header).isEqualTo("This is a note.")
        assertThat(notesEntry.headerA11y).isEqualTo("This is a note.")
    }

    @Test
    fun formatRecordDetails_withVolume_returnsFormattedString() = runBlocking {
        val record = getAlcoholConsumptionRecord(volume = Volume.fromLiters(0.5))
        val details = formatter.formatRecordDetails(record)
        assertThat(details.size).isEqualTo(2)
        assertThat(details[0]).isInstanceOf(FormattedEntry.FormattedSectionTitle::class.java)
        val sectionTitle = details[0] as FormattedEntry.FormattedSectionTitle
        assertThat(sectionTitle.title).isEqualTo("Details")

        assertThat(details[1]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val recordDetail = details[1] as FormattedEntry.ReverseSessionDetail
        assertThat(recordDetail.title).isEqualTo("Serving Volume")
        assertThat(recordDetail.titleA11y).isEqualTo("Serving Volume")
        assertThat(recordDetail.header).isEqualTo("500 mL")
        assertThat(recordDetail.headerA11y).isEqualTo("500 milliliters")
    }

    @Test
    fun formatRecordDetails_withVolumeOver1L_returnsFormattedString() = runBlocking {
        val record = getAlcoholConsumptionRecord(volume = Volume.fromLiters(1.234))
        val details = formatter.formatRecordDetails(record)
        assertThat(details.size).isEqualTo(2)
        assertThat(details[0]).isInstanceOf(FormattedEntry.FormattedSectionTitle::class.java)
        val sectionTitle = details[0] as FormattedEntry.FormattedSectionTitle
        assertThat(sectionTitle.title).isEqualTo("Details")

        assertThat(details[1]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val recordDetail = details[1] as FormattedEntry.ReverseSessionDetail
        assertThat(recordDetail.title).isEqualTo("Serving Volume")
        assertThat(recordDetail.titleA11y).isEqualTo("Serving Volume")
        assertThat(recordDetail.header).isEqualTo("1.234 L")
        assertThat(recordDetail.headerA11y).isEqualTo("1.234 liters")
    }

    @Test
    fun formatRecordDetails_withPercentage_returnsFormattedString() = runBlocking {
        val record = getAlcoholConsumptionRecord(percentage = Percentage.fromValue(12.0))
        val details = formatter.formatRecordDetails(record)
        assertThat(details.size).isEqualTo(2)
        assertThat(details[0]).isInstanceOf(FormattedEntry.FormattedSectionTitle::class.java)
        val sectionTitle = details[0] as FormattedEntry.FormattedSectionTitle
        assertThat(sectionTitle.title).isEqualTo("Details")

        assertThat(details[1]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val recordDetail = details[1] as FormattedEntry.ReverseSessionDetail
        assertThat(recordDetail.title).isEqualTo("Alcohol by Volume")
        assertThat(recordDetail.titleA11y).isEqualTo("Alcohol by Volume")
        assertThat(recordDetail.header).isEqualTo("12%")
        assertThat(recordDetail.headerA11y).isEqualTo("12 percent")
    }

    @Test
    fun formatRecordDetails_withNote_returnsFormattedString() = runBlocking {
        val record = getAlcoholConsumptionRecord(notes = "This is a note.")
        val details = formatter.formatRecordDetails(record)
        assertThat(details.size).isEqualTo(2)
        assertThat(details[0]).isInstanceOf(FormattedEntry.FormattedSectionTitle::class.java)
        val sectionTitle = details[0] as FormattedEntry.FormattedSectionTitle
        assertThat(sectionTitle.title).isEqualTo("Details")

        assertThat(details[1]).isInstanceOf(FormattedEntry.ReverseSessionDetail::class.java)
        val recordDetail = details[1] as FormattedEntry.ReverseSessionDetail
        assertThat(recordDetail.title).isEqualTo("Notes")
        assertThat(recordDetail.titleA11y).isEqualTo("Notes")
        assertThat(recordDetail.header).isEqualTo("This is a note.")
        assertThat(recordDetail.headerA11y).isEqualTo("This is a note.")
    }

    private fun getAlcoholConsumptionRecord(
        @AlcoholConsumptionBeverageType beverageType: Int = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER,
        volume: Volume? = null,
        percentage: Percentage? = null,
        notes: String? = null,
    ): AlcoholConsumptionRecord {
        return AlcoholConsumptionRecord.Builder(
                getMetaData(),
                NOW,
                NOW.plusSeconds(1),
                beverageType,
            )
            .setServingVolume(volume)
            .setAlcoholByVolume(percentage)
            .setNotes(notes)
            .build()
    }
}
