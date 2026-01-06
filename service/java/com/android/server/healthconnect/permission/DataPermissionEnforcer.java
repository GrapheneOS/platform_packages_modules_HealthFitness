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
 */

package com.android.server.healthconnect.permission;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;

import static com.android.healthfitness.flags.AconfigFlagHelper.isDeviceUdiEnabled;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import android.content.AttributionSource;
import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.UserHandle;
import android.permission.PermissionManager;

import com.android.modules.utils.build.SdkLevel;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to force caller of data apis to hold api required permissions.
 *
 * @hide
 */
public class DataPermissionEnforcer {
    private final PermissionManager mPermissionManager;
    private final Context mContext;
    private final HealthConnectMappings mHealthConnectMappings;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;

    public DataPermissionEnforcer(
            PermissionManager permissionManager,
            Context context,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        mPermissionManager = permissionManager;
        mContext = context;
        mHealthConnectMappings = internalHealthConnectMappings.getExternalMappings();
        mInternalHealthConnectMappings = internalHealthConnectMappings;
    }

    /** Enforces default write permissions for given recordTypeIds */
    public void enforceWritePermissions(
            Collection<Integer> recordTypeIds, AttributionSource attributionSource) {
        for (Integer recordTypeId : recordTypeIds) {
            enforceWritePermission(recordTypeId, attributionSource);
        }
    }

    /** Enforces default read permissions for given recordTypeIds */
    public void enforceReadPermissions(
            Collection<Integer> recordTypeIds, AttributionSource attributionSource) {
        for (Integer recordTypeId : recordTypeIds) {
            enforceReadPermission(recordTypeId, attributionSource);
        }
    }

    private void enforceReadPermission(int recordTypeId, AttributionSource attributionSource) {
        Set<Integer> permissionCategories =
                mHealthConnectMappings.getHealthPermissionCategoriesForRecordType(recordTypeId);
        if (permissionCategories.isEmpty()) {
            throw new SecurityException("No permissions defined for record type " + recordTypeId);
        }

        List<String> permissionNames =
                permissionCategories.stream()
                        .map(mHealthConnectMappings::getHealthReadPermission)
                        .toList();

        // Shortcut general case of a single permission.
        if (permissionNames.size() == 1) {
            enforceReadPermission(permissionNames.get(0), attributionSource, recordTypeId);
            return;
        }

        // For a record type with multiple read permissions, at least one is required to be granted.
        // We can't use enforceAnyOfPermissions because we want to check each permission
        // individually and ignore SecurityException for denied ones.
        for (String permissionName : permissionNames) {
            try {
                enforceReadPermission(permissionName, attributionSource, recordTypeId);
                return;
            } catch (SecurityException e) {
                // Ignore and check the next permission
            }
        }

        // If no permission was granted for this record type, throw an exception.
        throw new SecurityException(
                "Caller requires one of "
                        + permissionNames
                        + " to read record type "
                        + getExternalRecordClass(recordTypeId));
    }

    /**
     * Enforces that caller has either read or write permissions for given recordTypeId. Returns
     * flag which indicates that caller is allowed to read only records written by itself.
     */
    public boolean enforceReadAccessAndGetEnforceSelfRead(
            int recordTypeId, AttributionSource attributionSource) {
        boolean enforceSelfRead = false;
        try {
            enforceReadPermission(recordTypeId, attributionSource);
        } catch (SecurityException readSecurityException) {
            try {
                enforceWritePermission(recordTypeId, attributionSource);
                // Apps are always allowed to read self data if they have insert
                // permission.
                enforceSelfRead = true;
            } catch (SecurityException writeSecurityException) {
                throw readSecurityException;
            }
        }
        return enforceSelfRead;
    }

