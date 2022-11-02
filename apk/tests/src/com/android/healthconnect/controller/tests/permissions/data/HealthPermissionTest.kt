/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.permissions.data

import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import org.junit.Assert
import org.junit.Test

// TODO(b/257075983) move this to unit tests
class HealthPermissionTest {

    @Test
    fun fromPermission_returnsCorrectReadHealthPermission() {
        val result =
            HealthPermission.fromPermissionString("android.permission.health.READ_ACTIVE_CALORIES_BURNED")

        Assert.assertEquals(
            HealthPermission(HealthPermissionType.ACTIVE_CALORIES_BURNED, PermissionsAccessType.READ), result)
    }

    @Test
    fun fromPermission_returnsCorrectWriteHealthPermission() {
        val result = HealthPermission.fromPermissionString("android.permission.health.WRITE_BLOOD_GLUCOSE")

        Assert.assertEquals(
            HealthPermission(HealthPermissionType.BLOOD_GLUCOSE, PermissionsAccessType.WRITE), result)
    }

    @Test
    fun fromPermission_nonHealthPermission_returnsNull() {
        val result = HealthPermission.fromPermissionString("android.permission.INTERNET")

        Assert.assertEquals(null, result)
    }
}
