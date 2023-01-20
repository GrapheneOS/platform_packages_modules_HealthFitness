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

import static com.android.server.healthconnect.migration.MigrationConstants.HC_PACKAGE_NAME_CONFIG_NAME;
import static com.android.server.healthconnect.migration.MigrationUtils.filterIntent;
import static com.android.server.healthconnect.migration.MigrationUtils.filterPermissions;

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.health.connect.Constants;
import android.health.connect.HealthConnectManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;

import java.util.List;
import java.util.Objects;

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

        String hcMigratorPackage =
                mContext.getResources()
                        .getString(
                                Resources.getSystem()
                                        .getIdentifier(HC_PACKAGE_NAME_CONFIG_NAME, null, null));
        String migrationAwarePackage;

        List<String> permissionFilteredPackages = filterPermissions(mContext);
        List<String> filteredPackages = filterIntent(mContext, permissionFilteredPackages);

        int numPackages = filteredPackages.size();

        if (numPackages == 0) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "There are no migration aware apps");
            }
            return;
        } else if (numPackages == 1) {
            if (Objects.equals(hcMigratorPackage, filteredPackages.get(0))) {
                migrationAwarePackage = filteredPackages.get(0);
            } else {
                throw new Exception("Migration aware app is not Health Connect");
            }
        } else {
            if (filteredPackages.contains(hcMigratorPackage)) {
                migrationAwarePackage = hcMigratorPackage;
            } else {
                throw new Exception("Multiple packages are migration aware");
            }
        }

        if (Constants.DEBUG) {
            Slog.d(TAG, "Checking if migration aware package is installed on user");
        }

        if (isPackageInstalled(migrationAwarePackage, mUser)) {
            if (Objects.requireNonNull(mContext.getSystemService(UserManager.class))
                    .isUserRunning(mUser)) {
                // TODO(b/271951037): Use explicit intent by setting the target component
                Intent intent =
                        new Intent(HealthConnectManager.ACTION_HEALTH_CONNECT_MIGRATION_READY)
                                .setPackage(migrationAwarePackage);
                mContext.sendBroadcastAsUser(intent, mUser);
                if (Constants.DEBUG) {
                    Slog.d(TAG, "Sent broadcast to migration aware application.");
                }
            } else if (Constants.DEBUG) {
                Slog.d(TAG, "User " + mUser + " is not currently active");
            }
        } else if (Constants.DEBUG) {
            Slog.d(TAG, "Migration aware app is not installed on the current user");
        }
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
