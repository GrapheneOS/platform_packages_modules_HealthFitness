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
package com.android.healthconnect.controller.tests.permissions.connectedapps.wear

import android.content.Context
import android.health.connect.Constants
import android.health.connect.HealthPermissions.READ_HEART_RATE
import android.health.connect.HealthPermissions.READ_OXYGEN_SATURATION
import android.health.connect.HealthPermissions.READ_SKIN_TEMPERATURE
import android.health.connect.accesslog.AccessLog
import android.health.connect.datatypes.RecordTypeIdentifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.app.ILoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.connectedapps.wear.ControlBackgroundReadForSingleAppScreen
import com.android.healthconnect.controller.permissions.connectedapps.wear.WearConnectedAppsViewModel
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TestComposeActivity
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionAppsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeRecentAccessUseCase
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class ControlBackgroundPermissionForSingleAppScreenTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<TestComposeActivity>()

    private lateinit var wearConnectedAppsViewModel: WearConnectedAppsViewModel
    private val loadHealthPermissionApps: ILoadHealthPermissionApps =
        FakeHealthPermissionAppsUseCase()
    private val loadAppPermissionsStatusUseCase: ILoadAppPermissionsStatusUseCase =
        FakeLoadAppPermissionsStatusUseCase()
    @BindValue val grantPermissionsStatusUseCase: GrantHealthPermissionUseCase = mock()
    @BindValue val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase = mock()
    private val loadRecentAccessUseCase: ILoadRecentAccessUseCase = FakeRecentAccessUseCase()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()

    lateinit var context: Context

    @Before
    fun setup() = runTest {
        hiltRule.inject()
        context = getInstrumentation().context
        composeTestRule.mainClock.autoAdvance = false

        whenever(healthPermissionReader.getSystemHealthPermissions()).then {
            listOf(READ_HEART_RATE, READ_SKIN_TEMPERATURE, READ_OXYGEN_SATURATION)
        }

        wearConnectedAppsViewModel =
            WearConnectedAppsViewModel(
                loadHealthPermissionApps,
                loadAppPermissionsStatusUseCase,
                grantPermissionsStatusUseCase,
                revokeHealthPermissionUseCase,
                loadRecentAccessUseCase,
                healthPermissionReader,
            )
    }

    @After
    fun tearDown() {
        (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).reset()
        (loadAppPermissionsStatusUseCase as FakeLoadAppPermissionsStatusUseCase).reset()
        (loadRecentAccessUseCase as FakeRecentAccessUseCase).reset()
    }

    @Test
    fun displaysCorrectly() {
        val app =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
                            isGranted = true,
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = true,
                        ),
                        HealthPermissionStatus(
                            healthPermission =
                                HealthPermission.AdditionalPermission
                                    .READ_HEALTH_DATA_IN_BACKGROUND,
                            isGranted = true,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(
            listOf(app),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                ControlBackgroundReadForSingleAppScreen(
                    viewModel = wearConnectedAppsViewModel,
                    packageName = app.appMetadata.packageName,
                    onBackClick = {},
                    onAppInfoPermissionClick = {},
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().printToLog("ControlBackgroundReadForSingleAppScreenTest")
        composeTestRule
            .onNodeWithText(
                "Choose when ${app.appMetadata.appName} " +
                    "can access your fitness and wellness data"
            )
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "${app.appMetadata.appName} has access to heart rate and skin temperature."
            )
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("All the time").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("While using the app").performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Manage fitness and wellness permissions")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun whenNoReadPermissions_displaysCorrectMessage() {
        val app =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission =
                                HealthPermission.AdditionalPermission
                                    .READ_HEALTH_DATA_IN_BACKGROUND,
                            isGranted = true,
                        )
                    ),
                recentAccess = listOf(),
            )

        setupConnectedApps(
            listOf(app),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                ControlBackgroundReadForSingleAppScreen(
                    viewModel = wearConnectedAppsViewModel,
                    packageName = app.appMetadata.packageName,
                    onBackClick = {},
                    onAppInfoPermissionClick = {},
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().printToLog("ControlBackgroundReadForSingleAppScreenTest")
        composeTestRule
            .onNodeWithText(
                "Enable at least one read permission in order to turn on background access for this app"
            )
            .performScrollTo()
            .assertIsDisplayed()
    }
}
