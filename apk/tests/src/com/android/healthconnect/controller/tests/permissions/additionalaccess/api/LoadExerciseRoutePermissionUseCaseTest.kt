/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.android.healthconnect.controller.tests.permissions.additionalaccess.api

import android.content.pm.PackageManager
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_SET
import android.health.connect.HealthPermissions.READ_BODY_FAT
import android.health.connect.HealthPermissions.READ_EXERCISE
import android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES
import android.health.connect.HealthPermissions.READ_HEART_RATE
import android.health.connect.HealthPermissions.READ_STEPS
import android.health.connect.HealthPermissions.WRITE_DISTANCE
import android.health.connect.HealthPermissions.WRITE_HEIGHT
import android.health.connect.HealthPermissions.WRITE_SPEED
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.additionalaccess.api.ExerciseRouteState
import com.android.healthconnect.controller.permissions.additionalaccess.api.LoadDeclaredHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.api.LoadExerciseRoutePermissionUseCase
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState.ALWAYS_ALLOW
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState.ASK_EVERY_TIME
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState.NEVER_ALLOW
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState.NOT_DECLARED
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeGetGrantedHealthPermissionsUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadExerciseRoutePermissionUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private val loadDeclaredHealthPermissionUseCase: LoadDeclaredHealthPermissionUseCase = mock()
    private val getHealthPermissionsFlagsUseCase: GetHealthPermissionsFlagsUseCase = mock()
    private val getGrantedHealthPermissionsUseCase = FakeGetGrantedHealthPermissionsUseCase()

    private lateinit var useCase: LoadExerciseRoutePermissionUseCase

    @Before
    fun setup() {
        hiltRule.inject()
        useCase =
            LoadExerciseRoutePermissionUseCase(
                loadDeclaredHealthPermissionUseCase,
                getHealthPermissionsFlagsUseCase,
                getGrantedHealthPermissionsUseCase,
                Dispatchers.Main,
            )
        getGrantedHealthPermissionsUseCase.updateData(TEST_APP_PACKAGE_NAME, emptyList())
        whenever(loadDeclaredHealthPermissionUseCase.invoke(eq(TEST_APP_PACKAGE_NAME))).then {
            listOf(READ_EXERCISE_ROUTES, READ_EXERCISE)
        }
        whenever(getHealthPermissionsFlagsUseCase.invoke(any(), any())).then {
            mapOf(
                READ_EXERCISE_ROUTES to FLAG_PERMISSION_USER_SET,
                READ_EXERCISE to FLAG_PERMISSION_USER_SET,
            )
        }
    }

    @Test
    fun execute_exerciseRoutePermissionGranted_returnGrantedState() = runTest {
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE_ROUTES),
        )

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)

        val expected =
            ExerciseRouteState(
                exerciseRoutePermissionState = ALWAYS_ALLOW,
                exercisePermissionState = ASK_EVERY_TIME,
            )
        assertThat(state).isEqualTo(UseCaseResults.Success(expected))
    }

    @Test
    fun execute_exerciseRoutePermissionDeclared_returnDeclaredState() = runTest {
        whenever(loadDeclaredHealthPermissionUseCase.invoke(eq(TEST_APP_PACKAGE_NAME))).then {
            listOf(READ_EXERCISE_ROUTES, READ_EXERCISE)
        }

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)

        val expected =
            ExerciseRouteState(
                exerciseRoutePermissionState = ASK_EVERY_TIME,
                exercisePermissionState = ASK_EVERY_TIME,
            )
        assertThat(state).isEqualTo(UseCaseResults.Success(expected))
    }

    @Test
    fun execute_exerciseRoutePermissionRevoked_returnRevokedState() = runTest {
        val flags = mapOf(READ_EXERCISE_ROUTES to PackageManager.FLAG_PERMISSION_USER_FIXED)
        whenever(getHealthPermissionsFlagsUseCase.invoke(any(), any())).then { flags }

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)
        val expected =
            ExerciseRouteState(
                exerciseRoutePermissionState = NEVER_ALLOW,
                exercisePermissionState = NOT_DECLARED,
            )
        assertThat(state).isEqualTo(UseCaseResults.Success(expected))
        verify(getHealthPermissionsFlagsUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, listOf(READ_EXERCISE_ROUTES, READ_EXERCISE))
    }

    @Test
    fun execute_emptyFlags_returnNotDeclaredState() = runTest {
        whenever(getHealthPermissionsFlagsUseCase.invoke(any(), any())).then {
            mapOf<String, Int>()
        }

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)

        val expected =
            ExerciseRouteState(
                exerciseRoutePermissionState = NOT_DECLARED,
                exercisePermissionState = NOT_DECLARED,
            )
        assertThat(state).isEqualTo(UseCaseResults.Success(expected))
    }

    @Test
    fun execute_noFlagsForExerciseRoutes_returnNotDeclaredState() = runTest {
        whenever(getHealthPermissionsFlagsUseCase.invoke(any(), any())).then {
            emptyMap<String, Int>()
        }

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)

        val expected =
            ExerciseRouteState(
                exerciseRoutePermissionState = NOT_DECLARED,
                exercisePermissionState = NOT_DECLARED,
            )
        assertThat(state).isEqualTo(UseCaseResults.Success(expected))
    }

    @Test
    fun execute_onlyReadExerciseDeclared_routePermissionNotDeclared() = runTest {
        whenever(loadDeclaredHealthPermissionUseCase.invoke(TEST_APP_PACKAGE_NAME)).then {
            listOf(READ_EXERCISE)
        }

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)

        val expected =
            ExerciseRouteState(
                exerciseRoutePermissionState = NOT_DECLARED,
                exercisePermissionState = ASK_EVERY_TIME,
            )
        assertThat(state).isEqualTo(UseCaseResults.Success(expected))
        verify(getHealthPermissionsFlagsUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, listOf(READ_EXERCISE))
    }

    @Test
    fun execute_onlyRoutePermissionDeclared_exercisePermissionNotDeclared() = runTest {
        whenever(loadDeclaredHealthPermissionUseCase.invoke(TEST_APP_PACKAGE_NAME)).then {
            listOf(READ_EXERCISE_ROUTES)
        }

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)

        val expected =
            ExerciseRouteState(
                exerciseRoutePermissionState = ASK_EVERY_TIME,
                exercisePermissionState = NOT_DECLARED,
            )
        assertThat(state).isEqualTo(UseCaseResults.Success(expected))
        verify(getHealthPermissionsFlagsUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, listOf(READ_EXERCISE_ROUTES))
    }

    @Test
    fun execute_permissionsNotDeclared_returnNotDeclaredState() = runTest {
        whenever(loadDeclaredHealthPermissionUseCase.invoke(TEST_APP_PACKAGE_NAME)).then {
            listOf<String>()
        }

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)

        val expected =
            ExerciseRouteState(
                exerciseRoutePermissionState = NOT_DECLARED,
                exercisePermissionState = NOT_DECLARED,
            )
        assertThat(state).isEqualTo(UseCaseResults.Success(expected))
        verify(getHealthPermissionsFlagsUseCase).invoke(TEST_APP_PACKAGE_NAME, listOf())
    }

    @Test
    fun execute_ignoresOtherPermissions() = runTest {
        whenever(loadDeclaredHealthPermissionUseCase.invoke(TEST_APP_PACKAGE_NAME)).then {
            listOf(
                READ_STEPS,
                WRITE_DISTANCE,
                READ_EXERCISE,
                READ_HEART_RATE,
                WRITE_SPEED,
                READ_EXERCISE_ROUTES,
                READ_BODY_FAT,
                WRITE_HEIGHT,
            )
        }

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)

        val expected =
            ExerciseRouteState(
                exerciseRoutePermissionState = ASK_EVERY_TIME,
                exercisePermissionState = ASK_EVERY_TIME,
            )
        assertThat(state).isEqualTo(UseCaseResults.Success(expected))
        verify(getHealthPermissionsFlagsUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, listOf(READ_EXERCISE_ROUTES, READ_EXERCISE))
    }

    @Test
    fun execute_onException_returnFailedResults() = runTest {
        val ex = IllegalStateException()
        whenever(loadDeclaredHealthPermissionUseCase.invoke(TEST_APP_PACKAGE_NAME)).then {
            throw ex
        }

        val state = useCase.invoke(TEST_APP_PACKAGE_NAME)

        assertThat(state).isEqualTo(UseCaseResults.Failed(ex))
    }
}
