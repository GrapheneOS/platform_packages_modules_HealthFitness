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
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.theme.R as SettingslibR

/**
 * A [BaseExpandablePreference] that contains a toggle.
 *
 * A preference that displays a collapsible arrow on the left and a switch toggle on the right. The
 * arrow can be clicked to expand or collapse nested preferences.
 */
class HealthToggleExpandablePreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    BaseExpandablePreference(context, attrs) {

    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null
    var isChecked: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    init {
        layoutResource =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                R.layout.preference_expressive_switch
            } else {
                R.layout.preference_non_expressive_switch
            }
        widgetLayoutResource =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                SettingslibR.layout.settingslib_expressive_preference_switch
            } else {
                androidx.preference.R.layout.preference_widget_switch_compat
            }
    }

    fun setOnSwitchChangeListener(listener: ((Boolean) -> Unit)?) {
        onCheckedChangeListener = listener
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val switchWidgetContainer = holder.findViewById(android.R.id.widget_frame)
        val switch =
            findSwitchInView(switchWidgetContainer)?.apply {
                isFocusable = true
                isClickable = true
            } as SwitchCompat

        switch.setOnCheckedChangeListener(null)
        switch.isChecked = isChecked
        switch.setOnCheckedChangeListener { _, isChecked ->
            if (this.isChecked != isChecked) {
                this.isChecked = isChecked
                onCheckedChangeListener?.invoke(isChecked)
            }
        }
    }

    override fun getDropDownIconId(): Int {
        return R.id.expand_arrow
    }

    private fun findSwitchInView(viewGroup: View?): SwitchCompat? {
        if (viewGroup is SwitchCompat) {
            return viewGroup
        }
        if (viewGroup is ViewGroup) {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                val foundSwitch = findSwitchInView(child)
                if (foundSwitch != null) {
                    return foundSwitch
                }
            }
        }
        return null
    }
}
