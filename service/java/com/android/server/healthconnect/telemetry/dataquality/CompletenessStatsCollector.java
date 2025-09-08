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

package com.android.server.healthconnect.telemetry.dataquality;

import static com.android.healthfitness.flags.Flags.latencyMetricsFlag;
import static com.android.server.healthconnect.common.metadata.DeviceInfoHelper.DEVICE_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.common.metadata.DeviceInfoHelper.MANUFACTURER_COLUMN_NAME;
import static com.android.server.healthconnect.common.metadata.DeviceInfoHelper.MODEL_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.DEVICE_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.RECORDING_METHOD_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;
import static com.android.server.healthconnect.telemetry.dataquality.DataQualityUtils.getPackageName;

import android.database.Cursor;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.Slog;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A class to collect Health Connect data completeness stats. Including recording method and device
 * info.
 *
 * @hide
 */
public final class CompletenessStatsCollector {
    private static final String TAG = "CompletenessStatsCollector";
    private static final HealthConnectMappings HEALTH_CONNECT_MAPPINGS =
            HealthConnectMappings.getInstance();
    private static final InternalHealthConnectMappings INTERNAL_HEALTH_CONNECT_MAPPINGS =
            InternalHealthConnectMappings.getInstance();
    private static final String RECORD_TYPE_ID_COLUMN_NAME = "record_type_id";

    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;
    private final Clock mClock;

