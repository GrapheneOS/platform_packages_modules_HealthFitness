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
package com.android.healthconnect.controller.tests.categories

import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.permissions.PERMISSIONS_STATE
import com.android.healthconnect.controller.permissions.PermissionsActivity
import com.android.healthconnect.controller.permissions.PermissionsState
import com.android.healthconnect.controller.permissions.PermissionsViewModel
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class PermissionsActivityTest {

  @get:Rule val hiltRule = HiltAndroidRule(this)

  @BindValue val viewModel: PermissionsViewModel = Mockito.mock(PermissionsViewModel::class.java)

  @Before
  fun setup() {
    hiltRule.inject()
  }

  @Test
  fun intentLaunchesPermissionsActivity() {
    Mockito.`when`(viewModel.permissions).then {
      MutableLiveData(PermissionsState(listOf(), listOf()))
    }

    val startActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                            getInstrumentation().getContext(), PermissionsActivity::class.java))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

    getInstrumentation().getContext().startActivity(startActivityIntent)

    onView(withText("Cancel")).check(matches(isDisplayed()))
    onView(withText("Allow")).check(matches(isDisplayed()))
  }

  @Test
  fun intentDisplaysPermissions() {
    Mockito.`when`(viewModel.permissions).then { MutableLiveData(PERMISSIONS_STATE) }

    val startActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                            getInstrumentation().getContext(), PermissionsActivity::class.java))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

    getInstrumentation().getContext().startActivity(startActivityIntent)

    onView(withText("STEPS")).check(matches(isDisplayed()))
    onView(withText("HEART_RATE")).check(matches(isDisplayed()))
    onView(withText("DISTANCE")).check(matches(isDisplayed()))
    onView(withText("SESSION")).check(matches(isDisplayed()))
  }
}
