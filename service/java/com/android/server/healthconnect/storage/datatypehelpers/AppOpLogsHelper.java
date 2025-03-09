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

package com.android.server.healthconnect.storage.datatypehelpers;

import static android.app.AppOpsManager.HISTORY_FLAG_GET_ATTRIBUTION_CHAINS;
import static android.app.AppOpsManager.OP_FLAG_SELF;
import static android.app.AppOpsManager.OP_FLAG_TRUSTED_PROXIED;
import static android.app.AppOpsManager.OP_FLAG_TRUSTED_PROXY;

import android.annotation.NonNull;
import android.app.AppOpsManager;
import android.app.AppOpsManager.AttributedOpEntry;
import android.app.AppOpsManager.HistoricalOp;
import android.app.AppOpsManager.HistoricalOps;
import android.app.AppOpsManager.HistoricalOpsRequest;
import android.app.AppOpsManager.HistoricalPackageOps;
import android.app.AppOpsManager.HistoricalUidOps;
import android.content.pm.PackageManager;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A helper class to fetch and store the access logs from AppOps.
 *
 * @hide
 */
public final class AppOpLogsHelper {
    private static final String TAG = "AppOpLogsHelper";
    private static final int AGGREGATE_DATA_FILTER_BEGIN_DAYS_1 = 1;
    private static final int AGGREGATE_DATA_FILTER_BEGIN_DAYS_7 = 7;

    private final AppOpsManager mAppOpsManager;
    private final PackageManager mPackageManager;
    private final Set<String> mGranularHealthAppOps;

    public AppOpLogsHelper(
            AppOpsManager appOpsManager,
            PackageManager packageManager,
            Set<String> healthPermissions) {
        mAppOpsManager = appOpsManager;
        mPackageManager = packageManager;
        mGranularHealthAppOps =
                healthPermissions.stream()
                        .filter(
                                permissionName -> {
                                    String appOp = AppOpsManager.permissionToOp(permissionName);
                                    // AppOp may be null if the permission is introduced but
                                    // disabled (i.e. READ_ACTIVITY_INTENSITY).
                                    return appOp != null
                                            && !appOp.equals(
                                                    AppOpsManager.OPSTR_READ_WRITE_HEALTH_DATA);
                                })
                        .map(AppOpsManager::permissionToOp)
                        .collect(Collectors.toSet());
    }

