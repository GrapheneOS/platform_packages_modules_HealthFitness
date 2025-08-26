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

package com.android.healthconnect.controller.tests.shared.dialog

import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.HealthConnectBottomSheetDialogFragment
import com.android.healthconnect.controller.tests.TestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HealthConnectBottomSheetDialogFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun bottomSheet_displaysContentFragment() {
        val scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            val bottomSheet =
                HealthConnectBottomSheetDialogFragment.newInstance(TestFragment::class.java)
            bottomSheet.show(activity.supportFragmentManager, "TestBottomSheet")
        }
        Thread.sleep(1000)
        onView(withId(R.id.test_fragment_view)).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    class TestFragment : Fragment() {
        override fun onCreateView(
            inflater: android.view.LayoutInflater,
            container: android.view.ViewGroup?,
            savedInstanceState: android.os.Bundle?,
        ): android.view.View? {
            val view = android.view.View(requireContext())
            view.id = R.id.test_fragment_view
            return view
        }
    }
}
