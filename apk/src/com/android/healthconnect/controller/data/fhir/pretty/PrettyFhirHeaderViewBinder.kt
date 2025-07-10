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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.shared.recyclerview.SimpleViewBinder
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.logging.PrettyFhirPageElement
import dagger.hilt.android.EntryPointAccessors

class PrettyFhirHeaderViewBinder(private val onClickedViewSourceDataListener: () -> Unit) :
    SimpleViewBinder<FormattedEntry.FormattedPrettyFhirDetailsHeader, View> {

    private lateinit var logger: HealthConnectLogger

    override fun newView(parent: ViewGroup): View {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                parent.context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pretty_fhir_header, parent, false)
    }

    override fun bind(
        view: View,
        data: FormattedEntry.FormattedPrettyFhirDetailsHeader,
        index: Int,
    ) {
        val headerText = view.findViewById<TextView>(R.id.item_pretty_fhir_entry_header)
        val titleText = view.findViewById<TextView>(R.id.item_pretty_fhir_title)

        view.setOnClickListener {
            onClickedViewSourceDataListener()
            logger.logInteraction(PrettyFhirPageElement.PRETTY_FHIR_VIEW_SOURCE_DATA_BUTTON)
        }
        headerText.text = data.header
        titleText.text = data.title
        logger.logImpression(PrettyFhirPageElement.PRETTY_FHIR_HEADER_CONTAINER)
    }
}
