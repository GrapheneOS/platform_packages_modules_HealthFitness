/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.healthconnect.testing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * Helper class for tests which use mock package managers to get health connect permissions handled
 * correctly.
 */
public class HealthPermissionsMocker {

    /**
     * Add mocking to the given mock package manager to make {@link
     * HealthConnectManager#getHealthPermissions(Context)} and {@link
     * HealthConnectManager#isHealthPermission(Context, String)} handle the current set of Health
     * Connect permissions.
     *
     * <p>This method is designed to help tests that use mock package managers. In particular,
     * because the static method {@link HealthConnectManager#getHealthPermissions(Context)} relies
     * on the context package manager to populate its cache of permissions, if a mock is not set up
     * correctly then the cache may be poisoned with incorrect values. Using this method in the
     * setup for the test can prevent this from happening.
     *
     * @param mockPackageManager a mock package manager to populate
     */
    public static void mockPackageManagerPermissions(PackageManager mockPackageManager)
            throws PackageManager.NameNotFoundException {
        // See implementation for HealthConnectManager.getHealthPermissions()
        // Here we are setting up fake package and permission information for the health connect
        // module as this is the source of truth for the health connect permissions.
        PackageManager realPackageManager =
                InstrumentationRegistry.getInstrumentation().getContext().getPackageManager();
        String healthControllerPackageName =
                realPackageManager.getPermissionInfo(HealthPermissions.READ_STEPS, /* flags= */ 0)
                        .packageName;
        PackageInfo packageInfo =
                realPackageManager.getPackageInfo(
                        healthControllerPackageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));

        when(mockPackageManager.getPermissionGroupInfo(
                        eq(android.health.connect.HealthPermissions.HEALTH_PERMISSION_GROUP),
                        anyInt()))
                .thenReturn(
                        realPackageManager.getPermissionGroupInfo(
                                android.health.connect.HealthPermissions.HEALTH_PERMISSION_GROUP,
                                0));

        when(mockPackageManager.getPackageInfo(eq(healthControllerPackageName), any()))
                .thenReturn(packageInfo);
    }
}
