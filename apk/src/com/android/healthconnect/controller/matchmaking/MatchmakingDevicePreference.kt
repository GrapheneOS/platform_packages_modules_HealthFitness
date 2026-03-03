/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.healthconnect.controller.matchmaking

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.TextView
import androidx.core.view.children
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.settingslib.widget.SettingsThemeHelper

/**
 * A dedicated preference for devices in the matchmaking screen. The entire preference is clickable
 * and toggles the switch.
 */
class MatchmakingDevicePreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    HealthSwitchPreference(context, attrs) {

    init {
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            layoutResource = R.layout.matchmaking_device_preference
            widgetLayoutResource = R.layout.matchmaking_device_switch_widget
        } else {
            layoutResource = R.layout.matchmaking_device_preference_legacy
            widgetLayoutResource = R.layout.matchmaking_device_switch_widget_legacy
        }
        summary =
            context.getString(R.string.matchmaking_screen_continue_to_device_preferences_to_enable)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        setupHyphenation(holder.itemView)

        val switch = holder.findViewById(R.id.switch_widget) as? Checkable
        switch?.isChecked = isChecked

        holder.findViewById(R.id.switch_widget)?.isClickable = false
    }

    private fun setupHyphenation(view: View) {
        if (view is TextView) {
            view.hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NORMAL
            view.breakStrategy = Layout.BREAK_STRATEGY_SIMPLE
        } else if (view is ViewGroup) {
            view.children.forEach { child -> setupHyphenation(child) }
        }
    }
}
