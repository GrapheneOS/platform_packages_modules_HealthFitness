/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.healthconnect.controller.recentaccess

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Custom preference for displaying Recent access apps, including dash lines for timeline views. */
class RecentAccessPreference
constructor(context: Context, val recentAccessApp: RecentAccessEntry, val showCategories: Boolean) :
    Preference(context) {

    private lateinit var appIcon: ImageView
    private lateinit var appTitle: TextView
    private lateinit var dataTypesWritten: TextView
    private lateinit var dataTypesRead: TextView
    private lateinit var accessTime: TextView
    private val separator: String = context.getString(R.string.data_type_separator)

    init {
        layoutResource = R.layout.widget_recent_access_timeline
        isSelectable = true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        appIcon = holder.findViewById(R.id.recent_access_app_icon) as ImageView
        appIcon.setImageDrawable(recentAccessApp.metadata.icon)

        appTitle = holder.findViewById(R.id.title) as TextView
        appTitle.text = recentAccessApp.metadata.appName

        dataTypesWritten = holder.findViewById(R.id.data_types_written) as TextView
        dataTypesRead = holder.findViewById(R.id.data_types_read) as TextView

        if (showCategories) {
            if (recentAccessApp.dataTypesWritten.isNotEmpty()) {
                dataTypesWritten.text =
                    context.getString(
                        R.string.write_data_access_label,
                        recentAccessApp.dataTypesWritten.sorted().joinToString(separator) {
                            context.getString(it)
                        })
                dataTypesWritten.isVisible = true
            }

            if (recentAccessApp.dataTypesRead.isNotEmpty()) {
                dataTypesRead.text =
                    context.getString(
                        R.string.read_data_access_label,
                        recentAccessApp.dataTypesRead.sorted().joinToString(separator) {
                            context.getString(it)
                        })
                dataTypesRead.isVisible = true
            }
        }

        accessTime = holder.findViewById(R.id.time) as TextView
        accessTime.text = formatTime(recentAccessApp.instantTime)
    }

    private fun formatTime(instant: Instant): String {
        val localTime: LocalTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}
