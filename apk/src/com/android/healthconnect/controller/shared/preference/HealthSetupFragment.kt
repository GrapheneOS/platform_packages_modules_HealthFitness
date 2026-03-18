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
 *
 *
 */
package com.android.healthconnect.controller.shared.preference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.android.healthconnect.controller.R
import com.android.settingslib.widget.SettingsThemeHelper

/** Base fragment class for AOB-like screens that need a bottom button bar. */
abstract class HealthSetupFragment : HealthPreferenceFragment() {
    private lateinit var preferenceContainer: ViewGroup
    private lateinit var preferenceArea: ViewGroup

    private lateinit var buttonArea: FrameLayout
    private lateinit var primaryButtonFull: Button
    private lateinit var primaryButtonOutline: Button
    private lateinit var secondaryButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_setup, container, false)

        rootView.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_background_transparent)

        val buttonLayoutId =
            if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
                R.layout.widget_setup_bottom_button_bar_expressive
            } else {
                R.layout.widget_setup_bottom_button_bar_legacy
            }

        buttonArea = rootView.findViewById<FrameLayout>(R.id.action_container)
        val buttons = inflater.inflate(buttonLayoutId, buttonArea, false)
        buttonArea.addView(buttons)

        preferenceArea = rootView.findViewById(R.id.preference_container)
        preferenceContainer =
            super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        preferenceArea.addView(preferenceContainer)

        primaryButtonFull = buttonArea.findViewById(R.id.primary_button_full)
        primaryButtonOutline = buttonArea.findViewById(R.id.primary_button_outline)
        secondaryButton = buttonArea.findViewById(R.id.secondary_button)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val bars =
                windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout() or
                        WindowInsetsCompat.Type.statusBars()
                )
            preferenceContainer.updatePadding(top = bars.top, left = bars.left, right = bars.right)
            buttonArea.updatePadding(bottom = bars.bottom, left = bars.left, right = bars.right)
            WindowInsetsCompat.CONSUMED
        }

        return rootView
    }

    fun getPrimaryButtonFull(): Button {
        primaryButtonOutline.visibility = View.GONE
        primaryButtonFull.visibility = View.VISIBLE
        showButtons()
        return primaryButtonFull
    }

    fun getPrimaryButtonOutline(): Button {
        primaryButtonOutline.visibility = View.VISIBLE
        primaryButtonFull.visibility = View.GONE
        showButtons()
        return primaryButtonOutline
    }

    fun getSecondaryButton(): Button {
        showButtons()
        return secondaryButton
    }

    fun hideButtons() {
        buttonArea.visibility = View.GONE
    }

    fun hideSecondaryButton() {
        secondaryButton.visibility = View.GONE
    }

    private fun showButtons() {
        buttonArea.visibility = View.VISIBLE
    }
}
