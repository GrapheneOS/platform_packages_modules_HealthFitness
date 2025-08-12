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

import android.health.connect.accesslog.AccessLog
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.app.ILoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.connectedapps.ILoadHealthPermissionApps
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.recentaccess.ILoadRecentAccessUseCase
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionAppsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeRecentAccessUseCase
import kotlin.collections.forEach

fun assertTitleAndSummary(composeTestRule: ComposeTestRule, title: String, summary: String) {
    val titleItem = composeTestRule.onNodeWithText(title)
    titleItem.performScrollTo().assertIsDisplayed()
    val summaryItem = composeTestRule.onNodeWithText(summary)
    summaryItem.performScrollTo().assertIsDisplayed()
    titleItem.assert(hasParent(hasAnyChild(hasText(summary))))
}

fun assertTitleOnly(composeTestRule: ComposeTestRule, title: String) {
    val buttonNode = composeTestRule.onNode(hasText(title, substring = false))
    buttonNode.performScrollTo().assertIsDisplayed()
    // Assert that the text of the button is exactly the title, with no summary
    buttonNode.assert(hasText(title, ignoreCase = false, substring = false))
}

fun setupConnectedApps(
    apps: List<AppConnectionsAndRecentAccess>,
    loadHealthPermissionApps: ILoadHealthPermissionApps,
    loadAppPermissionsStatusUseCase: ILoadAppPermissionsStatusUseCase,
    loadRecentAccessUseCase: ILoadRecentAccessUseCase,
) {
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
        (loadAppPermissionsStatusUseCase as FakeLoadAppPermissionsStatusUseCase).updatePackageName(
            it.appMetadata.packageName,
            permissions = it.permissionStatus,
        )
        (loadRecentAccessUseCase as FakeRecentAccessUseCase).addToList(it.recentAccess)
    }
}

val appMetadataOne =
    AppMetadata(packageName = "packageName1", appName = "AppName1", isSystem = false, icon = null)
val appMetadataTwo =
    AppMetadata(packageName = "packageName2", appName = "AppName2", isSystem = false, icon = null)
val appMetadataThree =
    AppMetadata(packageName = "packageName3", appName = "AppName3", isSystem = false, icon = null)
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
    HealthPermission.FitnessPermission(FitnessPermissionType.HEART_RATE, PermissionsAccessType.READ)
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

data class AppConnectionsAndRecentAccess(
    val appMetadata: AppMetadata,
    val permissionStatus: List<HealthPermissionStatus>,
    val recentAccess: List<AccessLog>,
)
