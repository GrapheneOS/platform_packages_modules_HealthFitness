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

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.healthconnect.controller.permissions.app.wear.WearViewAppInfoPermissionsActivity
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission

/** Wear Settings Permissions navigation graph. */
@Composable
fun WearSettingsPermissionsNavGraph(showRecentAccess: Boolean = false) {
    val viewModel = hiltViewModel<WearConnectedAppsViewModel>()
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = PermissionManagerScreen.Vitals.name) {
        composable(route = PermissionManagerScreen.Vitals.name) {
            AllDataTypesScreen(
                viewModel,
                showRecentAccess,
                onClick = { permissionStr ->
                    navController.navigate(
                        "${PermissionManagerScreen.PerDataType.name}/$permissionStr/$showRecentAccess"
                    )
                },
            )
        }

        composable(
            route =
                "${PermissionManagerScreen.PerDataType.name}/{permissionStr}/{showRecentAccess}",
            arguments =
                listOf(
                    navArgument("permissionStr") { type = NavType.StringType },
                    navArgument("showRecentAccess") { type = NavType.BoolType },
                ),
        ) { backStackEntry ->
            val permissionStr = backStackEntry.arguments?.getString("permissionStr") ?: ""
            viewModel.updateShowSystem(false)
            PerDataTypeScreen(
                viewModel,
                fitnessPermission = FitnessPermission.fromPermissionString(permissionStr),
                showRecentAccess,
                onAppChipClick = { permissionStr, packageName ->
                    navController.navigate(
                        "${PermissionManagerScreen.PerDataTypePerApp.name}/$permissionStr/$packageName"
                    )
                },
                onRemoveAllAppAccessButtonClick = { permissionStr ->
                    navController.navigate(
                        "${PermissionManagerScreen.RemoveAll.name}/$permissionStr"
                    )
                },
                onShowSystemClick = { show -> run { viewModel.updateShowSystem(show) } },
            )
        }

        composable(
            route = "${PermissionManagerScreen.RemoveAll.name}/{permissionStr}",
            arguments = listOf(navArgument("permissionStr") { type = NavType.StringType }),
        ) { backStackEntry ->
            val permissionStr = backStackEntry.arguments?.getString("permissionStr") ?: ""
            RemoveAllAppsOnePermissionScreen(
                viewModel,
                fitnessPermission = FitnessPermission.fromPermissionString(permissionStr),
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route =
                "${PermissionManagerScreen.PerDataTypePerApp.name}/{permissionStr}/{packageName}",
            arguments =
                listOf(
                    navArgument("permissionStr") { type = NavType.StringType },
                    navArgument("packageName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            val permissionStr = backStackEntry.arguments?.getString("permissionStr") ?: ""
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            ControlSingleDataTypeForSingleAppScreen(
                viewModel,
                fitnessPermission = FitnessPermission.fromPermissionString(permissionStr),
                packageName,
                onAdditionalPermissionClick = {
                    navController.navigate(
                        "${PermissionManagerScreen.BackgroundPermission.name}/$packageName"
                    )
                },
            )
        }

        composable(
            route = "${PermissionManagerScreen.BackgroundPermission.name}/{packageName}",
            arguments = listOf(navArgument("packageName") { type = NavType.StringType }),
        ) { backStackEntry ->
            // Handle the activity result and re-initialize the ViewModel when swipe back.
            val launcher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    viewModel.loadConnectedApps()
                }
            val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
            val context = LocalContext.current
            ControlBackgroundReadForSingleAppScreen(
                viewModel,
                packageName,
                onBackClick = { navController.popBackStack() },
                onAppInfoPermissionClick = {
                    // Launch AppInfo->Permissions->Fitness&Wellness screen.
                    val intent =
                        Intent(context, WearViewAppInfoPermissionsActivity::class.java).apply {
                            putExtra(android.content.Intent.EXTRA_PACKAGE_NAME, packageName)
                        }
                    launcher.launch(intent)
                },
            )
        }
    }
}

enum class PermissionManagerScreen() {
    Vitals,
    PerDataType,
    RemoveAll,
    PerDataTypePerApp,
    BackgroundPermission,
}
