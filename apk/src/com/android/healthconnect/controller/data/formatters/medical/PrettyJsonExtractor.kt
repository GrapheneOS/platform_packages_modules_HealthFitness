/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Converts a FHIR JSON resource string into a list of [PrettyJsonGroup]s suitable for structured
 * display, applying specific formatting and filtering rules.
 *
 * **Key Formatting Logic:**
 * - Parses a JSON string, expected to be a single JSON object.
 * - Produces a [List]<[PrettyJsonGroup]>, where each group contains related lines.
 * - **Grouping:** A new [PrettyJsonGroup] starts for each top-level property (depth 0). It includes
 *   the initial depth 0 line and all subsequent nested lines (depth > 0) until the next property at
 *   depth 0 is encountered.
 * - **Line Format:** Each [PrettyJsonLine] within a group has a `depth` for indentation and a
 *   formatted `line` string.
 * - **Key Formatting:** Converts JSON keys from `camelCase` or `PascalCase` to `Title Case` (e.g.,
 *   `birthDate` -> `Birth Date`).
 * - **Value Cleaning:** Primitive string values are trimmed. Quotes (`"`) that strictly surround
 *   the *entire* trimmed value are removed; internal quotes are preserved.
 * - **Key Hiding:** Specific keys are hidden based on rules:
 * - **Hidden only at top level** (`depth == 0`): `"text"`, `"contained"`, "meta"`,
 *   `"implicitRules"`, `"language"`.
 * - **Depth Limit:** Recursion stops at `MAX_DEPTH` (currently 3). Content nested deeper than this
 *   limit is omitted.
 * - **Array Handling:**
 * - Arrays associated with a key display a header line (e.g., `Identifiers:`).
 * - The indentation (`contentDepth`) for items inside an array depends on whether the array had a
 *   key/header: `contentDepth = if (hasKey) depth + 1 else depth`.
 * - An **empty line** is inserted as a separator between array items (after the first item) *only
 *   if* the array contains at least one complex item (JSONObject or JSONArray). Arrays containing
 *   only primitives have no empty line separators.
 * - **Null Handling:**
 * - JSON `null` values (`JSONObject.NULL`) associated with a key result in a line like `Key: null`.
 * - `JSONObject.NULL` values occurring directly within arrays (e.g. in a primitive array `[1, null,
 *   2]`) are rendered as a line containing the string `"null"`.
 * - Keys that are entirely absent from a JSON object produce no line.
 * - **Error Handling:** If initial JSON parsing fails, returns an empty list.
 */
@Singleton
class PrettyJsonExtractor @Inject constructor() {

    companion object {
        /** Maximum nesting depth to process. Content beyond this depth is omitted. */
        private const val MAX_DEPTH = 3

        private val HIDDEN_TOP_LEVEL_KEYS =
            setOf("text", "contained", "meta", "implicitRules", "language")
    }

    /** Parses the JSON string and extracts displayable groups according to rules. */
    fun extract(json: String): List<PrettyJsonGroup> {
        return try {
            val root = JSONObject(json)
            val allLines = extractLinesFromJsonObject(root, 0)

            val finalGroups = mutableListOf<PrettyJsonGroup>()
            val currentGroupLines = mutableListOf<PrettyJsonLine>()

            for (line in allLines) {
                if (line.depth == 0 && currentGroupLines.isNotEmpty()) {
                    finalGroups.add(PrettyJsonGroup(currentGroupLines.toList()))
                    currentGroupLines.clear()
                }
                currentGroupLines.add(line)
            }

            if (currentGroupLines.isNotEmpty()) {
                finalGroups.add(PrettyJsonGroup(currentGroupLines.toList()))
            }
            finalGroups
        } catch (e: JSONException) {
            Log.e(
                "PrettyJsonExtractor",
                "JSONException during initial JSON parsing. Input might be invalid.",
                e,
            )
            emptyList()
        } catch (e: Exception) {
            Log.e("PrettyJsonExtractor", "Unexpected Exception during extraction process.", e)
            emptyList()
        }
    }

    /** Extracts lines from a JSONObject's keys and values. */
    private fun extractLinesFromJsonObject(obj: JSONObject, depth: Int): List<PrettyJsonLine> {
        if (depth > MAX_DEPTH) return emptyList()

        val lines = mutableListOf<PrettyJsonLine>()
        obj.keys().forEach { key ->
            obj.opt(key)?.let { value -> lines.addAll(extractLinesFromValue(value, key, depth)) }
        }
        return lines
    }

    /**
     * Top-level recursive extraction function for a generic JSON value. Assumes [value] is not
     * null.
     */
    private fun extractLinesFromValue(value: Any, key: String?, depth: Int): List<PrettyJsonLine> {
        if (shouldSkipProcessing(key, depth)) return emptyList()
        if (depth > MAX_DEPTH) {
            return emptyList()
        }

        return when (value) {
            is JSONObject -> extractLinesForJsonObjectValue(value, key, depth)
            is JSONArray -> extractLinesForJsonArrayValue(value, key, depth)
            JSONObject.NULL -> extractLineForJsonNullValue(key, depth)
            else -> extractLineForPrimitiveValue(value, key, depth)
        }
    }

    /** Determines if processing for a key should be skipped based on hiding rules. */
    private fun shouldSkipProcessing(key: String?, depth: Int): Boolean {
        return key != null && depth == 0 && key in HIDDEN_TOP_LEVEL_KEYS
    }

    /** Extracts lines specifically for JSONObject values. */
    private fun extractLinesForJsonObjectValue(
        obj: JSONObject,
        key: String?,
        depth: Int,
    ): List<PrettyJsonLine> {
        val lines = mutableListOf<PrettyJsonLine>()
        var contentDepth = depth
        key?.let {
            lines.add(PrettyJsonLine(depth, "${formatKey(it)}:"))
            contentDepth++
        }

        if (contentDepth <= MAX_DEPTH) {
            lines.addAll(extractLinesFromJsonObject(obj, contentDepth))
        }
        return lines
    }

    /** Extracts lines specifically for JSONArray values. */
    private fun extractLinesForJsonArrayValue(
        jsonArray: JSONArray,
        key: String?,
        depth: Int,
    ): List<PrettyJsonLine> {
        val lines = mutableListOf<PrettyJsonLine>()
        var contentDepth = depth
        key?.let {
            lines.add(PrettyJsonLine(depth, "${formatKey(it)}:"))
            contentDepth++
        }

        if (contentDepth > MAX_DEPTH) {
            return lines
        }

        val needsSeparators = arrayContainsComplexItems(jsonArray)
        for (i in 0 until jsonArray.length()) {
            if (i > 0 && needsSeparators) {
                lines.add(PrettyJsonLine(contentDepth, ""))
            }
            val item = jsonArray.opt(i)
            // For items in an array, the key is null.
            // The depth check for these items is handled by extractLinesFromValue
            lines.addAll(extractLinesFromValue(item, null, contentDepth))
        }
        return lines
    }

    /** Checks if a JSONArray contains at least one JSONObject or JSONArray. */
    private fun arrayContainsComplexItems(arr: JSONArray): Boolean {
        return (0 until arr.length())
            .asSequence()
            .mapNotNull { arr.opt(it) }
            .any { it is JSONObject || it is JSONArray }
    }

    /**
     * Extracts a line specifically for `JSONObject.NULL` values. If the null value is associated
     * with a key, it formats as "Key: null". If the null value is an item in an array, it formats
     * as "null".
     */
    private fun extractLineForJsonNullValue(key: String?, depth: Int): List<PrettyJsonLine> {
        return if (key != null) {
            // Null value associated with a key
            listOf(PrettyJsonLine(depth, "${formatKey(key)}: null"))
        } else {
            // Null value as an item in an array (e.g., [1, null, 2])
            listOf(PrettyJsonLine(depth, "null"))
        }
    }

    /** Extracts a line specifically for primitive values. Assumes [value] is not null. */
    private fun extractLineForPrimitiveValue(
        value: Any,
        key: String?,
        depth: Int,
    ): List<PrettyJsonLine> {
        val displayValue = cleanValue(value.toString())
        val lineText =
            if (key != null) {
                "${formatKey(key)}: $displayValue"
            } else {
                displayValue
            }
        return listOf(PrettyJsonLine(depth, lineText))
    }

    /** Formats camelCase/PascalCase keys to Title Case. */
    private fun formatKey(key: String): String {
        return key.replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace(Regex("([A-Z])([A-Z][a-z])"), "$1 $2")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /** Cleans primitive values by trimming and removing surrounding quotes only. */
    private fun cleanValue(value: String): String {
        return value.trim().removeSurrounding("\"")
    }
}

/** Represents a single formatted line with its depth. */
data class PrettyJsonLine(val depth: Int = 0, val line: String)

/**
 * Represents a group of related formatted lines, starting with a depth 0 line and including
 * subsequent nested lines.
 */
data class PrettyJsonGroup(val nestedLines: List<PrettyJsonLine>)
