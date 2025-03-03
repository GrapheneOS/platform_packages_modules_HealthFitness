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
package com.android.healthconnect.controller.permissions.app.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.permissioncontroller.wear.permission.components.ScrollableScreen
import com.android.permissioncontroller.wear.permission.components.theme.ResourceHelper

/**
 * Wear View App Permissions Screen. This screen includes: Allow/Deny foreground and background
 * health permissions for an app.
 */
@Composable
fun WearViewAppPermissionsScreen(viewModel: AppPermissionViewModel) {
    val res = LocalContext.current.resources
    val appMetadata: State<AppMetadata?> = viewModel.appInfo.observeAsState(null)
    val appName = appMetadata.value?.appName ?: ""
    val packageName = appMetadata.value?.packageName ?: ""
    val allFitnessPermissionsGranted by viewModel.allFitnessPermissionsGranted.observeAsState(false)
    val grantedFitnessPermissions by viewModel.grantedFitnessPermissions.observeAsState(emptyList())
    val allFitnessPermissions = viewModel.fitnessPermissions.observeAsState(emptyList())
    val allDataTypes =
        allFitnessPermissions.value.map { permission ->
            stringResource(
                FitnessPermissionStrings.fromPermissionType(permission.fitnessPermissionType)
                    .uppercaseLabel
            )
        }
    val checkedStates =
        remember(allFitnessPermissions.value) { // Recalculate when permissions change
            mutableStateListOf<Boolean>(
                *(allFitnessPermissions.value)
                    .map { perm ->
                        grantedFitnessPermissions.any { grantedPerm ->
                            perm.fitnessPermissionType == grantedPerm.fitnessPermissionType
                        }
                    }
                    .toTypedArray()
            )
        }

    // Background read permission request state.
    val allRequestedAdditionalPermissions by
        viewModel.additionalPermissions.observeAsState(emptyList())
    val backgroundReadPermissionRequested by
        remember(allRequestedAdditionalPermissions) {
            derivedStateOf { allRequestedAdditionalPermissions.any { it.isBackgroundReadPermission() } }
        }
    // Background read permission grant state.
    val grantedAdditionalPermissions by
        viewModel.grantedAdditionalPermissions.observeAsState(emptySet())
    val allowAllTheTimeGranted by
        remember(grantedAdditionalPermissions) {
            derivedStateOf { grantedAdditionalPermissions.any { it.isBackgroundReadPermission() } }
        }

    ScrollableScreen(
        materialUIVersion = ResourceHelper.materialUIVersionInApp,
        asScalingList = true,
        showTimeText = false,
        title = res.getString(R.string.fitness_and_wellness),
    ) {
        // Allow all toggle.
        item {
            var isAllowAllChecked by remember { mutableStateOf(allFitnessPermissionsGranted) }
            val allDataTypesSelected by
                remember(checkedStates) { derivedStateOf { checkedStates.all { it } } }
            // Update isAllowAllChecked when allFitnessPermissionsGranted changes.
            LaunchedEffect(allFitnessPermissionsGranted) {
                isAllowAllChecked = allFitnessPermissionsGranted
            }
            SwitchButton(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(res.getString(R.string.request_permissions_allow_all)) },
                checked = isAllowAllChecked,
                onCheckedChange = { isChecked ->
                    isAllowAllChecked = isChecked
                    for (i in checkedStates.indices) {
                        checkedStates[i] = isChecked
                    }
                    if (isChecked) {
                        viewModel.grantAllFitnessPermissions(packageName)
                    } else {
                        viewModel.revokeAllFitnessAndMaybeAdditionalPermissions(packageName)
                    }
                },
                enabled = true,
            )
        }

        // Granular data type toggles.
        item {
            Row(horizontalArrangement = Arrangement.Start) {
                Text(res.getString(R.string.allowed_to_read))
            }
        }
        items(allDataTypes.size) { index ->
            val dataType = allDataTypes[index]
            val isChecked = checkedStates[index]
            SwitchButton(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(dataType) },
                checked = isChecked,
                onCheckedChange = { newCheckedValue ->
                    checkedStates[index] = newCheckedValue
                    viewModel.updatePermission(
                        packageName,
                        allFitnessPermissions.value[index],
                        newCheckedValue as Boolean,
                    )
                },
                enabled = true,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.Start) {
                Text(res.getString(R.string.give_permission_prompt, appName))
            }
        }

        // Only render background permission selection if the app has requested it.
        if (backgroundReadPermissionRequested) {
            // Background permission.
            // Allow all the time.
            item {
                Row(horizontalArrangement = Arrangement.Start) {
                    Text(res.getString(R.string.allowed_to_access))
                }
            }
            item {
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    selected = allowAllTheTimeGranted,
                    onSelect = {
                        viewModel.updateAdditionalPermission(
                            packageName,
                            AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND,
                            true,
                        )
                    },
                    label = { Text(res.getString(R.string.view_permissions_all_the_time_cap)) },
                )
            }
            // Allow while in use. (Deny background read permission.)
            item {
                RadioButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = true,
                    selected = !allowAllTheTimeGranted,
                    onSelect = {
                        viewModel.updateAdditionalPermission(
                            packageName,
                            AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND,
                            false,
                        )
                    },
                    label = { Text(res.getString(R.string.view_permissions_while_in_use_cap)) },
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.Start) {
                    val resourceId =
                        if (allowAllTheTimeGranted) {
                            R.string.view_permissions_description_all_the_time
                        } else {
                            R.string.view_permissions_description_while_in_use
                        }
                    Text(res.getString(resourceId, appName))
                }
            }
        }
    }
}
