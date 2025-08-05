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
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission.Companion.READ_HEALTH_DATA_IN_BACKGROUND
import com.android.healthconnect.controller.shared.WearPermissionsPaddingValues
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
    val healthAppDataList = viewModel.wearHealthApps.collectAsState()
    val healthAppData = healthAppDataList.value.firstOrNull { it.packageName == packageName }
    if (healthAppData == null) {
        return
    }

    val appMetadata = healthAppData.appMetadata
    val appName = healthAppData.appMetadata.appName

    val anyDataTypesAllowed = healthAppData.anyFitnessPermissionsAllowed()
    val subtitle =
        if (anyDataTypesAllowed) {
            stringResource(
                R.string.current_access,
                appName,
                ListFormatter.getInstance()
                    .format(
                        healthAppData.getAllowedFitnessPermissionsStringResources().map {
                            stringResource(it)
                        }
                    ),
            )
        } else {
            stringResource(R.string.additional_access_background_footer)
        }

    ScrollableScreen(
        asScalingList = true,
        showTimeText = true,
        title = stringResource(R.string.allow_all_the_time_prompt, appName),
        subtitle = subtitle,
    ) {
        // Allow all the time button.
        item {
            WearPermissionButton(
                label = stringResource(R.string.request_permissions_allow_all_the_time),
                labelMaxLines = 3,
                onClick = {
                    viewModel.updatePermission(
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        appMetadata,
                        grant = true,
                    )
                    onBackClick()
                },
                enabled = anyDataTypesAllowed,
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
                        appMetadata,
                        grant = false,
                    )
                    onBackClick()
                },
                enabled = anyDataTypesAllowed,
            )
        }

        // Manage fitness&wellness button, clicking this launches AppInfoPermission page.
        item {
            WearPermissionButton(
                label = stringResource(R.string.manage_fitness_and_wellness_permissions),
                labelMaxLines = 3,
                onClick = { onAppInfoPermissionClick() },
                modifier = Modifier.padding(WearPermissionsPaddingValues.defaultButtonPaddingValues),
            )
        }
    }
}
