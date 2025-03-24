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
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
        // For on device tests we can get permissions from the real package manager.
        // But for Robolectric tests this will not be set up. So we need to have the values hard
        // coded.
        // See implementation for HealthConnectManager.getHealthPermissions()
        // Here we are setting up fake package and permission information for the health connect
        // module as this is the source of truth for the health connect permissions.
        String modulePackageName = "android.health.connect";

        // Use deprecated constructor as we are simulating construction by the System.
        PermissionGroupInfo permissionGroupInfo = new PermissionGroupInfo();
        permissionGroupInfo.packageName = modulePackageName;
        when(mockPackageManager.getPermissionGroupInfo(
                        eq(android.health.connect.HealthPermissions.HEALTH_PERMISSION_GROUP),
                        anyInt()))
                .thenReturn(permissionGroupInfo);

        PackageInfo modulePackageInfo = new PackageInfo();
        modulePackageInfo.packageName = modulePackageName;

        // See implementation for HealthConnectManager.isValidHealthPermission()
        List<String> allPermissions = getAllHealthPermissionsByReflection();
        PermissionInfo[] allModulePermissions = new PermissionInfo[allPermissions.size()];
        int index = 0;
        for (String perm : allPermissions) {
            // Use deprecated constructor as we are simulating construction by the system
            PermissionInfo permissionInfo = new PermissionInfo();
            permissionInfo.packageName = modulePackageName;
            permissionInfo.name = perm;
            permissionInfo.group = android.health.connect.HealthPermissions.HEALTH_PERMISSION_GROUP;

            when(mockPackageManager.getPermissionInfo(eq(perm), anyInt()))
                    .thenReturn(permissionInfo);
            allModulePermissions[index++] = permissionInfo;
        }
        modulePackageInfo.permissions = allModulePermissions;

        when(mockPackageManager.getPackageInfo(eq(modulePackageName), any()))
                .thenReturn(modulePackageInfo);
        // This is necessary in case the cache was earlier initialized when this was not mocked.
        HealthConnectManager.resetHealthPermissionsCache();
    }

    private static List<String> getAllHealthPermissionsByReflection() {
        ArrayList<String> result = new ArrayList<>();
        for (Field field : HealthPermissions.class.getFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers)
                    && Modifier.isStatic(modifiers)
                    // We don't check for final here, because under robolectric they aren't final
                    && field.getType() == String.class) {
                try {
                    String value = (String) field.get(null);
                    if (value.startsWith("android.permission.")) {
                        result.add(value);
                    }
                } catch (IllegalAccessException e) {
                    // skip any errors
                }
            }
        }
        return result;
    }
}
