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
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedPrettyFhir
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Formatter for printing raw FHIR data. */
@Singleton
class PrettyFhirFormatter @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * Formats a PrettyJsonGroup.
     *
     * Top level JSON line becomes the header and the rest of the JSON becomes the content. This
     * should keep
     */
    fun format(prettyFhir: PrettyJsonGroup): FormattedPrettyFhir {

        // Assumes the first line of the group is the header top level json line
        val header = prettyFhir.nestedLines[0].line
        val content =
            PrettyJsonGroup(
                nestedLines = prettyFhir.nestedLines.subList(1, prettyFhir.nestedLines.size)
            )
        return FormattedPrettyFhir(header, content)
    }
}
