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
package com.android.healthconnect.controller.filters

import android.content.Context
import android.widget.RadioGroup
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.AppMetadata

/**
 * The ChipPreference displays a horizontal list of {@link FilterChip}s.
 *
 * Each FilterChip represents a contributing app to the HealthConnect data. A default `All apps`
 * chip is added to the beginning of the list.
 */
class ChipPreference
@JvmOverloads
constructor(context: Context, private val appMetadataList: List<AppMetadata> = listOf()) :
    Preference(context) {

    init {
        layoutResource = R.layout.widget_horizontal_chip_preference
        isSelectable = false
    }

    private lateinit var chipGroup: RadioGroup

    private var allAppsButton: FilterChip? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        chipGroup = holder.findViewById(R.id.chip_group) as RadioGroup

        // prevents chip duplication as the onBindViewHolder method is called twice
        chipGroup.removeAllViews()

        addAllAppsFilterChip()

        for (appMetadata in appMetadataList) {
            addFilterChip(appMetadata)
        }
    }

    private fun addFilterChip(appMetadata: AppMetadata) {
        val newFilterChip = FilterChip(context)
        newFilterChip.setUnselectedIcon(appMetadata.icon)
        newFilterChip.text = appMetadata.appName
        chipGroup.addView(newFilterChip)
    }

    private fun addAllAppsFilterChip() {
        allAppsButton = FilterChip(context)
        allAppsButton?.id = R.id.select_all_chip
        allAppsButton?.text = context.resources.getString(R.string.select_all_apps_title)
        allAppsButton?.isChecked = true
        chipGroup.addView(allAppsButton)
    }
}
