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

import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.matchmaking.MatchmakingPrivacyFooterPreference
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.theme.R as SettingsLibResources

/**
 * A custom [PreferenceGroupAdapter] that provides special styling for preferences in an expressive
 * theme.
 *
 * This adapter is responsible for applying custom backgrounds and horizontal padding to preferences
 * that are part of the [customStyledPreferences] list, based on their position relative to other
 * styled preferences.
 */
class ExpandablePreferenceAdapter(
    preferenceScreen: PreferenceScreen,
    private val customStyledPreferences: List<Preference>,
) : PreferenceGroupAdapter(preferenceScreen) {

    override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val preference = getItem(position)
        val itemView = holder.itemView
        if (preference in customStyledPreferences) {
            val horizontalPadding =
                itemView.context.resources.getDimensionPixelSize(R.dimen.spacing_xlarge)
            itemView.setPadding(horizontalPadding, 0, horizontalPadding, 0)

            if (SettingsThemeHelper.isExpressiveTheme(holder.itemView.context)) {
                val backgroundResId =
                    when {
                        preference is MatchmakingPrivacyFooterPreference ->
                            SettingsLibResources.drawable.settingslib_round_background_bottom

                        preference is BaseExpandablePreference && preference.mIsExpanded ->
                            SettingsLibResources.drawable.settingslib_round_background_top

                        preference is BaseExpandablePreference && !preference.mIsExpanded ->
                            SettingsLibResources.drawable.settingslib_round_background

                        else -> SettingsLibResources.drawable.settingslib_round_background_center
                    }

                itemView.background = ContextCompat.getDrawable(itemView.context, backgroundResId)
            }
        }
    }
}
