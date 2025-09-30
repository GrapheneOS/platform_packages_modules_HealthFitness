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

package com.android.healthconnect.controller.shared.preference

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.HealthAppPreference
import com.android.healthconnect.controller.shared.app.AppMetadata

/** A preference for apps that are not currently connected to Health Connect */
class NotConnectedAppPreference(context: Context, appMetadata: AppMetadata) :
    HealthAppPreference(context, appMetadata) {

    init {
        widgetLayoutResource = R.layout.widget_not_conencted_app
        summary = context.getString(R.string.app_not_connected_summary)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val color = com.android.settingslib.widget.theme.R.color.settingslib_colorAccentPrimary
        val titleView = holder.findViewById(android.R.id.title) as TextView
        titleView.setTextColor(ContextCompat.getColor(context, color))

        val summaryView = holder.findViewById(android.R.id.summary) as TextView
        summaryView.setTextColor(ContextCompat.getColor(context, color))

        val connectIcon = holder.findViewById(R.id.connect_button) as ImageView
        connectIcon.setColorFilter(ContextCompat.getColor(context, color))
    }
}
