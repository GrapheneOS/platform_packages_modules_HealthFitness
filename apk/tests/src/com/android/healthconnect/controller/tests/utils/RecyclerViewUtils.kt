/**
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.utils

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Matcher

fun clickOnRecyclerViewItemWithText(text: String) {
    performActionOnRecyclerViewItem(text, click())
}

fun clickSwitchOnRecyclerViewItemWithText(text: String) {
    performActionOnRecyclerViewItem(text, clickSwitchInView())
}

private fun performActionOnRecyclerViewItem(text: String, action: ViewAction) {
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(text)),
                action,
            )
        )
}

private fun clickSwitchInView(): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View>? {
            return null
        }

        override fun getDescription(): String {
            return "Click on a child view with specified id."
        }

        override fun perform(uiController: UiController, view: View) {
            findSwitchInView(view)?.performClick()
        }

        private fun findSwitchInView(viewGroup: View?): SwitchCompat? {
            if (viewGroup is SwitchCompat) {
                return viewGroup
            }
            if (viewGroup is ViewGroup) {
                for (i in 0 until viewGroup.childCount) {
                    val child = viewGroup.getChildAt(i)
                    val foundSwitch = findSwitchInView(child)
                    if (foundSwitch != null) {
                        return foundSwitch
                    }
                }
            }
            return null
        }
    }
}
