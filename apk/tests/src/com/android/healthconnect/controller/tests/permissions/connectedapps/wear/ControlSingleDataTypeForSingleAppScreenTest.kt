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
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.ILoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.connectedapps.wear.ControlSingleDataTypeForSingleAppScreen
import com.android.healthconnect.controller.permissions.connectedapps.wear.WearConnectedAppsViewModel
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.tests.recentaccess.api.FakeRecentAccessUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TestComposeActivity
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionAppsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadAppPermissionsStatusUseCase
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
class ControlSingleDataTypeForSingleAppScreenTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<TestComposeActivity>()
    @get:Rule(order = 2) val fakeUseCaseRule = FakeUseCaseRule()

    private lateinit var wearConnectedAppsViewModel: WearConnectedAppsViewModel
    private val loadHealthPermissionApps: ILoadHealthPermissionApps =
        FakeHealthPermissionAppsUseCase()
    private val loadAppPermissionsStatusUseCase: ILoadAppPermissionsStatusUseCase =
        FakeLoadAppPermissionsStatusUseCase()
    @BindValue val grantPermissionsStatusUseCase: GrantHealthPermissionUseCase = mock()
    @BindValue val revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase = mock()
    private val loadRecentAccessUseCase = fakeUseCaseRule.watch(FakeRecentAccessUseCase())
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
                context,
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
    }

    @Test
    fun displaysCorrectly_whenPermissionAllowedAndBgGranted() {
        val app =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
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
                isSystem = false,
            )

        setupConnectedApps(
            listOf(app),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        composeTestRule.setContent {
            ControlSingleDataTypeForSingleAppScreen(
                viewModel = wearConnectedAppsViewModel,
                fitnessPermission = READ_HEART_RATE_PERMISSION,
                packageName = appMetadataOne.packageName,
                onAdditionalPermissionClick = { _ -> },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("ControlSingleDataTypeForSingleAppScreenTest")
        composeTestRule.onNodeWithText(appMetadataOne.appName).assertExists()
        composeTestRule.onNodeWithText("Heart rate").assertExists()
        composeTestRule.onNodeWithText("Allow").performScrollTo().assertIsOn()
        composeTestRule.onNodeWithText("Don't allow").performScrollTo().assertIsOff()
        composeTestRule.onNodeWithText("Additional access").performScrollTo().assertExists()
        composeTestRule
            .onNodeWithText(
                "Currently, ${appMetadataOne.appName} can access fitness and wellness data all the time"
            )
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun displaysCorrectly_whenPermissionNotAllowedAndBgNotGranted() {
        val app =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
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
                isSystem = false,
            )

        setupConnectedApps(
            listOf(app),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        composeTestRule.setContent {
            ControlSingleDataTypeForSingleAppScreen(
                viewModel = wearConnectedAppsViewModel,
                fitnessPermission = READ_HEART_RATE_PERMISSION,
                packageName = appMetadataOne.packageName,
                onAdditionalPermissionClick = { _ -> },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("ControlSingleDataTypeForSingleAppScreenTest")
        composeTestRule.onNodeWithText(appMetadataOne.appName).assertExists()
        composeTestRule.onNodeWithText("Heart rate").assertExists()
        composeTestRule.onNodeWithText("Allow").performScrollTo().assertIsOff()
        composeTestRule.onNodeWithText("Don't allow").performScrollTo().assertIsOn()
        composeTestRule.onNodeWithText("Additional access").performScrollTo().assertExists()
        composeTestRule
            .onNodeWithText(
                "Currently, ${appMetadataOne.appName} can access fitness and wellness data while in use"
            )
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun displaysCorrectly_whenPermissionNotAllowedAndBgNotRequested() {
        val app =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
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
                isSystem = false,
            )

        setupConnectedApps(
            listOf(app),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        composeTestRule.setContent {
            ControlSingleDataTypeForSingleAppScreen(
                viewModel = wearConnectedAppsViewModel,
                fitnessPermission = READ_HEART_RATE_PERMISSION,
                packageName = appMetadataOne.packageName,
                onAdditionalPermissionClick = { _ -> },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("ControlSingleDataTypeForSingleAppScreenTest")
        composeTestRule.onNodeWithText(appMetadataOne.appName).assertExists()
        composeTestRule.onNodeWithText("Heart rate").assertExists()
        composeTestRule.onNodeWithText("Allow").performScrollTo().assertIsOff()
        composeTestRule.onNodeWithText("Don't allow").performScrollTo().assertIsOn()
        composeTestRule.onNodeWithText("Additional access").assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                "Currently, ${appMetadataOne.appName} can access fitness and wellness data while in use"
            )
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun doesNotDisplay_whenHealthAppDataDoesNotExist() {
        val app =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
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
                isSystem = false,
            )

        setupConnectedApps(
            listOf(app),
            loadHealthPermissionApps,
            loadAppPermissionsStatusUseCase,
            loadRecentAccessUseCase,
        )

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        composeTestRule.setContent {
            ControlSingleDataTypeForSingleAppScreen(
                viewModel = wearConnectedAppsViewModel,
                fitnessPermission = READ_HEART_RATE_PERMISSION,
                packageName = appMetadataTwo.packageName,
                onAdditionalPermissionClick = { _ -> },
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("ControlSingleDataTypeForSingleAppScreenTest")
        composeTestRule.onNodeWithText("Heart rate").assertDoesNotExist()
        composeTestRule.onNodeWithText("Allow").assertDoesNotExist()
        composeTestRule.onNodeWithText("Don't allow").assertDoesNotExist()
        composeTestRule.onNodeWithText("Additional access").assertDoesNotExist()
    }
}
