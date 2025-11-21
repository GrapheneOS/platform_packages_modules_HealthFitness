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
package com.android.healthconnect.controller.matchmaking

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.settingslib.widget.GroupSectionDividerMixin

/** A preference that displays a header for the matchmaking screen. */
internal class MatchmakingHeaderPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : Preference(context, attrs, defStyleAttr, defStyleRes), GroupSectionDividerMixin {

    var headerTitle: CharSequence? = null
    var headerSummary: CharSequence? = null
    var requestingAppIcon: Drawable? = null
        set(value) {
            field = value
            notifyChanged()
        }

    var matchedAppIcons: List<Drawable> = emptyList()
        set(value) {
            field = value
            notifyChanged()
        }

    var isIconViewVisible: Boolean = true
        set(value) {
            field = value
            notifyChanged()
        }

    init {
        layoutResource = R.layout.widget_matchmaking_header
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val iconView =
            holder.findViewById(R.id.matchmaking_header_icon_view) as MatchmakingHeaderIconView
        iconView.requestingAppIcon = requestingAppIcon
        iconView.healthConnectIcon =
            ContextCompat.getDrawable(context, R.drawable.health_connect_logo_contrast)
        iconView.matchedAppIcons = matchedAppIcons
        iconView.visibility = if (isIconViewVisible) VISIBLE else GONE

        (holder.findViewById(R.id.header_title) as TextView).text = headerTitle
        (holder.findViewById(R.id.header_summary) as TextView).text = headerSummary
    }
}
