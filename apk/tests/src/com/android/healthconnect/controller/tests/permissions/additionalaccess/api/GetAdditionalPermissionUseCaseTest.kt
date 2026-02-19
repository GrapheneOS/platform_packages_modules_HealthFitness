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

import android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.additionalaccess.api.GetAdditionalPermissionUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GetAdditionalPermissionUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var useCase: GetAdditionalPermissionUseCase

    @BindValue val healthPermissionReader = Mockito.mock(HealthPermissionReader::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
        whenever(healthPermissionReader.getAdditionalPermissions(TEST_APP_PACKAGE_NAME)).then {
            emptyList<String>()
        }
    }

    @Test
    fun execute_callsGetHealthPermissions() {
        useCase.invoke(TEST_APP_PACKAGE_NAME)

        verify(healthPermissionReader).getAdditionalPermissions(eq(TEST_APP_PACKAGE_NAME))
    }

    @Test
    fun execute_returnsAdditionalPermissions() {
        whenever(healthPermissionReader.getAdditionalPermissions(TEST_APP_PACKAGE_NAME)).then {
            listOf(READ_EXERCISE_ROUTES)
        }

        assertThat(useCase.invoke(TEST_APP_PACKAGE_NAME)).isEqualTo(listOf(READ_EXERCISE_ROUTES))
    }
}
