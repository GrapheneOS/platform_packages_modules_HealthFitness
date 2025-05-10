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
import com.android.healthconnect.controller.permissions.connectedapps.ComparablePreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.formatRecentAccessTime
import com.android.healthconnect.controller.utils.logging.RecentAccessElement

/** Custom preference for displaying Recent access apps, including dash lines for timeline views. */
class RecentAccessPreference(
    context: Context,
    private val recentAccessEntry: RecentAccessEntry,
    private val timeSource: TimeSource,
    private val showCategories: Boolean,
) : HealthPreference(context), ComparablePreference {

    private val separator: String by lazy { context.getString(R.string.separator) }

    init {
        layoutResource =
            if (showCategories) {
                // Top-aligned elements for use in the recent access fragment
                R.layout.widget_recent_access_timeline_with_details
            } else {
                // Center-aligned elements for use on the home page
                R.layout.widget_recent_access_timeline_home
            }
        isSelectable = true
        this.logName = RecentAccessElement.RECENT_ACCESS_ENTRY_BUTTON
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val appIcon = holder.findViewById(R.id.recent_access_app_icon) as ImageView
        appIcon.setImageDrawable(recentAccessEntry.metadata.icon)

        val appTitle = holder.findViewById(R.id.title) as TextView
        appTitle.text = recentAccessEntry.metadata.appName

        val dataTypesWritten = holder.findViewById(R.id.data_types_written) as TextView
        if (showCategories && recentAccessEntry.dataTypesWritten.isNotEmpty()) {
            dataTypesWritten.text = getWrittenText()
            dataTypesWritten.isVisible = true
        } else {
            dataTypesWritten.isVisible = false
        }

        val dataTypesRead = holder.findViewById(R.id.data_types_read) as TextView
        if (showCategories && recentAccessEntry.dataTypesRead.isNotEmpty()) {
            dataTypesRead.text = getReadText()
            dataTypesRead.isVisible = true
        } else {
            dataTypesRead.isVisible = false
        }

        val accessTime = holder.findViewById(R.id.time) as TextView
        val formattedTime =
            formatRecentAccessTime(recentAccessEntry.instantTime, timeSource, context)
        accessTime.text = formattedTime
        accessTime.contentDescription =
            context.getString(R.string.recent_access_time_content_descritption, formattedTime)
    }

    override fun isSameItem(preference: Preference): Boolean {
        return preference is RecentAccessPreference && this == preference
    }

    override fun hasSameContents(preference: Preference): Boolean {
        return preference is RecentAccessPreference &&
            this.recentAccessEntry == preference.recentAccessEntry
    }

    private fun getWrittenText(): String {
        val sortedDataTypes = sortWithHealthRecordsFirst(recentAccessEntry.dataTypesWritten)
        return context.getString(
            R.string.write_data_access_label,
            sortedDataTypes.joinToString(separator),
        )
    }

    private fun getReadText(): String {
        val sortedDataTypes = sortWithHealthRecordsFirst(recentAccessEntry.dataTypesRead)
        return context.getString(
            R.string.read_data_access_label,
            sortedDataTypes.joinToString(separator),
        )
    }

    private fun sortWithHealthRecordsFirst(dataTypes: MutableSet<Int>): List<String> {
        val sortedList = dataTypes.map { context.getString(it) }.sorted().toMutableList()

        val healthRecordsString = context.getString(R.string.medical_permissions)
        if (sortedList.contains(healthRecordsString)) {
            sortedList.remove(healthRecordsString)
            sortedList.add(0, healthRecordsString)
        }

        return sortedList
    }
}
