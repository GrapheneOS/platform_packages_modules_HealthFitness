/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.healthconnect;

import static android.Manifest.permission.INTERACT_ACROSS_USERS_FULL;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.healthconnect.Constants;
import android.os.Binder;
import android.os.UserHandle;

import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/** A handler for HealthConnect permission-related logic. */
final class HealthConnectPermissionHelper {

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final Set<String> mHealthPermissions;

    /**
     * Constructs a {@link HealthConnectPermissionHelper}.
     *
     * @param context the service context.
     * @param packageManager a {@link PackageManager} instance.
     * @param healthPermissions a {@link Set} of permissions that are recognized as
     *     HealthConnect-defined permissions.
     */
    HealthConnectPermissionHelper(
            Context context, PackageManager packageManager, Set<String> healthPermissions) {
        mContext = context;
        mPackageManager = packageManager;
        mHealthPermissions = healthPermissions;
    }

    /** See {@link android.healthconnect.HealthConnectManager#grantHealthPermission}. */
    void grantHealthPermission(
            @NonNull String packageName, @NonNull String permissionName, @NonNull UserHandle user) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(permissionName);
        // TODO(b/249527134): Add a check to ensure the SHOW_HEALTH_PERMISSION_RATIONALE
        //   intent-filter is listed in the target app's manifest before granting permissions.
        enforceManageHealthPermissions(/* message= */ "grantHealthPermission");
        enforceValidPermission(permissionName);
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName);
        final long token = Binder.clearCallingIdentity();
        try {
            mPackageManager.grantRuntimePermission(packageName, permissionName, checkedUser);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link android.healthconnect.HealthConnectManager#revokeHealthPermission}. */
    void revokeHealthPermission(
            @NonNull String packageName,
            @NonNull String permissionName,
            @Nullable String reason,
            @NonNull UserHandle user) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(permissionName);
        enforceManageHealthPermissions(/* message= */ "revokeHealthPermission");
        enforceValidPermission(permissionName);
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName);
        final long token = Binder.clearCallingIdentity();
        try {
            mPackageManager.revokeRuntimePermission(
                    packageName, permissionName, checkedUser, reason);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link android.healthconnect.HealthConnectManager#revokeAllHealthPermissions}. */
    void revokeAllHealthPermissions(
            @NonNull String packageName, @Nullable String reason, @NonNull UserHandle user) {
        Objects.requireNonNull(packageName);
        enforceManageHealthPermissions(/* message= */ "revokeAllHealthPermissions");
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName);
        final long token = Binder.clearCallingIdentity();
        try {
            revokeAllHealthPermissionsUnchecked(packageName, checkedUser, reason);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link android.healthconnect.HealthConnectManager#getGrantedHealthPermissions}. */
    List<String> getGrantedHealthPermissions(
            @NonNull String packageName, @NonNull UserHandle user) {
        Objects.requireNonNull(packageName);
        enforceManageHealthPermissions(/* message= */ "getGrantedHealthPermissions");
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName);
        final long token = Binder.clearCallingIdentity();
        try {
            return getGrantedHealthPermissionsUnchecked(packageName, checkedUser);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private List<String> getGrantedHealthPermissionsUnchecked(String packageName, UserHandle user) {
        PackageInfo packageInfo;
        try {
            PackageManager packageManager =
                    mContext.createContextAsUser(user, /* flags= */ 0).getPackageManager();
            packageInfo =
                    packageManager.getPackageInfo(
                            packageName,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Invalid package", e);
        }
        List<String> grantedHealthPerms = new ArrayList<>(packageInfo.requestedPermissions.length);
        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
            String currPerm = packageInfo.requestedPermissions[i];
            if (mHealthPermissions.contains(currPerm)
                    && ((packageInfo.requestedPermissionsFlags[i]
                                    & PackageInfo.REQUESTED_PERMISSION_GRANTED)
                            != 0)) {
                grantedHealthPerms.add(currPerm);
            }
        }
        return grantedHealthPerms;
    }

    private void revokeAllHealthPermissionsUnchecked(
            String packageName, UserHandle user, String reason) {
        List<String> grantedHealthPermissions =
                getGrantedHealthPermissionsUnchecked(packageName, user);
        for (String perm : grantedHealthPermissions) {
            mPackageManager.revokeRuntimePermission(packageName, perm, user, reason);
        }
    }

    private void enforceValidPermission(String permissionName) {
        if (!mHealthPermissions.contains(permissionName)) {
            throw new IllegalArgumentException("invalid health permission");
        }
    }

    private void enforceValidPackage(String packageName) {
        try {
            mPackageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0));
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("invalid package", e);
        }
    }

    private void enforceManageHealthPermissions(String message) {
        mContext.enforceCallingOrSelfPermission(Constants.MANAGE_HEALTH_PERMISSIONS_NAME, message);
    }

    /**
     * Returns the target userId after handling the incoming user for packages with {@link
     * INTERACT_ACROSS_USERS_FULL}.
     *
     * @throws java.lang.SecurityException if the caller is affecting different users without
     *     holding the {@link INTERACT_ACROSS_USERS_FULL} permission.
     */
    private int handleIncomingUser(int userId) {
        int callingUserId = UserHandle.getUserHandleForUid(Binder.getCallingUid()).getIdentifier();
        if (userId == callingUserId) {
            return userId;
        }

        boolean canInteractAcrossUsersFull =
                mContext.checkCallingPermission(INTERACT_ACROSS_USERS_FULL)
                        == PackageManager.PERMISSION_GRANTED;
        if (canInteractAcrossUsersFull) {
            if (userId == UserHandle.CURRENT.getIdentifier()) {
                return ActivityManager.getCurrentUser();
            }
            return userId;
        }

        throw new SecurityException(
                "Permission denied. Need to run as either the calling user id ("
                        + callingUserId
                        + "), or with "
                        + INTERACT_ACROSS_USERS_FULL
                        + " permission");
    }
}
