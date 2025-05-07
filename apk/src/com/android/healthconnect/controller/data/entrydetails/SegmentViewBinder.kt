/**
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
package com.android.healthconnect.controller.data.entrydetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedSegment
import com.android.healthconnect.controller.shared.recyclerview.SimpleViewBinder
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import dagger.hilt.android.EntryPointAccessors

class SegmentViewBinder : SimpleViewBinder<FormattedSegment, View> {
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
            .inflate(R.layout.item_data_session_detail_segment, parent, false)
    }

    override fun bind(view: View, data: FormattedSegment, index: Int) {
        val segmentHeader = view.findViewById<TextView>(R.id.segment_header)
        val segmentTitle = view.findViewById<TextView>(R.id.segment_title)
        val setIndex = view.findViewById<TextView>(R.id.segment_set_index)
        val weight = view.findViewById<TextView>(R.id.segment_weight)
        val rpe = view.findViewById<TextView>(R.id.segment_rpe)

        segmentHeader.text = data.header
        segmentHeader.contentDescription = data.headerA11y
        segmentTitle.text = data.title
        segmentTitle.contentDescription = data.titleA11y
        if (data.setIndex != null) {
            setIndex.visibility = View.VISIBLE
            setIndex.setPaddingRelative(
                view.context.resources.getDimension(R.dimen.spacing_small).toInt(),
                /* top= */ 0,
                /* end= */ 0,
                /* bottom= */ 0,
            )
            setIndex.text = view.context.getString(R.string.bulleted_content, data.setIndex)
            setIndex.contentDescription = data.setIndexA11y
        }
        if (data.weight != null) {
            weight.visibility = View.VISIBLE
            weight.setPaddingRelative(
                view.context.resources.getDimension(R.dimen.spacing_small).toInt(),
                /* top= */ 0,
                /* end= */ 0,
                /* bottom= */ 0,
            )
            weight.text = view.context.getString(R.string.bulleted_content, data.weight)
            weight.contentDescription = data.weightA11y
        }
        if (data.rpe != null) {
            rpe.visibility = View.VISIBLE
            rpe.setPaddingRelative(
                view.context.resources.getDimension(R.dimen.spacing_small).toInt(),
                /* top= */ 0,
                /* end= */ 0,
                /* bottom= */ 0,
            )
            rpe.text = view.context.getString(R.string.bulleted_content, data.rpe)
            rpe.contentDescription = data.rpeA11y
        }
    }
}
