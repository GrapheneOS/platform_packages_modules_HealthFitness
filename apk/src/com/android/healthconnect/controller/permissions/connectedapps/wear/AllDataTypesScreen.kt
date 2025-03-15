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

import android.icu.text.MessageFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.permissioncontroller.wear.permission.components.ScrollableScreen
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionButton

/** Wear Settings Permissions Screen to see allowed/disallowed status for all apps. */
@Composable
fun AllDataTypesScreen(
    viewModel: WearConnectedAppsViewModel,
    showRecentAccess: Boolean,
    onClick: (String, String) -> Unit,
) {
    val res = LocalContext.current.resources
    val connectedApps by viewModel.connectedApps.collectAsState()
    val dataTypeToAllowedApps by viewModel.dataTypeToAllowedApps.collectAsState()
    val dataTypeToDeniedApps by viewModel.dataTypeToDeniedApps.collectAsState()
    val dataTypeToAppToLastAccessTime by viewModel.dataTypeToAppToLastAccessTime.collectAsState()
    val systemHealthPermissionsUnsorted by viewModel.systemHealthPermissions.collectAsState()
    val nTotalApps = connectedApps.size

    val systemHealthPermissionToAllowedNonSystemApps =
        systemHealthPermissionsUnsorted.associateWith {
            dataTypeToAllowedApps[it]?.filter { !it.isSystem }
        }
    val systemHealthPermissionToDeniedNonSystemApps =
        systemHealthPermissionsUnsorted.associateWith {
            dataTypeToDeniedApps[it]?.filter { !it.isSystem }
        }

    // Sort system health order alphabetically, and defer no-usage data types to the last.
    val systemHealthPermissions =
        systemHealthPermissionsUnsorted.sortedWith(
            compareBy<HealthPermission> { healthPermission ->
                    val nAllowedApps =
                        systemHealthPermissionToAllowedNonSystemApps[healthPermission]?.size ?: 0
                    val nDeniedApps =
                        systemHealthPermissionToDeniedNonSystemApps[healthPermission]?.size ?: 0
                    // If a health permission is not requested by any apps, put to the end of list.
                    if ((nAllowedApps + nDeniedApps) > 0) {
                        0
                    } else {
                        1
                    }
                }
                .thenBy { permission ->
                    // For all health permissions that are requested by at least one app, sort by
                    // user-visible strings alphabetically.
                    res.getString(
                        FitnessPermissionStrings.fromPermissionType(
                                (permission as HealthPermission.FitnessPermission)
                                    .fitnessPermissionType
                            )
                            .uppercaseLabel
                    )
                }
        )

    ScrollableScreen(
        asScalingList = true,
        showTimeText = true,
        title = stringResource(R.string.fitness_and_wellness),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(stringResource(R.string.vitals_category_uppercase))
            }
        }

        // Granular data type and the number of apps allowed.
        items(systemHealthPermissions.size) { index ->
            val healthPermission = systemHealthPermissions[index]
            val strDataType =
                stringResource(
                    FitnessPermissionStrings.fromPermissionType(
                            (healthPermission as HealthPermission.FitnessPermission)
                                .fitnessPermissionType
                        )
                        .uppercaseLabel
                )
            val nAllowedApps =
                systemHealthPermissionToAllowedNonSystemApps[healthPermission]?.size ?: 0
            val nDeniedApps =
                systemHealthPermissionToDeniedNonSystemApps[healthPermission]?.size ?: 0
            val nUsedApps =
                dataTypeToAppToLastAccessTime
                    .find { it.permission == healthPermission }
                    ?.appAccesses
                    ?.filter { !it.app.isSystem }
                    ?.size ?: 0
            val nRequestedApps = nAllowedApps + nDeniedApps
            val enabled =
                if (showRecentAccess) {
                    nUsedApps != 0
                } else {
                    nRequestedApps != 0
                }
            val message =
                when {
                    enabled && showRecentAccess ->
                        MessageFormat.format(
                            res.getString(R.string.used_by_apps_count),
                            mapOf("count" to nUsedApps),
                        )
                    enabled && !showRecentAccess ->
                        stringResource(R.string.allowed_apps_count, nAllowedApps, nRequestedApps)
                    !enabled && showRecentAccess ->
                        stringResource(R.string.not_used_in_past_24_hours)
                    else -> stringResource(R.string.no_apps_requesting)
                }
            WearPermissionButton(
                label = strDataType,
                labelMaxLines = 3,
                secondaryLabel = message,
                secondaryLabelMaxLines = 3,
                onClick = { onClick(healthPermission.toString(), strDataType) },
                enabled = enabled,
            )
        }
    }
}
