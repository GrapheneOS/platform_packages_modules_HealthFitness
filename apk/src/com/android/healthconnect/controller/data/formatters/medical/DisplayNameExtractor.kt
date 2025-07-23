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
package com.android.healthconnect.controller.data.formatters.medical

import android.content.Context
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.ALLERGY_INTOLERANCE
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.CONDITION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.ENCOUNTER
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.IMMUNIZATION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.LOCATION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.MEDICATION_REQUEST
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.MEDICATION_RESOURCE
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.MEDICATION_STATEMENT
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.OBSERVATION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.ORGANIZATION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.PATIENT
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.PRACTITIONER
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.PRACTITIONER_ROLE
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.PROCEDURE
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/** Extracts the most relevant display name from the FHIR resource. */
@Singleton
class DisplayNameExtractor
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val extractorUtils: ExtractorUtils,
) {

    private lateinit var unknownResource: String
    private lateinit var fhirData: JSONObject

    companion object {
        private const val NAME = "name"
        private const val USUAL = "usual"
        private const val OFFICIAL = "official"
        private const val USE = "use"
        private const val TEXT = "text"
        private const val FAMILY = "family"
        private const val GIVEN = "given"
        private const val CLASS = "class"
        private const val SERVICE_TYPE = "serviceType"
        private const val CODE = "code"
        private const val CODING = "coding"
        private const val DISPLAY = "display"
        private const val SYSTEM = "system"
        private const val SNOMED_SYSTEM = "http://snomed.info/sct"
        private const val LOINC_SYSTEM = "http://loinc.org"
        private const val CVX_SYSTEM = "http://hl7.org/fhir/sid/cvx"
        private const val VACCINE_CODE = "vaccineCode"
        private const val MEDICATION_CODEABLE_CONCEPT = "medicationCodeableConcept"
        private const val MEDICATION_REFERENCE = "medicationReference"
        private const val MEDICATION = "medication"
        private const val ALIAS = "alias"
        private const val PREFIX = "prefix"
        private const val SPECIALTY = "specialty"
    }

    fun getDisplayName(fhirResourceJson: String): String {
        unknownResource = context.getString(R.string.unkwown_resource)
        fhirData = JSONObject(fhirResourceJson)
        val resourceType = extractorUtils.getResourceType(fhirResourceJson)

        return when (resourceType) {
            ALLERGY_INTOLERANCE,
            CONDITION,
            MEDICATION_RESOURCE,
            PROCEDURE -> extractCodeDisplay(CODE, listOf(SNOMED_SYSTEM))
            ENCOUNTER -> extractEncounter()
            IMMUNIZATION -> extractCodeDisplay(VACCINE_CODE, listOf(CVX_SYSTEM, SNOMED_SYSTEM))
            LOCATION,
            ORGANIZATION -> extractNameOrAlias()
            MEDICATION_REQUEST,
            MEDICATION_STATEMENT -> extractMedicationConcept()
            OBSERVATION -> extractCodeDisplay(CODE, listOf(LOINC_SYSTEM, SNOMED_SYSTEM))
            PATIENT -> extractPatient()
            PRACTITIONER -> extractPractitioner()
            PRACTITIONER_ROLE -> extractPractitionerRole()
            else -> unknownResource
        }
    }

    private fun extractCodeDisplay(code: String, codings: List<String>): String {
        val codeOrVaccineCode = fhirData.optJSONObject(code) ?: return unknownResource
        return codeOrVaccineCode.optString(TEXT).takeIf { !it.isNullOrEmpty() }
            ?: codeOrVaccineCode.optJSONArray(CODING)?.let { codingArray ->
                for (codingSystem in codings) {
                    for (i in 0 until codingArray.length()) {
                        val coding = codingArray.getJSONObject(i)
                        if (coding.optString(SYSTEM) == codingSystem) {
                            return coding.optString(DISPLAY)
                        }
                    }
                }
                codingArray.optJSONObject(0)?.optString(DISPLAY)
            }
            ?: unknownResource
    }

    private fun extractEncounter(): String {
        val classText = fhirData.optJSONObject(CLASS)?.optStringOrNull(DISPLAY)
        val serviceText = extractTextOrDisplay(SERVICE_TYPE)
        return if (classText == null && serviceText == null) {
            unknownResource
        } else {
            concat(classText, serviceText, delim = " - ")
        }
    }

    private fun extractTextOrDisplay(key: String) =
        (fhirData.optJSONObject(key)?.optStringOrNull(TEXT)
            ?: fhirData
                .optJSONObject(key)
                ?.optJSONArray(CODING)
                ?.optJSONObject(0)
                ?.optString(DISPLAY))

    private fun extractNameOrAlias() =
        (fhirData.optStringOrNull(NAME)
            ?: fhirData.optJSONArray(ALIAS)?.optString(0).takeIf { !it.isNullOrEmpty() }
            ?: unknownResource)

    private fun extractMedicationConcept(): String {
        val medicationReference = fhirData.optJSONObject(MEDICATION_REFERENCE)
        val medicationCodeableConcept = fhirData.optJSONObject(MEDICATION_CODEABLE_CONCEPT)
        return medicationReference?.optStringOrNull(DISPLAY)
            ?: medicationCodeableConcept?.optStringOrNull(TEXT)
            ?: medicationCodeableConcept?.optJSONArray(CODING)?.let { codingArray ->
                for (i in 0 until codingArray.length()) {
                    val coding = codingArray.getJSONObject(i)
                    if (coding.optString(SYSTEM) == SNOMED_SYSTEM) {
                        return coding.optString(DISPLAY)
                    }
                }
                codingArray.optJSONObject(0)?.optString(DISPLAY)
            }
            ?: unknownResource
    }

    private fun extractPatient(): String {
        val names = fhirData.optJSONArray(NAME) ?: return unknownResource
        for (i in 0 until names.length()) {
            val name = names.getJSONObject(i)
            if (name.optString(USE) == USUAL) {
                return name.optString(TEXT)
                    ?: concat(name.optJSONArray(GIVEN)?.optString(0), name.optString(FAMILY))
            } else if (name.optString(USE) == OFFICIAL) {
                return name.optString(TEXT)
                    ?: concat(name.optJSONArray(GIVEN)?.optString(0), name.optString(FAMILY))
            }
        }
        if (names.length() > 0) {
            val firstName = names.getJSONObject(0)
            return firstName.optString(TEXT).takeIf { !it.isNullOrEmpty() }
                ?: concat(firstName.optJSONArray(GIVEN)?.optString(0), firstName.optString(FAMILY))
        }
        return unknownResource
    }

    private fun extractPractitioner(): String {
        val names = fhirData.optJSONArray(NAME) ?: return unknownResource
        for (i in 0 until names.length()) {
            val name = names.getJSONObject(i)
            if (name.optString(USE) == USUAL) {
                return concatWholeName(name)
            } else if (name.optString(USE) == OFFICIAL) {
                return concatWholeName(name)
            }
        }
        if (names.length() > 0) {
            val name = names.getJSONObject(0)
            return concatWholeName(name)
        }
        return unknownResource
    }

    private fun concatWholeName(name: JSONObject): String =
        name.optStringOrNull(TEXT)
            ?: concat(
                name.optJSONArray(PREFIX)?.optString(0),
                name.optJSONArray(GIVEN)?.optString(0),
                name.optString(FAMILY),
            )

    private fun extractPractitionerRole(): String {
        val codeText =
            fhirData.optJSONArray(CODE)?.optJSONObject(0)?.optStringOrNull(TEXT)
                ?: fhirData
                    .optJSONArray(CODE)
                    ?.optJSONObject(0)
                    ?.optJSONArray(CODING)
                    ?.optJSONObject(0)
                    ?.optString(DISPLAY)
        val specialtyText =
            fhirData.optJSONArray(SPECIALTY)?.optJSONObject(0)?.optStringOrNull(TEXT)
                ?: fhirData
                    .optJSONArray(SPECIALTY)
                    ?.optJSONObject(0)
                    ?.optJSONArray(CODING)
                    ?.optJSONObject(0)
                    ?.optString(DISPLAY)
        return if (codeText == null && specialtyText == null) {
            unknownResource
        } else {
            concat(codeText, specialtyText, delim = " - ")
        }
    }

    private fun concat(vararg args: String?, delim: String = " "): String {
        return args.filterNotNull().filter { it.isNotBlank() }.joinToString(delim)
    }
}

private fun JSONObject.optStringOrNull(s: String): String? {
    return this.optString(s).takeIf { it != "" }
}
