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

package com.android.server.healthconnect.migration;

import android.Manifest;
import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.health.connect.Constants;
import android.health.connect.HealthConnectManager;
import android.os.UserHandle;
import android.util.Slog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class contains methods to:
 *
 * <ul>
 *   <li>Filter and get the package names of migration aware apps present on the device. Migration
 *       aware apps are those that both hold {@link
 *       android.Manifest.permission#MIGRATE_HEALTH_CONNECT_DATA} and handle {@link
 *       android.health.connect.HealthConnectManager#ACTION_SHOW_MIGRATION_INFO}.
 *   <li>Send an explicit broadcast with action {@link
 *       android.health.connect.HealthConnectManager#ACTION_HEALTH_CONNECT_MIGRATION_READY} to
 *       migration aware apps to prompt them to start/continue HC data migration.
 * </ul>
 *
 * @hide
 */
public class MigrationBroadcast {

    private static final String TAG = "HealthConnectMigrationBroadcast";
    private final Context mContext;
    private final UserHandle mUser;

    /**
     * Constructs a {@link MigrationBroadcast} object.
     *
     * @param context the service context.
     * @param user the user to send the broadcasts to.
     */
    public MigrationBroadcast(@NonNull Context context, UserHandle user) {
        mContext = context;
        mUser = user;
    }

    /**
     * Sends a broadcast with action {@link
     * android.health.connect.HealthConnectManager#ACTION_HEALTH_CONNECT_MIGRATION_READY} to
     * applications which hold {@link android.Manifest.permission#MIGRATE_HEALTH_CONNECT_DATA} and
     * handle {@link android.health.connect.HealthConnectManager#ACTION_SHOW_MIGRATION_INFO}.
     */
    public void sendInvocationBroadcast() throws Exception {
        if (Constants.DEBUG) {
            Slog.d(TAG, "Calling sendInvocationBroadcast()");
        }

        List<String> permissionFilteredPackages = filterPermissions();
        List<String> filteredPackages = filterIntent(permissionFilteredPackages);

        int numPackages = filteredPackages.size();

        if (numPackages == 0) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "There are no migration aware apps");
            }
        } else if (numPackages == 1) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "Checking if package is installed on user");
            }

            if (isPackageInstalled(filteredPackages.get(0), mUser)) {
                Intent intent =
                        new Intent(HealthConnectManager.ACTION_HEALTH_CONNECT_MIGRATION_READY)
                                .setPackage(filteredPackages.get(0));
                mContext.sendBroadcastAsUser(intent, mUser);
                if (Constants.DEBUG) {
                    Slog.d(TAG, "Sent broadcast to migration aware application.");
                }
            } else {
                if (Constants.DEBUG) {
                    Slog.d(TAG, "Migration aware app is not installed on the current user");
                }
            }
        } else {
            // TODO(b/267255123): Explicitly check for certificate and only send to that if
            // that filters it down to one package name
            throw new Exception("Multiple packages are migration aware");
        }
    }

    /**
     * Filters and returns the package names of applications which hold permission {@link
     * android.Manifest.permission#MIGRATE_HEALTH_CONNECT_DATA}.
     *
     * @return List of filtered app package names which hold the specified permission
     */
    private List<String> filterPermissions() {
        if (Constants.DEBUG) {
            Slog.d(TAG, "Calling filterPermissions()");
        }

        String[] permissions = new String[] {Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA};

        List<PackageInfo> packageInfos =
                mContext.getPackageManager()
                        .getPackagesHoldingPermissions(
                                permissions, PackageManager.PackageInfoFlags.of(0));

        List<String> permissionFilteredPackages =
                packageInfos.stream().map(info -> info.packageName).collect(Collectors.toList());

        if (Constants.DEBUG) {
            Slog.d(TAG, "permissionFilteredPackages : " + permissionFilteredPackages);
        }
        return permissionFilteredPackages;
    }

    /**
     * Filters and returns the package names of applications which handle intent {@link
     * android.health.connect.HealthConnectManager#ACTION_SHOW_MIGRATION_INFO}.
     *
     * @param permissionFilteredPackages List of app package names holding permission {@link
     *     android.Manifest.permission#MIGRATE_HEALTH_CONNECT_DATA}
     * @return List of filtered app package names which handle the specified intent action
     */
    private List<String> filterIntent(List<String> permissionFilteredPackages) {
        if (Constants.DEBUG) {
            Slog.d(TAG, "Calling filterIntents()");
        }

        List<String> filteredPackages = new ArrayList<String>(permissionFilteredPackages.size());

        for (String packageName : permissionFilteredPackages) {

            if (Constants.DEBUG) {
                Slog.d(TAG, "Checking intent for package : " + packageName);
            }

            Intent intentToCheck =
                    new Intent(HealthConnectManager.ACTION_SHOW_MIGRATION_INFO)
                            .setPackage(packageName);

            ResolveInfo resolveResult =
                    mContext.getPackageManager()
                            .resolveActivity(
                                    intentToCheck,
                                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL));

            if (resolveResult != null) {
                filteredPackages.add(packageName);
            }
        }
        if (Constants.DEBUG) {
            Slog.d(TAG, "filteredPackages : " + filteredPackages);
        }
        return filteredPackages;
    }

    private boolean isPackageInstalled(String packageName, UserHandle user) {
        try {
            PackageManager packageManager =
                    mContext.createContextAsUser(user, 0).getPackageManager();
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0));
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
