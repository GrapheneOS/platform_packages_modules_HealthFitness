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

package com.android.server.healthconnect.common.logging;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__LAST_ERROR_CODE__NATIVE_TRACKING_ERROR_CODE_UNSPECIFIED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__NATIVE_DATA_TYPES_ACTIVE__NATIVE_TRACKING_DATA_TYPE_STEPS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__NATIVE_DATA_TYPES_DISABLED__NATIVE_TRACKING_DATA_TYPE_STEPS;

import static com.android.server.healthconnect.device.DeviceRecordHelper.DEVICE_DATA_PROVIDER_PACKAGE;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.health.connect.HealthPermissions;
import android.os.SystemClock;
import android.os.UserHandle;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.device.tracker.TrackerManager;
import com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.StepsRecordHelper;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.List;

/**
 * Class to collect native tracking metrics.
 *
 * @hide
 */
public class NativeTrackingStatsCollector {
    private final PackageInfoUtils mPackageInfoUtils;
    private final Context mContext;
    private final UserHandle mUser;
    private final TrackerManager mTrackerManager;
    private final HealthConnectPermissionHelper mHealthConnectPermissionHelper;
    private int[] mNativeDataTypesActive;
    private int[] mNativeDataTypesDisabled;
    private int mNumberOfWrites;
    private int mLastError;
    private int mStepsReadersCount;
    private int mStepsWritersCount;
    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;

    public NativeTrackingStatsCollector(
            PackageInfoUtils packageInfoUtils,
            Context context,
            UserHandle user,
            TransactionManager transactionManager,
            AppInfoHelper appInfoHelper,
            TrackerManager trackerManager,
            HealthConnectPermissionHelper healthConnectPermissionHelper) {
        mPackageInfoUtils = packageInfoUtils;
        mContext = context;
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mUser = user;
        mTrackerManager = trackerManager;
        mHealthConnectPermissionHelper = healthConnectPermissionHelper;
        mNativeDataTypesActive = new int[0];
        mNativeDataTypesDisabled = new int[0];
        mStepsReadersCount = 0;
        mStepsWritersCount = 0;
    }

    public void processStats() {
        List<PackageInfo> packagesWithPermissions =
                mPackageInfoUtils.getPackagesHoldingHealthPermissions(mUser, mContext);
        for (PackageInfo packageInfo : packagesWithPermissions) {
            List<String> permissions =
                    mHealthConnectPermissionHelper.getGrantedHealthPermissions(
                            packageInfo.packageName, mUser);
            if (permissions.contains(HealthPermissions.READ_STEPS)) {
                mStepsReadersCount++;
            } else if (permissions.contains(HealthPermissions.WRITE_STEPS)) {
                mStepsWritersCount++;
            }
        }
        long ddpAppInfoId = mAppInfoHelper.getAppInfoId(DEVICE_DATA_PROVIDER_PACKAGE);
        ReadTableRequest stepRecordsCountSinceBootRequest =
                new ReadTableRequest(StepsRecordHelper.STEPS_TABLE_NAME);
        stepRecordsCountSinceBootRequest.setJoinClause(getJoinClauseWithAppInfoTable());
        WhereClauses whereClauses =
                new WhereClauses(AND)
                        .addWhereInLongsClause(APP_INFO_ID_COLUMN_NAME, List.of(ddpAppInfoId));
        long bootTimeEpochMillis = System.currentTimeMillis() - SystemClock.uptimeMillis();
        whereClauses.addWhereLaterThanTimeClause(
                IntervalRecordHelper.START_TIME_COLUMN_NAME, bootTimeEpochMillis);
        stepRecordsCountSinceBootRequest.setWhereClause(whereClauses);

        mNumberOfWrites = mTransactionManager.count(stepRecordsCountSinceBootRequest);

        if (mTrackerManager.isStepTrackingActive()) {
            mNativeDataTypesActive =
                    new int[] {
                        HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__NATIVE_DATA_TYPES_ACTIVE__NATIVE_TRACKING_DATA_TYPE_STEPS
                    };
        }
        // This corresponds to the user actively turning off the 'step tracking' toggle.
        if (mTrackerManager.isStepTrackingExplicitlyDisabled()) {
            mNativeDataTypesDisabled =
                    new int[] {
                        HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__NATIVE_DATA_TYPES_DISABLED__NATIVE_TRACKING_DATA_TYPE_STEPS
                    };
        }

        // TODO(b438130821): track and log the last error code.
        mLastError =
                HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__LAST_ERROR_CODE__NATIVE_TRACKING_ERROR_CODE_UNSPECIFIED;
    }

    public int[] getNativeDataTypesActive() {
        return mNativeDataTypesActive;
    }

    public int[] getNativeDataTypesDisabled() {
        return mNativeDataTypesDisabled;
    }

    public int getNumberOfWrites() {
        return mNumberOfWrites;
    }

    public int getLastError() {
        return mLastError;
    }

    public int getLastErrorCode() {
        return mLastError;
    }

    public int getStepsReadersCount() {
        return mStepsReadersCount;
    }

    public int getStepsWritersCount() {
        return mStepsWritersCount;
    }

    private static SqlJoin getJoinClauseWithAppInfoTable() {
        return new SqlJoin(
                        StepsRecordHelper.STEPS_TABLE_NAME,
                        AppInfoHelper.TABLE_NAME,
                        APP_INFO_ID_COLUMN_NAME,
                        PRIMARY_COLUMN_NAME)
                .setJoinType(SqlJoin.SQL_JOIN_INNER);
    }
}
