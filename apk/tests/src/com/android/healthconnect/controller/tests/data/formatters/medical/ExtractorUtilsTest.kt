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
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExtractorUtilsTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var extractorUtils: ExtractorUtils
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context

        hiltRule.inject()
    }

    @Test
    fun unknownResourceType() {
        val json =
            """{
            "resourceType": "UnknownResource"
        }"""
        assertEquals("UnknownResource", extractorUtils.getResourceType(json))
    }

    @Test
    fun patient() {
        val json =
            """{
            "resourceType": "Patient"
        }"""
        assertEquals("Patient", extractorUtils.getResourceType(json))
    }

    @Test
    fun encounter() {
        val json =
            """{
            "resourceType": "Encounter"
        }"""
        assertEquals("Encounter", extractorUtils.getResourceType(json))
    }

    @Test
    fun condition() {
        val json =
            """{
            "resourceType": "Condition"
        }"""
        assertEquals("Condition", extractorUtils.getResourceType(json))
    }

    @Test
    fun procedure() {
        val json =
            """{
            "resourceType": "Procedure"
        }"""
        assertEquals("Procedure", extractorUtils.getResourceType(json))
    }

    @Test
    fun observation() {
        val json =
            """{
            "resourceType": "Observation"
        }"""
        assertEquals("Observation", extractorUtils.getResourceType(json))
    }

    @Test
    fun allergyIntolerance() {
        val json =
            """{
            "resourceType": "AllergyIntolerance"
        }"""
        assertEquals("AllergyIntolerance", extractorUtils.getResourceType(json))
    }

    @Test
    fun immunization() {
        val json =
            """{
            "resourceType": "Immunization"
        }"""
        assertEquals("Immunization", extractorUtils.getResourceType(json))
    }

    @Test
    fun medication() {
        val json =
            """{
            "resourceType": "Medication"
        }"""
        assertEquals("Medication", extractorUtils.getResourceType(json))
    }

    @Test
    fun medicationRequest() {
        val json =
            """{
            "resourceType": "MedicationRequest"
        }"""
        assertEquals("MedicationRequest", extractorUtils.getResourceType(json))
    }

    @Test
    fun medicationStatement() {
        val json =
            """{
            "resourceType": "MedicationStatement"
        }"""
        assertEquals("MedicationStatement", extractorUtils.getResourceType(json))
    }

    @Test
    fun location() {
        val json =
            """{
            "resourceType": "Location"
        }"""
        assertEquals("Location", extractorUtils.getResourceType(json))
    }

    @Test
    fun organization() {
        val json =
            """{
            "resourceType": "Organization"
        }"""
        assertEquals("Organization", extractorUtils.getResourceType(json))
    }

    @Test
    fun practitionerRole() {
        val json =
            """{
            "resourceType": "PractitionerRole"
        }"""
        assertEquals("PractitionerRole", extractorUtils.getResourceType(json))
    }

    @Test
    fun practitioner() {
        val json =
            """{
            "resourceType": "Practitioner"
        }"""
        assertEquals("Practitioner", extractorUtils.getResourceType(json))
    }
}
