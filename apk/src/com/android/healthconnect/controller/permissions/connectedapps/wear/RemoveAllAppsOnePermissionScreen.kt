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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.permissioncontroller.wear.permission.components.material3.DialogButtonContent
import com.android.permissioncontroller.wear.permission.components.material3.WearPermissionConfirmationDialog

/** Wear Settings Permissions Screen to remove access to a data type for all apps. */
@Composable
fun RemoveAllAppsOnePermissionScreen(
    viewModel: WearConnectedAppsViewModel,
    permissionStr: String,
    dataTypeStr: String,
    onBackClick: () -> Unit,
) {
    val healthPermission = fromPermissionString(permissionStr)

    WearPermissionConfirmationDialog(
        show = true,
        message = stringResource(R.string.remove_one_permission_for_all, dataTypeStr),
        positiveButtonContent =
            DialogButtonContent(
                onClick = {
                    viewModel.removeFitnessPermissionForAllApps(
                        healthPermission as HealthPermission.FitnessPermission
                    )
                    onBackClick()
                }
            ),
        negativeButtonContent = DialogButtonContent(onClick = onBackClick),
    )
}
