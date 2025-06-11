/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.data.entriesandaccess

import android.health.connect.HealthConnectManager
import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.PERMISSION_TYPE_NAME_KEY
import com.android.healthconnect.controller.data.entriesandaccess.EntriesAndAccessFragment
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.STEPS
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType.VACCINES
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class EntriesAndAccessFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue lateinit var appInfoReader: AppInfoReader
    @BindValue val healthConnectManager: HealthConnectManager = mock()

    @Before
    fun setup() = runTest {
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
    }

    @Test
    fun entriesAndAccessInit_showsTabs() {
        launchFragment<EntriesAndAccessFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))

        onView(withText("Entries")).check(matches(isDisplayed()))
        onView(withText("Access")).check(matches(isDisplayed()))
    }

    @Test
    fun entriesAndAccessInit_medicalData_showsTabs() {
        launchFragment<EntriesAndAccessFragment>(
            bundleOf(PERMISSION_TYPE_NAME_KEY to VACCINES.name)
        )

        onView(withText("Entries")).check(matches(isDisplayed()))
        onView(withText("Access")).check(matches(isDisplayed()))
    }
}
