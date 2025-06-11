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

package com.android.server.healthconnect.permission;

import static android.content.pm.PackageManager.GET_PERMISSIONS;

import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectManager;
import android.os.UserHandle;
import android.util.Slog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility class with PackageInfo-related methods for {@link FirstGrantTimeManager}
 *
 * @hide
 */
public final class PackageInfoUtils {
    private static final String TAG = "HCPackageInfoUtils";

    public PackageInfoUtils() {}

    public List<PackageInfo> getPackagesHoldingHealthPermissions(UserHandle user, Context context) {
        // TODO(b/260707328): replace with getPackagesHoldingPermissions
        List<PackageInfo> allInfos =
                getPackageManagerAsUser(context, user)
                        .getInstalledPackages(PackageManager.PackageInfoFlags.of(GET_PERMISSIONS));
        List<PackageInfo> healthAppsInfos = new ArrayList<>();

        for (PackageInfo info : allInfos) {
            if (anyRequestedHealthPermissionGranted(context, info)) {
                healthAppsInfos.add(info);
            }
        }
        return healthAppsInfos;
    }

    public List<PackageInfo> getPackagesCompatibleWithHealthConnect(
            Context context, UserHandle user) {
        List<PackageInfo> allInfos =
                getPackageManagerAsUser(context, user)
                        .getInstalledPackages(PackageManager.PackageInfoFlags.of(GET_PERMISSIONS));
        List<PackageInfo> healthAppsInfos = new ArrayList<>();

        for (PackageInfo info : allInfos) {
            if (hasRequestedHealthPermission(context, info)) {
                healthAppsInfos.add(info);
            }
        }
        return healthAppsInfos;
    }

    boolean hasGrantedHealthPermissions(String[] packageNames, UserHandle user, Context context) {
        for (String packageName : packageNames) {
            PackageInfo info = getPackageInfoWithPermissionsAsUser(packageName, user, context);
            if (info != null && anyRequestedHealthPermissionGranted(context, info)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    String[] getPackagesForUid(Context context, UserHandle user, int packageUid) {
        return getPackageManagerAsUser(context, user).getPackagesForUid(packageUid);
    }

    String[] getPackagesForUidNonNull(Context context, UserHandle user, int packageUid) {
        String[] packages = getPackagesForUid(context, user, packageUid);
        return packages != null ? packages : new String[] {};
    }

    /**
     * Checks if the given package had any read/write permissions to Health Connect.
     *
     * @param context Context
     * @param packageInfo Package to check
     * @return If the given package is connected to Health Connect.
     */
    private static boolean anyRequestedHealthPermissionGranted(
            Context context, PackageInfo packageInfo) {
        if (packageInfo.requestedPermissions == null) {
            return false;
        }
        Set<String> healthPermissions = HealthConnectManager.getHealthPermissions(context);

        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
            String currPerm = packageInfo.requestedPermissions[i];
            if (healthPermissions.contains(currPerm)
                    && ((packageInfo.requestedPermissionsFlags[i]
                                    & PackageInfo.REQUESTED_PERMISSION_GRANTED)
                            != 0)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public PackageInfo getPackageInfoWithPermissionsAsUser(
            String packageName, UserHandle user, Context context) {
        try {
            return getPackageManagerAsUser(context, user)
                    .getPackageInfo(
                            packageName, PackageManager.PackageInfoFlags.of(GET_PERMISSIONS));
        } catch (PackageManager.NameNotFoundException e) {
            // App not found.
            Slog.e(TAG, "NameNotFoundException for " + packageName);
            return null;
        }
    }

    @Nullable
    String getSharedUserNameFromUid(int uid, Context context) {
        UserHandle user = UserHandle.getUserHandleForUid(uid);
        PackageManager packageManager = getPackageManagerAsUser(context, user);
        String[] packages = packageManager.getPackagesForUid(uid);
        if (packages == null || packages.length == 0) {
            Slog.e(TAG, "Can't get package names for UID: " + uid);
            return null;
        }
        try {
            PackageInfo info =
                    packageManager.getPackageInfo(
                            packages[0], PackageManager.PackageInfoFlags.of(0));
            return info.sharedUserId;
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "Package " + packages[0] + " not found.");
            return null;
        }
    }

    @Nullable
    Integer getPackageUid(String packageName, UserHandle user, Context context) {
        Integer uid = null;
        try {
            uid =
                    getPackageManagerAsUser(context, user)
                            .getPackageUid(
                                    packageName,
                                    PackageManager.PackageInfoFlags.of(/* flags= */ 0));
        } catch (PackageManager.NameNotFoundException e) {
            Slog.e(TAG, "NameNotFound exception for " + packageName);
        }
        return uid;
    }

    /**
     * Returns the list of health permissions granted to a given package name. It does not check if
     * the given package name is valid.
     */
    public static List<String> getGrantedHealthPermissions(
            Context context, String packageName, UserHandle user) {
        PackageInfo packageInfo =
                getPackageInfoUnchecked(
                        packageName,
                        user,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS),
                        context);

        return getGrantedHealthPermissions(context, packageInfo);
    }

    /** Returns the list of health permissions granted to the given {@link PackageInfo}. */
    public static List<String> getGrantedHealthPermissions(
            Context context, PackageInfo packageInfo) {
        Set<String> healthPermissions = HealthConnectManager.getHealthPermissions(context);

        if (packageInfo.requestedPermissions == null) {
            return List.of();
        }

        List<String> grantedHealthPerms = new ArrayList<>(packageInfo.requestedPermissions.length);
        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
            String currPerm = packageInfo.requestedPermissions[i];
            if (packageInfo.requestedPermissionsFlags != null
                    && healthPermissions.contains(currPerm)
                    && ((packageInfo.requestedPermissionsFlags[i]
                                    & PackageInfo.REQUESTED_PERMISSION_GRANTED)
                            != 0)) {
                grantedHealthPerms.add(currPerm);
            }
        }
        return grantedHealthPerms;
    }

    /**
     * Returns the list of {@link PackageInfo} for a given package. It does not check if the given
     * package name is valid.
     */
    public static PackageInfo getPackageInfoUnchecked(
            String packageName,
            UserHandle user,
            PackageManager.PackageInfoFlags flags,
            Context context) {
        try {
            return getPackageManagerAsUser(context, user).getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("invalid package", e);
        }
    }

    private static PackageManager getPackageManagerAsUser(Context context, UserHandle user) {
        return context.createContextAsUser(user, /* flags */ 0).getPackageManager();
    }

    private boolean hasRequestedHealthPermission(Context context, PackageInfo packageInfo) {
        if (packageInfo == null || packageInfo.requestedPermissions == null) {
            return false;
        }

        Set<String> healthPermissions = HealthConnectManager.getHealthPermissions(context);
        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {
            if (healthPermissions.contains(packageInfo.requestedPermissions[i])) {
                return true;
            }
        }
        return false;
    }
}
