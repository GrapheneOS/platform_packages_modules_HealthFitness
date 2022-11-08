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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Custom preference for displaying Recent access apps, including dash lines for timeline views. */
class RecentAccessPreference
constructor(
    context: Context,
    val recentAccessApp: RecentAccessApp,
    val isHistory: Boolean = true,
    val isLastUsage: Boolean = false
) : Preference(context) {

    init {
        super.setTitle(recentAccessApp.appName)

        if (isHistory) {
            val writeData: String = recentAccessApp.dataTypesWritten.joinToString(", ")
            super.setSummary(writeData)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        val widgetFrame: ViewGroup = holder?.findViewById(android.R.id.widget_frame) as ViewGroup
        val widgetFrameParent: LinearLayout = widgetFrame.parent as LinearLayout

        val iconFrame: LinearLayout? = holder.findViewById(android.R.id.icon_frame) as LinearLayout?
        widgetFrameParent.removeView(iconFrame)

        var recentAccessWidget: ViewGroup? =
            holder.findViewById(R.id.recent_access_widget_layout) as ViewGroup?
        if (recentAccessWidget == null) {
            val inflater: LayoutInflater = context.getSystemService(LayoutInflater::class.java)
            recentAccessWidget =
                inflater.inflate(R.layout.widget_recent_access_timeline, widgetFrameParent, false)
                    as ViewGroup?
            widgetFrameParent.addView(recentAccessWidget, 0)
        }

        widgetFrameParent.gravity = Gravity.TOP

        val recentAccessTime: TextView? =
            recentAccessWidget?.findViewById(R.id.recent_access_time) as TextView?
        recentAccessTime?.text = formatTime(recentAccessApp.instantTime)

        val recentAccessAppIcon: ImageView? =
            recentAccessWidget?.findViewById(R.id.recent_access_app_icon) as ImageView?
        recentAccessAppIcon?.setImageResource(recentAccessApp.icon)

        val dashLine: View? = recentAccessWidget?.findViewById(R.id.recent_access_dash_line)

        if (isHistory && !isLastUsage) {
            dashLine?.visibility = View.VISIBLE
        } else {
            dashLine?.visibility = View.GONE
        }
    }

    private fun formatTime(instant: Instant): String {
        val localTime: LocalTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
        return localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}
