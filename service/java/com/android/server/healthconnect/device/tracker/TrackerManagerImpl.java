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

package com.android.server.healthconnect.device.tracker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthPermissions;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tracker that subscribes to SensorManager pedometer.
 *
 * @hide
 */
public class TrackerManagerImpl implements TrackerManager {

    private static final String TAG = "HealthConnectTrackerManagerImpl";

    private final HealthConnectPermissionHelper mPermissionHelper;

    public TrackerManagerImpl(HealthConnectPermissionHelper permissionHelper) {
        mPermissionHelper = permissionHelper;
    }

    @Override
    public void initialize() {
        if (Flags.stepTrackingEnabled()) {
            // Implementation goes here. Do nothing for now.
        }
    }

    @Override
    public void setStepTrackingEnabled(boolean enabled) {
        if (Flags.stepTrackingEnabled()) {
            // Implementation goes here. Do nothing for now.
        }
    }

    /**
     * Returns the package names of applications which hold permission {@link
     * HealthPermissions.READ_STEPS}.
     *
     * @return List of app package names which hold the {@link HealthPermissions.READ_STEPS}
     *     permission.
     */
    @VisibleForTesting
    static List<String> packagesEligibleForStepTracking(
            Context context, HealthConnectPermissionHelper mPermissionHelper) {
        if (android.health.connect.Constants.DEBUG) {
            Slog.d(TAG, "Calling packagesEligibleForStepTracking()");
        }

        String[] permissions = new String[] {HealthPermissions.READ_STEPS};
        List<PackageInfo> packageInfos =
                context.getPackageManager()
                        .getPackagesHoldingPermissions(
                                permissions, PackageManager.PackageInfoFlags.of(0));

        // Get app package names and filter out any system apps pre-granted READ_STEPS as step
        // tracking is initialized for them separately.
        List<String> permissionFilteredPackages =
                packageInfos.stream()
                        .map(info -> info.packageName)
                        .filter(
                                packageName ->
                                        hasUserGrantedStepsPermission(
                                                context, mPermissionHelper, packageName))
                        .collect(Collectors.toList());

        if (android.health.connect.Constants.DEBUG) {
            Slog.d(TAG, "permissionFilteredPackages : " + permissionFilteredPackages);
        }

        return permissionFilteredPackages;
    }

    /**
     * Checks that the {@link HealthPermissions.READ_STEPS} permission is not pre-granted by the
     * system for the specified package.
     *
     * <p>The flag {@link PackageManager.FLAG_PERMISSION_GRANTED_BY_DEFAULT} is set for
     * pre-installed apps pre-granted with the permission so if this flag is set, the method will
     * return false.
     */
    private static boolean hasUserGrantedStepsPermission(
            Context context, HealthConnectPermissionHelper mPermissionHelper, String packageName) {
        int flag =
                mPermissionHelper.getHealthPermissionFlags(
                        packageName, context.getUser(), HealthPermissions.READ_STEPS);
        boolean isPregrantedPermission =
                (flag & PackageManager.FLAG_PERMISSION_GRANTED_BY_DEFAULT) != 0;

        if (android.health.connect.Constants.DEBUG && isPregrantedPermission) {
            Slog.d(TAG, "Filtering out pre-granted package : " + packageName);
        }

        return !isPregrantedPermission;
    }
}
