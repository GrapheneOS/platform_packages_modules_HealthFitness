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

package com.android.healthconnect.controller.tests.permissions.additionalaccess

import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import com.android.healthconnect.controller.permissions.additionalaccess.LoadDeclaredHealthPermissionUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthfitness.flags.Flags
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
class LoadDeclaredHealthPermissionUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Inject lateinit var useCase: LoadDeclaredHealthPermissionUseCase

    @BindValue val healthPermissionReader = mock(HealthPermissionReader::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
        whenever(healthPermissionReader.getDeclaredHealthPermissions(TEST_APP_PACKAGE_NAME)).then {
            emptyList<String>()
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun execute_callsGetDeclaredHealthPermissions() {
        useCase.invoke(TEST_APP_PACKAGE_NAME)

        verify(healthPermissionReader).getDeclaredHealthPermissions(eq(TEST_APP_PACKAGE_NAME))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun execute_callsGetValidHealthPermissions() {
        useCase.invoke(TEST_APP_PACKAGE_NAME)

        verify(healthPermissionReader).getValidHealthPermissions(eq(TEST_APP_PACKAGE_NAME))
    }
}
