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
import android.health.connect.DeviceDataProviderInfo;
import android.health.connect.DeviceDataSourceInfo;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Record;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.internal.annotations.GuardedBy;
import com.android.server.healthconnect.device.DeviceDataProviderManager;
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
 * Manages the logic for determining and displaying matching data sources for a calling app.
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

    private final MatchmakingDenialStateManager mMatchmakingDenialStateManager;

    private final DeviceDataProviderManager mDeviceDataProviderManager;

    public MatchmakingManager(
            HealthConnectContext userContext,
            HealthConnectPermissionHelper healthConnectPermissionHelper,
            PackageInfoUtils packageInfoUtils,
            HealthConnectMappings healthConnectMappings,
            MatchmakingDenialStateManager matchmakingDenialStateManager,
            DeviceDataProviderManager deviceDataProviderManager) {
        mUserContext = userContext;
        mHealthConnectPermissionHelper = healthConnectPermissionHelper;
        mPackageInfoUtils = packageInfoUtils;
        mHealthConnectMappings = healthConnectMappings;
        mMatchmakingDenialStateManager = matchmakingDenialStateManager;
        mDeviceDataProviderManager = deviceDataProviderManager;
    }

    /** Setup MatchingAppsManager for the given user. */
    public synchronized void setupForUser(HealthConnectContext userContext) {
        mUserContext = userContext;
    }

    /**
     * Returns all matching applications and their matching permissions for a given package name
     * based on: its granted read permissions, a set of specified record types, a set of data
     * sources to include or a set of data sources to exclude.
     *
     * <p>Returns an empty map if matchmaking is paused for the package.
     *
     * @param recordTypes A {@link Set} of {@link Class} objects extending {@link
     *     android.health.connect.datatypes.Record}, representing the specific types of health
     *     records to consider. If this set is empty, all granted read permissions for the package
     *     are considered.
     * @param packageName The name of the package for which to check if matching applications should
     *     be shown.
     * @param includeDataSources A {@link Set} of {@link DataOrigin} objects to limit the
     *     matchmaking to. Matchmaking will only consider data sources from this list. Cannot be set
     *     at the same time as excludeDataSources.
     * @param excludeDataSources A {@link Set} of {@link DataOrigin} objects to exclude from
     *     matchmaking. Matching data sources included in this list will not be shown. Cannot be set
     *     at the same time as includeDataSources.
     */
    public Map<String, Set<String>> fetchMatchingApps(
            Set<Class<? extends Record>> recordTypes,
            String packageName,
            Set<DataOrigin> includeDataSources,
            Set<DataOrigin> excludeDataSources) {
        if (!includeDataSources.isEmpty() && !excludeDataSources.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot set both includeDataSources and excludeDataSources");
        }
        // filter include/exclude based on visibility
        Set<DataOrigin> includeDataSourcesFiltered =
                filterDataSourceByVisibility(includeDataSources, packageName);
        Set<DataOrigin> excludeDataSourcesFiltered =
                filterDataSourceByVisibility(excludeDataSources, packageName);

        synchronized (this) {
            Set<String> writePermissions = getWritePermissionsToMatch(recordTypes, packageName);
            if (writePermissions.isEmpty()) {
                return Map.of();
            }
            return getAllAvailableWritingApps(
                    writePermissions,
                    packageName,
                    includeDataSourcesFiltered,
                    excludeDataSourcesFiltered);
        }
    }

    public Map<String, Set<String>> fetchMatchingApps(
            Set<Class<? extends Record>> recordTypes, String packageName) {
        return fetchMatchingApps(recordTypes, packageName, Set.of(), Set.of());
    }

    /**
     * Returns all matching devices and their matching permissions (configs) for a given package
     * name based on: its granted read permissions, a set of specified record types, a set of data
     * sources to include or a set of data sources to exclude.
     *
     * <p>Returns an empty map if matchmaking is paused for the device.
     *
     * @param recordTypes A {@link Set} of {@link Class} objects extending {@link
     *     android.health.connect.datatypes.Record}, representing the specific types of health
     *     records to consider. If this set is empty, all granted read permissions for the package
     *     are considered.
     * @param packageName The name of the package for which to check if matching applications should
     *     be shown.
     * @param includeDataSources A {@link Set} of {@link DataOrigin} objects to limit the
     *     matchmaking to. Matchmaking will only consider data sources from this list. Cannot be set
     *     at the same time as excludeDataSources.
     * @param excludeDataSources A {@link Set} of {@link DataOrigin} objects to exclude from
     *     matchmaking. Matching data sources included in this list will not be shown. Cannot be set
     *     at the same time as includeDataSources.
     */
    public Map<String, Set<String>> fetchMatchingDevices(
            Set<Class<? extends Record>> recordTypes,
            String packageName,
            Set<DataOrigin> includeDataSources,
            Set<DataOrigin> excludeDataSources) {
        if (!includeDataSources.isEmpty() && !excludeDataSources.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot set both includeDataSources and excludeDataSources");
        }
        synchronized (this) {
            Set<String> writePermissions = getWritePermissionsToMatch(recordTypes, packageName);
            if (writePermissions.isEmpty()) {
                return Map.of();
            }
            if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                return Map.of();
            }
            return getAllAvailableWritingDevicesMap(
                    writePermissions, packageName, includeDataSources, excludeDataSources);
        }
    }

    /** Increments the denial counter for the given package and permissions. */
    public void recordMatchmakingDenial(
            String callingPackageName, Map<String, List<String>> deniedApps) {
        for (Map.Entry<String, List<String>> entry : deniedApps.entrySet()) {
            String matchingPackageName = entry.getKey();
            List<String> permissions = entry.getValue();
            Set<Integer> writeCategories = getUniqueWriteCategories(permissions);
            for (int category : writeCategories) {
                mMatchmakingDenialStateManager.recordMatchmakingDenial(
                        callingPackageName, matchingPackageName, category);
            }
        }
    }

    private Set<Integer> getUniqueWriteCategories(List<String> permissions) {
        return permissions.stream()
                .distinct()
                .filter(mHealthConnectMappings::isWritePermission)
                .map(mHealthConnectMappings::getHealthDataCategoryForWritePermission)
                .filter(category -> category != -1)
                .collect(Collectors.toSet());
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
                        .flatMap(
                                recordType ->
                                        mHealthConnectMappings
                                                .getHealthPermissionCategoriesForRecordType(
                                                        recordType)
                                                .stream())
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

    private Set<DataOrigin> filterDataSourceByVisibility(
            Set<DataOrigin> dataSourceList, String readingAppPackageName) {
        return dataSourceList.stream()
                .filter(
                        dataOrigin ->
                                mPackageInfoUtils.hasPackageVisibility(
                                        readingAppPackageName,
                                        dataOrigin.getPackageName(),
                                        mUserContext.getUser(),
                                        mUserContext))
                .collect(Collectors.toSet());
    }

    /**
     * Retrieves a {@link Map} of all available writing apps and their grantable permissions that
     * match the matchmaking criteria.
     *
     * @param writePermissions A {@link Set} of write permissions({@link String}) for which to check
     *     matches
     * @param readingAppPackageName The calling app package name
     * @param includeDataSources A filtered {@link Set} of {@link DataOrigin} to include in the
     *     matchmaking checks, that the {@code readingAppPackageName} has visibility over.
     * @param excludeDataSources A filtered {@link Set} of {@link DataOrigin} to exclude from the
     *     matchmaking checks, that the {@code readingAppPackageName} has visibility over. {@code
     *     includeDataSources} and {@code excludeDataSources} cannot both be set simultaneously.
     * @return A {link Map} where keys are the package names of available writing apps, and values
     *     are a {@link Set} of their grantable write permissions.
     */
    private synchronized Map<String, Set<String>> getAllAvailableWritingApps(
            Set<String> writePermissions,
            String readingAppPackageName,
            Set<DataOrigin> includeDataSources,
            Set<DataOrigin> excludeDataSources) {
        if (writePermissions.isEmpty()) {
            return Map.of();
        }

        return getCompatibleApps().stream()
                .filter(
                        packageInfo ->
                                !packageInfo.packageName.equals(readingAppPackageName)
                                        && packageInfo.requestedPermissions != null)
                .filter(includeExcludeFilter(includeDataSources, excludeDataSources))
                .map(
                        packageInfo -> {
                            Set<String> unpausedWritePermissions =
                                    getUnpausedWritePermissions(
                                            writePermissions,
                                            readingAppPackageName,
                                            packageInfo.packageName);
                            if (unpausedWritePermissions.isEmpty()) {
                                return Map.entry(packageInfo.packageName, Set.<String>of());
                            }
                            Set<String> grantablePermissions =
                                    allGrantableWritePermission(
                                            unpausedWritePermissions, packageInfo);
                            return Map.entry(packageInfo.packageName, grantablePermissions);
                        })
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean dataSourceListContains(Set<DataOrigin> dataSourceList, DataOrigin dataOrigin) {
        return dataSourceList.contains(dataOrigin);
    }

    private boolean dataSourceListContains(Set<DataOrigin> dataSourceList, String packageName) {
        return dataSourceList.stream()
                .map(DataOrigin::getPackageName)
                .collect(Collectors.toSet())
                .contains(packageName);
    }

    private Predicate<PackageInfo> includeExcludeFilter(
            Set<DataOrigin> includeDataSources, Set<DataOrigin> excludeDataSources) {
        return packageInfo -> {
            if (includeDataSources.isEmpty() && excludeDataSources.isEmpty()) {
                // With no include/exclude filters set we consider all packages
                return true;
            } else if (includeDataSources.isEmpty()) {
                return !dataSourceListContains(excludeDataSources, packageInfo.packageName);
            } else { // if (excludeDataSources.isEmpty())
                return dataSourceListContains(includeDataSources, packageInfo.packageName);
            }
        };
    }

    private Predicate<DeviceDataSourceInfo> includeExcludeFilterForDevices(
            Set<DataOrigin> includeDataSources, Set<DataOrigin> excludeDataSources) {
        return deviceDataSourceInfo -> {
            if (includeDataSources.isEmpty() && excludeDataSources.isEmpty()) {
                // With no include/exclude filters set we consider all packages
                return true;
            } else if (includeDataSources.isEmpty()) {
                return !dataSourceListContains(
                        excludeDataSources, deviceDataSourceInfo.getDeviceDataOrigin());
            } else { // if (excludeDataSources.isEmpty())
                return dataSourceListContains(
                        includeDataSources, deviceDataSourceInfo.getDeviceDataOrigin());
            }
        };
    }

    /**
     * Retrieves a {@link Map} of all available writing devices and their associated Device Data
     * Providers' configs that match the matchmaking criteria.
     *
     * @param writePermissions A {@link Set} of write permissions({@link String}) for which to check
     *     matches
     * @param readingAppPackageName The calling app package name
     * @param includeDataSources A filtered {@link Set} of {@link DataOrigin} to include in the
     *     matchmaking checks.
     * @param excludeDataSources A filtered {@link Set} of {@link DataOrigin} to exclude from the
     *     matchmaking checks. {@code includeDataSources} and {@code excludeDataSources} cannot both
     *     be set simultaneously.
     * @return A {@link Map} where keys are device package names ({@link String}) derived from
     *     {@link DeviceDataSourceInfo.DataOrigin#getPackageName()}, and values are a {@link Set} of
     *     matching write permissions ({@link String}), representing the union of all matching DDP
     *     write permissions for that device.
     */
    private synchronized Map<String, Set<String>> getAllAvailableWritingDevicesMap(
            Set<String> writePermissions,
            String readingAppPackageName,
            Set<DataOrigin> includeDataSources,
            Set<DataOrigin> excludeDataSources) {
        if (writePermissions.isEmpty()) {
            return Map.of();
        }

        List<DeviceDataSourceInfo> deviceInfos =
                mDeviceDataProviderManager.getDeviceDataSourceInfos();

        if (deviceInfos.isEmpty()) {
            return Map.of();
        }

        return deviceInfos.stream()
                .filter(includeExcludeFilterForDevices(includeDataSources, excludeDataSources))
                // Filter out paused devices
                .filter(
                        deviceDataSourceInfo ->
                                !isMatchmakingForDevicePaused(
                                        readingAppPackageName,
                                        deviceDataSourceInfo
                                                .getDeviceDataOrigin()
                                                .getPackageName()))
                .map(
                        deviceDataSourceInfo -> {
                            Set<String> grantablePermissionsForDevice =
                                    getMatchingWritePermissionsForDevice(
                                            deviceDataSourceInfo, writePermissions);
                            return Map.entry(
                                    deviceDataSourceInfo.getDeviceDataOrigin().getPackageName(),
                                    grantablePermissionsForDevice);
                        })
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Examines all available Device Data Providers (DDPs) for an unpaused device and aggregates a
     * {@link Set} of all write configs ({@link String}) that match the request.
     *
     * @param deviceDataSourceInfo The {@link DeviceDataSourceInfo} for the device.
     * @param writePermissions A {@link Set} of requested write permissions ({@link String}).
     * @return The union of all matching write config ({@link String}) across all DDPs associated
     *     with this device.
     */
    private Set<String> getMatchingWritePermissionsForDevice(
            DeviceDataSourceInfo deviceDataSourceInfo, Set<String> writePermissions) {

        return deviceDataSourceInfo.getDeviceDataProviderInfos().stream()
                .flatMap(
                        deviceDataProviderInfo ->
                                getMatchingWritePermissionsForDDP(
                                        deviceDataProviderInfo, writePermissions)
                                        .stream())
                .collect(Collectors.toSet());
    }

    private Set<String> getMatchingWritePermissionsForDDP(
            DeviceDataProviderInfo ddp, Set<String> requestedWritePermissions) {
        Set<Class<? extends Record>> availableDataTypes =
                ddp.getDeviceDataTypeAdvertisements().stream()
                        .filter(Predicate.not(DeviceDataTypeAdvertisement::isUserEnabled))
                        // TODO (b/466983701) filter out data types not visible by default
                        // if device not in include filter
                        .map(DeviceDataTypeAdvertisement::getDataType)
                        .collect(Collectors.toSet());

        // Transform Class<? extends Record> to write permissions
        Set<String> availableWriteConfigs = getWritePermissionsFromRecordTypes(availableDataTypes);

        // Return only those configs that match the request
        return availableWriteConfigs.stream()
                .filter(requestedWritePermissions::contains)
                .collect(Collectors.toSet());
    }

    private Set<String> getWritePermissionsFromRecordTypes(
            Set<Class<? extends Record>> recordTypes) {
        // TODO (b/462180668) check specifically for Symptoms records
        return recordTypes.stream()
                .map(mHealthConnectMappings::getRecordType)
                .flatMap(
                        recordType ->
                                mHealthConnectMappings
                                        .getHealthPermissionCategoriesForRecordType(recordType)
                                        .stream())
                .map(mHealthConnectMappings::getHealthWritePermission)
                .collect(Collectors.toSet());
    }

    private boolean isMatchmakingForDevicePaused(
            String readingAppPackageName, String matchingDevicePackageName) {
        return mMatchmakingDenialStateManager.isMatchmakingForDevicePaused(
                readingAppPackageName, matchingDevicePackageName);
    }

    private Set<String> getUnpausedWritePermissions(
            Set<String> writePermissions,
            String readingAppPackageName,
            String matchingPackageName) {
        Set<Integer> unpausedDataCategories =
                writePermissions.stream()
                        .map(mHealthConnectMappings::getHealthDataCategoryForWritePermission)
                        .filter(category -> category != -1)
                        .distinct()
                        .filter(
                                category ->
                                        !mMatchmakingDenialStateManager.isMatchmakingPaused(
                                                readingAppPackageName,
                                                matchingPackageName,
                                                category))
                        .collect(Collectors.toSet());

        return writePermissions.stream()
                .filter(
                        permission ->
                                unpausedDataCategories.contains(
                                        mHealthConnectMappings
                                                .getHealthDataCategoryForWritePermission(
                                                        permission)))
                .collect(Collectors.toSet());
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
                        && !isUserFixed(packageInfo, permission);
    }

    private boolean isDenied(PackageInfo packageInfo, String permission) {
        return mPackageInfoUtils.checkPermission(
                        mUserContext, mUserContext.getUser(), permission, packageInfo.packageName)
                == PERMISSION_DENIED;
    }

    private boolean isUserFixed(PackageInfo packageInfo, String permission) {
        int flags =
                mHealthConnectPermissionHelper.getHealthPermissionFlags(
                        packageInfo.packageName, mUserContext.getUser(), permission);
        return (flags & PackageManager.FLAG_PERMISSION_USER_FIXED) != 0;
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
