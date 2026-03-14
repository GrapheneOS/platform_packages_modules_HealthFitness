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

package com.android.healthconnect.controller.data.entries.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.settingslib.widget.SettingsThemeHelper

/**
 * A header view designed to display a large application icon and name prominently at the top of a
 * page. Serves a similar visual purpose to {@link AppHeaderPreference} and {@link IntroPreference}.
 */
class AppHeaderView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var appMetadata: AppMetadata? = null
    private lateinit var appIcon: ImageView
    private lateinit var appTitle: TextView

    init {
        val layout =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                R.layout.widget_app_header_expressive
            } else {
                R.layout.widget_app_header
            }
        val view = inflate(context, layout, this)
        bindAppHeader(view)
    }

    fun setAppInfo(appMetadata: AppMetadata) {
        this.appMetadata = appMetadata
        updateAppView()
    }

    private fun bindAppHeader(view: View) {
        appIcon = view.findViewById(R.id.app_icon)
        appTitle = view.findViewById(R.id.app_title)

        updateAppView()
    }

    private fun updateAppView() {
        appMetadata?.let {
            appIcon.background = it.icon
            appTitle.text = it.appName
        }
    }
}
