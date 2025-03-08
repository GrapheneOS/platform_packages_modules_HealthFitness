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
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.EntryPointAccessors

/** Base fragment class for AOB-like screens that need a bottom button bar. */
abstract class HealthSetupFragment : SettingsBasePreferenceFragment() {
    private lateinit var preferenceContainer: ViewGroup
    private lateinit var preferenceArea: ViewGroup

    private lateinit var primaryButtonFull: Button
    private lateinit var primaryButtonOutline: Button
    private lateinit var secondaryButton: Button
    private var pageName: PageName = PageName.UNKNOWN_PAGE
    private lateinit var logger: HealthConnectLogger

    fun setPageName(pageName: PageName) {
        this.pageName = pageName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setupLogger()
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        logger.setPageId(pageName)
        logger.logPageImpression()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        logger.setPageId(pageName)
        val rootView = inflater.inflate(R.layout.fragment_setup, container, false)

        val buttonLayoutId =
            if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
                R.layout.widget_setup_bottom_button_bar_expressive
            } else {
                R.layout.widget_setup_bottom_button_bar_legacy
            }

        val buttonArea = rootView.findViewById<FrameLayout>(R.id.action_container)
        val buttons = inflater.inflate(buttonLayoutId, buttonArea, false)
        buttonArea.addView(buttons)

        preferenceArea = rootView.findViewById(R.id.preference_container)
        preferenceContainer =
            super.onCreateView(inflater, container, savedInstanceState) as ViewGroup
        preferenceArea.addView(preferenceContainer)

        primaryButtonFull = buttonArea.findViewById(R.id.primary_button_full)
        primaryButtonOutline = buttonArea.findViewById(R.id.primary_button_outline)
        secondaryButton = buttonArea.findViewById(R.id.secondary_button)

        return rootView
    }

    fun getPrimaryButtonFull(): Button {
        primaryButtonOutline.visibility = View.GONE
        primaryButtonFull.visibility = View.VISIBLE
        return primaryButtonFull
    }

    fun getPrimaryButtonOutline(): Button {
        primaryButtonOutline.visibility = View.VISIBLE
        primaryButtonFull.visibility = View.GONE
        return primaryButtonOutline
    }

    fun getSecondaryButton(): Button = secondaryButton

    private fun setupLogger() {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                requireContext().applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
        logger.setPageId(pageName)
    }
}
