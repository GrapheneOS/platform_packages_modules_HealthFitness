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
import android.health.connect.datatypes.FhirResource
import com.android.healthconnect.controller.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Formatter for printing raw FHIR data. */
@Singleton
class RawFhirFormatter @Inject constructor(@ApplicationContext private val context: Context) {
    /**
     * Formats the whole FHIR resource string into a more readable format with proper indentation
     * and line breaks.
     */
    fun format(fhirResource: FhirResource): String {
        return try {
            val fhirString = fhirResource.data
            val jsonObject = JSONObject(fhirString)
            var formattedJson = jsonObject.toString(4)
            formattedJson = formattedJson.replace("\\/", "/")
            formattedJson
        } catch (e: JSONException) {
            fhirResource.data
        }
    }

    fun fhirContentDescription(fhirResource: FhirResource): String {
        return try {
            val fhirString = fhirResource.data
            val jsonObject = JSONObject(fhirString)
            buildContentDescription(jsonObject)
        } catch (e: JSONException) {
            fhirResource.data
        }
    }

    private fun buildContentDescription(jsonObject: JSONObject, prefix: String = ""): String {
        val description = StringBuilder()

        // Read "open bracket" for the starting of this object
        description.append(context.getString(R.string.raw_fhir_open_bracket, prefix))

        jsonObject.keys().forEach { key ->
            val value = jsonObject.get(key)

            description.append(context.getString(R.string.raw_fhir_field_value, prefix, key))

            when (value) {
                is JSONObject -> {
                    // Recursively handle nested objects
                    description.append(buildContentDescription(value, "$prefix  "))
                }
                is JSONArray -> {
                    // Handle JSON arrays by iterating over the array elements
                    for (i in 0 until value.length()) {
                        val arrayItem = value.get(i)
                        if (arrayItem is JSONObject) {
                            description.append(buildContentDescription(arrayItem, "$prefix  "))
                        } else {
                            // Handle primitive values in array
                            description.append("$arrayItem. ")
                        }
                    }
                }
                else -> {
                    // Handle primitive values (String, Number, Boolean)
                    description.append(context.getString(R.string.raw_fhir_value, prefix, value))
                }
            }
        }

        // Read "close bracket" for the end of this object
        description.append(context.getString(R.string.raw_fhir_closed_bracket, prefix))

        return context.getString(R.string.raw_fhir_content_description, description.toString())
    }
}
