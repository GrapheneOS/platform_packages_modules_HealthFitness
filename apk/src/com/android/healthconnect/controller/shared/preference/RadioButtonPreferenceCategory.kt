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
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceCategory
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.settingslib.widget.SelectorWithWidgetPreference

/**
 * A generic [PreferenceCategory] that displays selectable radio button options.
 *
 * This class creates a group of radio button preferences, allowing the user to select one option
 * from a list. It utilizes [SelectorWithWidgetPreference] to create individual radio buttons and
 * handles the selection logic.
 *
 * @param context The Context the preference is associated with.
 * @param childFragmentManager The FragmentManager used to set fragment results.
 * @param options A list of [RadioButtonOption] objects, each representing a radio button option.
 * @param logger The [HealthConnectLogger] used for logging interactions and impressions.
 * @param preferenceKey The key associated with this preference category.
 * @param preferenceTitleResId The resource ID of the title for this preference category.
 * @param currentSelectedKey The key of the currently selected option (optional).
 * @param attrs The set of attributes associated with the preference (optional).
 */
class RadioButtonPreferenceCategory
@JvmOverloads
constructor(
    context: Context,
    private val childFragmentManager: FragmentManager,
    private val options: List<RadioButtonOption>,
    private val logger: HealthConnectLogger,
    preferenceKey: String,
    preferenceTitleResId: Int,
    currentSelectedKey: String? = null,
    attrs: AttributeSet? = null,
) : PreferenceCategory(context, attrs) {

    private var selectedKey: String? = currentSelectedKey

    init {
        key = preferenceKey
        title = context.getString(preferenceTitleResId)
    }

    data class RadioButtonOption(
        val key: String,
        val title: String,
        val element: ElementName,
        val listener: SelectorWithWidgetPreference.OnClickListener,
    )

    override fun onAttached() {
        super.onAttached()

        if (preferenceCount == 0) {
            options.forEach { option -> addSelectorPreference(option) }
        }
        updateSelectedPreference()
    }

    private fun addSelectorPreference(option: RadioButtonOption) {
        val selectorPreference =
            SelectorWithWidgetPreference(context).apply {
                title = option.title
                key = option.key
                setOnClickListener {
                    logger.logInteraction(option.element)
                    option.listener.onRadioButtonClicked(it)
                }
                logger.logImpression(option.element)
            }
        addPreference(selectorPreference)
    }

    private fun updateSelectedPreference() {
        for (i in 0 until preferenceCount) {
            val preference = getPreference(i)
            if (preference is SelectorWithWidgetPreference) {
                preference.isChecked = preference.key == selectedKey
            }
        }
    }

    fun updateSelectedOption(newSelectedOptionKey: String) {
        selectedKey = newSelectedOptionKey
        updateSelectedPreference()
    }
}
