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
package com.android.server.healthconnect.onboarding.matchmaking;

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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manages the logic for determining and displaying matching applications from a calling app.
 *
 * @hide
 */
public final class MatchmakingManager {
    private static final String TAG = MatchmakingManager.class.getSimpleName();

    private final HealthConnectPermissionHelper mHealthConnectPermissionHelper;
    private final PackageInfoUtils mPackageInfoUtils;

    private final HealthConnectMappings mHealthConnectMappings;

    @GuardedBy("this")
    private HealthConnectContext mUserContext;

    private final PackageManager mPackageManager;

    private final MatchmakingDenialStateManager mMatchmakingDenialStateManager;

    public MatchmakingManager(
            HealthConnectContext userContext,
            HealthConnectPermissionHelper healthConnectPermissionHelper,
            PackageInfoUtils packageInfoUtils,
            HealthConnectMappings healthConnectMappings,
            PackageManager packageManager,
            MatchmakingDenialStateManager matchmakingDenialStateManager) {
        mUserContext = userContext;
        mHealthConnectPermissionHelper = healthConnectPermissionHelper;
        mPackageInfoUtils = packageInfoUtils;
        mHealthConnectMappings = healthConnectMappings;
        mPackageManager = packageManager;
        mMatchmakingDenialStateManager = matchmakingDenialStateManager;
    }

    /** Setup MatchingAppsManager for the given user. */
    public synchronized void setupForUser(HealthConnectContext userContext) {
        mUserContext = userContext;
    }

    /**
     * Returns all matching applications and their matching permissions for a given package name
     * based on its granted read permissions and a set of specified record types.
     *
     * <p>Returns an empty map if matchmaking is paused for the package.
     *
     * @param recordTypes A {@link Set} of {@link Class} objects extending {@link
     *     android.health.connect.datatypes.Record}, representing the specific types of health
     *     records to consider. If this set is empty, all granted read permissions for the package
     *     are considered.
     * @param packageName The name of the package for which to check if matching applications should
     *     be shown.
     */
    public Map<String, Set<String>> fetchMatchingApps(
            Set<Class<? extends Record>> recordTypes, String packageName) {
        synchronized (this) {
            if (mMatchmakingDenialStateManager.isMatchmakingPaused(packageName)) {
                return Map.of();
            }
            Set<String> writePermissions = getWritePermissionsToMatch(recordTypes, packageName);
            if (writePermissions.isEmpty()) {
                return Map.of();
            }
            return getAllAvailableWritingApps(writePermissions);
        }
    }

    /** Increments the denial counter for the given package. */
    public void recordMatchmakingDenial(String packageName) {
        mMatchmakingDenialStateManager.recordMatchmakingDenial(packageName);
    }

    private Set<String> getWritePermissionsToMatch(
            Set<Class<? extends Record>> recordTypes, String packageName) {
        Set<String> readPermissions = getReadPermissionsToMatch(recordTypes, packageName);
        return mapToWritePermissions(readPermissions);
    }

    private Set<String> getReadPermissionsToMatch(
            Set<Class<? extends Record>> recordTypes, String packageName) {
        Set<String> readPermissionsFilter =
                recordTypes.stream()
                        .map(mHealthConnectMappings::getRecordType)
                        .map(mHealthConnectMappings::getHealthPermissionCategoryForRecordType)
                        .map(mHealthConnectMappings::getHealthReadPermission)
                        .collect(Collectors.toSet());
        return getReadPermissions(readPermissionsFilter, packageName);
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

    private synchronized Map<String, Set<String>> getAllAvailableWritingApps(
            Set<String> writePermissions) {
        if (writePermissions.isEmpty()) {
            return Map.of();
        }

        return getCompatibleApps().stream()
                .filter(packageInfo -> packageInfo.requestedPermissions != null)
                .map(
                        packageInfo -> {
                            Set<String> grantablePermissions =
                                    allGrantableWritePermission(writePermissions, packageInfo);
                            return Map.entry(packageInfo.packageName, grantablePermissions);
                        })
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<String> allGrantableWritePermission(
            Set<String> writePermissions, PackageInfo packageInfo) {
        return Arrays.stream(packageInfo.requestedPermissions)
                .filter(writePermissionFilter(writePermissions, packageInfo))
                .collect(Collectors.toSet());
    }

    private Predicate<String> writePermissionFilter(
            Set<String> writePermissions, PackageInfo packageInfo) {
        return permission ->
                writePermissions.contains(permission)
                        && isDenied(packageInfo, permission)
                        && isNotUserFixed(packageInfo, permission);
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
