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

package com.android.server.healthconnect.storage.datatypehelpers;

import static android.healthconnect.Constants.DEFAULT_INT;
import static android.healthconnect.Constants.DEFAULT_LONG;
import static android.healthconnect.Constants.MAXIMUM_PAGE_SIZE;

import static com.android.server.healthconnect.storage.request.ReadTransactionRequest.TYPE_NOT_PRESENT_PACKAGE_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL_UNIQUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.healthconnect.AggregateResult;
import android.healthconnect.aidl.ReadRecordsRequestParcel;
import android.healthconnect.datatypes.AggregationType;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.utils.RecordMapper;
import android.util.Pair;

import com.android.server.healthconnect.storage.request.AggregateTableRequest;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.OrderByClause;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Parent class for all the helper classes for all the records
 *
 * @hide
 */
public abstract class RecordHelper<T extends RecordInternal<?>> {
    public static final String PRIMARY_COLUMN_NAME = "row_id";
    public static final String UUID_COLUMN_NAME = "uuid";
    public static final String CLIENT_RECORD_ID_COLUMN_NAME = "client_record_id";
    public static final String APP_INFO_ID_COLUMN_NAME = "app_info_id";
    private static final String LAST_MODIFIED_TIME_COLUMN_NAME = "last_modified_time";
    private static final String CLIENT_RECORD_VERSION_COLUMN_NAME = "client_record_version";
    private static final String DEVICE_INFO_ID_COLUMN_NAME = "device_info_id";
    @RecordTypeIdentifier.RecordType private final int mRecordIdentifier;

    RecordHelper() {
        HelperFor annotation = this.getClass().getAnnotation(HelperFor.class);
        Objects.requireNonNull(annotation);
        mRecordIdentifier = annotation.recordIdentifier();
    }

    public DeleteTableRequest getDeleteRequestForAutoDelete(int recordAutoDeletePeriodInDays) {
        return new DeleteTableRequest(getMainTableName())
                .setTimeFilter(
                        getStartTimeColumnName(),
                        Instant.EPOCH.toEpochMilli(),
                        Instant.now()
                                .minus(recordAutoDeletePeriodInDays, ChronoUnit.DAYS)
                                .toEpochMilli());
    }

    @RecordTypeIdentifier.RecordType
    public int getRecordIdentifier() {
        return mRecordIdentifier;
    }

    // Called on DB update. Inheriting classes should implement this if they need to add new
    // columns.
    public void onUpgrade(int newVersion, @NonNull SQLiteDatabase db) {
        // empty by default
    }

    /**
     * @return {@link AggregateTableRequest} corresponding to {@code aggregationType}
     */
    public final AggregateTableRequest getAggregateTableRequest(
            AggregationType<?> aggregationType,
            List<String> packageFilter,
            long startTime,
            long endTime) {
        AggregateParams params = getAggregateParams(aggregationType);
        Objects.requireNonNull(params);

        return new AggregateTableRequest(
                        params.mTableName, params.mColumnNames, aggregationType, this)
                .setPackageFilter(
                        AppInfoHelper.getInstance().getAppInfoIds(packageFilter),
                        APP_INFO_ID_COLUMN_NAME)
                .setTimeFilter(startTime, endTime, params.mTimeColumnName)
                .setSqlJoin(params.mJoin)
                .setAdditionalColumnsToFetch(Collections.singletonList(getZoneOffsetColumnName()));
    }

