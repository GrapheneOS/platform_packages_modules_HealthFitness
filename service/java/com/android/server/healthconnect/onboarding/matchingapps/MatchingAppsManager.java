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
package com.android.server.healthconnect.onboarding.matchingapps;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;

import com.android.internal.annotations.GuardedBy;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.storage.HealthConnectContext;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages the logic for determining and displaying matching applications from a calling app.
 *
 * @hide
 */
public final class MatchingAppsManager {
    private static final String TAG = MatchingAppsManager.class.getSimpleName();

    private final HealthConnectPermissionHelper mHealthConnectPermissionHelper;
    private final PackageInfoUtils mPackageInfoUtils;

    private final HealthConnectMappings mHealthConnectMappings;

    @GuardedBy("this")
    private HealthConnectContext mUserContext;

    private final PackageManager mPackageManager;

    public MatchingAppsManager(
            HealthConnectContext userContext,
            HealthConnectPermissionHelper healthConnectPermissionHelper,
            PackageInfoUtils packageInfoUtils,
            HealthConnectMappings healthConnectMappings,
            PackageManager packageManager) {
        mUserContext = userContext;
        mHealthConnectPermissionHelper = healthConnectPermissionHelper;
        mPackageInfoUtils = packageInfoUtils;
        mHealthConnectMappings = healthConnectMappings;
        mPackageManager = packageManager;
    }

    /** Setup MatchingAppsManager for the given user. */
    public synchronized void setupForUser(HealthConnectContext userContext) {
        mUserContext = userContext;
    }

    /**
     * Determines whether matching applications should be shown for a given package based on its
     * granted read permissions and a set of specified record types.
     *
     * <p>This method first retrieves all read permissions granted to the {@code packageName}. If
     * {@code recordTypes} is not empty, it further filters these read permissions to only include
     * those that correspond to the provided record types. This is achieved by mapping each record
     * type to its corresponding health permission category.
     *
     * <p>If, after this filtering, there are no remaining read permissions, the method returns
     * {@code false}.
     *
     * <p>Otherwise, it converts the remaining read permissions into their corresponding write
     * permissions and then checks if any applications are available that can write these record
     * types.
     *
     * @param recordTypes A {@link Set} of {@link Class} objects extending {@link
     *     android.health.connect.datatypes.Record}, representing the specific types of health
     *     records to consider. If this set is empty, all granted read permissions for the package
     *     are considered.
     * @param packageName The name of the package for which to check if matching applications should
     *     be shown.
     * @return {@code true} if there are granted read permissions (potentially filtered by {@code
     *     recordTypes}) that correspond to available writing applications; {@code false} otherwise.
     */
    public boolean canConnectMatchingApps(
            Set<Class<? extends Record>> recordTypes, String packageName) {
        synchronized (this) {
            Set<String> readPermissionsFilter =
                    recordTypes.stream()
                            .map(mHealthConnectMappings::getRecordType)
                            .map(mHealthConnectMappings::getHealthPermissionCategoryForRecordType)
                            .map(mHealthConnectMappings::getHealthReadPermission)
                            .collect(Collectors.toSet());
            Set<String> readPermissions = getReadPermissions(readPermissionsFilter, packageName);
            if (readPermissions.isEmpty()) {
                return false;
            }

            Set<String> writePermissions = mapToWritePermissions(readPermissions);
            return anyAvailableWritingApps(writePermissions);
        }
    }

    private synchronized Set<String> getReadPermissions(
            Set<String> permissionFilter, String packageName) {
        return mHealthConnectPermissionHelper
                .getGrantedHealthPermissions(packageName, mUserContext.getUser())
                .stream()
                .filter(mHealthConnectMappings::isReadPermission)
                .filter(
                        permission ->
                                permissionFilter.isEmpty() || permissionFilter.contains(permission))
                .collect(Collectors.toSet());
    }

    private Set<String> mapToWritePermissions(Set<String> readPermissions) {
        return readPermissions.stream()
                .map(mHealthConnectMappings::getWritePermissionForReadPermission)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private synchronized boolean anyAvailableWritingApps(Set<String> writePermissions) {
        if (writePermissions.isEmpty()) {
            return false;
        }
        return getCompatibleApps().stream()
                .filter(packageInfo -> packageInfo.requestedPermissions != null)
                .anyMatch(
                        packageInfo -> hasGrantableWritePermission(writePermissions, packageInfo));
    }

    private boolean hasGrantableWritePermission(
            Set<String> writePermissions, PackageInfo packageInfo) {
        return Arrays.stream(packageInfo.requestedPermissions)
                .anyMatch(
                        permission ->
                                writePermissions.contains(permission)
                                        && isDenied(packageInfo, permission)
                                        && isNotUserFixed(packageInfo, permission));
    }

    private boolean isDenied(PackageInfo packageInfo, String permission) {
        return mPackageManager.checkPermission(permission, packageInfo.packageName)
                == PERMISSION_DENIED;
    }

    private boolean isNotUserFixed(PackageInfo packageInfo, String permission) {
        int flags =
                mHealthConnectPermissionHelper.getHealthPermissionFlags(
                        packageInfo.packageName, mUserContext.getUser(), permission);
        return (flags & PackageManager.FLAG_PERMISSION_USER_FIXED) == 0;
    }

    private synchronized List<PackageInfo> getCompatibleApps() {
        return mPackageInfoUtils
                .getPackagesCompatibleWithHealthConnect(mUserContext, mUserContext.getUser())
                .stream()
                .filter(info -> !isSystemApp(info.packageName))
                .toList();
    }

    private synchronized boolean isSystemApp(String packageName) {
        return mHealthConnectPermissionHelper.hasNonUserSensitiveHealthPermission(
                packageName, mUserContext.getUser(), mUserContext);
    }
}
