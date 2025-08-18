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
package com.android.healthconnect.controller.data.entries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.recyclerview.DeletionViewBinder
import com.android.healthconnect.controller.utils.logging.EntriesElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import dagger.hilt.android.EntryPointAccessors

class SelectAllViewBinder(private val onClickSelectAllListener: OnClickSelectAllListener) :
    DeletionViewBinder<FormattedEntry.SelectAllHeader, View> {
    private lateinit var logger: HealthConnectLogger
    private val logName = EntriesElement.SELECT_ALL_BUTTON

    override fun newView(parent: ViewGroup): View {
        val context = parent.context.applicationContext
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(context, HealthConnectLoggerEntryPoint::class.java)
        logger = hiltEntryPoint.logger()
        return LayoutInflater.from(parent.context).inflate(R.layout.item_select_all, parent, false)
    }

    override fun bind(
        view: View,
        data: FormattedEntry.SelectAllHeader,
        index: Int,
        isDeletionState: Boolean,
        isChecked: Boolean,
    ) {
        val checkBox = view.findViewById<CheckBox>(R.id.item_checkbox_button)
        val container = view.findViewById<LinearLayout>(R.id.item_select_all_container)
        val selectAllText = view.findViewById<TextView>(R.id.select_all_text)
        logger.logImpression(logName)

        container.setOnClickListener {
            logger.logInteraction(logName)
            checkBox.toggle()
            onClickSelectAllListener.onClicked(checkBox.isChecked)
            container.contentDescription = selectAllText.text.toString()
        }

        checkBox.isChecked = isChecked
        checkBox.setOnClickListener {
            logger.logInteraction(logName)
            // check all entries
            onClickSelectAllListener.onClicked(checkBox.isChecked)
            container.contentDescription = selectAllText.text.toString()
        }
        checkBox.tag = if (isDeletionState) "checkbox" else ""
        checkBox.contentDescription = selectAllText.text.toString()
    }
}
