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
package com.android.healthconnect.controller.tests.data.entries.api

import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.api.LoadSymptomEntriesUseCase
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.tests.utils.TestData.getSymptomRecord
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
class LoadSymptomEntriesUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val loadEntriesHelper: LoadEntriesHelper = mock()
    private lateinit var useCase: LoadSymptomEntriesUseCase

    @Before
    fun setup() {
        hiltRule.inject()
        useCase = LoadSymptomEntriesUseCase(Dispatchers.Main, loadEntriesHelper)
    }

    @Test
    fun execute_returnsSymptomEntries() = runTest {
        val symptomRecord = getSymptomRecord()
        val expected =
            listOf(
                FormattedEntry.SymptomEntry(
                    uuid = "uuid",
                    header = "Apr 5, 2023",
                    headerA11y = "Wednesday, April 5, 2023",
                    title = "Mild Cough",
                    titleA11y = "Mild Cough",
                    dataType = symptomRecord::class,
                    notes = "note",
                )
            )
        whenever(loadEntriesHelper.readRecords(any())).thenReturn(listOf(symptomRecord))
        whenever(loadEntriesHelper.maybeAddDateSectionHeaders(any(), any(), any()))
            .thenReturn(expected)

        val result =
            useCase.execute(
                LoadDataEntriesInput(
                    permissionType = FitnessPermissionType.SYMPTOM_COUGH,
                    packageName = null,
                    showDataOrigin = false,
                    period = DateNavigationPeriod.PERIOD_WEEK,
                    displayedStartTime = Instant.now(),
                )
            )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun execute_noRecords_returnsEmptyList() = runTest {
        whenever(loadEntriesHelper.readRecords(any())).thenReturn(emptyList())
        whenever(loadEntriesHelper.maybeAddDateSectionHeaders(any(), any(), any()))
            .thenReturn(emptyList())

        val result =
            useCase.execute(
                LoadDataEntriesInput(
                    permissionType = FitnessPermissionType.SYMPTOM_COUGH,
                    packageName = null,
                    showDataOrigin = false,
                    period = DateNavigationPeriod.PERIOD_WEEK,
                    displayedStartTime = Instant.now(),
                )
            )

        assertThat(result).isEmpty()
    }

    @Test
    fun execute_filtersCorrectSymptomType() = runTest {
        val coughRecord = getSymptomRecord(symptomType = 13) // COUGH
        val feverRecord = getSymptomRecord(symptomType = 23) // FEVER
        val expected =
            listOf(
                FormattedEntry.SymptomEntry(
                    uuid = "uuid",
                    header = "Apr 5, 2023",
                    headerA11y = "Wednesday, April 5, 2023",
                    title = "Mild Cough",
                    titleA11y = "Mild Cough",
                    dataType = coughRecord::class,
                    notes = "note",
                )
            )
        whenever(loadEntriesHelper.readRecords(any())).thenReturn(listOf(coughRecord, feverRecord))
        whenever(
                loadEntriesHelper.maybeAddDateSectionHeaders(eq(listOf(coughRecord)), any(), any())
            )
            .thenReturn(expected)

        val result =
            useCase.execute(
                LoadDataEntriesInput(
                    permissionType = FitnessPermissionType.SYMPTOM_COUGH,
                    packageName = null,
                    showDataOrigin = false,
                    period = DateNavigationPeriod.PERIOD_WEEK,
                    displayedStartTime = Instant.now(),
                )
            )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun execute_filtersBySymptomType_onlyReturnsMatchingType() = runTest {
        val permissionType = FitnessPermissionType.SYMPTOM_COUGH
        val correctSymptomType = 13 // COUGH
        val otherSymptomType1 = 2 // Acne
        val otherSymptomType2 = 3 // Back Pain

        val record1 = getSymptomRecord(symptomType = correctSymptomType)
        val record2 = getSymptomRecord(symptomType = otherSymptomType1)
        val record3 = getSymptomRecord(symptomType = correctSymptomType)
        val record4 = getSymptomRecord(symptomType = otherSymptomType2)

        val input =
            LoadDataEntriesInput(
                permissionType,
                packageName = null,
                displayedStartTime = Instant.now(),
                period = DateNavigationPeriod.PERIOD_WEEK,
                showDataOrigin = false,
            )
        whenever(loadEntriesHelper.readRecords(input))
            .thenReturn(listOf(record1, record2, record3, record4))

        val formattedEntry1: FormattedEntry = mock()
        val formattedEntry3: FormattedEntry = mock()
        val expectedFilteredRecords = listOf(record1, record3)
        whenever(
                loadEntriesHelper.maybeAddDateSectionHeaders(
                    eq(expectedFilteredRecords),
                    any(),
                    any(),
                )
            )
            .thenReturn(listOf(formattedEntry1, formattedEntry3))

        val result = useCase.execute(input)

        assertThat(result).containsExactly(formattedEntry1, formattedEntry3)
    }
}
