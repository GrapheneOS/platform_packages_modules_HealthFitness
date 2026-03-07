/*
 * Copyright (C) 2026 The Android Open Source Project
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
 */

package com.android.healthconnect.controller.permissions.additionalaccess.api

import android.content.pm.PackageManager
import android.health.connect.HealthPermissions.READ_EXERCISE
import android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.permissions.api.IGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.UseCaseContract
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Use case that determines the UI state for the [READ_EXERCISE_ROUTES] permission.
 *
 * This class evaluates the current status of [READ_EXERCISE] and [READ_EXERCISE_ROUTES] based on
 * the app's declared permissions, grant status, and system flags. Each permission can result in one
 * of four states: [PermissionUiState.NOT_DECLARED], [PermissionUiState.ALWAYS_ALLOW],
 * [PermissionUiState.ASK_EVERY_TIME], or [PermissionUiState.NEVER_ALLOW].
 */
@Singleton
class LoadExerciseRoutePermissionUseCase
@Inject
constructor(
    private val loadDeclaredHealthPermissionUseCase: ILoadDeclaredHealthPermissionUseCase,
    private val getHealthPermissionsFlagsUseCase: GetHealthPermissionsFlagsUseCase,
    private val getGrantedHealthPermissionsUseCase: IGetGrantedHealthPermissionsUseCase,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : BaseUseCase<String, ExerciseRouteState>(dispatcher), ILoadExerciseRoutePermissionUseCase {

    override suspend fun execute(input: String): ExerciseRouteState {
        val grantedPermissions = getGrantedHealthPermissionsUseCase(input)
        val appPermissions =
            when (val result = loadDeclaredHealthPermissionUseCase(input)) {
                is UseCaseResults.Success -> result.data
                is UseCaseResults.Failed -> throw result.exception
            }
        val permissionFlags =
            getHealthPermissionsFlagsUseCase(
                input,
                listOf(READ_EXERCISE_ROUTES, READ_EXERCISE).filter { appPermissions.contains(it) },
            )
        return ExerciseRouteState(
            exerciseRoutePermissionState =
                getPermissionState(
                    READ_EXERCISE_ROUTES,
                    grantedPermissions,
                    appPermissions,
                    permissionFlags,
                ),
            exercisePermissionState =
                getPermissionState(
                    READ_EXERCISE,
                    grantedPermissions,
                    appPermissions,
                    permissionFlags,
                ),
        )
    }

    private fun getPermissionState(
        permission: String,
        grantedPermissions: List<String>,
        appPermissions: List<String>,
        permissionFlags: Map<String, Int>,
    ): PermissionUiState {

        if (grantedPermissions.contains(permission)) {
            return PermissionUiState.ALWAYS_ALLOW
        }

        if (!appPermissions.contains(permission)) {
            return PermissionUiState.NOT_DECLARED
        }

        val flag = permissionFlags[permission] ?: return PermissionUiState.NOT_DECLARED
        if (flag.and(PackageManager.FLAG_PERMISSION_USER_FIXED) != 0) {
            return PermissionUiState.NEVER_ALLOW
        }

        return PermissionUiState.ASK_EVERY_TIME
    }
}

data class ExerciseRouteState(
    val exerciseRoutePermissionState: PermissionUiState,
    val exercisePermissionState: PermissionUiState,
)

enum class PermissionUiState {
    NOT_DECLARED,
    ASK_EVERY_TIME,
    ALWAYS_ALLOW,
    NEVER_ALLOW,
}

interface ILoadExerciseRoutePermissionUseCase : UseCaseContract<String, ExerciseRouteState>
