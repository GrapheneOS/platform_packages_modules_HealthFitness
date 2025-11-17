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

package com.android.server.healthconnect.fitness.recordhelpers;

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.MAXIMUM_ALLOWED_CURSOR_COUNT;
import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;
import static android.health.connect.Constants.PARENT_KEY;
import static android.health.connect.PageTokenWrapper.EMPTY_PAGE_TOKEN;

import static com.android.server.healthconnect.fitness.FitnessRecordReadHelper.TYPE_NOT_PRESENT_PACKAGE_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper.END_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB_UNIQUE_NON_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB_UNIQUE_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorUUID;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getDedupeByteBuffer;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.OR;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.HealthFitnessStatsLog;
import android.health.connect.AggregateResult;
import android.health.connect.PageTokenWrapper;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.aidl.RecordIdFiltersParcel;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;

import androidx.annotation.Nullable;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.fitness.RecordDeleteTableRequest;
import com.android.server.healthconnect.fitness.RecordReadTableRequest;
import com.android.server.healthconnect.fitness.aggregation.AggregateParams;
import com.android.server.healthconnect.fitness.aggregation.AggregateRecordRequest;
import com.android.server.healthconnect.fitness.aggregation.TimeSplits;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderMetadataHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.AlterTableRequest;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.OrderByClause;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.TableColumnPair;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
    public static final String LAST_MODIFIED_TIME_COLUMN_NAME = "last_modified_time";
    public static final String RECORDING_METHOD_COLUMN_NAME = "recording_method";
    public static final String DEVICE_INFO_ID_COLUMN_NAME = "device_info_id";
    private static final String CLIENT_RECORD_VERSION_COLUMN_NAME = "client_record_version";
    private static final String DEDUPE_HASH_COLUMN_NAME = "dedupe_hash";
    private static final List<Pair<String, Integer>> UNIQUE_COLUMNS_INFO =
            List.of(
                    new Pair<>(DEDUPE_HASH_COLUMN_NAME, UpsertTableRequest.TYPE_BLOB),
                    new Pair<>(UUID_COLUMN_NAME, UpsertTableRequest.TYPE_BLOB));
    @RecordTypeIdentifier.RecordType private final int mRecordIdentifier;

    /**
     * Public because it is used in {@link
     * com.android.server.healthconnect.storage.DevelopmentDatabaseHelper} to check if the DDP
     * upgrade has already been applied.
     */
    public static final String DDP_ID_COLUMN_NAME = "device_data_provider_id";

    RecordHelper(@RecordTypeIdentifier.RecordType int recordIdentifier) {
        mRecordIdentifier = recordIdentifier;
    }

    /**
     * Returns a collection of granular write permissions required for this record. The default
     * implementation returns an empty collection, assuming category-level checks are sufficient.
     *
     * @param record The specific record being written.
     */
    public Set<String> getGranularWritePermissions(RecordInternal<?> record) {
        return Collections.emptySet();
    }

    /**
     * Returns a set of granular read permissions that apply to this record type. The default
     * implementation returns an empty set.
     */
    public Set<String> getGranularReadPermissions() {
        return Collections.emptySet();
    }

    /** Database migration. Introduces automatic local time generation. */
    public abstract void applyGeneratedLocalTimeUpgrade(SQLiteDatabase db);

    @RecordTypeIdentifier.RecordType
    public int getRecordIdentifier() {
        return mRecordIdentifier;
    }

    /**
     * @return {@link AggregateRecordRequest} corresponding to {@code aggregationType}
     */
    public final AggregateRecordRequest getAggregateRecordRequest(
            AggregationType<?> aggregationType,
            String callingPackage,
            List<String> packageFilters,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            InternalHealthConnectMappings internalHealthConnectMappings,
            AppInfoHelper appInfoHelper,
            TransactionManager transactionManager,
            TimeSplits timeSplits,
            long startDateAccess,
            boolean useLocalTime) {
        AggregateParams params = getAggregateParams(aggregationType);
        if (params == null) {
            throw new NullPointerException(
                    "Unsupported aggregation requested for "
                            + aggregationType
                            + " from "
                            + getRecordIdentifier());
        }
        String physicalTimeColumnName = getStartTimeColumnName();
        String startTimeColumnName;
        String endTimeColumnName;
        if (useLocalTime) {
            startTimeColumnName = getLocalStartTimeColumnName();
            endTimeColumnName = getLocalEndTimeColumnName();
        } else {
            // TODO(b/326058390): Handle local time filter for series data types
            startTimeColumnName =
                    getSampleTimestampsColumnName() != null
                            ? getSampleTimestampsColumnName()
                            : physicalTimeColumnName;
            endTimeColumnName =
                    getSampleTimestampsColumnName() != null
                            ? getSampleTimestampsColumnName()
                            : getEndTimeColumnName();
        }
        params.setTimeColumnName(startTimeColumnName);
        params.setExtraTimeColumn(endTimeColumnName);
        params.setOffsetColumnToFetch(getZoneOffsetColumnName());

        if (internalHealthConnectMappings.supportsPriority(
                mRecordIdentifier, aggregationType.getAggregateOperationType())) {
            List<String> columns =
                    Arrays.asList(
                            physicalTimeColumnName,
                            END_TIME_COLUMN_NAME,
                            APP_INFO_ID_COLUMN_NAME,
                            LAST_MODIFIED_TIME_COLUMN_NAME);
            params.appendAdditionalColumns(columns);
        }
        if (internalHealthConnectMappings.isDerivedType(mRecordIdentifier)) {
            params.appendAdditionalColumns(Collections.singletonList(physicalTimeColumnName));
        }

        WhereClauses whereClauses = new WhereClauses(AND);
        // filters by package names
        whereClauses.addWhereInLongsClause(
                APP_INFO_ID_COLUMN_NAME, appInfoHelper.getAppInfoIds(packageFilters));
        // filter by start date access
        whereClauses.addNestedWhereClauses(
                getFilterByStartAccessDateWhereClauses(
                        appInfoHelper.getAppInfoId(callingPackage), startDateAccess));
        // data start time < filter end time
        whereClauses.addWhereLessThanClause(startTimeColumnName, timeSplits.getEndTime());
        if (endTimeColumnName != null) {
            // for IntervalRecord, filters by overlapping
            // data end time >= filter start time
            whereClauses.addWhereGreaterThanOrEqualClause(
                    endTimeColumnName, timeSplits.getStartTime());
        } else {
            // for InstantRecord, filters by whether time falls into [startTime, endTime)
            whereClauses.addWhereGreaterThanOrEqualClause(
                    startTimeColumnName, timeSplits.getStartTime());
        }

        return new AggregateRecordRequest(
                params,
                aggregationType,
                this,
                whereClauses,
                healthDataCategoryPriorityHelper,
                internalHealthConnectMappings,
                appInfoHelper,
                transactionManager,
                useLocalTime,
                timeSplits);
    }

    /**
     * Used to get an {@link AggregateResult} for data types which don't support priority.
     *
     * @param cursor the result of the aggregation database query. Contains one row per aggregation
     *     group. The query is constructed based on the return value of {@link
     *     #getAggregateParams(AggregationType)}. The cursor points to the row representing the
     *     group and must not be moved.
     * @param aggregationType the aggregation type being calculated.
     * @return {@link AggregateResult} for {@link AggregationType}.
     */
    @Nullable
    public AggregateResult<?> getNoPriorityAggregateResult(
            Cursor cursor, AggregationType<?> aggregationType, Set<DataOrigin> dataOrigins) {
        if (Flags.refactorAggregations()) {
            throw new UnsupportedOperationException("Not implemented by the subclass");
        }

        return null;
    }

    /**
     * Used to get an {@link AggregateResult} for derived types.
     *
     * <p>Called once per aggregation group.
     *
     * @param results the result of the aggregation database query. Contains one row per aggregation
     *     group. The query is constructed based on the return value of {@link
     *     #getAggregateParams(AggregationType)}. The cursor points to the row representing the
     *     first group.
     * @param aggregationType the aggregation type being calculated.
     * @param total the calculated derived value for this group returned by {@link
     *     #deriveAggregate(Cursor, AggregateRecordRequest, TransactionManager)}.
     * @return {@link AggregateResult} for {@link AggregationType} or null if the type is not
     *     supported
     */
    @Nullable
    public AggregateResult<?> getDerivedAggregateResult(
            Cursor results,
            AggregationType<?> aggregationType,
            double total,
            Set<DataOrigin> dataOrigins) {
        if (Flags.refactorAggregations()) {
            throw new UnsupportedOperationException("Not implemented by the subclass");
        }

        return null;
    }

    /**
     * Used to calculate and get aggregate results for data types that support derived aggregates.
     *
     * @param cursor the result of the aggregation database query. Contains one row per aggregation
     *     group. The query is constructed based on the return value of {@link
     *     #getAggregateParams(AggregationType)}.
     * @return an array of aggregated values, one element per aggregation group.
     */
    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    public double[] deriveAggregate(
            Cursor cursor, AggregateRecordRequest request, TransactionManager transactionManager) {
        if (Flags.refactorAggregations()) {
            throw new UnsupportedOperationException("Not implemented by the subclass");
        }

        return null;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    public final CreateTableRequest getCreateTableRequest() {
        CreateTableRequest request =
                new CreateTableRequest(getMainTableName(), getColumnInfo())
                        .addForeignKey(
                                DeviceInfoHelper.TABLE_NAME,
                                Collections.singletonList(DEVICE_INFO_ID_COLUMN_NAME),
                                Collections.singletonList(PRIMARY_COLUMN_NAME))
                        .addForeignKey(
                                AppInfoHelper.TABLE_NAME,
                                Collections.singletonList(APP_INFO_ID_COLUMN_NAME),
                                Collections.singletonList(PRIMARY_COLUMN_NAME));

        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            request.addForeignKey(
                    DeviceDataProviderMetadataHelper.TABLE_NAME,
                    Collections.singletonList(DDP_ID_COLUMN_NAME),
                    Collections.singletonList(PRIMARY_COLUMN_NAME));
        }

        request.setChildTableRequests(getChildTableCreateRequests())
                .setGeneratedColumnInfo(getGeneratedColumnInfo());

        return request;
    }

    /** Gets {@link UpsertTableRequest} from {@code recordInternal}. */
    public UpsertTableRequest getUpsertTableRequest(RecordInternal<?> recordInternal) {
        return getUpsertTableRequest(recordInternal, null);
    }

    @SuppressWarnings("unchecked")
    public UpsertTableRequest getUpsertTableRequest(
            RecordInternal<?> recordInternal,
            @Nullable ArrayMap<String, Boolean> extraWritePermissionToStateMap) {
        ContentValues upsertValues = getContentValues((T) recordInternal);
        updateUpsertValuesIfRequired(upsertValues, extraWritePermissionToStateMap);
        return new UpsertTableRequest(getMainTableName(), upsertValues, UNIQUE_COLUMNS_INFO)
                .setRequiresUpdateClause(
                        new UpsertTableRequest.IRequiresUpdate() {
                            @Override
                            public boolean requiresUpdate(
                                    Cursor cursor,
                                    ContentValues contentValues,
                                    UpsertTableRequest request) {
                                final UUID newUUID =
                                        StorageUtils.convertBytesToUUID(
                                                contentValues.getAsByteArray(UUID_COLUMN_NAME));
                                final UUID oldUUID =
                                        StorageUtils.getCursorUUID(cursor, UUID_COLUMN_NAME);

                                if (!Objects.equals(newUUID, oldUUID)) {
                                    // Use old UUID in case of conflicts on de-dupe.
                                    contentValues.put(
                                            UUID_COLUMN_NAME,
                                            StorageUtils.convertUUIDToBytes(oldUUID));
                                    recordInternal.setUuid(oldUUID);
                                    // This means there was a duplication conflict, we want
                                    // to update in this case.
                                    return true;
                                }

                                long clientRecordVersion =
                                        StorageUtils.getCursorLong(
                                                cursor, CLIENT_RECORD_VERSION_COLUMN_NAME);
                                long newClientRecordVersion =
                                        contentValues.getAsLong(CLIENT_RECORD_VERSION_COLUMN_NAME);

                                return newClientRecordVersion >= clientRecordVersion;
                            }
                        })
                .setChildTableRequests(getChildTableUpsertRequests((T) recordInternal))
                .setChildTablesWithRowsToBeDeletedDuringUpdate(
                        getChildTablesWithRowsToBeDeletedDuringUpdate(
                                extraWritePermissionToStateMap))
                .setPostUpsertCommands(getPostUpsertCommands(recordInternal));
    }

    /* Updates upsert content values based on extra permissions state. */
    protected void updateUpsertValuesIfRequired(
            ContentValues values,
            @Nullable ArrayMap<String, Boolean> extraWritePermissionToStateMap) {}

    /**
     * Returns child tables and the columns within them that references their parents. This is used
     * during updates to determine which child rows should be deleted.
     */
    public List<TableColumnPair> getChildTablesWithRowsToBeDeletedDuringUpdate(
            @Nullable ArrayMap<String, Boolean> extraWritePermissionToState) {
        return getAllChildTables().stream().map(it -> new TableColumnPair(it, PARENT_KEY)).toList();
    }

    public List<String> getAllChildTables() {
        List<String> childTables = new ArrayList<>();
        for (CreateTableRequest childTableCreateRequest : getChildTableCreateRequests()) {
            populateWithTablesNames(childTableCreateRequest, childTables);
        }

        return childTables;
    }

    protected List<CreateTableRequest.GeneratedColumnInfo> getGeneratedColumnInfo() {
        return Collections.emptyList();
    }

    private void populateWithTablesNames(
            CreateTableRequest childTableCreateRequest, List<String> childTables) {
        childTables.add(childTableCreateRequest.getTableName());
        for (CreateTableRequest childTableRequest :
                childTableCreateRequest.getChildTableRequests()) {
            populateWithTablesNames(childTableRequest, childTables);
        }
    }

    /**
     * Returns RecordReadTableRequest for { @code request} and package name { @code packageName},
     * including a DataPermissionEnforcer. This method is intended to be overridden by record
     * helpers that require special permission handling.
     *
     * @param request The read request describing what to read.
     * @param callingPackageName The package name of the app making this request.
     * @param enforceSelfRead Whether returned data should be filtered for data written by the
     *     calling app.
     * @param startDateAccessMillis The earliest time this app is allowed to read from.
     * @param grantedExtraReadPermissions List of permissions granted to this app to read associated
     *     data.
     * @param grantedGranularPermissions A set of granted granular permissions for the read
     *     operation. These will be used to filter data in cases where a record type is associated
     *     with multiple permissions, ensuring only data permitted based on these permissions is
     *     returned.
     * @param isInForeground If the calling app is in the foreground.
     * @param appInfoHelper The AppInfoHelper to use.
     */
    public RecordReadTableRequest getReadTableRequest(
            ReadRecordsRequestParcel request,
            String callingPackageName,
            boolean enforceSelfRead,
            long startDateAccessMillis,
            Set<String> grantedExtraReadPermissions,
            Set<String> grantedGranularPermissions,
            boolean isInForeground,
            AppInfoHelper appInfoHelper) {
        WhereClauses whereClause =
                getReadTableWhereClause(
                        request,
                        callingPackageName,
                        enforceSelfRead,
                        startDateAccessMillis,
                        appInfoHelper);
        addCustomReadTableWhereClauses(whereClause, grantedGranularPermissions);
        ReadTableRequest readTableRequest =
                new ReadTableRequest(getMainTableName())
                        .setJoinClause(getJoinForReadRequest())
                        .setWhereClause(whereClause)
                        .setOrderBy(getOrderByClause(request))
                        .setLimit(getLimitSize(request))
                        .setExtraReadRequests(
                                getExtraDataReadRequests(
                                        request,
                                        callingPackageName,
                                        startDateAccessMillis,
                                        grantedExtraReadPermissions,
                                        isInForeground,
                                        appInfoHelper));
        return new RecordReadTableRequest(readTableRequest, this);
    }

    /**
     * Logs metrics specific to a record type's insertion/update.
     *
     * @param statsLog the log to write to
     * @param recordInternals List of records being inserted/updated
     * @param packageName Caller package name
     */
    public void logUpsertMetrics(
            HealthFitnessStatsLog statsLog,
            List<RecordInternal<?>> recordInternals,
            String packageName) {
        // Do nothing, implement in record specific helpers
    }

    /**
     * Logs metrics specific to a record type's read.
     *
     * @param statsLog the log to write to
     * @param recordInternals List of records being read
     * @param packageName Caller package name
     */
    public void logReadMetrics(
            HealthFitnessStatsLog statsLog,
            List<RecordInternal<?>> recordInternals,
            String packageName) {
        // Do nothing, implement in record specific helpers
    }

    /**
     * Returns RecordReadTableRequest for {@code uuids}. This method is intended to be overridden by
     * record helpers that require special permission handling.
     *
     * @param packageName The package name of the app making this request.
     * @param uuids The list of UUIDs to read.
     * @param startDateAccess The earliest time this app is allowed to read from.
     * @param grantedExtraReadPermissions List of permissions granted to this app to read associated
     *     data.
     * @param grantedGranularPermissions A set of granted granular permissions for the read
     *     operation. These will be used to filter data in cases where a record type is associated
     *     with multiple permissions, ensuring only data permitted based on these permissions is
     *     returned.
     * @param isInForeground If the calling app is in the foreground.
     * @param appInfoHelper The AppInfoHelper to use.
     */
    public RecordReadTableRequest getReadTableRequest(
            String packageName,
            List<UUID> uuids,
            long startDateAccess,
            Set<String> grantedExtraReadPermissions,
            Set<String> grantedGranularPermissions,
            boolean isInForeground,
            AppInfoHelper appInfoHelper) {
        WhereClauses whereClause =
                new WhereClauses(AND)
                        .addWhereInClauseWithoutQuotes(
                                UUID_COLUMN_NAME, StorageUtils.getListOfHexStrings(uuids))
                        .addWhereLaterThanTimeClause(getStartTimeColumnName(), startDateAccess);
        addCustomReadTableWhereClauses(whereClause, grantedGranularPermissions);
        ReadTableRequest readTableRequest =
                new ReadTableRequest(getMainTableName())
                        .setJoinClause(getJoinForReadRequest())
                        .setWhereClause(whereClause)
                        .setExtraReadRequests(
                                getExtraDataReadRequests(
                                        packageName,
                                        uuids,
                                        startDateAccess,
                                        grantedExtraReadPermissions,
                                        isInForeground,
                                        appInfoHelper));
        return new RecordReadTableRequest(readTableRequest, this);
    }

    /**
     * Adds custom WHERE clauses for reading records. Subclasses can override this to add their own
     * filtering logic.
     *
     * @param whereClauses The WhereClauses object to add clauses to.
     * @param grantedGranularPermissions A set of granted granular permissions for the read
     *     operation.
     */
    protected void addCustomReadTableWhereClauses(
            WhereClauses whereClauses, Set<String> grantedGranularPermissions) {
        // Default is a no-op
    }

    /**
     * Returns a list of ReadSingleTableRequest for {@code request} and package name {@code
     * packageName} to populate extra data. Called in database read requests.
     */
    List<ReadTableRequest> getExtraDataReadRequests(
            ReadRecordsRequestParcel request,
            String packageName,
            long startDateAccess,
            Set<String> grantedExtraReadPermissions,
            boolean isInForeground,
            AppInfoHelper appInfoHelper) {
        return Collections.emptyList();
    }

    /**
     * Returns a list of ReadSingleTableRequest for {@code uuids} to populate extra data. Called in
     * change logs read requests.
     */
    List<ReadTableRequest> getExtraDataReadRequests(
            String packageName,
            List<UUID> uuids,
            long startDateAccess,
            Set<String> grantedExtraReadPermissions,
            boolean isInForeground,
            AppInfoHelper appInfoHelper) {
        return Collections.emptyList();
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

    /**
     * Returns List of Internal records from the cursor. If the cursor contains more than {@link
     * android.health.connect.Constants#MAXIMUM_ALLOWED_CURSOR_COUNT} records, it throws {@link
     * IllegalArgumentException}.
     */
    public List<RecordInternal<?>> getInternalRecords(
            Cursor cursor, DeviceInfoHelper deviceInfoHelper, AppInfoHelper appInfoHelper) {
        if (cursor.getCount() > MAXIMUM_ALLOWED_CURSOR_COUNT) {
            throw new IllegalArgumentException(
                    "Too many records in the cursor. Max allowed: " + MAXIMUM_ALLOWED_CURSOR_COUNT);
        }
        List<RecordInternal<?>> recordInternalList = new ArrayList<>();
        while (cursor.moveToNext()) {
            recordInternalList.add(
                    getRecord(
                            cursor,
                            /* packageNamesByAppIds= */ null,
                            deviceInfoHelper,
                            appInfoHelper));
        }
        return recordInternalList;
    }

    /**
     * Returns a list of Internal records from the cursor up to the requested size, with pagination
     * handled.
     *
     * @see #getNextInternalRecordsPageAndToken
     */
    public Pair<List<RecordInternal<?>>, PageTokenWrapper> getNextInternalRecordsPageAndToken(
            DeviceInfoHelper deviceInfoHelper,
            Cursor cursor,
            int requestSize,
            PageTokenWrapper pageToken,
            AppInfoHelper appInfoHelper) {
        return getNextInternalRecordsPageAndToken(
                deviceInfoHelper,
                cursor,
                requestSize,
                pageToken,
                /* packageNamesByAppIds= */ null,
                appInfoHelper);
    }

    /**
     * Returns List of Internal records from the cursor up to the requested size, with pagination
     * handled.
     *
     * <p>Note that the cursor limit is set to {@code requestSize + offset + 1},
     * <li>+ offset: {@code offset} records has already been returned in previous page(s). See
     *     go/hc-page-token for details.
     * <li>+ 1: if number of records queried is more than pageSize we know there are more records
     *     available to return for the next read.
     *
     *     <p>Note that the cursor may contain more records that we need to return. Cursor limit set
     *     to sum of the following:
     * <li>offset: {@code offset} records have already been returned in previous page(s), and should
     *     be skipped from this current page. In rare occasions (e.g. records deleted in between two
     *     reads), there are less than {@code offset} records, an empty list is returned, with no
     *     page token.
     * <li>requestSize: {@code requestSize} records to return in the response.
     * <li>one extra record: If there are more records than (offset+requestSize), a page token is
     *     returned for the next page. If not, then a default token is returned.
     *
     * @see #getLimitSize(ReadRecordsRequestParcel)
     */
    public Pair<List<RecordInternal<?>>, PageTokenWrapper> getNextInternalRecordsPageAndToken(
            DeviceInfoHelper deviceInfoHelper,
            Cursor cursor,
            int requestSize,
            PageTokenWrapper prevPageToken,
            @Nullable Map<Long, String> packageNamesByAppIds,
            AppInfoHelper appInfoHelper) {
        Slog.d("HealthConnectRecordHelper", "requestSize = " + requestSize);
        // Ignore <offset> records of the same start time, because it was returned in previous
        // page(s).
        // If the offset is greater than number of records in the cursor, it'll move to the last
        // index and will not enter the while loop below.
        long prevStartTime;
        long currentStartTime = DEFAULT_LONG;
        for (int i = 0; i < prevPageToken.offset(); i++) {
            if (!cursor.moveToNext()) {
                break;
            }
            prevStartTime = currentStartTime;
            currentStartTime = getCursorLong(cursor, getStartTimeColumnName());
            if (prevStartTime != DEFAULT_LONG && prevStartTime != currentStartTime) {
                // The current record should not be skipped
                cursor.moveToPrevious();
                break;
            }
        }

        currentStartTime = DEFAULT_LONG;
        int offset = 0;
        List<RecordInternal<?>> recordInternalList = new ArrayList<>();
        PageTokenWrapper nextPageToken = EMPTY_PAGE_TOKEN;
        while (cursor.moveToNext()) {
            prevStartTime = currentStartTime;
            currentStartTime = getCursorLong(cursor, getStartTimeColumnName());
            if (currentStartTime != prevStartTime) {
                offset = 0;
            }

            if (recordInternalList.size() >= requestSize) {
                nextPageToken =
                        PageTokenWrapper.of(prevPageToken.isAscending(), currentStartTime, offset);
                break;
            } else {
                T record = getRecord(cursor, packageNamesByAppIds, deviceInfoHelper, appInfoHelper);
                recordInternalList.add(record);
                offset++;
            }
        }
        return Pair.create(recordInternalList, nextPageToken);
    }

    private T getRecord(
            Cursor cursor,
            @Nullable Map<Long, String> packageNamesByAppIds,
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper) {
        T record = populateRecordValue(cursor);
        record.setUuid(getCursorUUID(cursor, UUID_COLUMN_NAME));
        record.setLastModifiedTime(getCursorLong(cursor, LAST_MODIFIED_TIME_COLUMN_NAME));
        record.setClientRecordId(getCursorString(cursor, CLIENT_RECORD_ID_COLUMN_NAME));
        record.setClientRecordVersion(getCursorLong(cursor, CLIENT_RECORD_VERSION_COLUMN_NAME));
        record.setRecordingMethod(getCursorInt(cursor, RECORDING_METHOD_COLUMN_NAME));
        record.setRowId(getCursorInt(cursor, PRIMARY_COLUMN_NAME));
        long deviceInfoId = getCursorLong(cursor, DEVICE_INFO_ID_COLUMN_NAME);
        deviceInfoHelper.populateRecordWithValue(deviceInfoId, record);
        long appInfoId = getCursorLong(cursor, APP_INFO_ID_COLUMN_NAME);
        try {
            String packageName =
                    packageNamesByAppIds != null
                            ? packageNamesByAppIds.get(appInfoId)
                            : appInfoHelper.getPackageName(appInfoId);
            record.setPackageName(packageName);
        } catch (PackageManager.NameNotFoundException exception) {
            Slog.e("HealthConnectRecordHelper", "Failed to read", exception);
            throw new IllegalArgumentException(exception);
        }
        record.setAppInfoId(appInfoId);
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            // TODO(b/459827738): Remove manual fixing when ddp caches have been properly set
            long ddpId = getCursorLong(cursor, DDP_ID_COLUMN_NAME);
            ddpId = (ddpId < 1L) ? DEFAULT_LONG : ddpId;
            record.setDeviceDataProviderId(ddpId);
        }

        return record;
    }

    /** Populate internalRecords fields using extraDataCursor */
    @SuppressWarnings("unchecked")
    public void updateInternalRecordsWithExtraFields(
            List<RecordInternal<?>> internalRecords, Cursor cursorExtraData) {
        readExtraData((List<T>) internalRecords, cursorExtraData);
    }

    public RecordDeleteTableRequest getDeleteTableRequest(
            @Nullable List<String> packageFilters,
            long startTime,
            long endTime,
            boolean usesLocalTimeFilter,
            AppInfoHelper appInfoHelper) {
        final String timeColumnName =
                usesLocalTimeFilter ? getLocalStartTimeColumnName() : getStartTimeColumnName();
        DeleteTableRequest deleteTableRequest =
                new DeleteTableRequest(getMainTableName())
                        .setTimeFilter(timeColumnName, startTime, endTime)
                        .setIdColumnName(UUID_COLUMN_NAME);
        if (packageFilters == null) {
            deleteTableRequest.setPackageColumnName(APP_INFO_ID_COLUMN_NAME);
        } else {
            deleteTableRequest.setPackageFilter(
                    APP_INFO_ID_COLUMN_NAME, appInfoHelper.getAppInfoIds(packageFilters));
        }
        return new RecordDeleteTableRequest(deleteTableRequest, getRecordIdentifier());
    }

    public RecordDeleteTableRequest getDeleteTableRequest(List<UUID> ids) {
        DeleteTableRequest deleteTableRequest =
                new DeleteTableRequest(getMainTableName())
                        .setPackageColumnName(APP_INFO_ID_COLUMN_NAME)
                        .setIds(UUID_COLUMN_NAME, StorageUtils.getListOfHexStrings(ids));
        return new RecordDeleteTableRequest(deleteTableRequest, getRecordIdentifier());
    }

    public RecordDeleteTableRequest getDeleteRequestForAutoDelete(
            int recordAutoDeletePeriodInDays) {
        DeleteTableRequest deleteTableRequest =
                new DeleteTableRequest(getMainTableName())
                        .setTimeFilter(
                                getStartTimeColumnName(),
                                Instant.EPOCH.toEpochMilli(),
                                Instant.now()
                                        .minus(recordAutoDeletePeriodInDays, ChronoUnit.DAYS)
                                        .toEpochMilli())
                        .setPackageColumnName(APP_INFO_ID_COLUMN_NAME)
                        .setIdColumnName(UUID_COLUMN_NAME);
        return new RecordDeleteTableRequest(deleteTableRequest, getRecordIdentifier());
    }

    public abstract String getPeriodGroupByColumnName();

    public abstract String getStartTimeColumnName();

    public abstract String getLocalStartTimeColumnName();

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    public String getLocalEndTimeColumnName() {
        return null;
    }

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    public String getEndTimeColumnName() {
        return null;
    }

    /** Populate internalRecords with extra data. */
    void readExtraData(List<T> internalRecords, Cursor cursorExtraData) {}

    /**
     * Child classes should implement this if it wants to create additional tables, apart from the
     * main table.
     */
    List<CreateTableRequest> getChildTableCreateRequests() {
        return Collections.emptyList();
    }

    /** Returns the table name to be created corresponding to this helper */
    public abstract String getMainTableName();

    /**
     * Returns the column name that holds the timestamps for samples in a series, where the record
     * type is a SeriesRecord
     */
    @Nullable
    public String getSampleTimestampsColumnName() {
        return null;
    }

    /**
     * Returns the information required to perform aggregate operation or null if this type is not
     * supported.
     */
    @Nullable
    AggregateParams getAggregateParams(AggregationType<?> aggregateRequest) {
        if (Flags.refactorAggregations()) {
            throw new UnsupportedOperationException("Not implemented by the subclass");
        }

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
    abstract List<Pair<String, String>> getSpecificColumnInfo();

    /**
     * Child classes implementation should add the values of {@code recordInternal} that needs to be
     * populated in the DB to {@code contentValues}.
     */
    abstract void populateContentValues(ContentValues contentValues, T recordInternal);

    /**
     * Child classes implementation should populate the values to the {@code record} using the
     * cursor {@code cursor} queried from the DB .
     */
    abstract T populateRecordValue(Cursor cursor);

    List<UpsertTableRequest> getChildTableUpsertRequests(T record) {
        return Collections.emptyList();
    }

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    SqlJoin getJoinForReadRequest() {
        return null;
    }

    static int getLimitSize(ReadRecordsRequestParcel request) {
        // Querying extra records on top of page size
        // + pageOffset: <pageOffset> records has already been returned in previous page(s). See
        //               go/hc-page-token for details.
        // + 1: if number of records queried is more than pageSize we know there are more records
        //      available to return for the next read.
        if (request.getRecordIdFiltersParcel() == null) {
            int pageOffset =
                    PageTokenWrapper.from(request.getPageToken(), request.isAscending()).offset();
            return request.getPageSize() + pageOffset + 1;
        } else {
            return MAXIMUM_PAGE_SIZE;
        }
    }

    final WhereClauses getReadTableWhereClause(
            ReadRecordsRequestParcel request,
            String callingPackageName,
            boolean enforceSelfRead,
            long startDateAccessMillis,
            AppInfoHelper appInfoHelper) {
        long callingAppInfoId = appInfoHelper.getAppInfoId(callingPackageName);
        RecordIdFiltersParcel recordIdFiltersParcel = request.getRecordIdFiltersParcel();
        if (recordIdFiltersParcel == null) {
            List<Long> appInfoIds =
                    appInfoHelper.getAppInfoIds(request.getPackageFilters()).stream()
                            .distinct()
                            .toList();
            if (enforceSelfRead) {
                appInfoIds = Collections.singletonList(callingAppInfoId);
            }
            if (appInfoIds.size() == 1 && appInfoIds.get(0) == DEFAULT_INT) {
                throw new TypeNotPresentException(TYPE_NOT_PRESENT_PACKAGE_NAME, new Throwable());
            }

            WhereClauses clauses = new WhereClauses(AND);

            // package names filter
            clauses.addWhereInLongsClause(APP_INFO_ID_COLUMN_NAME, appInfoIds);

            // page token filter
            PageTokenWrapper pageToken =
                    PageTokenWrapper.from(request.getPageToken(), request.isAscending());
            if (pageToken.isTimestampSet()) {
                long timestamp = pageToken.timeMillis();
                if (pageToken.isAscending()) {
                    clauses.addWhereGreaterThanOrEqualClause(getStartTimeColumnName(), timestamp);
                } else {
                    clauses.addWhereLessThanOrEqualClause(getStartTimeColumnName(), timestamp);
                }
            }

            // start/end time filter
            String timeColumnName =
                    request.usesLocalTimeFilter()
                            ? getLocalStartTimeColumnName()
                            : getStartTimeColumnName();
            long startTimeMillis = request.getStartTime();
            long endTimeMillis = request.getEndTime();
            if (startTimeMillis != DEFAULT_LONG) {
                clauses.addWhereGreaterThanOrEqualClause(timeColumnName, startTimeMillis);
            }
            if (endTimeMillis != DEFAULT_LONG) {
                clauses.addWhereLessThanClause(timeColumnName, endTimeMillis);
            }

            // start date access
            clauses.addNestedWhereClauses(
                    getFilterByStartAccessDateWhereClauses(
                            callingAppInfoId, startDateAccessMillis));

            return clauses;
        }

        // Since for now we don't support mixing IDs and filters, we need to look for IDs now
        List<UUID> ids =
                recordIdFiltersParcel.getRecordIdFilters().stream()
                        .map(
                                (recordIdFilter) ->
                                        StorageUtils.getUUIDFor(recordIdFilter, callingPackageName))
                        .toList();
        WhereClauses filterByIdsWhereClauses =
                new WhereClauses(AND)
                        .addWhereInClauseWithoutQuotes(
                                UUID_COLUMN_NAME, StorageUtils.getListOfHexStrings(ids));

        if (enforceSelfRead) {
            if (callingAppInfoId == DEFAULT_LONG) {
                throw new TypeNotPresentException(TYPE_NOT_PRESENT_PACKAGE_NAME, new Throwable());
            }
            // if self read is enforced, startDateAccess must not be applied.
            return filterByIdsWhereClauses.addWhereInLongsClause(
                    APP_INFO_ID_COLUMN_NAME, Collections.singletonList(callingAppInfoId));
        } else {
            return filterByIdsWhereClauses.addNestedWhereClauses(
                    getFilterByStartAccessDateWhereClauses(
                            callingAppInfoId, startDateAccessMillis));
        }
    }

    /**
     * Returns a {@link WhereClauses} that takes in to account start date access date & reading own
     * data.
     */
    private WhereClauses getFilterByStartAccessDateWhereClauses(
            long callingAppInfoId, long startDateAccessMillis) {
        WhereClauses resultWhereClauses = new WhereClauses(OR);

        // if the data point belongs to the calling app, then we should not enforce startDateAccess
        resultWhereClauses.addWhereEqualsClause(
                APP_INFO_ID_COLUMN_NAME, String.valueOf(callingAppInfoId));

        // Otherwise, we should enforce startDateAccess. Also we must use physical time column
        // regardless whether local time filter is used or not.
        String physicalTimeColumn = getStartTimeColumnName();
        resultWhereClauses.addWhereGreaterThanOrEqualClause(
                physicalTimeColumn, startDateAccessMillis);

        return resultWhereClauses;
    }

    abstract String getZoneOffsetColumnName();

    OrderByClause getOrderByClause(ReadRecordsRequestParcel request) {
        if (request.getRecordIdFiltersParcel() != null) {
            return new OrderByClause();
        }
        PageTokenWrapper pageToken =
                PageTokenWrapper.from(request.getPageToken(), request.isAscending());
        return new OrderByClause()
                .addOrderByClause(getStartTimeColumnName(), pageToken.isAscending())
                .addOrderByClause(PRIMARY_COLUMN_NAME, /* isAscending= */ true);
    }

    private ContentValues getContentValues(T recordInternal) {
        ContentValues recordContentValues = new ContentValues();

        recordContentValues.put(
                UUID_COLUMN_NAME, StorageUtils.convertUUIDToBytes(recordInternal.getUuid()));
        recordContentValues.put(
                LAST_MODIFIED_TIME_COLUMN_NAME, recordInternal.getLastModifiedTime());
        recordContentValues.put(CLIENT_RECORD_ID_COLUMN_NAME, recordInternal.getClientRecordId());
        recordContentValues.put(
                CLIENT_RECORD_VERSION_COLUMN_NAME, recordInternal.getClientRecordVersion());
        recordContentValues.put(RECORDING_METHOD_COLUMN_NAME, recordInternal.getRecordingMethod());
        recordContentValues.put(DEVICE_INFO_ID_COLUMN_NAME, recordInternal.getDeviceInfoId());
        recordContentValues.put(APP_INFO_ID_COLUMN_NAME, recordInternal.getAppInfoId());
        recordContentValues.put(DEDUPE_HASH_COLUMN_NAME, getDedupeByteBuffer(recordInternal));
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()
                && recordInternal.getDeviceDataProviderId() != DEFAULT_LONG) {
            // TODO(b/459827738): Remove manual extra check when ddp caches have been properly set
            recordContentValues.put(DDP_ID_COLUMN_NAME, recordInternal.getDeviceDataProviderId());
        }

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
    private List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT));
        columnInfo.add(new Pair<>(UUID_COLUMN_NAME, BLOB_UNIQUE_NON_NULL));
        columnInfo.add(new Pair<>(LAST_MODIFIED_TIME_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(CLIENT_RECORD_ID_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(CLIENT_RECORD_VERSION_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(DEVICE_INFO_ID_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(APP_INFO_ID_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(RECORDING_METHOD_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(DEDUPE_HASH_COLUMN_NAME, BLOB_UNIQUE_NULL));

        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            columnInfo.add(new Pair<>(DDP_ID_COLUMN_NAME, INTEGER));
        }

        columnInfo.addAll(getSpecificColumnInfo());

        return columnInfo;
    }

    /** Adds the required column to reference device data provider information */
    public AlterTableRequest getAlterTableRequestForDdpName() {
        var columns = List.of(new Pair<>(DDP_ID_COLUMN_NAME, INTEGER));
        return new AlterTableRequest(getMainTableName(), columns)
                .addForeignKeyConstraint(
                        DDP_ID_COLUMN_NAME,
                        DeviceDataProviderMetadataHelper.TABLE_NAME,
                        PRIMARY_COLUMN_NAME);
    }

    /** Returns permissions required to read extra record data. */
    public List<String> getExtraReadPermissions() {
        return Collections.emptyList();
    }

    /** Returns all extra permissions associated with current record type. */
    public List<String> getExtraWritePermissions() {
        return Collections.emptyList();
    }

    /** Returns extra permissions required to write given record. */
    public List<String> getRequiredExtraWritePermissions(RecordInternal<?> recordInternal) {
        return Collections.emptyList();
    }

    /**
     * Returns any SQL commands that should be executed after the provided record has been upserted.
     */
    List<String> getPostUpsertCommands(RecordInternal<?> record) {
        return Collections.emptyList();
    }

    /**
     * When a record is deleted, this will be called. The read requests must return a cursor with
     * {@link #UUID_COLUMN_NAME} and {@link #APP_INFO_ID_COLUMN_NAME} values. This information will
     * be used to generate modification changelogs for each UUID.
     *
     * <p>A concrete example of when this is used is for training plans. The deletion of a training
     * plan will nullify the 'plannedExerciseSessionId' field of any exercise sessions that
     * referenced it. When a training plan is deleted, a read request is made on the exercise
     * session table to find any exercise sessions that referenced it.
     */
    public List<RecordReadTableRequest> getReadRequestsForRecordsModifiedByDeletion(
            UUID deletedRecordUuid) {
        return Collections.emptyList();
    }

    /**
     * When a record is upserted, this will be called. The read requests must return a cursor with a
     * {@link #UUID_COLUMN_NAME} and {@link #APP_INFO_ID_COLUMN_NAME} values. This information will
     * be used to generate modification changelogs for each UUID.
     *
     * <p>A concrete example of when this is used is for training plans. The upsertion of an
     * exercise session may modify the 'completedSessionId' field of any planned sessions that
     * referenced it.
     */
    public List<RecordReadTableRequest> getReadRequestsForRecordsModifiedByUpsertion(
            RecordInternal<?> record) {
        return Collections.emptyList();
    }
}
