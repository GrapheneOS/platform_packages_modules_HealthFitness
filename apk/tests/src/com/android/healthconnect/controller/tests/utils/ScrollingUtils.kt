/**
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.utils

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToLastPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText

/**
 * Scrolls to the top of a `RecyclerView` within a PreferenceScreen. This should be used when
 * interacting with a `RecyclerView`.
 */
fun scrollToTopOfPreferenceScreen() {
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(scrollToPosition<RecyclerView.ViewHolder>(0))
    onIdle()
}

/**
 * Scrolls to the bottom of a `RecyclerView` within a PreferenceScreen. This should be used when
 * interacting with a `RecyclerView`.
 */
fun scrollToBottomOfPreferenceScreen() {
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
    onIdle()
}

// Note: Espresso's `ViewActions.scrollTo()` only works on `ScrollView`. For `RecyclerView`,
// `RecyclerViewActions.scrollTo()` or other methods from `RecyclerViewActions` must be used.

/**
 * Scrolls a `RecyclerView` within a PreferenceScreen until the item containing the given [text] is
 * visible. This should be used when interacting with a `RecyclerView`.
 *
 * @param text The text to scroll to.
 */
fun scrollToText(text: String) {
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(text))))
}

/**
 * Scrolls a `RecyclerView` within a PreferenceScreen until the item containing the given [text] is
 * visible and then clicks on it. This should be used when interacting with a `RecyclerView`.
 *
 * @param text The text of the item to scroll to and click.
 */
fun scrollToTextAndClick(text: String) {
    // Make sure the text is visible before clicking on it.
    scrollToText(text)
    onView(withId(androidx.preference.R.id.recycler_view))
        .perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(text)),
                click(),
            )
        )
}

/**
 * Checks if the given [text] is displayed on the screen. If not, it attempts to scroll a
 * `RecyclerView` within a PreferenceScreen to find the text. This is useful for views within a
 * `RecyclerView`.
 *
 * @param text The text to check for.
 */
fun checkTextIsDisplayed(text: String) {
    try {
        onView(withText(text)).check(matches(isDisplayed()))
    } catch (e: Throwable) { // Common ancestor of NoMatchingViewException and AssertionFailedError
        scrollToText(text)
        onView(withText(text)).check(matches(isDisplayed()))
    }
}