    /**
     * Reads from AppOps to fetch historical usage of health permissions.
     *
     * @return a list of {@link AccessLog} converted from AppOps historical usages.
     */
    public List<AccessLog> getAccessLogsFromAppOps(UserHandle callingUserHandle) {
        if (mGranularHealthAppOps.isEmpty()) {
            return new ArrayList<>();
        }

        // Create request.

        // Begin / End times are kept in sync with PermissionController.
        int aggregateDataFilterBeginDays =
                isWatch() ? AGGREGATE_DATA_FILTER_BEGIN_DAYS_1 : AGGREGATE_DATA_FILTER_BEGIN_DAYS_7;
        long beginTimeMillis =
                Math.max(
                        System.currentTimeMillis()
                                - TimeUnit.DAYS.toMillis(aggregateDataFilterBeginDays),
                        Instant.EPOCH.toEpochMilli());
        long endTimeMillis = Long.MAX_VALUE;
        List<String> opNamesToQuery = new ArrayList<>(mGranularHealthAppOps);
        final AppOpsManager.HistoricalOpsRequest request =
                new HistoricalOpsRequest.Builder(beginTimeMillis, endTimeMillis)
                        .setOpNames(opNamesToQuery)
                        .setFlags(
                                AppOpsManager.OP_FLAG_SELF | AppOpsManager.OP_FLAG_TRUSTED_PROXIED)
                        .setHistoryFlags(
                                AppOpsManager.HISTORY_FLAG_DISCRETE
                                        | HISTORY_FLAG_GET_ATTRIBUTION_CHAINS)
                        .build();
        // Get Historical Ops.
        final LinkedBlockingQueue<HistoricalOps> historicalOpsQueue = new LinkedBlockingQueue<>(1);
        mAppOpsManager.getHistoricalOps(
                request,
                Runnable::run,
                (HistoricalOps ops) -> {
                    if (!historicalOpsQueue.offer(ops)) {
                        Slog.e(TAG, "Failed to put historical ops into queue");
                    }
                });
        HistoricalOps histOps;
        try {
            // TODO: b/364643016 - This seems like a long delay?
            // TODO: b/364643016 - Explore moving this to an async call.
            histOps = historicalOpsQueue.poll(30, TimeUnit.SECONDS);
            if (histOps == null) {
                Slog.e(TAG, "Historical ops query timed out");
                return new ArrayList<>();
            }
        } catch (InterruptedException ignored) {
            Slog.e(TAG, "Historical ops query interrupted");
            Thread.currentThread().interrupt(); // Restore the interrupted status
            return new ArrayList<>();
        }

        List<AccessLog> logs = new ArrayList<>();
        // Generate HealthConnect access logs from AppOps.
        for (int uidIdx = 0; uidIdx < histOps.getUidCount(); uidIdx++) {
            HistoricalUidOps uidOps = histOps.getUidOpsAt(uidIdx);

            // Filter out any app ops from a different user.
            UserHandle opsUserHandle = UserHandle.getUserHandleForUid(uidOps.getUid());
            if (!opsUserHandle.equals(callingUserHandle)) {
                continue;
            }

            for (int pkgIdx = 0; pkgIdx < uidOps.getPackageCount(); pkgIdx++) {
                final HistoricalPackageOps packageOps = uidOps.getPackageOpsAt(pkgIdx);
                String packageName = packageOps.getPackageName();
                for (String opName : mGranularHealthAppOps) {
                    HistoricalOp historicalOp = packageOps.getOp(opName);
                    if (historicalOp == null) {
                        continue;
                    }
                    int recordType = opNameToRecordType(opName);
                    if (recordType == RecordTypeIdentifier.RECORD_TYPE_UNKNOWN) {
                        continue;
                    }
                    @RecordTypeIdentifier.RecordType
                    List<Integer> recordTypes = Arrays.asList(recordType);
                    for (int i = 0; i < historicalOp.getDiscreteAccessCount(); i++) {
                        AttributedOpEntry attributedOpEntry = historicalOp.getDiscreteAccessAt(i);
                        logs.add(
                                new AccessLog(
                                        packageName,
                                        recordTypes,
                                        attributedOpEntry.getLastAccessTime(
                                                OP_FLAG_SELF
                                                        | OP_FLAG_TRUSTED_PROXIED
                                                        | OP_FLAG_TRUSTED_PROXY),
                                        AccessLog.OperationType.OPERATION_TYPE_READ));
                    }
                }
            }
        }
        return logs;
    }

    private boolean isWatch() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH);
    }

    /** Returns all record types that are associated with a granular AppOp. */
    public Set<Integer> getRecordsWithSystemAppOps() {
        return mGranularHealthAppOps.stream()
                .map(AppOpLogsHelper::opNameToRecordType)
                .collect(Collectors.toSet());
    }

    /** Returns the RecordTypeIdentifier for the given AppOp name. */
    @VisibleForTesting
    int getGranularRecordType(String opName) {
        if (!mGranularHealthAppOps.contains(opName)) {
            return RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;
        }

        return opNameToRecordType(opName);
    }

    @VisibleForTesting
    static int opNameToRecordType(String opName) {
        switch (opName) {
            case AppOpsManager.OPSTR_READ_HEART_RATE:
                return RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
            case AppOpsManager.OPSTR_READ_OXYGEN_SATURATION:
                return RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION;
            case AppOpsManager.OPSTR_READ_SKIN_TEMPERATURE:
                return RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE;
            default:
                return RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;
        }
    }
}
