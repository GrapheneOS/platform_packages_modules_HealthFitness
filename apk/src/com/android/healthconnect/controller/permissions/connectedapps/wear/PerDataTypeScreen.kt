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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.permissioncontroller.wear.permission.components.ScrollableScreen
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionButton
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionIconBuilder
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Wear Settings Permissions Screen to see allowed/disallowed status for one app. */
@Composable
fun PerDataTypeScreen(
    viewModel: WearConnectedAppsViewModel,
    permissionStr: String,
    dataTypeStr: String,
    showRecentAccess: Boolean,
    onAppChipClick: (String, String, String) -> Unit,
    onRemoveAllAppAccessButtonClick: (String, String) -> Unit,
) {
    val healthPermission = fromPermissionString(permissionStr)
    ScrollableScreen(asScalingList = true, showTimeText = false, title = dataTypeStr) {
        // Allowed apps.
        item {
            AllowedAppsList(
                viewModel,
                healthPermission,
                dataTypeStr,
                showRecentAccess,
                onAppChipClick,
                onRemoveAllAppAccessButtonClick,
            )
        }

        // Notes on what this permission is about.
        item {
            Row(horizontalArrangement = Arrangement.Start) {
                Text(stringResource(R.string.access_sensor_note, dataTypeStr))
            }
        }

        // Not allowed apps.
        item {
            DeniedAppsList(
                viewModel,
                healthPermission,
                dataTypeStr,
                showRecentAccess,
                onAppChipClick,
            )
        }
    }
}

@Composable
fun AllowedAppsList(
    viewModel: WearConnectedAppsViewModel,
    healthPermission: HealthPermission,
    dataTypeStr: String,
    showRecentAccess: Boolean,
    onAppChipClick: (String, String, String) -> Unit,
    onRemoveAllAppAccessButtonClick: (String, String) -> Unit,
) {
    val dataTypeToAllowedApps by viewModel.dataTypeToAllowedApps.collectAsState()
    val allowedApps = dataTypeToAllowedApps[healthPermission]
    val dataTypeToAppToLastAccessTime by viewModel.dataTypeToAppToLastAccessTime.collectAsState()
    val usedApps =
        dataTypeToAppToLastAccessTime.find { it.permission == healthPermission }?.appAccesses
    if (allowedApps?.isNotEmpty() == true) {
        val nApps = allowedApps.size
        Column {
            // Allowed text.
            Text(stringResource(R.string.allowed))

            // A chip for each allowed app for this data type.
            allowedApps.forEach { app ->
                WearPermissionButton(
                    label = app.appName,
                    labelMaxLines = 3,
                    secondaryLabel =
                        if (showRecentAccess) {
                            val lastAccessTime = usedApps?.find { it.app == app }?.lastAccessTime
                            lastAccessTime?.let {
                                stringResource(R.string.accessed, formatTime(it))
                            }
                        } else {
                            null
                        },
                    onClick = {
                        onAppChipClick(healthPermission.toString(), dataTypeStr, app.packageName)
                    },
                    iconBuilder = app.icon?.let { WearPermissionIconBuilder.builder(it) },
                    modifier = Modifier.padding(4.dp),
                )
            }

            // Remove access for all apps button.
            WearPermissionButton(
                label = stringResource(R.string.disconnect_all_apps),
                labelMaxLines = 3,
                onClick = {
                    onRemoveAllAppAccessButtonClick(healthPermission.toString(), dataTypeStr)
                },
                iconBuilder =
                    WearPermissionIconBuilder.builder(R.drawable.ic_remove_access_for_all_apps),
            )
        }
    }
}

private fun formatTime(instant: Instant): String {
    val localTime: LocalTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
    return localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

@Composable
fun DeniedAppsList(
    viewModel: WearConnectedAppsViewModel,
    healthPermission: HealthPermission,
    dataTypeStr: String,
    showRecentAccess: Boolean,
    onAppChipClick: (String, String, String) -> Unit,
) {
    val dataTypeToDeniedApps by viewModel.dataTypeToDeniedApps.collectAsState()
    val deniedApps = dataTypeToDeniedApps[healthPermission]
    val dataTypeToAppToLastAccessTime by viewModel.dataTypeToAppToLastAccessTime.collectAsState()
    val usedApps =
        dataTypeToAppToLastAccessTime.find { it.permission == healthPermission }?.appAccesses
    if (deniedApps?.isNotEmpty() == true) {
        val nApps = deniedApps.size
        Column {
            // Not allowed text.
            Text(stringResource(R.string.not_allowed))

            // A chip for each denied app for this data type.
            deniedApps.forEach { app ->
                WearPermissionButton(
                    label = app.appName,
                    labelMaxLines = 3,
                    secondaryLabel =
                        if (showRecentAccess) {
                            val lastAccessTime = usedApps?.find { it.app == app }?.lastAccessTime
                            lastAccessTime?.let {
                                stringResource(R.string.accessed, formatTime(it))
                            }
                        } else {
                            null
                        },
                    onClick = {
                        onAppChipClick(healthPermission.toString(), dataTypeStr, app.packageName)
                    },
                    iconBuilder = app.icon?.let { WearPermissionIconBuilder.builder(it) },
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}
