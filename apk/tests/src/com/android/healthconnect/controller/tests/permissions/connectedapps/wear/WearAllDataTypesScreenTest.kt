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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.app.ILoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.connectedapps.wear.AllDataTypesScreen
import com.android.healthconnect.controller.permissions.connectedapps.wear.WearConnectedAppsViewModel
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
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
class WearAllDataTypesScreenTest {

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

    val appMetadataOne =
        AppMetadata(
            packageName = "packageName1",
            appName = "AppName1",
            isSystem = false,
            icon = null,
        )
    val appMetadataTwo =
        AppMetadata(
            packageName = "packageName2",
            appName = "AppName2",
            isSystem = false,
            icon = null,
        )
    val appMetadataThree =
        AppMetadata(
            packageName = "packageName3",
            appName = "AppName3",
            isSystem = false,
            icon = null,
        )
    val systemAppMetadata =
        AppMetadata(
            packageName = "packageName4",
            appName = "AppName4",
            isSystem = true,
            icon = null,
        )

    val READ_HEART_RATE_PERMISSION =
        HealthPermission.FitnessPermission(
            FitnessPermissionType.HEART_RATE,
            PermissionsAccessType.READ,
        )
    val READ_OXYGEN_SATURATION_PERMISSION =
        HealthPermission.FitnessPermission(
            FitnessPermissionType.OXYGEN_SATURATION,
            PermissionsAccessType.READ,
        )
    val READ_SKIN_TEMPERATURE_PERMISSION =
        HealthPermission.FitnessPermission(
            FitnessPermissionType.SKIN_TEMPERATURE,
            PermissionsAccessType.READ,
        )

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
    fun systemAppsNotCounted() {
        val app1 =
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
                            isGranted = false,
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

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataTwo,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
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
                            appMetadataTwo.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app3 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataThree,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = false,
                        )
                    ),
                recentAccess = listOf(),
            )

        val app4 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadata,
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
                            healthPermission = READ_OXYGEN_SATURATION_PERMISSION,
                            isGranted = true,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadata.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(listOf(app1, app2, app3, app4))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                AllDataTypesScreen(
                    viewModel = wearConnectedAppsViewModel,
                    showRecentAccess = false,
                    onClick = { _, _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fitness and wellness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vitals").assertIsDisplayed()
        assertTitleAndSummary("Heart rate", "2 of 2 apps allowed")
        assertTitleAndSummary("Skin temperature", "0 of 2 apps allowed")
        assertTitleAndSummary("Oxygen saturation", "No apps requesting")
    }

    @Test
    fun permissionSortedByUsage_thenAlphabetically() {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_OXYGEN_SATURATION_PERMISSION,
                            isGranted = true,
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = false,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataTwo,
                permissionStatus =
                    listOf(
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
                            appMetadataTwo.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app3 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataThree,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = false,
                        )
                    ),
                recentAccess = listOf(),
            )

        val app4 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadata,
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
                            healthPermission = READ_OXYGEN_SATURATION_PERMISSION,
                            isGranted = true,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadata.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(listOf(app1, app2, app3, app4))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                AllDataTypesScreen(
                    viewModel = wearConnectedAppsViewModel,
                    showRecentAccess = false,
                    onClick = { _, _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fitness and wellness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vitals").assertIsDisplayed()
        val listChildren = composeTestRule.onNodeWithText("Heart rate").onParent().onChildren()
        listChildren[2].assert(hasText("Oxygen saturation"))
        listChildren[3].assert(hasText("Skin temperature"))
        listChildren[4].assert(hasText("Heart rate"))
    }

    @Test
    fun doNotShowRecentAccess_showsXOfYAppsAllowed() {
        val app1 =
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
                            isGranted = false,
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

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataTwo,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
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
                            appMetadataTwo.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app3 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataThree,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = true,
                        )
                    ),
                recentAccess = listOf(),
            )

        val app4 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadata,
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
                            healthPermission = READ_OXYGEN_SATURATION_PERMISSION,
                            isGranted = true,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadata.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(listOf(app1, app2, app3, app4))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                AllDataTypesScreen(
                    viewModel = wearConnectedAppsViewModel,
                    showRecentAccess = false,
                    onClick = { _, _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fitness and wellness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vitals").assertIsDisplayed()
        assertTitleAndSummary("Heart rate", "2 of 2 apps allowed")
        assertTitleAndSummary("Skin temperature", "1 of 2 apps allowed")
    }

    @Test
    fun doNotShowRecentAccess_showsNoAppsRequesting() {
        val app1 =
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
                            isGranted = false,
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

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataTwo,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
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
                            appMetadataTwo.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app3 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataThree,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = true,
                        )
                    ),
                recentAccess = listOf(),
            )

        val app4 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadata,
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
                            healthPermission = READ_OXYGEN_SATURATION_PERMISSION,
                            isGranted = true,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadata.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(listOf(app1, app2, app3, app4))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                AllDataTypesScreen(
                    viewModel = wearConnectedAppsViewModel,
                    showRecentAccess = false,
                    onClick = { _, _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fitness and wellness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vitals").assertIsDisplayed()
        assertTitleAndSummary("Oxygen saturation", "No apps requesting")
    }

    @Test
    fun showRecentAccess_showsUsedByXApps() {
        val app1 =
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
                            isGranted = false,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                    ),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataTwo,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
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
                            appMetadataTwo.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app3 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataThree,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = false,
                        )
                    ),
                recentAccess = listOf(),
            )

        val app4 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadata,
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
                            healthPermission = READ_OXYGEN_SATURATION_PERMISSION,
                            isGranted = true,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadata.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(listOf(app1, app2, app3, app4))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                AllDataTypesScreen(
                    viewModel = wearConnectedAppsViewModel,
                    showRecentAccess = true,
                    onClick = { _, _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fitness and wellness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vitals").assertIsDisplayed()
        assertTitleAndSummary("Heart rate", "Used by 2 apps")
        assertTitleAndSummary("Skin temperature", "Used by 1 app")
    }

    @Test
    fun showRecentAccess_showsNotUsedInPast24Hours() {
        val app1 =
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
                            isGranted = false,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                        AccessLog(
                            appMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        ),
                    ),
            )

        val app2 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataTwo,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
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
                            appMetadataTwo.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app3 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataThree,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = false,
                        )
                    ),
                recentAccess = listOf(),
            )

        val app4 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadata,
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
                            healthPermission = READ_OXYGEN_SATURATION_PERMISSION,
                            isGranted = true,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadata.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(listOf(app1, app2, app3, app4))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                AllDataTypesScreen(
                    viewModel = wearConnectedAppsViewModel,
                    showRecentAccess = true,
                    onClick = { _, _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fitness and wellness").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vitals").assertIsDisplayed()
        assertTitleAndSummary("Oxygen saturation", "Not used in past 24 hours")
    }

    private fun assertTitleAndSummary(title: String, summary: String) {
        val titleItem = composeTestRule.onNodeWithText(title)
        titleItem.performScrollTo().assertIsDisplayed()
        val summaryItem = composeTestRule.onNodeWithText(summary)
        summaryItem.performScrollTo().assertIsDisplayed()
        titleItem.assert(hasParent(hasAnyChild(hasText(summary))))
    }

    data class AppConnectionsAndRecentAccess(
        val appMetadata: AppMetadata,
        val permissionStatus: List<HealthPermissionStatus>,
        val recentAccess: List<AccessLog>,
    )

    private fun setupConnectedApps(apps: List<AppConnectionsAndRecentAccess>) {
        apps.forEach {
            val connectedAppMetadata =
                ConnectedAppMetadata(
                    appMetadata = it.appMetadata,
                    status =
                        if (it.permissionStatus.any { permission -> permission.isGranted })
                            ConnectedAppStatus.ALLOWED
                        else ConnectedAppStatus.DENIED,
                    permissionsType = AppPermissionsType.FITNESS_PERMISSIONS_ONLY,
                    healthUsageLastAccess =
                        it.recentAccess.maxOfOrNull { accessLog -> accessLog.accessTime },
                )
            (loadHealthPermissionApps as FakeHealthPermissionAppsUseCase).addToList(
                connectedAppMetadata
            )
            (loadAppPermissionsStatusUseCase as FakeLoadAppPermissionsStatusUseCase)
                .updatePackageName(it.appMetadata.packageName, permissions = it.permissionStatus)
            (loadRecentAccessUseCase as FakeRecentAccessUseCase).addToList(it.recentAccess)
        }
    }
}