    public CompletenessStatsCollector(
            TransactionManager transactionManager, AppInfoHelper appInfoHelper, Clock clock) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mClock = clock;
    }

    List<RecordingMethodStat> readRecordingMethodStats() {
        if (!latencyMetricsFlag()) {
            return List.of();
        }

        List<ReadTableRequest> allRequests =
                HEALTH_CONNECT_MAPPINGS.getAllRecordTypeIdentifiers().stream()
                        .map(this::getReadRecordingMethodStatsRequest)
                        .toList();
        if (allRequests.isEmpty()) {
            return List.of();
        }

        ReadTableRequest readRequest = allRequests.get(0);
        if (allRequests.size() > 1) {
            readRequest.setUnionReadRequests(allRequests.subList(1, allRequests.size()));
        }

        List<RecordingMethodStat> recordingMethodStats = new ArrayList<>();
        try (Cursor cursor = mTransactionManager.read(readRequest)) {
            while (cursor.moveToNext()) {
                createRecordingMethodStat(cursor).ifPresent(recordingMethodStats::add);
            }
        } catch (Exception exception) {
            Slog.e(TAG, "Failed to log recording method stats", exception);
        }
        return recordingMethodStats;
    }

    private Optional<RecordingMethodStat> createRecordingMethodStat(Cursor cursor) {
        // Skip logging if package name not found in App info table
        return getPackageName(mAppInfoHelper, getCursorInt(cursor, APP_INFO_ID_COLUMN_NAME))
                .map(
                        packageName -> {
                            int recordTypeId = getCursorInt(cursor, RECORD_TYPE_ID_COLUMN_NAME);
                            int recordingMethod =
                                    getCursorInt(cursor, RECORDING_METHOD_COLUMN_NAME);
                            return new RecordingMethodStat(
                                    packageName, recordTypeId, recordingMethod);
                        });
    }

    private ReadTableRequest getReadRecordingMethodStatsRequest(int recordTypeId) {
        String tableName =
                INTERNAL_HEALTH_CONNECT_MAPPINGS.getRecordHelper(recordTypeId).getMainTableName();
        String recordIdColumn = recordTypeId + " AS " + RECORD_TYPE_ID_COLUMN_NAME;

        return new ReadTableRequest(tableName)
                .setDistinctClause(true)
                .setColumnNames(
                        List.of(
                                APP_INFO_ID_COLUMN_NAME,
                                RECORDING_METHOD_COLUMN_NAME,
                                recordIdColumn))
                .setWhereClause(getWhereClauseForRecentRecords());
    }

    Set<DeviceInfoStat> readDeviceInfoStats() {
        if (!latencyMetricsFlag()) {
            return Set.of();
        }

        List<ReadTableRequest> allRequests =
                HEALTH_CONNECT_MAPPINGS.getAllRecordTypeIdentifiers().stream()
                        .map(this::getReadDeviceInfoStatsRequest)
                        .toList();
        if (allRequests.isEmpty()) {
            return Set.of();
        }

        ReadTableRequest readRequest = allRequests.get(0);
        if (allRequests.size() > 1) {
            readRequest.setUnionReadRequests(allRequests.subList(1, allRequests.size()));
        }

        Set<DeviceInfoStat> deviceInfoStats = new HashSet<>();
        try (Cursor cursor = mTransactionManager.read(readRequest)) {
            while (cursor.moveToNext()) {
                createDeviceInfoStat(cursor).ifPresent(deviceInfoStats::add);
            }
        } catch (Exception exception) {
            Slog.e(TAG, "Failed to log device info stats.", exception);
        }
        return deviceInfoStats;
    }

    private ReadTableRequest getReadDeviceInfoStatsRequest(int recordTypeId) {
        String tableName =
                INTERNAL_HEALTH_CONNECT_MAPPINGS.getRecordHelper(recordTypeId).getMainTableName();
        String recordIdColumn = recordTypeId + " AS " + RECORD_TYPE_ID_COLUMN_NAME;

        return new ReadTableRequest(tableName)
                .setDistinctClause(true)
                .setColumnNames(
                        List.of(
                                APP_INFO_ID_COLUMN_NAME,
                                recordIdColumn,
                                MANUFACTURER_COLUMN_NAME,
                                MODEL_COLUMN_NAME,
                                DEVICE_TYPE_COLUMN_NAME))
                .setJoinClause(
                        new SqlJoin(
                                tableName,
                                DeviceInfoHelper.TABLE_NAME,
                                DEVICE_INFO_ID_COLUMN_NAME,
                                RecordHelper.PRIMARY_COLUMN_NAME))
                .setWhereClause(getWhereClauseForRecentRecords());
    }

    private Optional<DeviceInfoStat> createDeviceInfoStat(Cursor cursor) {
        // Skip logging if package name not found in App info table
        return getPackageName(mAppInfoHelper, getCursorInt(cursor, APP_INFO_ID_COLUMN_NAME))
                .map(
                        app -> {
                            int recordTypeId = getCursorInt(cursor, RECORD_TYPE_ID_COLUMN_NAME);
                            String manufacturer = getCursorString(cursor, MANUFACTURER_COLUMN_NAME);
                            String model = getCursorString(cursor, MODEL_COLUMN_NAME);
                            int deviceType = getCursorInt(cursor, DEVICE_TYPE_COLUMN_NAME);

                            boolean hasManufacturer =
                                    manufacturer != null && !manufacturer.isEmpty();
                            boolean hasModel = model != null && !model.isEmpty();
                            boolean hasType = deviceType != Device.DEVICE_TYPE_UNKNOWN;

                            return new DeviceInfoStat(
                                    app, recordTypeId, hasManufacturer, hasModel, hasType);
                        });
    }

    private WhereClauses getWhereClauseForRecentRecords() {
        long aWeekAgoMillis = mClock.instant().minus(7, ChronoUnit.DAYS).toEpochMilli();
        return new WhereClauses(WhereClauses.LogicalOperator.AND)
                .addWhereLaterThanTimeClause(LAST_MODIFIED_TIME_COLUMN_NAME, aWeekAgoMillis);
    }

    /**
     * Data class to hold latency i.e. time between session end and time when the session was
     * inserted for every record.
     *
     * @param packageName The package name of the app that inserted the records.
     * @param recordTypeId The {@link android.healthfitness.api.DataType} of the records.
     * @param recordingMethod The {@link android.healthfitness.api.RecordingMethod} used.
     */
    record RecordingMethodStat(
            String packageName,
            @RecordTypeIdentifier.RecordType int recordTypeId,
            @Metadata.RecordingMethod int recordingMethod) {}

    /**
     * Data class to hold device info stats.
     *
     * @param packageName The package name of the app that inserted the records.
     * @param recordTypeId The {@link android.healthfitness.api.DataType} of the records.
     * @param hasManufacturer whether the device has manufacturer info.
     * @param hasModel whether the device has model info.
     * @param hasType whether the device has type info.
     */
    record DeviceInfoStat(
            String packageName,
            @RecordTypeIdentifier.RecordType int recordTypeId,
            boolean hasManufacturer,
            boolean hasModel,
            boolean hasType) {}
}