    // TODO(b/312952346): Consider refactoring how permission enforcement is done within
    // HealthConnectServiceImpl. This goes beyond just this method.
    /**
     * Enforces that the caller has either read or write permissions for all the given recordTypes,
     * and returns {@code true} if the caller is allowed to read only records written by itself,
     * false otherwise.
     *
     * @throws SecurityException if the app has neither read nor write permissions for any of the
     *     specified record types.
     */
    public boolean enforceReadAccessAndGetEnforceSelfRead(
            List<Integer> recordTypes, AttributionSource attributionSource) {
        boolean enforceSelfRead = false;
        for (int recordTypeId : recordTypes) {
            enforceSelfRead |=
                    enforceReadAccessAndGetEnforceSelfRead(recordTypeId, attributionSource);
        }
        return enforceSelfRead;
    }

    /**
     * Enforces that caller has all write permissions to write given records. Includes permissions
     * for writing optional extra data if it's present in given records.
     */
    public void enforceRecordsWritePermissions(
            List<RecordInternal<?>> recordInternals, AttributionSource attributionSource) {
        // Enforce category-level permissions.
        Set<Integer> recordTypeIds =
                recordInternals.stream().map(RecordInternal::getRecordType).collect(toSet());
        enforceWritePermissions(recordTypeIds, attributionSource);

        // Enforce UDI permission.
        boolean isUdiProvided =
                recordInternals.stream().anyMatch(record -> record.getUdi() != null);
        if (isUdiProvided && isDeviceUdiEnabled()) {
            enforceWriteUdiPermission(attributionSource);
        }

        // Enforce per-record permissions.
        Set<String> grantedPerRecordPermissions = new HashSet<>();
        for (RecordInternal<?> recordInternal : recordInternals) {
            int recordTypeId = recordInternal.getRecordType();
            RecordHelper<?> recordHelper =
                    mInternalHealthConnectMappings.getRecordHelper(recordTypeId);
            for (String permission : recordHelper.getPerRecordWritePermissions(recordInternal)) {
                if (grantedPerRecordPermissions.contains(permission)) {
                    continue;
                }
                enforceWritePermission(permission, attributionSource, recordTypeId);
                grantedPerRecordPermissions.add(permission);
            }
        }
    }

    /** Enforces that caller has any of given permissions. */
    public void enforceAnyOfPermissions(String... permissions) {
        for (var permission : permissions) {
            if (mContext.checkCallingPermission(permission) == PERMISSION_GRANTED) {
                return;
            }
        }
        throw new SecurityException(
                "Caller requires one of the following permissions: "
                        + String.join(", ", permissions));
    }

    /**
     * Returns granted extra read permissions.
     *
     * <p>Used to not expose extra data if caller doesn't have corresponding permission.
     */
    public Set<String> collectGrantedExtraReadPermissions(
            Set<Integer> recordTypeIds, AttributionSource attributionSource) {
        return recordTypeIds.stream()
                .map(mInternalHealthConnectMappings::getRecordHelper)
                .flatMap(recordHelper -> recordHelper.getExtraReadPermissions().stream())
                .distinct()
                .filter(permission -> isPermissionGranted(permission, attributionSource))
                .collect(toSet());
    }

    /**
     * Returns all per-record write permissions for the specified record types.
     *
     * @see RecordHelper#getAllPerRecordWritePermissions()
     */
    public Set<String> collectGrantedPerRecordWritePermissions(
            Collection<@RecordTypeIdentifier.RecordType Integer> recordTypeIds,
            AttributionSource attributionSource) {
        return recordTypeIds.stream()
                .map(mInternalHealthConnectMappings::getRecordHelper)
                .flatMap(recordHelper -> recordHelper.getAllPerRecordWritePermissions().stream())
                .distinct()
                .filter(permission -> isPermissionGranted(permission, attributionSource))
                .collect(toSet());
    }

    private void enforceWriteUdiPermission(AttributionSource attributionSource) {
        if (!isPermissionGranted(HealthPermissions.WRITE_DEVICE_UDI, attributionSource)) {
            throw new SecurityException(
                    "Caller requires "
                            + HealthPermissions.WRITE_DEVICE_UDI
                            + " to write Device UDI");
        }
    }

