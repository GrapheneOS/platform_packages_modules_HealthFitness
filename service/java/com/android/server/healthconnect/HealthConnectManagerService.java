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

import static java.util.Collections.emptySet;

import android.content.Context;
import android.healthconnect.aidl.IHealthConnectService;

import com.android.server.SystemService;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.util.Slog;

import android.os.UserHandle;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionInfo;
import android.healthconnect.Constants;

import java.util.Set;
import java.util.List;
import java.util.HashSet;

/**
 * @hide HealthConnect system service scaffold. *
 */
public class HealthConnectManagerService extends SystemService {
    private static final String TAG = "HealthConnectManagerService";
    private static final boolean DEBUG = false;

    private final HealthConnectPermissionHelper mPermissionHelper;

    public HealthConnectManagerService(Context context) {
        super(context);
        PackageManager packageManager = context.getPackageManager();
        mPermissionHelper =
                new HealthConnectPermissionHelper(
                        context, packageManager, getDefinedHealthPerms(packageManager));
    }

    @Override
    public void onStart() {
        publishBinderService(Context.HEALTHCONNECT_SERVICE, new HealthConnectServiceImpl());
    }

    private final class HealthConnectServiceImpl extends IHealthConnectService.Stub {

        @Override
        public void grantHealthPermission(
                @NonNull String packageName,
                @NonNull String permissionName,
                @NonNull UserHandle user) {
            mPermissionHelper.grantHealthPermission(packageName, permissionName, user);
        }

        @Override
        public void revokeHealthPermission(
                @NonNull String packageName,
                @NonNull String permissionName,
                @Nullable String reason,
                @NonNull UserHandle user) {
            mPermissionHelper.revokeHealthPermission(packageName, permissionName, reason, user);
        }

        @Override
        public void revokeAllHealthPermissions(
                @NonNull String packageName, @Nullable String reason, @NonNull UserHandle user) {
            mPermissionHelper.revokeAllHealthPermissions(packageName, reason, user);
        }

        @Override
        public List<String> getGrantedHealthPermissions(
                @NonNull String packageName, @NonNull UserHandle user) {
            return mPermissionHelper.getGrantedHealthPermissions(packageName, user);
        }
    }

    /**
     * Returns a set of health permissions defined within the module and belonging to {@link
     * Constants.HEALTH_PERMISSION_GROUP_NAME}.
     *
     * <p><b>Note:</b> If we, for some reason, fail to retrieve these, we return an empty set rather
     * than crashing the device. This means the health permissions infra will be inactive.
     */
    private static Set<String> getDefinedHealthPerms(PackageManager packageManager) {
        PermissionInfo[] permissionInfos =
                getHealthPermissionControllerPermissionInfos(packageManager);
        if (permissionInfos == null) {
            // This should never happen. But if it does, let's mark our permissions infra as
            //   inactive. At least users can use other parts of their phone.
            return emptySet();
        }

        Set<String> definedHealthPerms = new HashSet<>(permissionInfos.length);
        for (PermissionInfo permInfo : permissionInfos) {
            if (Constants.HEALTH_PERMISSION_GROUP_NAME.equals(permInfo.group)) {
                definedHealthPerms.add(permInfo.name);
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "Defined health permissions: " + definedHealthPerms.toString());
        }
        return definedHealthPerms;
    }

    /**
     * Returns a list of permissions defined in the health permission controller APK, {@code null}
     * if it could not be retrieved.
     */
    @Nullable
    private static PermissionInfo[] getHealthPermissionControllerPermissionInfos(
            PackageManager packageManager) {
        PackageInfo packageInfo;
        String healthConnectControllerPackageName = null;
        try {
            healthConnectControllerPackageName =
                    packageManager.getPermissionInfo(
                                    Constants.MANAGE_HEALTH_PERMISSIONS_NAME, /* flags= */ 0)
                            .packageName;
            packageInfo =
                    packageManager.getPackageInfo(
                            healthConnectControllerPackageName,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        } catch (PackageManager.NameNotFoundException e) {
            // This should never happen. But if it does, let's log it and return null
            if (healthConnectControllerPackageName == null) {
                // We couldn't find the permission
                Slog.e(
                        TAG,
                        "HealthConnect permission"
                                + Constants.MANAGE_HEALTH_PERMISSIONS_NAME
                                + ") not found");
            } else {
                // we couldn't find the package
                Slog.e(
                        TAG,
                        "HealthConnect permissions APK ("
                                + healthConnectControllerPackageName
                                + ") not found");
            }
            return null;
        }
        if (packageInfo.permissions == null) {
            // This should never happen. But if it does, let's log it and return null.
            Slog.e(
                    TAG,
                    "No HealthConnect permissions defined in APK ("
                            + healthConnectControllerPackageName
                            + ")");
            return null;
        }
        return packageInfo.permissions;
    }
}
