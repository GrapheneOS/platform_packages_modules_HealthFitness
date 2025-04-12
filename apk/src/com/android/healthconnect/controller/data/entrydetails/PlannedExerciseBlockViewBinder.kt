/**
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.data.entrydetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseBlockEntry
import com.android.healthconnect.controller.shared.recyclerview.SimpleViewBinder
import com.android.healthconnect.controller.shared.recyclerview.ViewBinder
import com.android.healthconnect.controller.utils.logging.EntryDetailsElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import dagger.hilt.android.EntryPointAccessors

class PlannedExerciseBlockViewBinder : SimpleViewBinder<PlannedExerciseBlockEntry, View> {
    private lateinit var logger: HealthConnectLogger

    override fun newView(parent: ViewGroup): View {
        val context = parent.context.applicationContext
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(context, HealthConnectLoggerEntryPoint::class.java)
        logger = hiltEntryPoint.logger()
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_planned_exercise_block_entry, parent, false)
    }

    override fun bind(view: View, data: PlannedExerciseBlockEntry, index: Int) {
        val title = view.findViewById<TextView>(R.id.planned_exercise_block_title)

        title.text = data.title
        title.contentDescription = data.titleA11y
        logger.logImpression(EntryDetailsElement.PLANNED_EXERCISE_BLOCK_ENTRY_VIEW)
    }
}
