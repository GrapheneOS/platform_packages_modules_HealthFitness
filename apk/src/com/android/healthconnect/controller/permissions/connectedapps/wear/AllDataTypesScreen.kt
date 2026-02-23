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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.Text
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.WearPermissionsPaddingValues
import com.android.permissioncontroller.wear.permission.components.ScrollableScreen
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionButton

/** Wear Settings Permissions Screen to see allowed/disallowed status for all apps. */
@Composable
fun AllDataTypesScreen(
    viewModel: WearConnectedAppsViewModel,
    showRecentAccess: Boolean,
    onClick: (String) -> Unit,
) {
    val res = LocalContext.current.resources
    val wearHealthApps by viewModel.wearHealthApps.collectAsState()
    val systemHealthPermissionsByCategory by
        viewModel.systemHealthPermissionsByCategory.collectAsState()

    ScrollableScreen(
        asScalingList = true,
        showTimeText = true,
        title = stringResource(R.string.fitness_and_wellness),
    ) {
        systemHealthPermissionsByCategory.entries.forEachIndexed { index, entry ->
            val category = entry.key
            val healthPermissions = entry.value
            val padding =
                if (index >= 1) {
                    WearPermissionsPaddingValues.dataTypeCategoryHeaderPaddingValues
                } else {
                    WearPermissionsPaddingValues.firstDataTypeCategoryHeaderPaddingValues
                }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(padding),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Text(text = stringResource(id = category.uppercaseTitle()))
                }
            }

            healthPermissions.forEach { healthPermission ->
                item {
                    val strPermission =
                        stringResource(
                            FitnessPermissionStrings.fromPermissionType(
                                    (healthPermission as HealthPermission.FitnessPermission)
                                        .fitnessPermissionType
                                )
                                .uppercaseLabel
                        )
                    val allowedAppCount =
                        wearHealthApps.getNumberOfAllowedAppsForFitnessPermission(healthPermission)
                    val deniedAppCount =
                        wearHealthApps.getNumberOfDeniedAppsForFitnessPermission(healthPermission)
                    val usedAppCount =
                        wearHealthApps.getNumberOfUsedAppsForFitnessPermission(healthPermission)
                    val requestedAppCount = allowedAppCount + deniedAppCount
                    val enabled =
                        if (showRecentAccess) {
                            usedAppCount != 0
                        } else {
                            requestedAppCount != 0
                        }
                    val message =
                        when {
                            enabled && showRecentAccess ->
                                MessageFormat.format(
                                    res.getString(R.string.used_by_apps_count),
                                    mapOf("count" to usedAppCount),
                                )

                            enabled && !showRecentAccess ->
                                stringResource(
                                    R.string.allowed_apps_count,
                                    allowedAppCount,
                                    requestedAppCount,
                                )

                            !enabled && showRecentAccess ->
                                stringResource(R.string.not_used_in_past_24_hours)

                            else -> stringResource(R.string.no_apps_requesting)
                        }
                    WearPermissionButton(
                        label = strPermission,
                        labelMaxLines = 3,
                        secondaryLabel = message,
                        secondaryLabelMaxLines = 3,
                        onClick = { onClick(healthPermission.toString()) },
                        enabled = enabled,
                    )
                }
            }
        }
    }
}
