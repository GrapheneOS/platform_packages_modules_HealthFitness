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
package com.android.healthconnect.controller.tests.permissions.connectedapps.wear

import android.content.Context
import android.health.connect.Constants
import android.health.connect.HealthPermissions.READ_HEART_RATE
import android.health.connect.HealthPermissions.READ_OXYGEN_SATURATION
import android.health.connect.HealthPermissions.READ_SKIN_TEMPERATURE
import android.health.connect.accesslog.AccessLog
import android.health.connect.datatypes.RecordTypeIdentifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
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
import com.android.healthconnect.controller.permissions.connectedapps.wear.PerDataTypeScreen
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
import com.android.healthconnect.controller.tests.permissions.connectedapps.wear.WearAllDataTypesScreenTest.AppConnectionsAndRecentAccess
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TestComposeActivity
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionAppsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeRecentAccessUseCase
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.TimeZone
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
class WearPerDataTypeScreenTest {
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
    val systemAppMetadataOne =
        AppMetadata(
            packageName = "packageName4",
            appName = "SystemAppName1",
            isSystem = true,
            icon = null,
        )
    val systemAppMetadataTwo =
        AppMetadata(
            packageName = "packageName5",
            appName = "SystemAppName2",
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
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

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
        wearConnectedAppsViewModel.updateShowSystem(false)
    }

