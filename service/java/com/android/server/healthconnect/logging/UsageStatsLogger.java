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

package com.android.server.healthconnect.logging;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PERMISSION_STATS;

import static com.android.healthfitness.flags.Flags.personalHealthRecordTelemetry;

import android.health.HealthFitnessStatsLog;

import com.android.healthfitness.flags.Flags;

import java.util.List;
import java.util.Map;

/**
 * Logs Health Connect usage stats.
 *
 * @hide
 */
final class UsageStatsLogger {

    private final HealthFitnessStatsLog mStatsLog;

    UsageStatsLogger(HealthFitnessStatsLog statsLog) {
        mStatsLog = statsLog;
    }

    /** Write Health Connect usage stats to statsd. */
    void log(UsageStatsCollector usageStatsCollector) {
        usageStatsCollector.upsertLastAccessLogTimeStamp();
        Map<String, List<String>> packageNameToPermissionsGranted =
                usageStatsCollector.getPackagesHoldingHealthPermissions();
        int numberOfConnectedApps = packageNameToPermissionsGranted.size();
        int numberOfAvailableApps =
                usageStatsCollector.getNumberOfAppsCompatibleWithHealthConnect();
        boolean isUserMonthlyActive = usageStatsCollector.isUserMonthlyActive();

        // If this condition is true then the user does not uses HC and we should not collect data.
        // This will reduce the load on logging service otherwise we will get daily data from
        // billions of Android devices.
        if (numberOfConnectedApps == 0 && numberOfAvailableApps == 0 && !isUserMonthlyActive) {
            return;
        }

        logExportImportStats(usageStatsCollector);
        logPermissionStats(packageNameToPermissionsGranted);
        logPhrStats(usageStatsCollector);

        mStatsLog.write(
                HealthFitnessStatsLog.HEALTH_CONNECT_USAGE_STATS,
                numberOfConnectedApps,
                numberOfAvailableApps,
                isUserMonthlyActive);
    }

    private void logPhrStats(UsageStatsCollector usageStatsCollector) {
        if (!personalHealthRecordTelemetry()) {
            return;
        }

        int medicalDataSourcesCount = usageStatsCollector.getMedicalDataSourcesCount();
        int medicalResourcesCount = usageStatsCollector.getMedicalResourcesCount();
        mStatsLog.write(
                HealthFitnessStatsLog.HEALTH_CONNECT_PHR_USAGE_STATS,
                medicalDataSourcesCount,
                medicalResourcesCount,
                usageStatsCollector.isPhrMonthlyActiveUser(),
                (int) usageStatsCollector.getGrantedPhrAppsCount());
    }

    void logExportImportStats(UsageStatsCollector usageStatsCollector) {
        int exportFrequency = usageStatsCollector.getExportFrequency();
        mStatsLog.write(
                HealthFitnessStatsLog.HEALTH_CONNECT_EXPORT_IMPORT_STATS_REPORTED, exportFrequency);
    }

    void logPermissionStats(Map<String, List<String>> packageNameToPermissionsGranted) {

        if (!Flags.permissionMetrics()) {
            return;
        }

        for (Map.Entry<String, List<String>> connectedAppToPermissionsGranted :
                packageNameToPermissionsGranted.entrySet()) {

            List<String> grantedPermissions = connectedAppToPermissionsGranted.getValue();

            // This is done to remove the common prefix android.permission.health from all
            // permissions
            String[] grantedPermissionsShortened = new String[grantedPermissions.size()];
            for (int permissionIndex = 0;
                    permissionIndex < grantedPermissions.size();
                    permissionIndex++) {
                String grantedPermission = grantedPermissions.get(permissionIndex);
                grantedPermissionsShortened[permissionIndex] =
                        grantedPermission.substring(grantedPermission.lastIndexOf('.') + 1);
            }

            mStatsLog.write(
                    HEALTH_CONNECT_PERMISSION_STATS,
                    connectedAppToPermissionsGranted.getKey(),
                    grantedPermissionsShortened);
        }
    }
}
