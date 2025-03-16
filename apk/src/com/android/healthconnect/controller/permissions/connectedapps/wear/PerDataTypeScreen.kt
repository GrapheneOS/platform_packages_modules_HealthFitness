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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.permissioncontroller.wear.permission.components.ScrollableScreen
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionButton
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionButtonStyle
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
    onShowSystemClick: (Boolean) -> Unit,
) {
    // TODO: b/401597500 - The HealthPermission should be passed into these composables.
    val healthPermission = fromPermissionString(permissionStr)
    val lowercaseDataTypeStr =
        stringResource(
            FitnessPermissionStrings.fromPermissionType(
                    (healthPermission as HealthPermission.FitnessPermission).fitnessPermissionType
                )
                .lowercaseLabel
        )
    val showSystem by viewModel.showSystemFlow.collectAsState()

    ScrollableScreen(asScalingList = true, showTimeText = true, title = dataTypeStr) {
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
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.access_sensor_note, lowercaseDataTypeStr),
                    style = TextStyle(fontSize = 12.sp),
                )
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

        // Show system apps button.
        item {
            WearPermissionButton(
                label =
                    if (showSystem) {
                        stringResource(R.string.menu_hide_system)
                    } else {
                        stringResource(R.string.menu_show_system)
                    },
                labelMaxLines = Int.MAX_VALUE,
                onClick = { onShowSystemClick(!showSystem) },
                modifier = Modifier.padding(start = 2.dp, end = 2.dp),
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
    val showSystem by viewModel.showSystemFlow.collectAsState()
    val dataTypeToAppToLastAccessTime by viewModel.dataTypeToAppToLastAccessTime.collectAsState()
    var allowedApps = dataTypeToAllowedApps[healthPermission]
    var usedApps =
        dataTypeToAppToLastAccessTime.find { it.permission == healthPermission }?.appAccesses
    if (!showSystem) {
        allowedApps = allowedApps?.filter { !it.isSystem }?.toMutableList()
        usedApps = usedApps?.filter { !it.app.isSystem }?.toMutableList()
    }

    if (allowedApps?.isNotEmpty() == true) {
        val nApps = allowedApps.size
        Column {
            // Allowed text.
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(stringResource(R.string.allowed))
            }

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
                    modifier = Modifier.padding(2.dp),
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
                    WearPermissionIconBuilder.builder(R.drawable.ic_remove_access_for_all_apps)
                        .tint(Color(0xFFEC928E)),
                modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 14.dp, bottom = 8.dp),
                style = WearPermissionButtonStyle.Warning,
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
    val showSystem by viewModel.showSystemFlow.collectAsState()
    val dataTypeToAppToLastAccessTime by viewModel.dataTypeToAppToLastAccessTime.collectAsState()
    var deniedApps = dataTypeToDeniedApps[healthPermission]
    var usedApps =
        dataTypeToAppToLastAccessTime.find { it.permission == healthPermission }?.appAccesses
    if (!showSystem) {
        deniedApps = deniedApps?.filter { !it.isSystem }?.toMutableList()
        usedApps = usedApps?.filter { !it.app.isSystem }?.toMutableList()
    }

    if (deniedApps?.isNotEmpty() == true) {
        val nApps = deniedApps.size
        Column {
            // Not allowed text.
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(stringResource(R.string.not_allowed))
            }

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
                    modifier = Modifier.padding(2.dp),
                )
            }
        }
    }
}
