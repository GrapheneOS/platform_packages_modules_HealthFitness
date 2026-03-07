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
package com.android.healthconnect.controller.tests.permissions.additionalaccess.api

import com.android.healthconnect.controller.permissions.additionalaccess.api.ExerciseRouteState
import com.android.healthconnect.controller.permissions.additionalaccess.api.IGetAdditionalPermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.api.ILoadDeclaredHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.api.ILoadExerciseRoutePermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState
import com.android.healthconnect.controller.tests.utils.di.FakeUseCase
import kotlin.collections.mutableMapOf

class FakeLoadExerciseRoutePermissionUseCase :
    FakeUseCase<String, ExerciseRouteState>(), ILoadExerciseRoutePermissionUseCase {

    private var state =
        ExerciseRouteState(
            exercisePermissionState = PermissionUiState.ASK_EVERY_TIME,
            exerciseRoutePermissionState = PermissionUiState.ASK_EVERY_TIME,
        )

    fun setExerciseRouteState(state: ExerciseRouteState) {
        this.state = state
    }

    override suspend fun successValue(input: String): ExerciseRouteState {
        return state
    }

    override fun reset() {
        super.reset()
        state =
            ExerciseRouteState(
                exercisePermissionState = PermissionUiState.ASK_EVERY_TIME,
                exerciseRoutePermissionState = PermissionUiState.ASK_EVERY_TIME,
            )
    }
}

class FakeGetAdditionalPermissionUseCase :
    FakeUseCase<String, List<String>>(), IGetAdditionalPermissionUseCase {

    private var additionalPermissions = mutableMapOf<String, List<String>>()

    fun setAdditionalPermissions(packageName: String, permissions: List<String>) {
        additionalPermissions[packageName] = permissions
    }

    override suspend fun successValue(input: String): List<String> {
        return additionalPermissions[input]!!
    }

    override fun reset() {
        super.reset()
        additionalPermissions.clear()
    }
}

class FakeLoadDeclaredHealthPermissionUseCase :
    FakeUseCase<String, List<String>>(), ILoadDeclaredHealthPermissionUseCase {

    private var declaredPermissions = mutableMapOf<String, List<String>>()

    fun setDeclaredPermissions(packageName: String, permissions: List<String>) {
        declaredPermissions[packageName] = permissions
    }

    override suspend fun successValue(input: String): List<String> {
        return declaredPermissions[input]!!
    }

    override fun reset() {
        super.reset()
        declaredPermissions.clear()
    }
}
