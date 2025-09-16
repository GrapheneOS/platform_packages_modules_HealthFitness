/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.navigation

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.widget.SettingsThemeHelper

class DestinationChangedListener(private val activity: CollapsingToolbarBaseActivity) :
    NavController.OnDestinationChangedListener {
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?,
    ) {
        activity.setTitle(destination.label)
        // force app bar to collapse when navigating to a new destination b/442774925
        if (SettingsThemeHelper.isExpressiveTheme(activity)) {
            activity.appBarLayout?.setExpanded(false)
        }
    }
}
