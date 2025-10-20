/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.data.formatters

import android.content.Context
import android.health.connect.datatypes.SymptomRecord
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.shared.BaseFormatter
import com.android.healthconnect.controller.units.UnitPreferences
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Formatter for printing SymptomRecord data. */
class SymptomFormatter
@Inject
constructor(
    @ApplicationContext context: Context,
    timeFormatter: LocalDateTimeFormatter,
    unitPreferences: UnitPreferences,
) : BaseFormatter<SymptomRecord>(context, timeFormatter, unitPreferences) {

    override suspend fun formatRecord(
        record: SymptomRecord,
        header: String,
        headerA11y: String,
    ): FormattedEntry {
        return FormattedEntry.SymptomEntry(
            uuid = record.metadata.id,
            header = header,
            headerA11y = headerA11y,
            title = formatValue(record),
            titleA11y = formatA11yValue(record),
            dataType = record::class,
            notes = record.notes,
        )
    }

    private fun formatValue(record: SymptomRecord): String {
        val severity = formatSeverity(record.severity)
        return if (severity.isNotEmpty()) {
            val type = formatSymptomType(record.symptomType)
            context.getString(R.string.symptom_entry_title, severity, type)
        } else {
            formatSymptomTypeTitleCase(record.symptomType)
        }
    }

    private fun formatA11yValue(record: SymptomRecord): String {
        return formatValue(record)
    }

    private fun formatSymptomType(type: Int): String {
        return when (type) {
            SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN ->
                context.getString(R.string.abdominal_pain_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_ACNE -> context.getString(R.string.acne_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_BACK_PAIN ->
                context.getString(R.string.back_pain_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_BLOATING ->
                context.getString(R.string.bloating_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG ->
                context.getString(R.string.brain_fog_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_BREAST_TENDERNESS ->
                context.getString(R.string.breast_tenderness_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_BRITTLE_NAILS ->
                context.getString(R.string.brittle_nails_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_BURNING_MOUTH ->
                context.getString(R.string.burning_mouth_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_CHEST_PAIN ->
                context.getString(R.string.chest_pain_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_CHEST_TIGHTNESS ->
                context.getString(R.string.chest_tightness_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_CHILLS -> context.getString(R.string.chills_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_CONSTIPATION ->
                context.getString(R.string.constipation_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_COUGH ->
                context.getString(R.string.symptom_cough_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_CRAMPS -> context.getString(R.string.cramps_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_CRAVINGS ->
                context.getString(R.string.cravings_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_DEHYDRATION ->
                context.getString(R.string.dehydration_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_DIARRHEA ->
                context.getString(R.string.diarrhea_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_DIFFICULTY_SWALLOWING ->
                context.getString(R.string.difficulty_swallowing_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_DIZZINESS ->
                context.getString(R.string.dizziness_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_DRY_SKIN ->
                context.getString(R.string.dry_skin_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_EARACHES ->
                context.getString(R.string.earaches_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_FATIGUE ->
                context.getString(R.string.fatigue_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_FEVER -> context.getString(R.string.fever_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_GENERALIZED_BODY_ACHE ->
                context.getString(R.string.generalized_body_ache_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_HAIR_LOSS ->
                context.getString(R.string.hair_loss_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_HEADACHE ->
                context.getString(R.string.headache_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_HEARTBURN ->
                context.getString(R.string.heartburn_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_HEART_PALPITATIONS ->
                context.getString(R.string.heart_palpitations_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_HOT_FLASHES ->
                context.getString(R.string.hot_flashes_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_INSOMNIA ->
                context.getString(R.string.insomnia_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_JOINT_PAIN ->
                context.getString(R.string.joint_pain_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_JOINT_STIFFNESS ->
                context.getString(R.string.joint_stiffness_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_LOSS_OF_APPETITE ->
                context.getString(R.string.loss_of_appetite_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_LOSS_OF_CONSCIOUSNESS ->
                context.getString(R.string.loss_of_consciousness_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN ->
                context.getString(R.string.lower_back_pain_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_MEMORY_LAPSE ->
                context.getString(R.string.memory_lapse_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_MOOD_CHANGE ->
                context.getString(R.string.mood_change_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_MUSCLE_PAIN ->
                context.getString(R.string.muscle_pain_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_NAUSEA -> context.getString(R.string.nausea_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_NIGHT_SWEATS ->
                context.getString(R.string.night_sweats_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_PELVIC_PAIN ->
                context.getString(R.string.pelvic_pain_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT ->
                context.getString(R.string.rapid_pounding_or_fluttering_heartbeat_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_REDUCED_CAPACITY_FOR_EXERCISE ->
                context.getString(R.string.reduced_capacity_for_exercise_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_RUNNY_NOSE ->
                context.getString(R.string.runny_nose_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_SHORTNESS_OF_BREATH ->
                context.getString(R.string.shortness_of_breath_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_SKIPPED_HEARTBEAT ->
                context.getString(R.string.skipped_heartbeat_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_SLEEP_CHANGES ->
                context.getString(R.string.sleep_changes_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_SLEEPINESS ->
                context.getString(R.string.sleepiness_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_SNEEZING ->
                context.getString(R.string.sneezing_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_SNORE -> context.getString(R.string.snore_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_SORE_THROAT ->
                context.getString(R.string.sore_throat_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_STOMACH_ACHE ->
                context.getString(R.string.stomach_ache_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_STUFFY_NOSE ->
                context.getString(R.string.stuffy_nose_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_UNEXPLAINED_WEIGHT_CHANGES ->
                context.getString(R.string.unexplained_weight_changes_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_VAGINAL_DRYNESS ->
                context.getString(R.string.vaginal_dryness_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_VAGINAL_ITCHINESS ->
                context.getString(R.string.vaginal_itchiness_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_VOMITING ->
                context.getString(R.string.vomiting_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_WATER_RETENTION ->
                context.getString(R.string.water_retention_lowercase_label)
            SymptomRecord.SYMPTOM_TYPE_WHEEZING ->
                context.getString(R.string.wheezing_lowercase_label)
            else -> context.getString(R.string.symptom_unknown)
        }
    }

    private fun formatSymptomTypeTitleCase(type: Int): String {
        return when (type) {
            SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN ->
                context.getString(R.string.abdominal_pain_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_ACNE -> context.getString(R.string.acne_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_BACK_PAIN ->
                context.getString(R.string.back_pain_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_BLOATING ->
                context.getString(R.string.bloating_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG ->
                context.getString(R.string.brain_fog_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_BREAST_TENDERNESS ->
                context.getString(R.string.breast_tenderness_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_BRITTLE_NAILS ->
                context.getString(R.string.brittle_nails_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_BURNING_MOUTH ->
                context.getString(R.string.burning_mouth_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_CHEST_PAIN ->
                context.getString(R.string.chest_pain_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_CHEST_TIGHTNESS ->
                context.getString(R.string.chest_tightness_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_CHILLS -> context.getString(R.string.chills_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_CONSTIPATION ->
                context.getString(R.string.constipation_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_COUGH ->
                context.getString(R.string.symptom_cough_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_CRAMPS -> context.getString(R.string.cramps_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_CRAVINGS ->
                context.getString(R.string.cravings_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_DEHYDRATION ->
                context.getString(R.string.dehydration_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_DIARRHEA ->
                context.getString(R.string.diarrhea_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_DIFFICULTY_SWALLOWING ->
                context.getString(R.string.difficulty_swallowing_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_DIZZINESS ->
                context.getString(R.string.dizziness_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_DRY_SKIN ->
                context.getString(R.string.dry_skin_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_EARACHES ->
                context.getString(R.string.earaches_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_FATIGUE ->
                context.getString(R.string.fatigue_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_FEVER -> context.getString(R.string.fever_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_GENERALIZED_BODY_ACHE ->
                context.getString(R.string.generalized_body_ache_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_HAIR_LOSS ->
                context.getString(R.string.hair_loss_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_HEADACHE ->
                context.getString(R.string.headache_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_HEARTBURN ->
                context.getString(R.string.heartburn_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_HEART_PALPITATIONS ->
                context.getString(R.string.heart_palpitations_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_HOT_FLASHES ->
                context.getString(R.string.hot_flashes_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_INSOMNIA ->
                context.getString(R.string.insomnia_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_JOINT_PAIN ->
                context.getString(R.string.joint_pain_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_JOINT_STIFFNESS ->
                context.getString(R.string.joint_stiffness_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_LOSS_OF_APPETITE ->
                context.getString(R.string.loss_of_appetite_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_LOSS_OF_CONSCIOUSNESS ->
                context.getString(R.string.loss_of_consciousness_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN ->
                context.getString(R.string.lower_back_pain_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_MEMORY_LAPSE ->
                context.getString(R.string.memory_lapse_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_MOOD_CHANGE ->
                context.getString(R.string.mood_change_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_MUSCLE_PAIN ->
                context.getString(R.string.muscle_pain_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_NAUSEA -> context.getString(R.string.nausea_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_NIGHT_SWEATS ->
                context.getString(R.string.night_sweats_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_PELVIC_PAIN ->
                context.getString(R.string.pelvic_pain_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT ->
                context.getString(R.string.rapid_pounding_or_fluttering_heartbeat_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_REDUCED_CAPACITY_FOR_EXERCISE ->
                context.getString(R.string.reduced_capacity_for_exercise_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_RUNNY_NOSE ->
                context.getString(R.string.runny_nose_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_SHORTNESS_OF_BREATH ->
                context.getString(R.string.shortness_of_breath_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_SKIPPED_HEARTBEAT ->
                context.getString(R.string.skipped_heartbeat_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_SLEEP_CHANGES ->
                context.getString(R.string.sleep_changes_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_SLEEPINESS ->
                context.getString(R.string.sleepiness_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_SNEEZING ->
                context.getString(R.string.sneezing_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_SNORE -> context.getString(R.string.snore_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_SORE_THROAT ->
                context.getString(R.string.sore_throat_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_STOMACH_ACHE ->
                context.getString(R.string.stomach_ache_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_STUFFY_NOSE ->
                context.getString(R.string.stuffy_nose_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_UNEXPLAINED_WEIGHT_CHANGES ->
                context.getString(R.string.unexplained_weight_changes_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_VAGINAL_DRYNESS ->
                context.getString(R.string.vaginal_dryness_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_VAGINAL_ITCHINESS ->
                context.getString(R.string.vaginal_itchiness_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_VOMITING ->
                context.getString(R.string.vomiting_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_WATER_RETENTION ->
                context.getString(R.string.water_retention_uppercase_label)
            SymptomRecord.SYMPTOM_TYPE_WHEEZING ->
                context.getString(R.string.wheezing_uppercase_label)
            else -> context.getString(R.string.symptom_unknown)
        }
    }

    private fun formatSeverity(severity: Int): String {
        return when (severity) {
            SymptomRecord.SEVERITY_MILD -> context.getString(R.string.symptom_severity_mild)
            SymptomRecord.SEVERITY_MODERATE -> context.getString(R.string.symptom_severity_moderate)
            SymptomRecord.SEVERITY_SEVERE -> context.getString(R.string.symptom_severity_severe)
            else -> ""
        }
    }
}
