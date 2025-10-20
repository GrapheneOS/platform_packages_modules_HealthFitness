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
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.preference.app.R as AppPreferenceR
import com.android.settingslib.widget.theme.R as SettingslibR

/**
 * A [PreferenceGroup] that can be expanded and collapsed.
 *
 * This preference will display an arrow that can be clicked to expand or collapse the preferences
 * contained within it.
 */
class HealthExpandablePreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    BaseExpandablePreference(context, attrs) {

    init {
        layoutResource =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                SettingslibR.layout.settingslib_expressive_preference
            } else {
                AppPreferenceR.layout.preference_app
            }
        widgetLayoutResource = R.layout.preference_expand_arrow_widget
    }

    override fun getDropDownIconId(): Int {
        return R.id.expand_arrow
    }
}
