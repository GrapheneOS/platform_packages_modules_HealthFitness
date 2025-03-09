/*
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

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.hamcrest.TypeSafeMatcher

/**
 * A custom matcher used when there are more than one view with the same resourceId/text/contentDesc
 * etc.
 *
 * @param matcher a view matcher for UI element which may have potentially more than one matched.
 *   Typical example is the element that is repeated in ListView or RecyclerView
 * @param index the index to select matcher if there are more than one matcher. it's started at
 *   zero.
 * @return the view matcher selected from given matcher and index.
 */
fun withIndex(matcher: Matcher<View?>, index: Int): Matcher<View?> {
    return object : TypeSafeMatcher<View?>() {
        var currentIndex = 0

        override fun describeTo(description: Description) {
            description.appendText("with index: ")
            description.appendValue(index)
            matcher.describeTo(description)
        }

        override fun matchesSafely(view: View?): Boolean {
            return matcher.matches(view) && currentIndex++ == index
        }
    }
}

/**
 * A custom matcher to find a CheckBox based on its sibling TextView's text.
 *
 * It is specifically useful for matching a CheckBox in a `SelectorWithWidgetPreference` layout,
 * where the CheckBox and the associated TextView are side by side within separate LinearLayouts.
 *
 * @param keyText The text of the associated TextView to locate the CheckBox.
 * @return A Matcher for the CheckBox that matches the given text of the sibling TextView.
 */
fun checkBoxOf(keyText: String): Matcher<View> {
    return allOf(
        withId(android.R.id.checkbox),
        isDescendantOfA(
            allOf(withId(android.R.id.widget_frame), hasSibling(hasDescendant(withText(keyText))))
        ),
    )
}

/**
 * Matches a view that is an indirect sibling of a given target view. An indirect sibling means the
 * view is a descendant of a view that has a sibling which contains the target view somewhere in its
 * hierarchy.
 *
 * Example: If `View A` and `View B` are inside `Parent 1`, and `Parent 1` has a sibling `Parent 2`
 * that contains `View C`, then `View A` has an indirect sibling relationship with `View C`.
 *
 * @param targetMatcher The matcher for the target view that should be an indirect sibling.
 * @return A matcher that verifies the indirect sibling relationship.
 */
fun hasIndirectSibling(targetMatcher: Matcher<View>): Matcher<View> {
    return isDescendantOfA(hasSibling(hasDescendant(targetMatcher)))
}

/** A custom matcher to find a [Preference] with the given title and summary. */
fun withTitleAndSummary(titleText: String, summaryText: String): Matcher<View> {
    return allOf(
        withId(android.R.id.title),
        withText(titleText),
        hasSibling(hasDescendant(withText(summaryText))),
    )
}

/** A custom matcher to find a [Preference] with the given title and no visible summary. */
fun withTitleNoSummary(titleText: String): Matcher<View> {
    return allOf(
        withId(android.R.id.title),
        withText(titleText),
        hasSibling(hasDescendant(allOf(withId(android.R.id.summary), not(isDisplayed())))),
    )
}

fun atPosition(position: Int, itemMatcher: Matcher<View?>): Matcher<View?> {
    return object : BoundedMatcher<View?, RecyclerView>(RecyclerView::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("has item at position $position: ")
            itemMatcher.describeTo(description)
        }

        override fun matchesSafely(view: RecyclerView): Boolean {
            val viewHolder: RecyclerView.ViewHolder =
                view.findViewHolderForAdapterPosition(position)
                    ?: // has no item on such position
                    return false
            return itemMatcher.matches(viewHolder.itemView)
        }
    }
}

fun isAbove(otherViewMatcher: Matcher<View>): TypeSafeMatcher<View> {
    return object : TypeSafeMatcher<View>() {
        private var otherView: View? = null

        override fun describeTo(description: Description) {
            description.appendText("is above view: ")
            otherViewMatcher.describeTo(description)
        }

        override fun matchesSafely(view: View): Boolean {
            if (otherView == null) {
                otherView = view.rootView.findViewByMatcher(otherViewMatcher)
                if (otherView == null) return false // Other view not found
            }

            val location1 = IntArray(2)
            val location2 = IntArray(2)
            view.getLocationOnScreen(location1)
            otherView!!.getLocationOnScreen(location2) // Safe call as otherView might be null

            return location1[1] < location2[1] // Compare y-coordinates
        }
    }
}

// Extension function to find a view by matcher
private fun View.findViewByMatcher(matcher: Matcher<View>): View? {
    if (matcher.matches(this)) return this // Check if this view itself matches
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val foundView = child.findViewByMatcher(matcher)
            if (foundView != null) return foundView
        }
    }
    return null // No match found
}

// Checkbox assertions
fun assertCheckboxNotChecked(
    recyclerViewId: Int,
    title: String,
    position: Int,
    tag: String = "checkbox",
) {
    onView(withId(recyclerViewId))
        .check(
            matches(
                atPosition(
                    position,
                    allOf(
                        hasDescendant(withText(title)),
                        hasDescendant(withTagValue(`is`(tag))),
                        hasDescendant(isNotChecked()),
                    ),
                )
            )
        )
}

fun assertCheckboxNotShown(
    recyclerViewId: Int,
    title: String,
    position: Int,
    tag: String = "checkbox",
) {
    onView(withId(recyclerViewId))
        .check(
            matches(
                atPosition(
                    position,
                    allOf(
                        hasDescendant(withText(title)),
                        not(hasDescendant(withTagValue(`is`(tag)))),
                    ),
                )
            )
        )
}

fun assertCheckboxChecked(
    recyclerViewId: Int,
    title: String,
    position: Int,
    tag: String = "checkbox",
) {
    onView(withId(recyclerViewId))
        .check(
            matches(
                atPosition(
                    position,
                    allOf(
                        hasDescendant(withText(title)),
                        hasDescendant(withTagValue(`is`(tag))),
                        hasDescendant(isChecked()),
                    ),
                )
            )
        )
}
