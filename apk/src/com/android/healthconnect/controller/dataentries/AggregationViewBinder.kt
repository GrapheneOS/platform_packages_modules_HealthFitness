/**
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.dataentries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.FormattedEntry.FormattedAggregation
import com.android.healthconnect.controller.shared.recyclerview.ViewBinder

class AggregationViewBinder : ViewBinder<FormattedAggregation, View> {

    override fun newView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_aggregation, parent, false)
    }

    override fun bind(view: View, data: FormattedAggregation, index: Int) {
        val apps = view.findViewById<TextView>(R.id.item_data_origin_apps)
        val aggregation = view.findViewById<TextView>(R.id.item_data_aggregation)

        aggregation.text = data.aggregation
        aggregation.contentDescription = data.aggregationA11y
        apps.text = data.contributingApps
    }
}
