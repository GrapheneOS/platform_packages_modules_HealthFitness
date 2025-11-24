/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.android.healthconnect.controller.tests.permissions.connectedapps

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_SET
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions
import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.app.FitnessAppFragment
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.service.HealthPermissionManagerModule
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.clickOnRecyclerViewItemWithText
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@UninstallModules(HealthPermissionManagerModule::class, HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class MockedFitnessAppFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val manager: HealthConnectManager = mock()
    @BindValue val healthPermissionManager: HealthPermissionManager = mock()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()
    @BindValue lateinit var appInfoReader: AppInfoReader

    @Before
    fun setup() = runTest {
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        whenever(healthPermissionReader.getValidHealthPermissions(TEST_APP_PACKAGE_NAME))
            .thenReturn(
                listOf(
                    fromPermissionString(HealthPermissions.READ_EXERCISE),
                    fromPermissionString(HealthPermissions.READ_HEART_RATE),
                    fromPermissionString(HealthPermissions.READ_EXERCISE_ROUTES),
                    fromPermissionString(HealthPermissions.WRITE_STEPS),
                    fromPermissionString(HealthPermissions.READ_HEALTH_DATA_HISTORY),
                )
            )
    }

    @After
    fun tearDown() {
        Mockito.reset(healthPermissionManager)
        Mockito.reset(healthPermissionReader)
    }

    @Test
    fun exerciseRoutesAlwaysAllow_revokeExercise_notLastReadPermission_showsDialog() {
        whenever(healthPermissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .thenReturn(
                listOf(
                    HealthPermissions.READ_EXERCISE,
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.READ_EXERCISE_ROUTES,
                )
            )

        launchFragment<FitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use {
                clickOnRecyclerViewItemWithText("Activity")
                onView(withText("Exercise")).perform(scrollTo()).perform(click())

                // check for dialog
                onView(withText("Disable both data types?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "$TEST_APP_NAME requires exercise access in order for exercise routes to be enabled"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Yes")).inRoot(isDialog()).perform(click())

                verify(healthPermissionManager)
                    .revokeHealthPermission(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_EXERCISE)
                verify(healthPermissionManager)
                    .revokeHealthPermission(
                        TEST_APP_PACKAGE_NAME,
                        HealthPermissions.READ_EXERCISE_ROUTES,
                    )
                verify(healthPermissionManager, never())
                    .revokeHealthPermission(
                        TEST_APP_PACKAGE_NAME,
                        HealthPermissions.READ_HEALTH_DATA_HISTORY,
                    )
            }
    }

    @Test
    fun exerciseRoutesAlwaysAllow_revokeExercise_lastReadPermission_showsDialog() {
        var revokeCalled = false
        whenever(healthPermissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .thenAnswer {
                if (revokeCalled) {
                    listOf(
                        HealthPermissions.READ_EXERCISE,
                        HealthPermissions.READ_HEALTH_DATA_HISTORY,
                    )
                } else {
                    listOf(
                        HealthPermissions.READ_EXERCISE,
                        HealthPermissions.READ_EXERCISE_ROUTES,
                        HealthPermissions.READ_HEALTH_DATA_HISTORY,
                    )
                }
            }

        // After revoking ER, do not return it in granted permissions anymore as this will trigger
        // calling the revoke API again
        whenever(
                healthPermissionManager.revokeHealthPermission(
                    TEST_APP_PACKAGE_NAME,
                    HealthPermissions.READ_EXERCISE_ROUTES,
                )
            )
            .then {
                revokeCalled = true
                null
            }

        launchFragment<FitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use {
                clickOnRecyclerViewItemWithText("Activity")
                onView(withText("Exercise")).perform(scrollTo()).perform(click())

                // check for dialog
                onView(withText("Disable both data types?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "$TEST_APP_NAME requires exercise access in order for exercise routes to be enabled"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Yes")).inRoot(isDialog()).perform(click())

                verify(healthPermissionManager)
                    .revokeHealthPermission(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_EXERCISE)
                verify(healthPermissionManager)
                    .revokeHealthPermission(
                        TEST_APP_PACKAGE_NAME,
                        HealthPermissions.READ_EXERCISE_ROUTES,
                    )
                verify(healthPermissionManager)
                    .revokeHealthPermission(
                        TEST_APP_PACKAGE_NAME,
                        HealthPermissions.READ_HEALTH_DATA_HISTORY,
                    )
            }
    }

    @Test
    fun exerciseRoutesAskEveryTime_revokeExercise_lastReadPermission_doesNotShowDialog() {
        whenever(healthPermissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .thenReturn(
                listOf(HealthPermissions.READ_EXERCISE, HealthPermissions.READ_HEALTH_DATA_HISTORY)
            )

        whenever(
                healthPermissionManager.getHealthPermissionsFlags(
                    TEST_APP_PACKAGE_NAME,
                    listOf(HealthPermissions.READ_EXERCISE_ROUTES, HealthPermissions.READ_EXERCISE),
                )
            )
            .thenReturn(
                mapOf(
                    HealthPermissions.READ_EXERCISE_ROUTES to FLAG_PERMISSION_USER_SET,
                    HealthPermissions.READ_EXERCISE to FLAG_PERMISSION_USER_SET,
                )
            )

        launchFragment<FitnessAppFragment>(
                Bundle().apply {
                    putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                    putString(EXTRA_APP_NAME, TEST_APP_NAME)
                }
            )
            .use {
                clickOnRecyclerViewItemWithText("Activity")
                onView(withText("Exercise")).perform(scrollTo()).perform(click())

                // check for dialog
                onView(withText("Disable both data types?")).check(doesNotExist())

                verify(healthPermissionManager)
                    .revokeHealthPermission(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_EXERCISE)
                verify(healthPermissionManager, never())
                    .revokeHealthPermission(
                        TEST_APP_PACKAGE_NAME,
                        HealthPermissions.READ_EXERCISE_ROUTES,
                    )
                verify(healthPermissionManager)
                    .revokeHealthPermission(
                        TEST_APP_PACKAGE_NAME,
                        HealthPermissions.READ_HEALTH_DATA_HISTORY,
                    )
            }
    }
}
