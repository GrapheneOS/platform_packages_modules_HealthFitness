/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.healthconnect.controller.exportimport

import android.content.Context
import android.util.AttributeSet
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.preference.HealthPreference

/** Preference for showing export status. */
class ExportStatusPreference(
    context: Context,
    private val exportTime: String,
    private val exportLocation: String?,
    attrs: AttributeSet? = null,
) : HealthPreference(context, attrs) {
    // TODO: b/325914485 - Add proper logging for this preference.

    companion object {
        const val EXPORT_STATUS_PREFERENCE = "export_status_preference"
    }

    init {
        layoutResource = R.layout.widget_export_status_preference
        key = EXPORT_STATUS_PREFERENCE
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val exportTimeTextView = holder.findViewById(R.id.export_time) as TextView
        exportTimeTextView.text = this.exportTime

        val exportLocationTextView = holder.findViewById(R.id.export_file_location) as TextView
        if (this.exportLocation.isNullOrEmpty()) {
            exportLocationTextView.visibility = GONE
        } else {
            exportLocationTextView.visibility = VISIBLE
            exportLocationTextView.text = this.exportLocation
        }
    }
}
