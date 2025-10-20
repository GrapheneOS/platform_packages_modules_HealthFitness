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

package com.android.healthconnect.controller.matchmaking

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.healthconnect.controller.R
import java.text.NumberFormat

/**
 * View to display the requesting app icon and icons of matched apps connected with a line through
 * the health connect icon in the matchmaking screen's header.
 */
class MatchmakingHeaderIconView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private val requestingAppIconView: ImageView
    private val healthConnectIconView: ImageView
    private val matchedAppIcon1View: ImageView
    private val matchedAppIcon1Container: FrameLayout
    private val matchedAppIcon2View: ImageView
    private val matchedAppIcon2Container: FrameLayout
    private val plusNContainer: FrameLayout
    private val plusNTextView: TextView
    private val matchedIconsGroup: ConstraintLayout
    private val line1: View
    private val line2: View

    var requestingAppIcon: Drawable? = null
        set(value) {
            field = value
            requestingAppIconView.setImageDrawable(value)
            updateVisibility()
        }

    var healthConnectIcon: Drawable? = null
        set(value) {
            field = value
            healthConnectIconView.setImageDrawable(value)
            updateVisibility()
        }

    var matchedAppIcons: List<Drawable> = emptyList()
        set(value) {
            field = value
            updateMatchedAppIcons()
            updateVisibility()
        }

    private val numberFormat = NumberFormat.getIntegerInstance()

    init {
        LayoutInflater.from(context).inflate(R.layout.matchmaking_header_icons_layout, this, true)

        matchedIconsGroup = findViewById(R.id.matched_icons_group)
        requestingAppIconView = findViewById(R.id.requesting_app_icon)
        healthConnectIconView = findViewById(R.id.health_connect_icon)
        matchedAppIcon1View = findViewById(R.id.matched_app_icon_1)
        matchedAppIcon1Container = findViewById(R.id.matched_app_icon_1_container)
        matchedAppIcon2View = findViewById(R.id.matched_app_icon_2)
        matchedAppIcon2Container = findViewById(R.id.matched_app_icon_2_container)
        plusNContainer = findViewById(R.id.plus_n_container)
        plusNTextView = findViewById(R.id.plus_n_text)
        line1 = findViewById(R.id.line_1)
        line2 = findViewById(R.id.line_2)

        updateVisibility()
    }

    private fun updateMatchedAppIcons() {
        matchedAppIcon1Container.visibility = GONE
        matchedAppIcon2Container.visibility = GONE
        plusNContainer.visibility = GONE

        when (matchedAppIcons.size) {
            1 -> {
                matchedAppIcon1View.setImageDrawable(matchedAppIcons[0])
                matchedAppIcon1Container.visibility = VISIBLE
            }
            2 -> {
                matchedAppIcon1View.setImageDrawable(matchedAppIcons[0])
                matchedAppIcon2View.setImageDrawable(matchedAppIcons[1])
                matchedAppIcon1Container.visibility = VISIBLE
                matchedAppIcon2Container.visibility = VISIBLE
            }
            in 3..Int.MAX_VALUE -> {
                matchedAppIcon1View.setImageDrawable(matchedAppIcons[0])
                matchedAppIcon1Container.visibility = VISIBLE
                plusNTextView.text =
                    context.getString(
                        R.string.plus_n_matched_apps_format,
                        numberFormat.format(matchedAppIcons.size - 1),
                    )
                plusNContainer.visibility = VISIBLE
            }
            0 -> {
                // No-op, all views are already GONE.
            }
        }
    }

    private fun updateVisibility() {
        val showRequestingApp = requestingAppIcon != null
        val showHealthConnect = healthConnectIcon != null
        val showMatchedApps = matchedAppIcons.isNotEmpty()

        requestingAppIconView.visibility = if (showRequestingApp) View.VISIBLE else View.GONE
        healthConnectIconView.visibility = if (showHealthConnect) View.VISIBLE else View.GONE

        line1.visibility = if (showRequestingApp && showHealthConnect) View.VISIBLE else View.GONE
        line2.visibility = if (showHealthConnect && showMatchedApps) View.VISIBLE else View.GONE

        matchedIconsGroup.visibility = if (showMatchedApps) View.VISIBLE else View.GONE

        if (showMatchedApps) {
            updateMatchedAppIcons()
        }
    }
}
