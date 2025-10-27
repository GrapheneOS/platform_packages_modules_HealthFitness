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
import android.health.connect.datatypes.SymptomRecord
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.read.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.testapps.toolbox.read.utils.DataEntryUtils.Companion.getHeader

class SymptomFormatter {

    fun format(record: SymptomRecord, context: Context): FormattedDataEntry {
        return FormattedDataEntry(header = getHeader(record), value = formatValue(record, context))
    }

    private fun formatValue(record: SymptomRecord, context: Context): String {
        return "${formatSymptomType(record.symptomType, context)} - ${formatSeverity(record.severity, context)}"
    }

    private fun formatSymptomType(symptomType: Int, context: Context): String {
        return when (symptomType) {
            SymptomRecord.SYMPTOM_TYPE_COUGH -> context.getString(R.string.symptom_cough_label)
            SymptomRecord.SYMPTOM_TYPE_SNORE -> context.getString(R.string.symptom_snore_label)
            SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN ->
                context.getString(R.string.symptom_abdominal_pain_label)
            SymptomRecord.SYMPTOM_TYPE_ACNE -> context.getString(R.string.symptom_acne_label)
            SymptomRecord.SYMPTOM_TYPE_BACK_PAIN ->
                context.getString(R.string.symptom_back_pain_label)
            SymptomRecord.SYMPTOM_TYPE_BLOATING ->
                context.getString(R.string.symptom_bloating_label)
            SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG ->
                context.getString(R.string.symptom_brain_fog_label)
            SymptomRecord.SYMPTOM_TYPE_BREAST_TENDERNESS ->
                context.getString(R.string.symptom_breast_tenderness_label)
            SymptomRecord.SYMPTOM_TYPE_BRITTLE_NAILS ->
                context.getString(R.string.symptom_brittle_nails_label)
            SymptomRecord.SYMPTOM_TYPE_BURNING_MOUTH ->
                context.getString(R.string.symptom_burning_mouth_label)
            SymptomRecord.SYMPTOM_TYPE_CHEST_PAIN ->
                context.getString(R.string.symptom_chest_pain_label)
            SymptomRecord.SYMPTOM_TYPE_CHEST_TIGHTNESS ->
                context.getString(R.string.symptom_chest_tightness_label)
            SymptomRecord.SYMPTOM_TYPE_CHILLS -> context.getString(R.string.symptom_chills_label)
            SymptomRecord.SYMPTOM_TYPE_CONSTIPATION ->
                context.getString(R.string.symptom_constipation_label)
            SymptomRecord.SYMPTOM_TYPE_CRAMPS -> context.getString(R.string.symptom_cramps_label)
            SymptomRecord.SYMPTOM_TYPE_CRAVINGS ->
                context.getString(R.string.symptom_cravings_label)
            SymptomRecord.SYMPTOM_TYPE_DEHYDRATION ->
                context.getString(R.string.symptom_dehydration_label)
            SymptomRecord.SYMPTOM_TYPE_DIARRHEA ->
                context.getString(R.string.symptom_diarrhea_label)
            SymptomRecord.SYMPTOM_TYPE_DIFFICULTY_SWALLOWING ->
                context.getString(R.string.symptom_difficulty_swallowing_label)
            SymptomRecord.SYMPTOM_TYPE_DIZZINESS ->
                context.getString(R.string.symptom_dizziness_label)
            SymptomRecord.SYMPTOM_TYPE_DRY_SKIN ->
                context.getString(R.string.symptom_dry_skin_label)
            SymptomRecord.SYMPTOM_TYPE_EARACHES ->
                context.getString(R.string.symptom_earaches_label)
            SymptomRecord.SYMPTOM_TYPE_FATIGUE -> context.getString(R.string.symptom_fatigue_label)
            SymptomRecord.SYMPTOM_TYPE_FEVER -> context.getString(R.string.symptom_fever_label)
            SymptomRecord.SYMPTOM_TYPE_GENERALIZED_BODY_ACHE ->
                context.getString(R.string.symptom_generalized_body_ache_label)
            SymptomRecord.SYMPTOM_TYPE_HAIR_LOSS ->
                context.getString(R.string.symptom_hair_loss_label)
            SymptomRecord.SYMPTOM_TYPE_HEADACHE ->
                context.getString(R.string.symptom_headache_label)
            SymptomRecord.SYMPTOM_TYPE_HEARTBURN ->
                context.getString(R.string.symptom_heartburn_label)
            SymptomRecord.SYMPTOM_TYPE_HEART_PALPITATIONS ->
                context.getString(R.string.symptom_heart_palpitations_label)
            SymptomRecord.SYMPTOM_TYPE_HOT_FLASHES ->
                context.getString(R.string.symptom_hot_flashes_label)
            SymptomRecord.SYMPTOM_TYPE_INSOMNIA ->
                context.getString(R.string.symptom_insomnia_label)
            SymptomRecord.SYMPTOM_TYPE_JOINT_PAIN ->
                context.getString(R.string.symptom_joint_pain_label)
            SymptomRecord.SYMPTOM_TYPE_JOINT_STIFFNESS ->
                context.getString(R.string.symptom_joint_stiffness_label)
            SymptomRecord.SYMPTOM_TYPE_LOSS_OF_APPETITE ->
                context.getString(R.string.symptom_loss_of_appetite_label)
            SymptomRecord.SYMPTOM_TYPE_LOSS_OF_CONSCIOUSNESS ->
                context.getString(R.string.symptom_loss_of_consciousness_label)
            SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN ->
                context.getString(R.string.symptom_lower_back_pain_label)
            SymptomRecord.SYMPTOM_TYPE_MEMORY_LAPSE ->
                context.getString(R.string.symptom_memory_lapse_label)
            SymptomRecord.SYMPTOM_TYPE_MOOD_CHANGE ->
                context.getString(R.string.symptom_mood_change_label)
            SymptomRecord.SYMPTOM_TYPE_MUSCLE_PAIN ->
                context.getString(R.string.symptom_muscle_pain_label)
            SymptomRecord.SYMPTOM_TYPE_NAUSEA -> context.getString(R.string.symptom_nausea_label)
            SymptomRecord.SYMPTOM_TYPE_NIGHT_SWEATS ->
                context.getString(R.string.symptom_night_sweats_label)
            SymptomRecord.SYMPTOM_TYPE_PELVIC_PAIN ->
                context.getString(R.string.symptom_pelvic_pain_label)
            SymptomRecord.SYMPTOM_TYPE_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT ->
                context.getString(R.string.symptom_rapid_pounding_or_fluttering_heartbeat_label)
            SymptomRecord.SYMPTOM_TYPE_REDUCED_CAPACITY_FOR_EXERCISE ->
                context.getString(R.string.symptom_reduced_capacity_for_exercise_label)
            SymptomRecord.SYMPTOM_TYPE_RUNNY_NOSE ->
                context.getString(R.string.symptom_runny_nose_label)
            SymptomRecord.SYMPTOM_TYPE_SHORTNESS_OF_BREATH ->
                context.getString(R.string.symptom_shortness_of_breath_label)
            SymptomRecord.SYMPTOM_TYPE_SKIPPED_HEARTBEAT ->
                context.getString(R.string.symptom_skipped_heartbeat_label)
            SymptomRecord.SYMPTOM_TYPE_SLEEPINESS ->
                context.getString(R.string.symptom_sleepiness_label)
            SymptomRecord.SYMPTOM_TYPE_SLEEP_CHANGES ->
                context.getString(R.string.symptom_sleep_changes_label)
            SymptomRecord.SYMPTOM_TYPE_SNEEZING ->
                context.getString(R.string.symptom_sneezing_label)
            SymptomRecord.SYMPTOM_TYPE_SORE_THROAT ->
                context.getString(R.string.symptom_sore_throat_label)
            SymptomRecord.SYMPTOM_TYPE_STOMACH_ACHE ->
                context.getString(R.string.symptom_stomach_ache_label)
            SymptomRecord.SYMPTOM_TYPE_STUFFY_NOSE ->
                context.getString(R.string.symptom_stuffy_nose_label)
            SymptomRecord.SYMPTOM_TYPE_UNEXPLAINED_WEIGHT_CHANGES ->
                context.getString(R.string.symptom_unexplained_weight_changes_label)
            SymptomRecord.SYMPTOM_TYPE_VAGINAL_DRYNESS ->
                context.getString(R.string.symptom_vaginal_dryness_label)
            SymptomRecord.SYMPTOM_TYPE_VAGINAL_ITCHINESS ->
                context.getString(R.string.symptom_vaginal_itchiness_label)
            SymptomRecord.SYMPTOM_TYPE_VOMITING ->
                context.getString(R.string.symptom_vomiting_label)
            SymptomRecord.SYMPTOM_TYPE_WATER_RETENTION ->
                context.getString(R.string.symptom_water_retention_label)
            SymptomRecord.SYMPTOM_TYPE_WHEEZING ->
                context.getString(R.string.symptom_wheezing_label)
            else -> "Unknown"
        }
    }

    private fun formatSeverity(severity: Int, context: Context): String {
        return when (severity) {
            SymptomRecord.SEVERITY_UNSPECIFIED -> "Unspecified"
            SymptomRecord.SEVERITY_MILD -> "Mild"
            SymptomRecord.SEVERITY_MODERATE -> "Moderate"
            SymptomRecord.SEVERITY_SEVERE -> "Severe"
            else -> "Unknown"
        }
    }
}
