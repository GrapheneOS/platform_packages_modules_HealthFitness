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

import android.content.Context
import android.health.connect.datatypes.SymptomRecord
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.SymptomFormatter
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.LocalDate
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
@RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS)
class SymptomFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Inject lateinit var formatter: SymptomFormatter
    @Inject lateinit var preferences: UnitPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        hiltRule.inject()
    }

    @Test
    fun formatRecord_coughMild_returnsCorrectlyFormattedEntry() = runBlocking {
        val record =
            SymptomRecord.Builder(SymptomRecord.SYMPTOM_TYPE_COUGH, Instant.EPOCH, getMetaData())
                .setSeverity(SymptomRecord.SEVERITY_MILD)
                .setNotes("Slight tickle")
                .build()

        val formatted = formatter.formatRecord(record, "header", "headerA11y")

        assertThat(formatted).isInstanceOf(FormattedEntry.SymptomEntry::class.java)
        val symptomEntry = formatted as FormattedEntry.SymptomEntry
        assertThat(symptomEntry.title).isEqualTo("Mild cough")
        assertThat(symptomEntry.titleA11y).isEqualTo("Mild cough")
        assertThat(symptomEntry.notes).isEqualTo("Slight tickle")
    }

    @Test
    fun formatRecord_coughModerate_returnsCorrectlyFormattedEntry() = runBlocking {
        val record =
            SymptomRecord.Builder(SymptomRecord.SYMPTOM_TYPE_COUGH, Instant.EPOCH, getMetaData())
                .setSeverity(SymptomRecord.SEVERITY_MODERATE)
                .build()

        val formatted = formatter.formatRecord(record, "header", "headerA11y")

        assertThat(formatted).isInstanceOf(FormattedEntry.SymptomEntry::class.java)
        val symptomEntry = formatted as FormattedEntry.SymptomEntry
        assertThat(symptomEntry.title).isEqualTo("Moderate cough")
        assertThat(symptomEntry.notes).isNull()
    }

    @Test
    fun formatRecord_coughSevere_returnsCorrectlyFormattedEntry() = runBlocking {
        val record =
            SymptomRecord.Builder(SymptomRecord.SYMPTOM_TYPE_COUGH, Instant.EPOCH, getMetaData())
                .setSeverity(SymptomRecord.SEVERITY_SEVERE)
                .setNotes("")
                .build()

        val formatted = formatter.formatRecord(record, "header", "headerA11y")

        assertThat(formatted).isInstanceOf(FormattedEntry.SymptomEntry::class.java)
        val symptomEntry = formatted as FormattedEntry.SymptomEntry
        assertThat(symptomEntry.title).isEqualTo("Severe cough")
        assertThat(symptomEntry.notes).isEqualTo("")
    }

    @Test
    fun formatRecord_coughUnknownSeverity_returnsCorrectlyFormattedEntry() = runBlocking {
        val record =
            SymptomRecord.Builder(SymptomRecord.SYMPTOM_TYPE_COUGH, Instant.EPOCH, getMetaData())
                .setSeverity(SymptomRecord.SEVERITY_UNSPECIFIED)
                .setNotes("Some notes")
                .build()

        val formatted = formatter.formatRecord(record, "header", "headerA11y")

        assertThat(formatted).isInstanceOf(FormattedEntry.SymptomEntry::class.java)
        val symptomEntry = formatted as FormattedEntry.SymptomEntry
        assertThat(symptomEntry.title).isEqualTo("Cough")
        assertThat(symptomEntry.notes).isEqualTo("Some notes")
    }

    @Test
    fun formatRecord_unknownSymptom_returnsCorrectlyFormattedEntry() = runBlocking {
        // Skip validation to construct unknown symptom type as this is not possible in the public
        // APIs
        val record =
            SymptomRecord.Builder(SymptomRecord.SYMPTOM_TYPE_UNKNOWN, Instant.EPOCH, getMetaData())
                .setSeverity(SymptomRecord.SEVERITY_MILD)
                .setNotes("Some notes")
                .buildWithoutValidation()

        val formatted = formatter.formatRecord(record, "header", "headerA11y")

        assertThat(formatted).isInstanceOf(FormattedEntry.SymptomEntry::class.java)
        val symptomEntry = formatted as FormattedEntry.SymptomEntry
        assertThat(symptomEntry.title).isEqualTo("Mild Unknown")
    }

    @Test
    fun formatRecord_unknownSymptomUnknownSeverity_returnsCorrectlyFormattedEntry() = runBlocking {
        // Skip validation to construct unknown symptom type as this is not possible in the public
        // APIs
        val record =
            SymptomRecord.Builder(SymptomRecord.SYMPTOM_TYPE_UNKNOWN, Instant.EPOCH, getMetaData())
                .setSeverity(SymptomRecord.SEVERITY_UNSPECIFIED)
                .buildWithoutValidation()

        val formatted = formatter.formatRecord(record, "header", "headerA11y")

        assertThat(formatted).isInstanceOf(FormattedEntry.SymptomEntry::class.java)
        val symptomEntry = formatted as FormattedEntry.SymptomEntry
        assertThat(symptomEntry.title).isEqualTo("Unknown")
    }

    @Test
    fun format_intervalRecord_usesTimeRangeInHeader() = runBlocking {
        val startTime = Instant.parse("2023-01-01T10:00:00Z")
        val endTime = Instant.parse("2023-01-01T11:00:00Z")
        val record =
            SymptomRecord.Builder(
                    SymptomRecord.SYMPTOM_TYPE_COUGH,
                    startTime,
                    endTime,
                    getMetaData(),
                )
                .setSeverity(SymptomRecord.SEVERITY_MILD)
                .build()

        val formatted = formatter.format(record, "TestApp") as FormattedEntry.SymptomEntry

        assertThat(formatted.header).isEqualTo("10:00 AM - 11:00 AM • TestApp")
    }

    @Test
    fun format_instantRecord_usesTimeInHeader() = runBlocking {
        val time = Instant.parse("2023-01-01T10:00:00Z")
        val record =
            SymptomRecord.Builder(SymptomRecord.SYMPTOM_TYPE_COUGH, time, getMetaData())
                .setSeverity(SymptomRecord.SEVERITY_MILD)
                .build()

        val formatted = formatter.format(record, "TestApp") as FormattedEntry.SymptomEntry

        assertThat(formatted.header).isEqualTo("10:00 AM • TestApp")
    }

    @Test
    fun format_dateRecord_usesDateInHeader() = runBlocking {
        val date = LocalDate.parse("2023-01-01")
        val record =
            SymptomRecord.Builder(SymptomRecord.SYMPTOM_TYPE_COUGH, date, getMetaData())
                .setSeverity(SymptomRecord.SEVERITY_MILD)
                .build()

        val formatted = formatter.format(record, "TestApp") as FormattedEntry.SymptomEntry

        assertThat(formatted.header).isEqualTo("Jan 1, 2023 • TestApp")
    }
}
