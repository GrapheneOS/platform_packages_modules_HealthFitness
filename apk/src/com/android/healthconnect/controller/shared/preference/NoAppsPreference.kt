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
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R

/** A custom preference used to display a message when there are no apps available. */
class NoAppsPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.preferenceStyle,
) : Preference(context, attrs, defStyleAttr) {

    private var learnMoreText = ""
    private var learnMoreAction: View.OnClickListener? = null

    init {
        key = "empty_health_apps"
        isSelectable = false
        layoutResource = R.layout.widget_no_apps_preference
    }

    fun setLearnMoreText(text: String) {
        learnMoreText = text
        notifyChanged()
    }

    fun setLearnMoreAction(action: View.OnClickListener) {
        learnMoreAction = action
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val learnMoreSection = holder.itemView.findViewById<TextView>(R.id.settingslib_learn_more)

        if (learnMoreText.isNotEmpty()) {
            learnMoreSection.text = learnMoreText
            learnMoreSection.visibility = View.VISIBLE
            learnMoreSection.setOnClickListener(learnMoreAction)
        }
    }
}