    /**
     * @return {@link AggregateResult} for {@link AggregationType}
     */
    public AggregateResult getAggregateResult(Cursor cursor, AggregationType<?> aggregationType) {
        // returns null by default
        return null;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    @NonNull
    public final CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(getMainTableName(), getColumnInfo())
                .addForeignKey(
                        DeviceInfoHelper.getInstance().getTableName(),
                        Collections.singletonList(DEVICE_INFO_ID_COLUMN_NAME),
                        Collections.singletonList(PRIMARY_COLUMN_NAME))
                .addForeignKey(
                        AppInfoHelper.getInstance().getTableName(),
                        Collections.singletonList(APP_INFO_ID_COLUMN_NAME),
                        Collections.singletonList(PRIMARY_COLUMN_NAME))
                .setChildTableRequests(getChildTableCreateRequests());
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public UpsertTableRequest getUpsertTableRequest(RecordInternal<?> recordInternal) {
        return new UpsertTableRequest(getMainTableName(), getContentValues((T) recordInternal))
                .setChildTableRequests(getChildTableUpsertRequests((T) recordInternal));
    }

    /** Returns ReadTableRequest for {@code request} and package name {@code packageName} */
    public ReadTableRequest getReadTableRequest(
            ReadRecordsRequestParcel request, String packageName, boolean enforceSelfRead) {
        return new ReadTableRequest(getMainTableName())
                .setJoinClause(getInnerJoinFoReadRequest())
                .setWhereClause(getReadTableWhereClause(request, packageName, enforceSelfRead))
                .setOrderBy(getOrderByClause(request))
                .setLimit(getLimitSize(request))
                .setRecordHelper(this);
    }

    /** Returns ReadTableRequest for {@code uuids} */
    public ReadTableRequest getReadTableRequest(List<String> uuids) {
        return new ReadTableRequest(getMainTableName())
                .setJoinClause(getInnerJoinFoReadRequest())
                .setWhereClause(new WhereClauses().addWhereInClause(UUID_COLUMN_NAME, uuids))
                .setRecordHelper(this);
    }

    /**
     * Returns ReadTableRequest for the record corresponding to this helper with a distinct clause
     * on the input column names.
     */
    public ReadTableRequest getReadTableRequestWithDistinctAppInfoIds() {
        return new ReadTableRequest(getMainTableName())
                .setColumnNames(new ArrayList<>(List.of(APP_INFO_ID_COLUMN_NAME)))
                .setDistinctClause(true);
    }

    /** Returns List of Internal records from the cursor */
    @SuppressWarnings("unchecked")
    public List<RecordInternal<?>> getInternalRecords(Cursor cursor, int requestSize) {
        List<RecordInternal<?>> recordInternalList = new ArrayList<>();
        int count = 0;
        while (cursor.moveToNext()) {
            try {
                T record =
                        (T)
                                RecordMapper.getInstance()
                                        .getRecordIdToInternalRecordClassMap()
                                        .get(getRecordIdentifier())
                                        .getConstructor()
                                        .newInstance();
                record.setUuid(getCursorString(cursor, UUID_COLUMN_NAME));
                record.setLastModifiedTime(getCursorLong(cursor, LAST_MODIFIED_TIME_COLUMN_NAME));
                record.setClientRecordId(getCursorString(cursor, CLIENT_RECORD_ID_COLUMN_NAME));
                record.setClientRecordVersion(
                        getCursorLong(cursor, CLIENT_RECORD_VERSION_COLUMN_NAME));
                long deviceInfoId = getCursorLong(cursor, DEVICE_INFO_ID_COLUMN_NAME);
                DeviceInfoHelper.getInstance().populateRecordWithValue(deviceInfoId, record);
                long appInfoId = getCursorLong(cursor, APP_INFO_ID_COLUMN_NAME);
                AppInfoHelper.getInstance().populateRecordWithValue(appInfoId, record);
                populateRecordValue(cursor, record);
                recordInternalList.add(record);
                count++;
                if (count == requestSize) {
                    break;
                }
            } catch (InstantiationException
                    | IllegalAccessException
                    | NoSuchMethodException
                    | InvocationTargetException exception) {
                throw new IllegalArgumentException(exception);
            }
        }
        return recordInternalList;
    }

    public DeleteTableRequest getDeleteTableRequest(
            List<String> packageFilters, long startTime, long endTime) {
        return new DeleteTableRequest(getMainTableName(), getRecordIdentifier())
                .setTimeFilter(getStartTimeColumnName(), startTime, endTime)
                .setPackageFilter(
                        APP_INFO_ID_COLUMN_NAME,
                        AppInfoHelper.getInstance().getAppInfoIds(packageFilters))
                .setRequiresUuId(UUID_COLUMN_NAME);
    }

    public DeleteTableRequest getDeleteTableRequest(List<String> ids) {
        return new DeleteTableRequest(getMainTableName(), getRecordIdentifier())
                .setIds(UUID_COLUMN_NAME, ids)
                .setRequiresUuId(UUID_COLUMN_NAME)
                .setEnforcePackageCheck(APP_INFO_ID_COLUMN_NAME, UUID_COLUMN_NAME);
    }

    public abstract String getDurationGroupByColumnName();

    public abstract String getPeriodGroupByColumnName();

    public abstract String getStartTimeColumnName();

    /**
     * Child classes should implement this if it wants to create additional tables, apart from the
     * main table.
     */
    @NonNull
    List<CreateTableRequest> getChildTableCreateRequests() {
        return Collections.emptyList();
    }

    /** Returns the table name to be created corresponding to this helper */
    @NonNull
    abstract String getMainTableName();

    /** Returns the information required to perform aggregate operation. */
    AggregateParams getAggregateParams(AggregationType<?> aggregateRequest) {
        // Null by default
        return null;
    }

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    @NonNull
    abstract List<Pair<String, String>> getSpecificColumnInfo();

    /**
     * Child classes implementation should add the values of {@code recordInternal} that needs to be
     * populated in the DB to {@code contentValues}.
     */
    abstract void populateContentValues(
            @NonNull ContentValues contentValues, @NonNull T recordInternal);

    /**
     * Child classes implementation should populate the values to the {@code record} using the
     * cursor {@code cursor} queried from the DB .
     */
    abstract void populateRecordValue(@NonNull Cursor cursor, @NonNull T recordInternal);

    List<UpsertTableRequest> getChildTableUpsertRequests(T record) {
        return Collections.emptyList();
    }

    SqlJoin getInnerJoinFoReadRequest() {
        return null;
    }

    private int getLimitSize(ReadRecordsRequestParcel request) {
        if (request.getRecordIdFiltersParcel() == null) {
            return request.getPageSize();
        } else {
            return MAXIMUM_PAGE_SIZE;
        }
    }

    private WhereClauses getReadTableWhereClause(
            ReadRecordsRequestParcel request, String packageName, boolean enforceSelfRead) {
        if (request.getRecordIdFiltersParcel() == null) {
            List<Long> appIds =
                    AppInfoHelper.getInstance().getAppInfoIds(request.getPackageFilters()).stream()
                            .distinct()
                            .collect(Collectors.toList());
            if (enforceSelfRead) {
                appIds = AppInfoHelper.getInstance().getAppInfoIds(request.getPackageFilters());
            }
            if (appIds.size() == 1 && appIds.get(0) == DEFAULT_INT) {
                throw new TypeNotPresentException(TYPE_NOT_PRESENT_PACKAGE_NAME, new Throwable());
            }

            WhereClauses clauses =
                    new WhereClauses().addWhereInLongsClause(APP_INFO_ID_COLUMN_NAME, appIds);

            if (request.getPageToken() != DEFAULT_LONG) {
                clauses.addWhereGreaterThanClause(
                        RecordHelper.PRIMARY_COLUMN_NAME, String.valueOf(request.getPageToken()));
            }

            return clauses.addWhereBetweenTimeClause(
                    getStartTimeColumnName(), request.getStartTime(), request.getEndTime());
        }

        // Since for now we don't support mixing IDs and filters, we need to look for IDs now
        List<String> ids =
                request.getRecordIdFiltersParcel().getRecordIdFilters().stream()
                        .map(
                                (recordIdFilter) ->
                                        StorageUtils.getUUIDFor(recordIdFilter, packageName))
                        .collect(Collectors.toList());
        WhereClauses whereClauses = new WhereClauses().addWhereInClause(UUID_COLUMN_NAME, ids);

        if (enforceSelfRead) {
            long id = AppInfoHelper.getInstance().getAppInfoId(packageName);
            if (id == DEFAULT_LONG) {
                throw new TypeNotPresentException(TYPE_NOT_PRESENT_PACKAGE_NAME, new Throwable());
            }
            whereClauses.addWhereInLongsClause(
                    APP_INFO_ID_COLUMN_NAME, Collections.singletonList(id));
        }

        return whereClauses;
    }

    abstract String getZoneOffsetColumnName();

    private OrderByClause getOrderByClause(ReadRecordsRequestParcel request) {
        OrderByClause orderByClause = new OrderByClause();
        if (request.getRecordIdFiltersParcel() != null) {
            orderByClause
                    .addOrderByClause(getStartTimeColumnName(), request.isAscending())
                    .addOrderByClause(PRIMARY_COLUMN_NAME, true);
        }
        return orderByClause;
    }

    @NonNull
    private ContentValues getContentValues(@NonNull T recordInternal) {
        ContentValues recordContentValues = new ContentValues();

        recordContentValues.put(UUID_COLUMN_NAME, recordInternal.getUuid());
        recordContentValues.put(
                LAST_MODIFIED_TIME_COLUMN_NAME, recordInternal.getLastModifiedTime());
        recordContentValues.put(CLIENT_RECORD_ID_COLUMN_NAME, recordInternal.getClientRecordId());
        recordContentValues.put(
                CLIENT_RECORD_VERSION_COLUMN_NAME, recordInternal.getClientRecordVersion());
        recordContentValues.put(DEVICE_INFO_ID_COLUMN_NAME, recordInternal.getDeviceInfoId());
        recordContentValues.put(APP_INFO_ID_COLUMN_NAME, recordInternal.getAppInfoId());

        populateContentValues(recordContentValues, recordInternal);

        return recordContentValues;
    }

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    @NonNull
    private List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT));
        columnInfo.add(new Pair<>(UUID_COLUMN_NAME, TEXT_NOT_NULL_UNIQUE));
        columnInfo.add(new Pair<>(LAST_MODIFIED_TIME_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(CLIENT_RECORD_ID_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(CLIENT_RECORD_VERSION_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(DEVICE_INFO_ID_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(APP_INFO_ID_COLUMN_NAME, INTEGER));

        columnInfo.addAll(getSpecificColumnInfo());

        return columnInfo;
    }

    static class AggregateParams {
        private final String mTableName;
        private final List<String> mColumnNames;
        private final String mTimeColumnName;
        private SqlJoin mJoin;

        public AggregateParams(String tableName, List<String> columnNames, String timeColumnName) {
            mTableName = tableName;
            mColumnNames = columnNames;
            mTimeColumnName = timeColumnName;
        }

        public AggregateParams setJoin(SqlJoin join) {
            mJoin = join;
            return this;
        }
    }
}
