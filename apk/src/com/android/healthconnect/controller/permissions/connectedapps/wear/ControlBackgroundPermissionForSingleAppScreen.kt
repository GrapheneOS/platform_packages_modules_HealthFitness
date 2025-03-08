/*
 * Copyright (C) 2024 The Android Open Source Project
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
 *
 *
 */
package com.android.healthconnect.controller.permissions.connectedapps.wear

import android.icu.text.ListFormatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission.Companion.READ_HEALTH_DATA_IN_BACKGROUND
import com.android.permissioncontroller.wear.permission.components.ScrollableScreen
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionButton

/** Wear Settings Permissions Screen to allow/disallow background permission for an app. */
@Composable
fun ControlBackgroundReadForSingleAppScreen(
    viewModel: WearConnectedAppsViewModel,
    packageName: String,
    onBackClick: () -> Unit,
    onAppInfoPermissionClick: () -> Unit,
) {
    // Get app metadata. PackageName is passed from allowed/denied apps page and must be in the
    // connectedApps list, thus it's safe to have nonnull!! assert.
    val appMetadata by viewModel.getAppMetadataByPackageName(packageName).collectAsState()
    val appName = appMetadata!!.appName

    val allowedDataTypePermissions by viewModel.appToAllowedDataTypes.collectAsState()
    val allowedDataTypesStrings =
        allowedDataTypePermissions[appMetadata!!]?.map { permission ->
            stringResource(
                FitnessPermissionStrings.fromPermissionType(
                        (permission as HealthPermission.FitnessPermission).fitnessPermissionType
                    )
                    .lowercaseLabel
            )
        }

    ScrollableScreen(
        asScalingList = true,
        showTimeText = false,
        title = stringResource(R.string.allow_all_the_time_prompt, appName),
        subtitle =
            stringResource(
                R.string.current_access,
                appName,
                ListFormatter.getInstance().format(allowedDataTypesStrings),
            ),
    ) {
        // Allow all the time button.
        item {
            WearPermissionButton(
                label = stringResource(R.string.request_permissions_allow_all_the_time),
                labelMaxLines = 3,
                onClick = {
                    viewModel.updatePermission(
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        appMetadata!!,
                        grant = true,
                    )
                    onBackClick()
                },
            )
        }

        // Only while in use button.
        item {
            WearPermissionButton(
                label = stringResource(R.string.request_permissions_while_using_the_app),
                labelMaxLines = 3,
                onClick = {
                    viewModel.updatePermission(
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        appMetadata!!,
                        grant = false,
                    )
                    onBackClick()
                },
            )
        }

        // Manage fitness&wellness button, clicking this launches AppInfoPermission page.
        item {
            WearPermissionButton(
                label = stringResource(R.string.manage_fitness_and_wellness_permissions),
                labelMaxLines = 3,
                onClick = { onAppInfoPermissionClick() },
            )
        }
    }
}
