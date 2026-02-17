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
package com.android.healthconnect.controller.tests.utils

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.utils.navigateSafe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class FragmentUtilsTest {

    val navController: NavController = mock()
    val currentDestination: NavDestination = mock()

    @Before
    fun setup() {
        whenever(navController.currentDestination).thenReturn(currentDestination)
    }

    @Test
    fun navigateSafe_idsMatch_navigates() {
        whenever(currentDestination.id).thenReturn(123)

        navController.navigateSafe(123, 456)

        verify(navController).navigate(456, null)
    }

    @Test
    fun navigateSafe_idsDoNotMatch_doesNotNavigate() {
        whenever(currentDestination.id).thenReturn(999)

        navController.navigateSafe(123, 456)

        verify(navController).currentDestination
        verifyNoMoreInteractions(navController)
    }

    @Test
    fun navigateSafe_navigateThrows_catchesException() {
        whenever(currentDestination.id).thenReturn(123)

        doThrow(IllegalArgumentException("Action not found"))
            .`when`(navController)
            .navigate(456, null)

        // This should not throw if navigateSafe catches the exception
        navController.navigateSafe(123, 456)

        verify(navController).navigate(456, null)
    }
}
