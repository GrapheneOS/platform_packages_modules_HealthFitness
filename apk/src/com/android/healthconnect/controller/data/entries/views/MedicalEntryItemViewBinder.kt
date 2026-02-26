/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.healthconnect.controller.data.entries.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.OnClickMedicalEntryListener
import com.android.healthconnect.controller.shared.recyclerview.SimpleViewBinder
import com.android.healthconnect.controller.utils.logging.EntriesElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthfitness.flags.Flags
import dagger.hilt.android.EntryPointAccessors

/** ViewBinder for FormattedMedicalDataEntry. */
class MedicalEntryItemViewBinder(
    private val onClickMedicalEntryListener: OnClickMedicalEntryListener?
) : SimpleViewBinder<FormattedEntry.FormattedMedicalDataEntry, View> {

    private lateinit var logger: HealthConnectLogger

    override fun newView(parent: ViewGroup): View {
        val context = parent.context.applicationContext
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_entry_new_ia, parent, false)
    }

    override fun bind(view: View, data: FormattedEntry.FormattedMedicalDataEntry, index: Int) {
        val header = view.findViewById<TextView>(R.id.item_data_entry_header)
        val title = view.findViewById<TextView>(R.id.item_data_entry_title)
        view.findViewById<CheckBox>(R.id.item_checkbox_button).apply {
            // deletion is not supported on medical items.
            visibility = View.GONE
        }
        logger.logImpression(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)

        title.text = data.title
        title.contentDescription = data.titleA11y

        header.text = data.header
        header.contentDescription = data.headerA11y

        if (Flags.personalHealthRecordEntriesScreen()) {
            view.setOnClickListener {
                logger.logInteraction(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
                onClickMedicalEntryListener?.onItemClicked(data, index)
            }
        } else {
            view.setOnClickListener {
                logger.logInteraction(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
                onClickMedicalEntryListener?.onItemClicked(data.medicalResourceId, index)
            }
        }
    }
}
