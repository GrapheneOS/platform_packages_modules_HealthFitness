/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.shared.preference

import android.content.Context
import android.icu.text.MessageFormat
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppUtils
import com.android.settingslib.widget.SettingsThemeHelper
import java.text.NumberFormat
import java.util.Locale

/**
 * A custom `Preference` that represents a ranked item with an action icon.
 *
 * This preference is designed for use in lists where items have a specific order (ranking) and
 * require an action menu for reordering or other operations. It displays:
 * - A position number indicating its rank, 1-based indexing.
 * - A label (e.g., app name).
 * - A clickable action icon for additional options (optional).
 *
 * @param context The context in which this preference is used.
 * @param appMetadata Metadata about the item (e.g., app details).
 * @param appUtils To determine whether a package name is the default app.
 * @param position The current position of the item in the ranked list, 0-based indexing.
 * @param onActionClick A lambda that handles action clicks (e.g., reordering, removing items).
 * @param shouldShowActionButton Whether the action button should be shown, by default true.
 */
class RankedActionPreference(
    context: Context,
    private val appMetadata: AppMetadata,
    private val appUtils: AppUtils,
    private var position: Int,
    private val onActionClick: (View, Int) -> Unit,
    private var shouldShowActionButton: Boolean = true,
) : HealthPreference(context, null) {

    private lateinit var rootView: LinearLayout
    private lateinit var appPositionView: TextView
    private lateinit var actionView: FrameLayout

    init {
        layoutResource =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                R.layout.widget_app_source_layout_expressive
            } else {
                R.layout.widget_app_source_layout_legacy
            }
        order = position
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        rootView = holder.itemView as LinearLayout
        appPositionView = holder.findViewById(R.id.app_position) as TextView
        val appNameView = holder.findViewById(R.id.app_name) as TextView
        val appSummary = holder.findViewById(R.id.app_source_summary) as TextView
        actionView = holder.findViewById(R.id.action_icon) as FrameLayout

        setPositionString()
        appNameView.text = appMetadata.appName
        setAppSummary(appSummary)
        setActionButton()
        setRootContentDescription()
    }

    fun reduceIndex() {
        position--
        setPositionString()
    }

    fun hideActionButton() {
        shouldShowActionButton = false
        actionView.isVisible = false
    }

    private fun setPositionString() {
        val positionString: String = NumberFormat.getIntegerInstance().format(position + 1)
        appPositionView.text = positionString
    }

    private fun setAppSummary(appSummary: TextView) {
        if (isDefaultApp()) {
            appSummary.text = context.getString(R.string.default_app_summary)
            appSummary.visibility = View.VISIBLE
        } else {
            appSummary.visibility = View.GONE
        }
    }

    private fun setActionButton() {
        if (shouldShowActionButton) {
            actionView.isVisible = true
            actionView.setOnClickListener { view -> onActionClick(view, position) }
            actionView.contentDescription =
                context.getString(
                    R.string.see_actions_button_content_description,
                    appMetadata.appName,
                )
        } else {
            actionView.isVisible = false
        }
    }

    private fun setRootContentDescription() {
        rootView.let {
            it.isClickable = false
            it.isLongClickable = false
            it.isFocusable = false
            it.contentDescription = getContentDescription()
        }
    }

    private fun getContentDescription(): String {
        val formatter = MessageFormat("{0,ordinal}", Locale.getDefault())
        val ordinal = formatter.format(arrayOf(position + 1))
        val description =
            context.getString(
                R.string.priority_list_item_content_description,
                ordinal,
                appMetadata.appName,
            )
        return if (isDefaultApp()) {
            description + "\n" + context.getString(R.string.default_app_summary)
        } else {
            description
        }
    }

    private fun isDefaultApp(): Boolean = appUtils.isDefaultApp(context, appMetadata.packageName)
}
