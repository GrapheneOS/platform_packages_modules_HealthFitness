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

import android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.additionalaccess.api.GetAdditionalPermissionUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GetAdditionalPermissionUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var useCase: GetAdditionalPermissionUseCase

    @BindValue val healthPermissionReader: HealthPermissionReader = mock()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun invoke_returnsAdditionalPermissions() = runTest {
        healthPermissionReader.stub {
            on { getAdditionalPermissions(TEST_APP_PACKAGE_NAME) } doReturn
                listOf(READ_EXERCISE_ROUTES)
        }

        val result = useCase.invoke(TEST_APP_PACKAGE_NAME)
        verify(healthPermissionReader).getAdditionalPermissions(eq(TEST_APP_PACKAGE_NAME))
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEqualTo(listOf(READ_EXERCISE_ROUTES))
    }

    @Test
    fun invoke_healthPermissionReaderError_returnsFailure() = runTest {
        healthPermissionReader.stub {
            on { getAdditionalPermissions(TEST_APP_PACKAGE_NAME) } doThrow
                RuntimeException("Exception")
        }

        val result = useCase.invoke(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(UseCaseResults.Failed::class.java)
        assertThat((result as UseCaseResults.Failed).exception)
            .isInstanceOf(RuntimeException::class.java)
    }
}
