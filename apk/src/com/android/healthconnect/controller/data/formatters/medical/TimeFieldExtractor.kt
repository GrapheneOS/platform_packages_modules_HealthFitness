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

import android.util.Log
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.ALLERGY_INTOLERANCE
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.CONDITION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.ENCOUNTER
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.IMMUNIZATION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.MEDICATION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.MEDICATION_REQUEST
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.MEDICATION_STATEMENT
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.OBSERVATION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.ORGANIZATION
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.PATIENT
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.PRACTITIONER_ROLE
import com.android.healthconnect.controller.data.formatters.medical.ExtractorUtils.Companion.PROCEDURE
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/** Extracts the most relevant display name from the FHIR resource. */
@Singleton
class TimeFieldExtractor
@Inject
constructor(
    private val extractorUtils: ExtractorUtils,
    private val localDateTimeFormatter: LocalDateTimeFormatter,
) {

    private lateinit var fhirData: JSONObject
    private val dateTimeRegex =
        Regex(
            """^([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])(T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\.[0-9]+)?(Z|(\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)))?)?)?${'$'}"""
        )

    companion object {
        private const val TAG = "TimeFieldExtractor"
    }

    /**
     * Attempts to extract a time field from a FHIR resource.
     *
     * If any error occurs, fallbacks to an empty string therefore default header is show.
     */
    fun getTimeField(fhirResourceJson: String): String {
        fhirData = JSONObject(fhirResourceJson)
        val resourceType = extractorUtils.getResourceType(fhirResourceJson)
        val rawTimeField =
            when (resourceType) {
                ALLERGY_INTOLERANCE -> extractRawTimeField(KeyPriorityList.AllergyIntolerance)
                CONDITION -> extractRawTimeField(KeyPriorityList.Condition)
                OBSERVATION -> extractRawTimeField(KeyPriorityList.Observation)
                PROCEDURE -> extractRawTimeField(KeyPriorityList.Procedure)
                IMMUNIZATION -> extractRawTimeField(KeyPriorityList.Immunization)
                ENCOUNTER -> extractRawTimeField(KeyPriorityList.Encounter)
                MEDICATION_REQUEST -> extractRawTimeField(KeyPriorityList.MedicationRequest)
                MEDICATION_STATEMENT -> extractRawTimeField(KeyPriorityList.MedicationStatement)
                MEDICATION,
                ORGANIZATION,
                PRACTITIONER_ROLE,
                PATIENT -> "" // No time field in resource type
                else -> {
                    Log.e(TAG, "Unknown resource type: $resourceType")
                    return ""
                }
            }

        if (rawTimeField.isEmpty()) {
            Log.w(TAG, "Time field empty in $resourceType")
            return ""
        }
        val timeFieldFormat = determineFormat(rawTimeField)
        return formatTime(rawTimeField, timeFieldFormat)
    }

    /**
     * Extracts the first available raw, unparsed time-related string from the `fhirData` based on a
     * prioritized set of keys.
     *
     * This function iterates through the keys in the given [keyPriorityList] in a predefined order.
     *
     * @param keyPriorityList The set of [TimeFieldKey]s to search for, in order of priority.
     * @return The raw string value of the first time field found, or an empty string if no field is
     *   found.
     */
    private fun extractRawTimeField(keyPriorityList: KeyPriorityList): String {
        keyPriorityList.keys.forEach { key ->
            val rawTimeFieldString = getNestedString(fhirData, key.path)
            if (!rawTimeFieldString.isNullOrBlank()) {
                return rawTimeFieldString
            }
        }
        Log.w(TAG, "No time field found in list ${keyPriorityList.keys}")
        return ""
    }

    /**
     * Formats the given time field string based on its [TimeFieldFormat].
     *
     * Supports the following formats:
     * - YYYY: Year (e.g. "2025")
     * - YYYY-MM: Year and month (e.g. "2025-07")
     * - YYYY-MM-DD: Year, month, and day (e.g. "2025-07-16")
     * - ISO-8601: Date time in ISO 8601 format (e.g. "2025-07-16T14:00:00+00:00")
     *
     * @return The formatted time field string (e.g. "July 16, 2025" or "July 16" or "2025").
     */
    private fun formatTime(timeFieldString: String, timeFieldFormat: TimeFieldFormat): String {
        return when (timeFieldFormat) {
            TimeFieldFormat.YYYY -> timeFieldString
            TimeFieldFormat.YYYY_MM -> formatYYYYMM(timeFieldString)
            TimeFieldFormat.YYYY_MM_DD -> formatYYYYMMDD(timeFieldString)
            TimeFieldFormat.ISO_8601 -> formatDateTime(timeFieldString)
            else -> {
                Log.w(TAG, "Unknown time field format: $timeFieldFormat")
                ""
            }
        }
    }

    private fun formatYYYYMMDD(dateString: String): String {
        val localDate = LocalDate.parse(dateString)
        val instant = localDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        return localDateTimeFormatter.formatLongDate(instant)
    }

    private fun formatYYYYMM(dateString: String): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val yearMonth = YearMonth.parse(dateString, formatter)
        val localDate = yearMonth.atDay(1)
        val instant = localDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        return localDateTimeFormatter.formatMonthWithYear(instant)
    }

    private fun formatDateTime(dateTime: String): String {
        try {
            val instant = ZonedDateTime.parse(dateTime).toInstant()
            return localDateTimeFormatter.formatLongDate(instant)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: $dateTime")
            Log.e(TAG, e.toString())
            return ""
        }
    }

    private fun determineFormat(timeString: String): TimeFieldFormat {
        val matchResult = dateTimeRegex.matchEntire(timeString) ?: return TimeFieldFormat.UNKNOWN

        // Matches YYYY-MM-DDThh:mm:ss+zz:zz
        if (matchResult.groups[8] != null) {
            return TimeFieldFormat.ISO_8601
        }

        // Matches YYYY-MM-DD
        if (matchResult.groups[6] != null) {
            return TimeFieldFormat.YYYY_MM_DD
        }

        // Matches YYYY-MM
        if (matchResult.groups[4] != null) {
            return TimeFieldFormat.YYYY_MM
        }

        // Matches YYYY
        if (matchResult.groups[1] != null) {
            return TimeFieldFormat.YYYY
        }
        Log.w(TAG, "Unknown time field format: $timeString")
        return TimeFieldFormat.UNKNOWN
    }

    private fun getNestedString(jsonObject: JSONObject, path: String): String? {
        val pathSegments = path.split('.')
        var currentJson = jsonObject
        pathSegments.forEachIndexed { index, segment ->
            if (index == pathSegments.lastIndex) {
                return currentJson.optString(segment)
            }
            currentJson = currentJson.optJSONObject(segment) ?: return null
        }
        return null
    }

    /** Defines the priority order of [TimeFieldKey]s for each resource type. */
    private sealed class KeyPriorityList(val keys: List<TimeFieldKey>) {
        object AllergyIntolerance :
            KeyPriorityList(
                listOf(
                    TimeFieldKey.RECORDED_DATE,
                    TimeFieldKey.ONSET_DATE_TIME,
                    TimeFieldKey.ONSET_PERIOD_START,
                    TimeFieldKey.LAST_OCCURRENCE,
                )
            )

        object Condition :
            KeyPriorityList(
                listOf(
                    TimeFieldKey.ONSET_DATE_TIME,
                    TimeFieldKey.ONSET_PERIOD_START,
                    TimeFieldKey.RECORDED_DATE,
                )
            )

        object Immunization :
            KeyPriorityList(listOf(TimeFieldKey.OCCURRENCE_DATE_TIME, TimeFieldKey.RECORDED_DATE))

        object Procedure :
            KeyPriorityList(
                listOf(
                    TimeFieldKey.PERFORMED_DATE_TIME,
                    TimeFieldKey.PERFORMED_PERIOD_START,
                    TimeFieldKey.PERFORMED_PERIOD_END,
                )
            )

        object MedicationRequest :
            KeyPriorityList(listOf(TimeFieldKey.AUTHORED_ON, TimeFieldKey.DISPENSE_REQUEST))

        object MedicationStatement :
            KeyPriorityList(
                listOf(
                    TimeFieldKey.DATE_ASSERTED,
                    TimeFieldKey.EFFECTIVE_DATE_TIME,
                    TimeFieldKey.EFFECTIVE_PERIOD_START,
                )
            )

        object Encounter :
            KeyPriorityList(listOf(TimeFieldKey.PERIOD_START, TimeFieldKey.PERIOD_END))

        object Observation :
            KeyPriorityList(
                listOf(
                    TimeFieldKey.EFFECTIVE_DATE_TIME,
                    TimeFieldKey.EFFECTIVE_PERIOD_START,
                    TimeFieldKey.EFFECTIVE_INSTANT,
                    TimeFieldKey.ISSUED,
                )
            )
    }

    /** List of time field keys from a FHIR resource. */
    private enum class TimeFieldKey(val path: String) {
        AUTHORED_ON("authoredOn"),
        DATE_ASSERTED("dateAsserted"),
        DISPENSE_REQUEST("dispenseRequest.validityPeriod.start"),
        EFFECTIVE_DATE_TIME("effectiveDateTime"),
        EFFECTIVE_PERIOD_START("effectivePeriod.start"),
        EFFECTIVE_INSTANT("effectiveInstant"),
        RECORDED_DATE("recordedDate"),
        ONSET_DATE_TIME("onsetDateTime"),
        ONSET_PERIOD_START("onsetPeriod.start"),
        LAST_OCCURRENCE("lastOccurrence"),
        ISSUED("issued"),
        PERFORMED_DATE_TIME("performedDateTime"),
        PERFORMED_PERIOD_START("performedPeriod.start"),
        PERFORMED_PERIOD_END("performedPeriod.end"),
        OCCURRENCE_DATE_TIME("occurrenceDateTime"),
        PERIOD_START("period.start"),
        PERIOD_END("period.end"),
    }

    private enum class TimeFieldFormat {
        YYYY,
        YYYY_MM,
        YYYY_MM_DD,
        ISO_8601,
        UNKNOWN,
    }
}
