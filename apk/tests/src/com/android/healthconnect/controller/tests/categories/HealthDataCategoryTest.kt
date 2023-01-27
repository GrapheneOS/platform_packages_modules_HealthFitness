/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.healthconnect.controller.tests.categories

import android.content.Context
import android.healthconnect.HealthConnectManager
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.categories.HEALTH_DATA_CATEGORIES
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class HealthDataCategoryTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun allHealthPermission_haveParentCategory() {
        val allPermissions = HealthConnectManager.getHealthPermissions(context)
        allPermissions.forEach { permissionString ->
            val healthPermission = HealthPermission.fromPermissionString(permissionString)
            assertThat(
                    HEALTH_DATA_CATEGORIES.any {
                        it.healthPermissionTypes.contains(healthPermission.healthPermissionType)
                    })
                .isEqualTo(true)
        }
    }
}
