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
package com.android.healthconnect.controller.utils

import android.app.Activity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.ExternalActivityLauncher.openHCGetStartedLink
import com.android.healthconnect.controller.utils.ExternalActivityLauncher.openSendFeedbackActivity

/** Sets fragment title on the collapsing layout, delegating to host if needed. */
fun Fragment.setTitle(@StringRes title: Int) {
    (requireActivity() as Activity).setTitle(title)
}

fun Fragment.setupMenu(
    @MenuRes menuRes: Int,
    viewLifecycleOwner: LifecycleOwner,
    onMenuItemSelected: (MenuItem) -> Boolean
) {

    val menuProvider =
        object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(menuRes, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_send_feedback -> {
                        openSendFeedbackActivity(requireActivity())
                        true
                    }
                    R.id.menu_help -> {
                        openHCGetStartedLink(requireActivity())
                        true
                    }
                    else -> onMenuItemSelected.invoke(menuItem)
                }
            }
        }

    (requireActivity() as MenuHost).addMenuProvider(
        menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
}

fun Fragment.setupSharedMenu(
    viewLifecycleOwner: LifecycleOwner,
    @MenuRes menuRes: Int = R.menu.send_feedback_and_help,
    onMenuItemSelected: (MenuItem) -> Boolean = { false }
) {
    setupMenu(menuRes, viewLifecycleOwner, onMenuItemSelected)
}
