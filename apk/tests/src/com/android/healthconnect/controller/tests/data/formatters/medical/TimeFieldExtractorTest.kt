/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.data.formatters.medical

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.formatters.medical.TimeFieldExtractor
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TimeFieldExtractorTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var timeFieldExtractor: TimeFieldExtractor
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context

        hiltRule.inject()
    }

    @Test
    fun unknownResourceType_returnsEmptyString() {
        val json =
            """{
            "resourceType": "UnknownResource"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEmpty()
    }

    @Test
    fun condition_returnsFormattedDateYYYYMM() {
        val json =
            """{
            "resourceType": "Condition",
            "recordedDate": "2025-02"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("February 2025")
    }

    @Test
    fun condition_returnsFormattedDateYYYY() {
        val json =
            """{
            "resourceType": "Condition",
            "recordedDate": "2025"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("2025")
    }

    @Test
    fun condition_multipleTimestamps_returnsCorrectFormattedDate() {
        val json =
            """{
            "resourceType": "Condition",
            "onsetDateTime" : "2025-02",
            "onsetPeriod": {
                "start" : "2023-02"
            },
            "recordedDate": "2024-02"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("February 2025")
    }

    @Test
    fun allergyIntolerance_returnsFormattedDate() {
        val json =
            """{
            "resourceType": "AllergyIntolerance",
            "recordedDate": "2025-01-01T12:00:00Z"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("January 1, 2025")
    }

    @Test
    fun condition_returnsFormattedDate() {
        val json =
            """{
            "resourceType": "Condition",
            "recordedDate": "2025-02-02"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("February 2, 2025")
    }

    @Test
    fun observation_returnsFormattedDate() {
        val json =
            """{
            "resourceType": "Observation",
            "effectiveDateTime": "2025-03-03T12:00:00Z"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("March 3, 2025")
    }

    @Test
    fun procedure_returnsFormattedDate() {
        val json =
            """{
            "resourceType": "Procedure",
            "performedDateTime": "2025-04-04"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("April 4, 2025")
    }

    @Test
    fun immunization_returnsFormattedDate() {
        val json =
            """{
            "resourceType": "Immunization",
            "occurrenceDateTime": "2025-05-05"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("May 5, 2025")
    }

    @Test
    fun encounter_returnsFormattedDate() {
        val json =
            """{
            "resourceType": "Encounter",
            "period": {
                "start": "2025-06-06T12:00:00Z"
            }
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("June 6, 2025")
    }

    @Test
    fun medicationRequestAuthoredOn_returnsFormattedDate() {
        val json =
            """{
            "resourceType": "MedicationRequest",
            "authoredOn": "2025-05-10"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("May 10, 2025")
    }

    @Test
    fun medicationRequestDispenseRequest_returnsFormattedDate() {
        val json =
            """{
            "resourceType": "MedicationRequest",
            "dispenseRequest": {
                "validityPeriod": {
                    "start": "2025-05-10"
                }
            }
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEqualTo("May 10, 2025")
    }

    @Test
    fun missingTimeField_returnsEmptyString() {
        val json =
            """{
            "resourceType": "AllergyIntolerance"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEmpty()
    }

    @Test
    fun invalidTimeField_returnsEmptyString() {
        val json =
            """{
            "resourceType": "AllergyIntolerance",
            "recordedDate": "invalid-date"
        }"""
        val timeField = timeFieldExtractor.getTimeField(json)
        assertThat(timeField).isEmpty()
    }
}
