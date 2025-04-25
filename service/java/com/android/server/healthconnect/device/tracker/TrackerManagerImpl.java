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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tracker that subscribes to SensorManager pedometer.
 *
 * @hide
 */
public class TrackerManagerImpl implements TrackerManager {

    private static final String TAG = "HealthConnectTrackerManagerImpl";

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
    static List<String> packagesEligibleForStepTracking(Context context) {
        if (android.health.connect.Constants.DEBUG) {
            Slog.d(TAG, "Calling packagesEligibleForStepTracking()");
        }

        String[] permissions = new String[] {HealthPermissions.READ_STEPS};
        List<PackageInfo> packageInfos =
                context.getPackageManager()
                        .getPackagesHoldingPermissions(
                                permissions, PackageManager.PackageInfoFlags.of(0));

        // TODO(b/412626578): Filter out any preinstalled apps holding the steps permission and
        // handle them separately.
        List<String> permissionFilteredPackages =
                packageInfos.stream().map(info -> info.packageName).collect(Collectors.toList());

        if (android.health.connect.Constants.DEBUG) {
            Slog.d(TAG, "permissionFilteredPackages : " + permissionFilteredPackages);
        }

        return permissionFilteredPackages;
    }
}
