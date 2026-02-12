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
package com.android.healthconnect.controller.permissions.data

import androidx.annotation.StringRes
import com.android.healthconnect.controller.R

data class MedicalPermissionStrings(
    @StringRes val uppercaseLabel: Int,
    @StringRes val lowercaseLabel: Int,
    @StringRes val contentDescription: Int,
) {
    companion object {
        fun fromPermissionType(
            medicalPermissionType: MedicalPermissionType
        ): MedicalPermissionStrings {
            return PERMISSION_TYPE_STRINGS[medicalPermissionType]
                ?: throw IllegalArgumentException(
                    "No strings for permission group " + medicalPermissionType.name
                )
        }
    }
}

private val PERMISSION_TYPE_STRINGS: Map<MedicalPermissionType, MedicalPermissionStrings> =
    mapOf(
        MedicalPermissionType.ALL_MEDICAL_DATA to
            MedicalPermissionStrings(
                R.string.all_medical_data_uppercase_label,
                R.string.all_medical_data_lowercase_label,
                R.string.all_medical_data_content_description,
            ),
        MedicalPermissionType.ALLERGIES_INTOLERANCES to
            MedicalPermissionStrings(
                R.string.allergies_intolerances_uppercase_label,
                R.string.allergies_intolerances_lowercase_label,
                R.string.allergies_intolerances_content_description,
            ),
        MedicalPermissionType.CONDITIONS to
            MedicalPermissionStrings(
                R.string.conditions_uppercase_label,
                R.string.conditions_lowercase_label,
                R.string.conditions_content_description,
            ),
        MedicalPermissionType.LABORATORY_RESULTS to
            MedicalPermissionStrings(
                R.string.laboratory_results_uppercase_label,
                R.string.laboratory_results_lowercase_label,
                R.string.laboratory_results_content_description,
            ),
        MedicalPermissionType.MEDICATIONS to
            MedicalPermissionStrings(
                R.string.medications_uppercase_label,
                R.string.medications_lowercase_label,
                R.string.medications_content_description,
            ),
        MedicalPermissionType.PERSONAL_DETAILS to
            MedicalPermissionStrings(
                R.string.personal_details_uppercase_label,
                R.string.personal_details_lowercase_label,
                R.string.personal_details_content_description,
            ),
        MedicalPermissionType.PRACTITIONER_DETAILS to
            MedicalPermissionStrings(
                R.string.practitioner_details_uppercase_label,
                R.string.practitioner_details_lowercase_label,
                R.string.practitioner_details_content_description,
            ),
        MedicalPermissionType.PREGNANCY to
            MedicalPermissionStrings(
                R.string.pregnancy_uppercase_label,
                R.string.pregnancy_lowercase_label,
                R.string.pregnancy_content_description,
            ),
        MedicalPermissionType.PROCEDURES to
            MedicalPermissionStrings(
                R.string.procedures_uppercase_label,
                R.string.procedures_lowercase_label,
                R.string.procedures_content_description,
            ),
        MedicalPermissionType.SOCIAL_HISTORY to
            MedicalPermissionStrings(
                R.string.social_history_uppercase_label,
                R.string.social_history_lowercase_label,
                R.string.social_history_content_description,
            ),
        MedicalPermissionType.VACCINES to
            MedicalPermissionStrings(
                R.string.vaccines_uppercase_label,
                R.string.vaccines_lowercase_label,
                R.string.vaccines_content_description,
            ),
        MedicalPermissionType.VISITS to
            MedicalPermissionStrings(
                R.string.visits_uppercase_label,
                R.string.visits_lowercase_label,
                R.string.visits_content_description,
            ),
        MedicalPermissionType.VITAL_SIGNS to
            MedicalPermissionStrings(
                R.string.vital_signs_uppercase_label,
                R.string.vital_signs_lowercase_label,
                R.string.vital_signs_content_description,
            ),
    )
