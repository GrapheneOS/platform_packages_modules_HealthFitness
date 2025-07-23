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

package com.android.healthconnect.controller.data.formatters.medical

import android.content.Context
import com.android.healthconnect.controller.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class ExtractorUtils @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val RESOURCE_TYPE = "resourceType"

        const val PATIENT = "Patient"
        const val ENCOUNTER = "Encounter"
        const val CONDITION = "Condition"
        const val PROCEDURE = "Procedure"
        const val OBSERVATION = "Observation"
        const val ALLERGY_INTOLERANCE = "AllergyIntolerance"
        const val IMMUNIZATION = "Immunization"
        const val MEDICATION = "Medication"
        const val MEDICATION_REQUEST = "MedicationRequest"
        const val MEDICATION_STATEMENT = "MedicationStatement"
        const val MEDICATION_RESOURCE = "Medication"
        const val LOCATION = "Location"
        const val ORGANIZATION = "Organization"
        const val PRACTITIONER_ROLE = "PractitionerRole"
        const val PRACTITIONER = "Practitioner"
    }

    fun getResourceType(fhirResourceJson: String): String {
        val unknownResource = context.getString(R.string.unkwown_resource)
        val fhirData = JSONObject(fhirResourceJson)
        return fhirData.optString(RESOURCE_TYPE) ?: unknownResource
    }
}
