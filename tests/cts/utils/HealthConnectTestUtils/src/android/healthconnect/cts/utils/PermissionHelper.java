/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.cts.utils;

import static android.Manifest.permission.GRANT_RUNTIME_PERMISSIONS;
import static android.Manifest.permission.PACKAGE_USAGE_STATS;
import static android.Manifest.permission.REVOKE_RUNTIME_PERMISSIONS;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE;
import static android.content.pm.PackageManager.GET_PERMISSIONS;

import static com.android.compatibility.common.util.SystemUtil.eventually;
import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.UiAutomation;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectManager;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;

import com.android.compatibility.common.util.SystemUtil;
import com.android.compatibility.common.util.ThrowingRunnable;
import com.android.compatibility.common.util.ThrowingSupplier;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PermissionHelper {

    /** Copy of hidden {@link android.health.connect.HealthPermissions#READ_EXERCISE_ROUTE}. */
    public static final String READ_EXERCISE_ROUTE_PERMISSION =
            "android.permission.health.READ_EXERCISE_ROUTE";

    /** Returns valid Health permissions declared in the Manifest of the given package. */
    public static List<String> getDeclaredHealthPermissions(String packageName) {
        return List.copyOf(getHealthPermissionFlags(packageName).keySet());
    }

    /** Returns all Health permissions that are granted to the specified package. */
    public static List<String> getGrantedHealthPermissions(String packageName) {
        ArrayList<String> permissions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : getHealthPermissionFlags(packageName).entrySet()) {
            if ((entry.getValue() & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                permissions.add(entry.getKey());
            }
        }
        return permissions;
    }

    /** Grants all health permissions to the app specified by {@code packageName}. */
    public static void grantAllHealthPermissions(String packageName) {
        for (Map.Entry<String, Integer> entry : getHealthPermissionFlags(packageName).entrySet()) {
            if ((entry.getValue() & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                grantHealthPermission(packageName, entry.getKey());
            }
        }
    }

    private static Map<String, Integer> getHealthPermissionFlags(String packageName) {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager packageManager = context.getPackageManager();

        PackageInfo packageInfo = getAppPackageInfo(packageManager, packageName);
        String[] requestedPermissions = packageInfo.requestedPermissions;
        int[] requestedPermissionsFlags = packageInfo.requestedPermissionsFlags;

        if (requestedPermissions == null || requestedPermissionsFlags == null) {
            return Map.of();
        }

        HashMap<String, Integer> flags = new HashMap<>();
        for (int i = 0; i < requestedPermissions.length; i++) {
            if (HealthConnectManager.isHealthPermission(context, requestedPermissions[i])) {
                flags.put(requestedPermissions[i], requestedPermissionsFlags[i]);
            }
        }
        return flags;
    }

    private static PackageInfo getAppPackageInfo(
            PackageManager packageManager, String packageName) {
        return runWithShellPermissionIdentity(
                () ->
                        packageManager.getPackageInfo(
                                packageName, PackageManager.PackageInfoFlags.of(GET_PERMISSIONS)));
    }

    /**
     * Grants the specified health permission to the app specified by {@code packageName}.
     *
     * <p>Permissions are granted via {@link PackageManager#grantRuntimePermission}, as {@link
     * HealthConnectManager#grantHealthPermission} is hidden and so can't be used by CTS. Unlike the
     * {@code HealthConnectManager} method, this does not modify any permission flags.
     */
    @SuppressLint("MissingPermission")
    public static void grantHealthPermission(String packageName, String permission) {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        UserHandle user = context.getUser();

        runWithShellPermissionIdentity(
                () -> packageManager.grantRuntimePermission(packageName, permission, user),
                GRANT_RUNTIME_PERMISSIONS);
    }

    /**
     * Grants the specified health permissions to the app specified by {@code packageName}.
     *
     * @see HealthConnectManager#grantHealthPermission(String, String)
     */
    public static void grantHealthPermissions(String packageName, Collection<String> permissions) {
        for (String permission : permissions) {
            grantHealthPermission(packageName, permission);
        }
    }

    /**
     * Revokes the specified health permission from the app specified by {@code packageName}.
     *
     * <p>Permissions are revoked via {@link PackageManager#revokeRuntimePermission}, as {@link
     * HealthConnectManager#revokeHealthPermission} is hidden and so can't be used by CTS. Unlike
     * the {@code HealthConnectManager} method, this does not modify any permission flags.
     */
    @SuppressLint("MissingPermission")
    public static void revokeHealthPermission(String packageName, String permission)
            throws PackageManager.NameNotFoundException {
        if (!getGrantedHealthPermissions(packageName).contains(permission)) {
            return;
        }

        Context context = ApplicationProvider.getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        UserHandle user = context.getUser();

        runWithShellPermissionIdentity(
                () -> packageManager.revokeRuntimePermission(packageName, permission, user),
                REVOKE_RUNTIME_PERMISSIONS);

        // Apps are killed following a revoke. Wait for this to ensure that it doesn't interfere
        // with subsequent interactions with the app.
        waitForNoRunningProcesses(packageName);
    }

    /**
     * Revokes all health permissions from the app specified by {@code packageName}.
     *
     * <p>Permissions are revoked via {@link PackageManager#revokeRuntimePermission}, as {@link
     * HealthConnectManager#revokeAllHealthPermissions} is hidden and so can't be used by CTS.
     * Unlike the {@code HealthConnectManager} method, this does not modify any permission flags.
     */
    @SuppressLint("MissingPermission")
    public static void revokeAllHealthPermissions(String packageName, String reason)
            throws PackageManager.NameNotFoundException {
        List<String> permissions = getGrantedHealthPermissions(packageName);
        if (permissions.isEmpty()) {
            return;
        }

        Context context = ApplicationProvider.getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        UserHandle user = context.getUser();

        runWithShellPermissionIdentity(
                () -> {
                    for (String permission : permissions) {
                        packageManager.revokeRuntimePermission(
                                packageName, permission, user, reason);
                    }
                },
                REVOKE_RUNTIME_PERMISSIONS);

        // Apps are killed following a revoke. Wait for this to ensure that it doesn't interfere
        // with subsequent interactions with the app.
        waitForNoRunningProcesses(packageName);
    }

    /** Revokes permission for the package for the duration of the runnable. */
    public static void runWithRevokedPermissions(
            String packageName, String permission, ThrowingRunnable runnable) throws Exception {
        runWithRevokedPermissions(
                (ThrowingSupplier<Void>)
                        () -> {
                            runnable.run();
                            return null;
                        },
                packageName,
                permission);
    }

    /** Revokes permission for the package for the duration of the supplier. */
    public static <T> T runWithRevokedPermission(
            String packageName, String permission, ThrowingSupplier<T> supplier) throws Exception {
        return runWithRevokedPermissions(supplier, packageName, permission);
    }

    /** Revokes permission for the package for the duration of the supplier. */
    public static <T> T runWithRevokedPermissions(
            ThrowingSupplier<T> supplier, String packageName, String... permissions)
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        checkArgument(
                !context.getPackageName().equals(packageName),
                "Can not be called on self, only on other apps");

        UiAutomation uiAutomation =
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                        .getUiAutomation();

        var grantedPermissions =
                Sets.intersection(
                        Set.copyOf(getGrantedHealthPermissions(packageName)), Set.of(permissions));

        try {
            grantedPermissions.forEach(
                    permission -> uiAutomation.revokeRuntimePermission(packageName, permission));
            return supplier.get();
        } finally {
            grantedPermissions.forEach(
                    permission -> uiAutomation.grantRuntimePermission(packageName, permission));
        }
    }

    /** Flags the permission as USER_FIXED for the duration of the supplier. */
    public static <T> T runWithUserFixedPermission(
            String packageName, String permission, ThrowingSupplier<T> supplier) throws Exception {
        SystemUtil.runShellCommand(
                String.format(
                        "pm set-permission-flags --user %d %s %s user-fixed",
                        UserHandle.myUserId(), packageName, permission));
        try {
            return supplier.get();
        } finally {
            SystemUtil.runShellCommand(
                    String.format(
                            "pm clear-permission-flags --user %d %s %s user-fixed",
                            UserHandle.myUserId(), packageName, permission));
        }
    }

    @SuppressLint("MissingPermission")
    private static void waitForNoRunningProcesses(String packageName)
            throws PackageManager.NameNotFoundException {
        Context context = ApplicationProvider.getApplicationContext();
        int uid = context.getPackageManager().getPackageUid(packageName, /* flags= */ 0);
        ActivityManager activityManager =
                requireNonNull(context.getSystemService(ActivityManager.class));

        runWithShellPermissionIdentity(
                () ->
                        eventually(
                                () ->
                                        assertThat(activityManager.getUidImportance(uid))
                                                .isEqualTo(IMPORTANCE_GONE)),
                PACKAGE_USAGE_STATS);
    }
}
