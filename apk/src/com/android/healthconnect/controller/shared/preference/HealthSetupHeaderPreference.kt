/**
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.shared.preference

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.settingslib.widget.GroupSectionDividerMixin
import com.android.settingslib.widget.SettingsThemeHelper

internal class HealthSetupHeaderPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : Preference(context, attrs, defStyleAttr, defStyleRes), GroupSectionDividerMixin {

    private lateinit var title: TextView
    private lateinit var summary: TextView
    private lateinit var icon: ImageView
    private var centerIcon = true
    private var centerTitle = false
    private var centerSummary = false

    init {
        layoutResource = R.layout.widget_health_setup_header_container
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val contentArea = holder.findViewById(R.id.header_content) as FrameLayout
        val contentLayoutId: Int
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            contentLayoutId = R.layout.widget_health_setup_header_content_expressive
            centerIcon = true
            centerTitle = false
            centerSummary = false
        } else {
            contentLayoutId = R.layout.widget_health_setup_header_content_legacy
            centerIcon = false
        }
        contentArea.addView(
            LayoutInflater.from(context).inflate(contentLayoutId, contentArea, false)
        )
        title = holder.findViewById(R.id.title) as TextView
        summary = holder.findViewById(R.id.summary) as TextView
        icon = holder.findViewById(R.id.icon) as ImageView

        title.text = getTitle()
        val summaryText = getSummary()
        if (!summaryText.isNullOrEmpty()) {
            summary.visibility = View.VISIBLE
            summary.text = summaryText
        } else {
            summary.visibility = View.GONE
        }
        icon.setBackgroundDrawable(getIcon())

        maybeCenterLayout(icon, centerIcon)
        maybeCenterLayout(title, centerTitle)
        maybeCenterLayout(summary, centerSummary)
    }

    private fun maybeCenterLayout(view: View, shouldCenter: Boolean) {
        val params = view.layoutParams as LayoutParams
        if (shouldCenter) {
            params.gravity = Gravity.CENTER_HORIZONTAL
            if (view is TextView) {
                view.gravity = Gravity.CENTER_HORIZONTAL
            }
        } else {
            params.gravity = Gravity.START
            if (view is TextView) {
                view.gravity = Gravity.START
            }
        }
        view.layoutParams = params
    }
}
