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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.permissioncontroller.wear.permission.components.ScrollableScreen
import com.android.permissioncontroller.wear.permission.components.material2.ToggleChip
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionButton
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionToggleControlType

/** Wear Settings Permissions Screen to allow/disallow a single data type permission for an app. */
@Composable
fun ControlSingleDataTypeForSingleAppScreen(
    viewModel: WearConnectedAppsViewModel,
    permissionStr: String,
    dataTypeStr: String,
    packageName: String,
    onAdditionalPermissionClick: (String) -> Unit,
) {
    val healthPermission = fromPermissionString(permissionStr)

    // Get app metadata. PackageName is passed from allowed/denied apps page and must be in the
    // connectedApps list, thus it's safe to have nonnull!! assert.
    val appMetadata by viewModel.getAppMetadataByPackageName(packageName).collectAsState()

    val dataTypeToAllowedApps by viewModel.dataTypeToAllowedApps.collectAsState()
    // Whether this data type permission is allowed (foreground).
    var allowed by remember { mutableStateOf(true) }
    allowed = dataTypeToAllowedApps[healthPermission]?.any { it.packageName == packageName } == true

    // Background permission status.
    val backgroundReadStatus by viewModel.appToBackgroundReadStatus.collectAsState()
    val isBackgroundPermissionRequested = appMetadata!! in backgroundReadStatus

    ScrollableScreen(asScalingList = true, showTimeText = true, title = appMetadata!!.appName) {
        // Data type text.
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(dataTypeStr)
            }
        }

        // "Allow" radio button.
        item {
            ToggleChip(
                checked = allowed,
                onCheckedChanged = { checked ->
                    if (checked) {
                        viewModel.updatePermission(healthPermission, appMetadata!!, grant = true)
                        allowed = true
                    }
                },
                label = stringResource(R.string.request_permissions_allow),
                toggleControl = WearPermissionToggleControlType.Radio,
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
            )
        }

        // "Don't allow" radio button.
        item {
            ToggleChip(
                checked = !allowed,
                onCheckedChanged = { checked ->
                    if (checked) {
                        viewModel.updatePermission(healthPermission, appMetadata!!, grant = false)
                        allowed = false
                    }
                },
                label = stringResource(R.string.request_permissions_dont_allow),
                toggleControl = WearPermissionToggleControlType.Radio,
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
            )
        }

        // Button to allow/disallow background permission.
        if (isBackgroundPermissionRequested) {
            item {
                WearPermissionButton(
                    label = stringResource(R.string.additional_access_label),
                    labelMaxLines = 3,
                    onClick = { onAdditionalPermissionClick(packageName) },
                )
            }
        }

        // Allow mode text.
        item {
            val resourceId =
                if (backgroundReadStatus[appMetadata!!] == true) {
                    R.string.current_allow_mode_all_the_time
                } else {
                    R.string.current_allow_mode_while_in_use
                }
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
            ) {
                Text(
                    text = stringResource(resourceId, appMetadata!!.appName),
                    style = TextStyle(fontSize = 10.sp),
                )
            }
        }
    }
}
