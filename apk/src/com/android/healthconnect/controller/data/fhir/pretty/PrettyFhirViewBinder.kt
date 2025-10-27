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

package com.android.healthconnect.controller.data.fhir.pretty

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedPrettyFhir
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonLine
import com.android.healthconnect.controller.shared.recyclerview.SimpleViewBinder
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.logging.PrettyFhirPageElement
import dagger.hilt.android.EntryPointAccessors

class PrettyFhirViewBinder : SimpleViewBinder<FormattedPrettyFhir, View> {

    companion object {
        private const val TAG = "PrettyFhirViewBinder"
    }

    private lateinit var logger: HealthConnectLogger

    override fun newView(parent: ViewGroup): View {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                parent.context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()

        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pretty_fhir_entry, parent, false)
    }

    override fun bind(view: View, prettyJsonData: FormattedPrettyFhir, index: Int) {

        val contentContainer =
            view.findViewById<LinearLayout>(R.id.item_pretty_fhir_content_container)
        val headerText = view.findViewById<TextView>(R.id.item_pretty_fhir_header)
        val prettyJsonContent = prettyJsonData.content.nestedLines

        headerText.text = prettyJsonData.header
        contentContainer.removeAllViews() // Remove existing views before rebinding.

        /** Dynamically creates a view according to its depth per line */
        prettyJsonContent.forEach { jsonLine ->
            val contentLevelView =
                jsonLine.getViewLevel() ?: return@forEach // Does not break entire loop
            val contentView = contentContainer.createContentView(jsonLine, contentLevelView)
            contentContainer.addView(contentView)
        }

        logger.logImpression(PrettyFhirPageElement.PRETTY_FHIR_GROUP_CONTAINER)
    }

    private fun PrettyJsonLine.getViewLevel(): ContentLevelView? {
        return when (depth) {
            0 -> {
                Log.w(
                    TAG,
                    "Should only have one Depth 0 Json Line." +
                        " Which is already displayed as the header. Content omitted.",
                )
                null
            }
            1 -> ContentLevelView.Level1View
            2 -> ContentLevelView.Level2View
            3 -> ContentLevelView.Level3View
            else -> {
                Log.w(TAG, "Depth $depth exceeds maximum threshold. Content omitted.")
                null
            }
        }
    }

    private fun ViewGroup.createContentView(
        data: PrettyJsonLine,
        contentLevelView: ContentLevelView,
    ): View {
        return LayoutInflater.from(context).inflate(contentLevelView.layout, this, false).apply {
            val textView = findViewById<TextView>(contentLevelView.textViewId)
            textView.text =
                // Blank lines treated as line breaks
                if (data.line.isBlank() || !contentLevelView.bulleted) {
                    data.line
                } else {
                    context.getString(R.string.bulleted_content, data.line)
                }
        }
    }

    /**
     * Objects for each level of content in the Pretty Fhir view to populate container for:
     * [R.layout.item_pretty_fhir_entry]
     */
    sealed class ContentLevelView(
        @param:LayoutRes val layout: Int,
        @param:IdRes val textViewId: Int,
        val bulleted: Boolean = false,
    ) {

        /** Object for [R.layout.item_pretty_fhir_entry_level_1] */
        internal object Level1View :
            ContentLevelView(
                layout = R.layout.item_pretty_fhir_entry_level_1,
                textViewId = R.id.item_pretty_fhir_content_level1,
            )

        /** Object for [R.layout.item_pretty_fhir_entry_level_2] */
        internal object Level2View :
            ContentLevelView(
                layout = R.layout.item_pretty_fhir_entry_level_2,
                textViewId = R.id.item_pretty_fhir_content_level2,
                bulleted = true,
            )

        /** Object for [R.layout.item_pretty_fhir_entry_level_3] */
        internal object Level3View :
            ContentLevelView(
                layout = R.layout.item_pretty_fhir_entry_level_3,
                textViewId = R.id.item_pretty_fhir_content_level3,
                bulleted = true,
            )
    }
}
