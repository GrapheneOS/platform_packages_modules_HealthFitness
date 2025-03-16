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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.permissioncontroller.wear.permission.components.ScrollableScreen

/** Wear Settings Permissions Screen to remove access to a data type for all apps. */
@Composable
fun RemoveAllAppsOnePermissionScreen(
    viewModel: WearConnectedAppsViewModel,
    permissionStr: String,
    dataTypeStr: String,
    onBackClick: () -> Unit,
) {

    val healthPermission = fromPermissionString(permissionStr)
    val primaryColor = MaterialTheme.colors.primary
    val transparentPrimary = primaryColor.copy(alpha = 0.1f)

    ScrollableScreen(
        asScalingList = true,
        showTimeText = true,
        title = stringResource(R.string.remove_one_permission_for_all, dataTypeStr),
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Not revoke permissions, get back to per data type screen.
                // TODO: b/373692569 - Use AlertDialog.Confirm and Dismiss Buttons.
                Button(
                    onClick = { onBackClick() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = transparentPrimary),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cross),
                        contentDescription = stringResource(R.string.icon_content_cross_mark),
                    )
                }
                // Button to revoke this permission for all apps.
                Button(
                    onClick = {
                        viewModel.removeFitnessPermissionForAllApps(
                            healthPermission as HealthPermission.FitnessPermission
                        )
                        onBackClick()
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = stringResource(R.string.icon_content_check_mark),
                    )
                }
            }
        }
    }
}