    @Test
    fun hidesSystemApps() {
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
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
                            isGranted = false,
                        ),
                    ),
                recentAccess = listOf(),
            )

        val app4 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
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
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app5 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataTwo,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
                            isGranted = false,
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = false,
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_OXYGEN_SATURATION_PERMISSION,
                            isGranted = false,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(listOf(app1, app2, app3, app4, app5))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                PerDataTypeScreen(
                    viewModel = wearConnectedAppsViewModel,
                    permissionStr = "android.permission.health.READ_HEART_RATE",
                    dataTypeStr = "Heart rate",
                    showRecentAccess = false,
                    onAppChipClick = { _, _, _ -> },
                    onRemoveAllAppAccessButtonClick = { _, _ -> },
                    onShowSystemClick = { _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("PerDataTypeScreenTest")

        val listChildren = composeTestRule.onNodeWithText("Heart rate").onParent().onChildren()
        listChildren[0].assert(hasText("Heart rate"))
        listChildren[1].performScrollTo().assert(hasText("Allowed"))
        listChildren[2].performScrollTo().assert(hasText("AppName1"))
        listChildren[3].performScrollTo().assert(hasText("AppName2"))
        listChildren[4]
            .performScrollTo()
            .assert(
                hasText(
                    "Apps with this permission can access heart rate data from your device sensors."
                )
            )
        listChildren[5].performScrollTo().assert(hasText("Not allowed"))
        listChildren[6].performScrollTo().assert(hasText("AppName3"))
        listChildren[7].performScrollTo().assert(hasText("Show system"))
    }

    @Test
    fun showsSystemApps() {
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
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
                            isGranted = false,
                        ),
                    ),
                recentAccess = listOf(),
            )

        val app4 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataOne,
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
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_HEART_RATE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        val app5 =
            AppConnectionsAndRecentAccess(
                appMetadata = systemAppMetadataTwo,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
                            isGranted = false,
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = false,
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_OXYGEN_SATURATION_PERMISSION,
                            isGranted = false,
                        ),
                    ),
                recentAccess =
                    listOf(
                        AccessLog(
                            systemAppMetadataOne.packageName,
                            listOf(RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE),
                            NOW.toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(listOf(app1, app2, app3, app4, app5))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()
        wearConnectedAppsViewModel.updateShowSystem(true)

        with(composeTestRule) {
            setContent {
                PerDataTypeScreen(
                    viewModel = wearConnectedAppsViewModel,
                    permissionStr = "android.permission.health.READ_HEART_RATE",
                    dataTypeStr = "Heart rate",
                    showRecentAccess = false,
                    onAppChipClick = { _, _, _ -> },
                    onRemoveAllAppAccessButtonClick = { _, _ -> },
                    onShowSystemClick = { _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("PerDataTypeScreenTest")
        val systemListChildren =
            composeTestRule.onNodeWithText("Heart rate").onParent().onChildren()
        systemListChildren[0].assert(hasText("Heart rate"))
        systemListChildren[1].performScrollTo().assert(hasText("Allowed"))
        systemListChildren[2].performScrollTo().assert(hasText("AppName1"))
        systemListChildren[3].performScrollTo().assert(hasText("AppName2"))
        systemListChildren[4].performScrollTo().assert(hasText("SystemAppName1"))
        systemListChildren[5]
            .performScrollTo()
            .assert(
                hasText(
                    "Apps with this permission can access heart rate data from your device sensors."
                )
            )
        systemListChildren[6].performScrollTo().assert(hasText("Not allowed"))
        systemListChildren[7].performScrollTo().assert(hasText("AppName3"))
        systemListChildren[8].performScrollTo().assert(hasText("SystemAppName2"))
        systemListChildren[9].performScrollTo().assert(hasText("Hide system"))
    }

    @Test
    fun showsRecentAccess_whenAppAccessedInPast24Hours() {
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
                            NOW.plusSeconds(60 * 2).toEpochMilli(),
                            Constants.READ,
                        )
                    ),
            )

        setupConnectedApps(listOf(app1, app2))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                PerDataTypeScreen(
                    viewModel = wearConnectedAppsViewModel,
                    permissionStr = "android.permission.health.READ_HEART_RATE",
                    dataTypeStr = "Heart rate",
                    showRecentAccess = true,
                    onAppChipClick = { _, _, _ -> },
                    onRemoveAllAppAccessButtonClick = { _, _ -> },
                    onShowSystemClick = { _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("PerDataTypeScreenTest")
        val systemListChildren =
            composeTestRule.onNodeWithText("Heart rate").onParent().onChildren()
        systemListChildren[0].assert(hasText("Heart rate"))
        systemListChildren[1].performScrollTo().assert(hasText("Allowed"))
        systemListChildren[2].performScrollTo().assert(hasText("AppName1"))
        assertTitleAndSummary(composeTestRule, "AppName1", "Accessed 07:06")
        systemListChildren[3].performScrollTo().assert(hasText("AppName2"))
        assertTitleAndSummary(composeTestRule, "AppName2", "Accessed 07:08")
    }

    @Test
    fun hidesRecentAccess_whenAppNotAccessedInPast24Hours() {
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
                recentAccess = listOf(),
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
                recentAccess = listOf(),
            )

        setupConnectedApps(listOf(app1, app2))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                PerDataTypeScreen(
                    viewModel = wearConnectedAppsViewModel,
                    permissionStr = "android.permission.health.READ_HEART_RATE",
                    dataTypeStr = "Heart rate",
                    showRecentAccess = true,
                    onAppChipClick = { _, _, _ -> },
                    onRemoveAllAppAccessButtonClick = { _, _ -> },
                    onShowSystemClick = { _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("PerDataTypeScreenTest")
        val systemListChildren =
            composeTestRule.onNodeWithText("Heart rate").onParent().onChildren()
        systemListChildren[0].assert(hasText("Heart rate"))
        systemListChildren[1].performScrollTo().assert(hasText("Allowed"))
        systemListChildren[2].performScrollTo().assert(hasText("AppName1"))
        assertTitleOnly(composeTestRule, "AppName1")
        assertTitleOnly(composeTestRule, "AppName2")
    }

    @Test
    fun whenNoAllowedApps_hidesAllowedSection() {
        val app1 =
            AppConnectionsAndRecentAccess(
                appMetadata = appMetadataOne,
                permissionStatus =
                    listOf(
                        HealthPermissionStatus(
                            healthPermission = READ_HEART_RATE_PERMISSION,
                            isGranted = false,
                        ),
                        HealthPermissionStatus(
                            healthPermission = READ_SKIN_TEMPERATURE_PERMISSION,
                            isGranted = false,
                        ),
                    ),
                recentAccess = listOf(),
            )

        setupConnectedApps(listOf(app1))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                PerDataTypeScreen(
                    viewModel = wearConnectedAppsViewModel,
                    permissionStr = "android.permission.health.READ_HEART_RATE",
                    dataTypeStr = "Heart rate",
                    showRecentAccess = false,
                    onAppChipClick = { _, _, _ -> },
                    onRemoveAllAppAccessButtonClick = { _, _ -> },
                    onShowSystemClick = { _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("PerDataTypeScreenTest")

        val listChildren = composeTestRule.onNodeWithText("Heart rate").onParent().onChildren()
        listChildren[0].assert(hasText("Heart rate"))
        composeTestRule.onNodeWithText("Allowed").assertDoesNotExist()
        listChildren[1]
            .performScrollTo()
            .assert(
                hasText(
                    "Apps with this permission can access heart rate data from your device sensors."
                )
            )
        listChildren[2].performScrollTo().assert(hasText("Not allowed"))
        listChildren[3].performScrollTo().assert(hasText("AppName1"))
        listChildren[4].performScrollTo().assert(hasText("Show system"))
    }

    @Test
    fun whenNoDeniedApps_hidesNotAllowedSection() {
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
                            isGranted = true,
                        ),
                    ),
                recentAccess = listOf(),
            )

        setupConnectedApps(listOf(app1))

        wearConnectedAppsViewModel.loadConnectedApps()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            setContent {
                PerDataTypeScreen(
                    viewModel = wearConnectedAppsViewModel,
                    permissionStr = "android.permission.health.READ_HEART_RATE",
                    dataTypeStr = "Heart rate",
                    showRecentAccess = false,
                    onAppChipClick = { _, _, _ -> },
                    onRemoveAllAppAccessButtonClick = { _, _ -> },
                    onShowSystemClick = { _ -> },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onRoot().printToLog("PerDataTypeScreenTest")

        val listChildren = composeTestRule.onNodeWithText("Heart rate").onParent().onChildren()
        listChildren[0].assert(hasText("Heart rate"))
        listChildren[1].performScrollTo().assert(hasText("Allowed"))
        listChildren[2].performScrollTo().assert(hasText("AppName1"))
        listChildren[3]
            .performScrollTo()
            .assert(
                hasText(
                    "Apps with this permission can access heart rate data from your device sensors."
                )
            )
        composeTestRule.onNodeWithText("Not allowed").assertDoesNotExist()
        listChildren[4].performScrollTo().assert(hasText("Show system"))
    }

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