    private void enforceWritePermission(int recordTypeId, AttributionSource attributionSource) {
        Set<Integer> permissionCategories =
                mHealthConnectMappings.getHealthPermissionCategoriesForRecordType(recordTypeId);
        if (permissionCategories.isEmpty()) {
            throw new SecurityException("No permissions defined for record type " + recordTypeId);
        }

        List<String> permissionNames =
                permissionCategories.stream()
                        .map(mHealthConnectMappings::getHealthWritePermission)
                        .toList();

        // Shortcut general case of a single permission.
        if (permissionNames.size() == 1) {
            enforceWritePermission(permissionNames.get(0), attributionSource, recordTypeId);
            return;
        }

        // For a record type with multiple write permissions, at least one is required to be
        // granted. We can't use enforceAnyOfPermissions because we want to check each permission
        // individually and ignore SecurityException for denied ones.
        for (String permissionName : permissionNames) {
            try {
                enforceWritePermission(permissionName, attributionSource, recordTypeId);
                return;
            } catch (SecurityException e) {
                // Ignore and check the next permission.
            }
        }

        // If no permission was granted for this record type, throw an exception.
        throw new SecurityException(
                "Caller requires one of "
                        + permissionNames
                        + " to write record type "
                        + getExternalRecordClass(recordTypeId));
    }

    private void enforceWritePermission(
            String permissionName, AttributionSource attributionSource, int recordTypeId) {
        if (!isPermissionGranted(permissionName, attributionSource)) {
            throw new SecurityException(
                    "Caller requires "
                            + permissionName
                            + " to write record type "
                            + getExternalRecordClass(recordTypeId));
        }
    }

    private void enforceReadPermission(
            String permissionName, AttributionSource attributionSource, int recordTypeId) {
        if (!isPermissionGranted(permissionName, attributionSource)) {
            throw new SecurityException(
                    "Caller requires "
                            + permissionName
                            + " to read record type "
                            + getExternalRecordClass(recordTypeId));
        }

        // Deny access to HealthConnect API if READ_HEART_RATE is a split permission.
        if (SdkLevel.isAtLeastB() && permissionName.equals(READ_HEART_RATE)) {
            String packageName = attributionSource.getPackageName();
            if (packageName == null) {
                throw new SecurityException("Caller packageName is null");
            }

            UserHandle user = UserHandle.getUserHandleForUid(attributionSource.getUid());
            int permissionFlags =
                    mContext.getPackageManager()
                            .getPermissionFlags(permissionName, packageName, user);
            int targetSdk;
            try {
                targetSdk =
                        PackageInfoUtils.getPackageInfoUnchecked(
                                        packageName,
                                        user,
                                        PackageManager.PackageInfoFlags.of(0),
                                        mContext)
                                .applicationInfo
                                .targetSdkVersion;
            } catch (Exception e) {
                throw new SecurityException(
                        "Caller package is not found. Unable to determine permission state.");
            }

            if (HealthConnectPermissionHelper.isFromSplitPermission(permissionFlags, targetSdk)) {
                throw new SecurityException(
                        "Caller is requesting HealthConnect API access from "
                                + "an implicitly granted split permission: "
                                + permissionName
                                + getExternalRecordClass(recordTypeId));
            }
        }
    }

    private Class<? extends Record> getExternalRecordClass(int recordTypeId) {
        return requireNonNull(
                mHealthConnectMappings.getRecordIdToExternalRecordClassMap().get(recordTypeId));
    }

    /** Checks if the given permission is granted for the given {@link AttributionSource}. */
    public boolean isPermissionGranted(String permissionName, AttributionSource attributionSource) {
        return mPermissionManager.checkPermissionForDataDelivery(
                        permissionName, attributionSource, null)
                == PERMISSION_GRANTED;
    }
}
