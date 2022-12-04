/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.permissiontypes

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.categories.HealthDataCategoriesFragment
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesFragment
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class HealthPermissionTypesFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: HealthPermissionTypesViewModel =
        Mockito.mock(HealthPermissionTypesViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun permissionTypesFragment_isDisplayed() {
        Mockito.`when`(viewModel.permissionTypesData).then {
            MutableLiveData<HealthPermissionTypesViewModel.PermissionTypesFragmentState>(
                HealthPermissionTypesViewModel.PermissionTypesFragmentState.WithData(
                    listOf(
                        HealthPermissionType.DISTANCE,
                        HealthPermissionType.EXERCISE,
                        HealthPermissionType.STEPS)))
        }
        launchFragment<HealthPermissionTypesFragment>(activityCategoryBundle())

        onView(withText("Active calories burned")).check(doesNotExist())
        onView(withText("Distance")).check(matches(isDisplayed()))
        onView(withText("Elevation gained")).check(doesNotExist())
        onView(withText("Exercise")).check(matches(isDisplayed()))
        onView(withText("Floors climbed")).check(doesNotExist())
        onView(withText("Power")).check(doesNotExist())
        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Total calories burned")).check(doesNotExist())
        onView(withText("VO2 max")).check(doesNotExist())
        onView(withText("Wheelchair pushes")).check(doesNotExist())
        onView(withText("Manage data")).check(matches(isDisplayed()))
        onView(withText("App priority")).check(matches(isDisplayed()))
        onView(withText("Delete activity data")).check(matches(isDisplayed()))
    }

    private fun activityCategoryBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString(HealthDataCategoriesFragment.CATEGORY_NAME_KEY, "ACTIVITY")
        return bundle
    }
}
