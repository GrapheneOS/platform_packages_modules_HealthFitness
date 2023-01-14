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

import android.content.Intent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.HelpCenterLauncher.openHCGetStartedLink

/** Menu that has Send feedback and Help options. */
object SendFeedbackAndHelpMenu {
    fun setupMenu(fragment: Fragment, viewLifecycleOwner: LifecycleOwner) {
        val menuProvider =
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.send_feedback_and_help, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.menu_send_feedback -> {
                            val intent = Intent(Intent.ACTION_BUG_REPORT)
                            fragment.activity?.startActivityForResult(intent, 0)
                            true
                        }
                        R.id.menu_help -> {
                            openHCGetStartedLink(fragment)
                            true
                        }
                        else -> true
                    }
                }
            }
        (fragment.activity as MenuHost).addMenuProvider(
            menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}
