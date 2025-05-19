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

package android.healthconnect.testing.unittest;

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.DELETE;

import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.OPERATION_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.UUIDS_COLUMN_NAME;
import static com.android.server.healthconnect.common.metadata.AppInfoHelper.PACKAGE_COLUMN_NAME;
import static com.android.server.healthconnect.common.metadata.AppInfoHelper.UNIQUE_COLUMN_INFO;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorUUIDList;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.RecordIdFiltersParcel;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;

import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import com.google.common.collect.ImmutableList;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Util class provides shared functionality for db transaction testing. */
// TODO(b/414747066): Remove all record builders and migrate callers to RecordInternalFactory.
public final class TransactionTestUtils {
    private static final Set<String> NO_EXTRA_PERMS = Set.of();
    private static final String TEST_PACKAGE_NAME = "package.name";
    private final TransactionManager mTransactionManager;
    private final FitnessRecordUpsertHelper mFitnessRecordUpsertHelper;
    private final FitnessRecordReadHelper mFitnessRecordReadHelper;
    private final FitnessRecordDeleteHelper mFitnessRecordDeleteHelper;
    private final HealthConnectInjector mHealthConnectInjector;

    public TransactionTestUtils(HealthConnectInjector injector) {
        mTransactionManager = injector.getTransactionManager();
        mFitnessRecordUpsertHelper = injector.getFitnessRecordUpsertHelper();
        mFitnessRecordReadHelper = injector.getFitnessRecordReadHelper();
        mFitnessRecordDeleteHelper = injector.getFitnessRecordDeleteHelper();
        mHealthConnectInjector = injector;
    }

    public void insertApp(String packageName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PACKAGE_COLUMN_NAME, packageName);
        mTransactionManager.insertOrThrowOnConflict(
                new UpsertTableRequest(
                        AppInfoHelper.TABLE_NAME, contentValues, UNIQUE_COLUMN_INFO));
        mHealthConnectInjector.getAppInfoHelper().clearCache();
        assertThat(mHealthConnectInjector.getAppInfoHelper().getAppInfoId(packageName))
                .isNotEqualTo(DEFAULT_LONG);
    }

    /** Inserts {@code packageName} into the given {@link HealthConnectDatabase}. */
    public void insertApp(HealthConnectDatabase db, String packageName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(PACKAGE_COLUMN_NAME, packageName);
        mTransactionManager.insertOrThrowOnConflict(
                db.getWritableDatabase(),
                new UpsertTableRequest(
                        AppInfoHelper.TABLE_NAME, contentValues, UNIQUE_COLUMN_INFO));
    }

    /** Inserts records attributed to the given package. */
    public List<String> insertRecords(String packageName, RecordInternal<?>... records) {
        return insertRecords(packageName, List.of(records));
    }

    /** Inserts records attributed to the given package. */
    public List<String> insertRecords(String packageName, List<RecordInternal<?>> records) {
        return mFitnessRecordUpsertHelper.insertRecords(
                packageName, records, /* extraPermsStateMap= */ new ArrayMap<>(), true);
    }

    /** Inserts records attributed to the given package. */
    public void updateRecords(String packageName, RecordInternal<?>... records) {
        updateRecords(packageName, List.of(records));
    }

    /** Inserts records attributed to the given package. */
    public void updateRecords(String packageName, List<RecordInternal<?>> records) {
        mFitnessRecordUpsertHelper.updateRecords(
                packageName, records, /* extraPermsStateMap= */ new ArrayMap<>());
    }

    /** Deletes records with the given IDs from storage. */
    public void deleteRecords(String packageName, RecordIdFilter... recordIdFilters) {
        deleteRecords(packageName, List.of(recordIdFilters));
    }

    private void deleteRecords(String packageName, List<RecordIdFilter> recordIdFilters) {
        DeleteUsingFiltersRequestParcel parcel =
                new DeleteUsingFiltersRequestParcel(
                        new RecordIdFiltersParcel(recordIdFilters), packageName);
        mFitnessRecordDeleteHelper.deleteRecords(
                packageName,
                parcel,
                /* holdsDataManagementPermission= */ false,
                /* shouldRecordAccessLog= */ false);
    }

    /** Read records with the given IDs from storage. */
    public List<RecordInternal<?>> readRecordsByIds(Map<Integer, List<UUID>> recordTypeToUuids) {
        return readRecordsByIds(TEST_PACKAGE_NAME, recordTypeToUuids);
    }

    /** Read records with the given IDs from storage, as the given package name. */
    public List<RecordInternal<?>> readRecordsByIds(
            String packageName, Map<Integer, List<UUID>> recordTypeToUuids) {
        return readRecordsByIds(packageName, recordTypeToUuids, /* shouldRecordAccessLog */ false);
    }

    /** Read records with the given IDs from storage, as the given package name. */
    public List<RecordInternal<?>> readRecordsByIds(
            String packageName,
            Map<Integer, List<UUID>> recordTypeToUuids,
            boolean shouldRecordAccessLogs) {
        return mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                packageName,
                recordTypeToUuids,
                /* startDateAccessMillis= */ NO_EXTRA_PERMS,
                0,
                /* isInForeground= */ true,
                shouldRecordAccessLogs);
    }

    /** Fetches all records of a given type. */
    public <T extends Record> List<RecordInternal<?>> readAllRecordsOfType(
            String packageName, Class<T> recordClass) {
        ReadRecordsRequestUsingFilters<T> request =
                new ReadRecordsRequestUsingFilters.Builder<T>(recordClass)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();

        return mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        packageName,
                        request.toReadRecordsRequestParcel(),
                        /* grantedExtraReadPermissions= */ Set.of(),
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ true,
                        /* shouldRecordAccessLogs= */ false,
                        /* enforceSelfRead */ false,
                        /* packageNamesByAppIds= */ null)
                .first;
    }

    /** Inserts one single fake access log. */
    public void insertAccessLog() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("record_type", "fake_record_type");
        contentValues.put("app_id", "fake_app_id");
        contentValues.put("access_time", "fake_access_time");
        contentValues.put("operation_type", "fake_operation_type");
        mTransactionManager.insertOrThrowOnConflict(
                new UpsertTableRequest(AccessLogsHelper.TABLE_NAME, contentValues));
    }

    /** Inserts one single fake change log. */
    public void insertChangeLog() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("record_type", "fake_record_type");
        contentValues.put("app_id", "fake_app_id");
        contentValues.put("uuids", "fake_uuids");
        contentValues.put("operation_type", "fake_operation_type");
        mTransactionManager.insertOrThrowOnConflict(
                new UpsertTableRequest(ChangeLogsHelper.TABLE_NAME, contentValues));
    }

    /** Retrieves all delete record change logs from change log table. */
    public List<UUID> getAllDeletedUuids() {
        WhereClauses whereClauses =
                new WhereClauses(AND).addWhereEqualsClause(OPERATION_TYPE_COLUMN_NAME, DELETE + "");
        ReadTableRequest readChangeLogsRequest =
                new ReadTableRequest(ChangeLogsHelper.TABLE_NAME).setWhereClause(whereClauses);
        ImmutableList.Builder<UUID> uuids = ImmutableList.builder();
        try (Cursor cursor = mTransactionManager.read(readChangeLogsRequest)) {
            while (cursor.moveToNext()) {
                uuids.addAll(getCursorUUIDList(cursor, UUIDS_COLUMN_NAME));
            }
            return uuids.build();
        }
    }

    /** Returns a valid UUID string. */
    public static String getUUID() {
        return "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    }
}
