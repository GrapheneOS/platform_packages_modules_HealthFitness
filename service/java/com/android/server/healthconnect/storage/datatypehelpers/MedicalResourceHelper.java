/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.MAXIMUM_ALLOWED_CURSOR_COUNT;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_DELETE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_READ;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_UPSERT;
import static android.health.connect.datatypes.FhirVersion.parseFhirVersion;

import static com.android.server.healthconnect.storage.HealthConnectDatabase.createTable;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.getDataSourceUuidColumnName;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.getFhirVersionColumnName;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.getReadTableWhereClause;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper.getCreateMedicalResourceIndicesTableRequest;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper.getMedicalResourceTypeColumnName;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.SqlJoin.INNER_QUERY_ALIAS;
import static com.android.server.healthconnect.storage.utils.SqlJoin.SQL_JOIN_INNER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.DELIMITER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLongList;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorUUID;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getListOfHexStrings;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.health.connect.Constants;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.MedicalResource.MedicalResourceType;
import android.util.Pair;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.fitness.aggregation.AggregateRecordRequest;
import com.android.server.healthconnect.phr.PhrPageTokenWrapper;
import com.android.server.healthconnect.phr.ReadMedicalResourcesInternalResponse;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.TransactionManager.RunnableWithReturn;
import com.android.server.healthconnect.storage.request.CreateIndexRequest;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertMedicalResourceInternalRequest;
import com.android.server.healthconnect.storage.utils.OrderByClause;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;
import com.android.server.healthconnect.utils.TimeSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Helper class for MedicalResource table.
 *
 * @hide
 */
public final class MedicalResourceHelper {
    private static final String TAG = "MedicalResourceHelper";
    @VisibleForTesting static final String MEDICAL_RESOURCE_TABLE_NAME = "medical_resource_table";
    private static final String MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME = "medical_resource_row_id";
    @VisibleForTesting static final String FHIR_RESOURCE_TYPE_COLUMN_NAME = "fhir_resource_type";
    @VisibleForTesting static final String FHIR_DATA_COLUMN_NAME = "fhir_data";

    @VisibleForTesting static final String DATA_SOURCE_ID_COLUMN_NAME = "data_source_id";
    @VisibleForTesting static final String FHIR_RESOURCE_ID_COLUMN_NAME = "fhir_resource_id";
    private static final String LAST_MODIFIED_TIMESTAMP_MEDICAL_RESOURCE_ALIAS =
            "medical_resource_last_modified_time";

    private static final String sLastModifiedTimeInInnerQuery =
            String.format(
                    "%1$s.%2$s AS %3$s",
                    INNER_QUERY_ALIAS,
                    LAST_MODIFIED_TIME_COLUMN_NAME,
                    LAST_MODIFIED_TIMESTAMP_MEDICAL_RESOURCE_ALIAS);

    private static final String sMedicalResourceLastModifiedTime =
            String.format(
                    "%1$s.%2$s AS %3$s",
                    getMainTableName(),
                    LAST_MODIFIED_TIME_COLUMN_NAME,
                    LAST_MODIFIED_TIMESTAMP_MEDICAL_RESOURCE_ALIAS);

    private static final List<String> sMedicalResourceColumns =
            List.of(
                    MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME,
                    FHIR_RESOURCE_TYPE_COLUMN_NAME,
                    FHIR_RESOURCE_ID_COLUMN_NAME,
                    FHIR_DATA_COLUMN_NAME,
                    MedicalDataSourceHelper.getFhirVersionColumnName(),
                    MedicalResourceIndicesHelper.getMedicalResourceTypeColumnName(),
                    MedicalDataSourceHelper.getDataSourceUuidColumnName());

    /**
     * A block of SQL with a where clause to read based on the medical resource id composite key.
     *
     * <p>For it to be syntactically correct it needs to have the result from {@link
     * #makeParametersAndArgs(List, Long)} appended to it. Both the resources table and the data
     * sources table must be joined in the SELECT which uses this clause.
     */
    private static final String SELECT_ON_IDS_WHERE_CLAUSE =
            "("
                    + MedicalDataSourceHelper.getMainTableName()
                    + "."
                    + MedicalDataSourceHelper.getDataSourceUuidColumnName()
                    + ","
                    + MEDICAL_RESOURCE_TABLE_NAME
                    + "."
                    + FHIR_RESOURCE_TYPE_COLUMN_NAME
                    + ","
                    + MEDICAL_RESOURCE_TABLE_NAME
                    + "."
                    + FHIR_RESOURCE_ID_COLUMN_NAME
                    + ") IN ";

    /**
     * A block of SQL with the inner select where clause for deleting based on the medical resource
     * id.
     *
     * <p>For it to be syntactically correct it needs to have the result from {@link
     * #makeParametersAndArgs} appended to it, followed by a ")"
     */
    // The SQL here is made more complicated because:
    // 1. The medical resource table has a 3 column composite primary key, on data source,
    // resource type, and resource id.
    // 2. Data source is a reference to another table, so a JOIN is needed
    // 3. SQLite does not allow JOIN in the FROM clause of a delete. This means an inner
    // SELECT is needed to reference the ids.
    // 4. You can't bind a list to SQL to put a dynamic range of values into an "IN" list
    // However, SQLite has a row_id value for every row which simplifies things.
    // We end up with a select clause looking like:
    // DELETE FROM resources WHERE medical_resource_row_id IN (
    //   SELECT medical_resource_row_id FROM resources INNER JOIN datasources ON ...
    //     WHERE ..key columns.. IN ( (?,?,?), (?,?,?), ...)
    private static final String DELETE_ON_IDS_WHERE_CLAUSE =
            MEDICAL_RESOURCE_TABLE_NAME
                    + "."
                    + MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME
                    + " IN ("
                    + "SELECT "
                    + MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME
                    + " FROM "
                    + MEDICAL_RESOURCE_TABLE_NAME
                    + " INNER JOIN "
                    + MedicalDataSourceHelper.getMainTableName()
                    + " ON "
                    + MEDICAL_RESOURCE_TABLE_NAME
                    + "."
                    + DATA_SOURCE_ID_COLUMN_NAME
                    + "="
                    + MedicalDataSourceHelper.getMainTableName()
                    + "."
                    + MedicalDataSourceHelper.getPrimaryColumnName()
                    + " WHERE ("
                    + MedicalDataSourceHelper.getMainTableName()
                    + "."
                    + MedicalDataSourceHelper.getDataSourceUuidColumnName()
                    + ","
                    + FHIR_RESOURCE_TYPE_COLUMN_NAME
                    + ","
                    + FHIR_RESOURCE_ID_COLUMN_NAME
                    + ") IN ";

    /**
     * An SQL string joining the three key tables for resource information - resources, data sources
     * and the index with resource types. Suitable for using in a FROM clause in a select.
     */
    private static final String RESOURCES_JOIN_DATA_SOURCES_JOIN_INDICES =
            MEDICAL_RESOURCE_TABLE_NAME
                    + " INNER JOIN "
                    + MedicalResourceIndicesHelper.getTableName()
                    + " ON "
                    + MEDICAL_RESOURCE_TABLE_NAME
                    + "."
                    + MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME
                    + " = "
                    + MedicalResourceIndicesHelper.getTableName()
                    + "."
                    + MedicalResourceIndicesHelper.getParentColumnReference()
                    + " INNER JOIN "
                    + MedicalDataSourceHelper.getMainTableName()
                    + " ON "
                    + MEDICAL_RESOURCE_TABLE_NAME
                    + "."
                    + DATA_SOURCE_ID_COLUMN_NAME
                    + " = "
                    + MedicalDataSourceHelper.getMainTableName()
                    + "."
                    + MedicalDataSourceHelper.getPrimaryColumnName();

    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;
    private final MedicalDataSourceHelper mMedicalDataSourceHelper;
    private final TimeSource mTimeSource;
    private final AccessLogsHelper mAccessLogsHelper;

    public MedicalResourceHelper(
            TransactionManager transactionManager,
            AppInfoHelper appInfoHelper,
            MedicalDataSourceHelper medicalDataSourceHelper,
            TimeSource timeSource,
            AccessLogsHelper accessLogsHelper) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mMedicalDataSourceHelper = medicalDataSourceHelper;
        mTimeSource = timeSource;
        mAccessLogsHelper = accessLogsHelper;
    }

    public static String getMainTableName() {
        return MEDICAL_RESOURCE_TABLE_NAME;
    }

    public static String getPrimaryColumn() {
        return MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME;
    }

    public static String getDataSourceIdColumnName() {
        return DATA_SOURCE_ID_COLUMN_NAME;
    }

    private static String getMedicalResourceColumns() {
        List<String> medicalResourceColumns = new ArrayList<>(sMedicalResourceColumns);
        medicalResourceColumns.add(sMedicalResourceLastModifiedTime);
        return String.join(DELIMITER, medicalResourceColumns);
    }

    private static List<Pair<String, String>> getColumnInfo() {
        return List.of(
                Pair.create(MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT),
                Pair.create(FHIR_RESOURCE_TYPE_COLUMN_NAME, INTEGER_NOT_NULL),
                Pair.create(FHIR_RESOURCE_ID_COLUMN_NAME, TEXT_NOT_NULL),
                Pair.create(FHIR_DATA_COLUMN_NAME, TEXT_NOT_NULL),
                Pair.create(DATA_SOURCE_ID_COLUMN_NAME, INTEGER_NOT_NULL),
                Pair.create(LAST_MODIFIED_TIME_COLUMN_NAME, INTEGER_NOT_NULL));
    }

    // TODO(b/352010531): Remove the use of setChildTableRequests and upsert child table directly
    // in {@code upsertMedicalResources} to improve readability.

    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(MEDICAL_RESOURCE_TABLE_NAME, getColumnInfo())
                .addForeignKey(
                        MedicalDataSourceHelper.getMainTableName(),
                        Collections.singletonList(DATA_SOURCE_ID_COLUMN_NAME),
                        Collections.singletonList(MedicalDataSourceHelper.getPrimaryColumnName()))
                .createIndexOn(LAST_MODIFIED_TIME_COLUMN_NAME)
                .setChildTableRequests(
                        Collections.singletonList(getCreateMedicalResourceIndicesTableRequest()));
    }

    /** Creates the medical_resource table. */
    public static void onInitialUpgrade(SQLiteDatabase db) {
        createTable(db, getCreateTableRequest());
        // There are 3 equivalent ways we could add the (Datasource, type, id) triple as a primary
        // key - primary key, unique index, or unique constraint.
        // Primary Key and unique constraints cannot be altered after table creation. Indexes can be
        // dropped later and added to. So it seems most flexible to add as a named index.
        db.execSQL(
                new CreateIndexRequest(
                                MEDICAL_RESOURCE_TABLE_NAME,
                                MEDICAL_RESOURCE_TABLE_NAME + "_fhir_idx",
                                /* isUnique= */ true,
                                List.of(
                                        DATA_SOURCE_ID_COLUMN_NAME,
                                        FHIR_RESOURCE_TYPE_COLUMN_NAME,
                                        FHIR_RESOURCE_ID_COLUMN_NAME))
                        .getCommand());
    }

    /** Returns the total number of medical resources in HC database. */
    public int getMedicalResourcesCount() {
        ReadTableRequest readTableRequest = new ReadTableRequest(getMainTableName());
        return mTransactionManager.count(readTableRequest);
    }

    /**
     * Reads the {@link MedicalResource}s stored in the HealthConnect database.
     *
     * @param medicalResourceIds a {@link MedicalResourceId}.
     * @return List of {@link MedicalResource}s read from medical_resource table based on ids.
     * @throws IllegalArgumentException if any of the ids has a data source id which is not valid
     *     (not a String form of a UUID)
     */
    public List<MedicalResource> readMedicalResourcesByIdsWithoutPermissionChecks(
            List<MedicalResourceId> medicalResourceIds) throws SQLiteException {
        if (medicalResourceIds.isEmpty()) {
            return List.of();
        }
        Pair<String, String[]> paramsAndArgs =
                makeParametersAndArgs(medicalResourceIds, /* appId= */ null);
        String sql =
                "SELECT "
                        + getMedicalResourceColumns()
                        + " FROM "
                        + RESOURCES_JOIN_DATA_SOURCES_JOIN_INDICES
                        + " WHERE "
                        + SELECT_ON_IDS_WHERE_CLAUSE
                        + paramsAndArgs.first;
        List<MedicalResource> medicalResources;
        try (Cursor cursor = mTransactionManager.rawQuery(sql, paramsAndArgs.second)) {
            medicalResources = getMedicalResources(cursor);
        }
        return medicalResources;
    }

    /**
     * Reads the {@link MedicalResource}s stored in the HealthConnect database filtering based on
     * the {@code callingPackageName}'s permissions.
     *
     * @return List of {@link MedicalResource}s read from medical_resource table based on ids.
     * @throws IllegalStateException if {@code hasWritePermission} is false and {@code
     *     grantedReadMedicalResourceTypes} is empty.
     * @throws IllegalArgumentException if any of the ids has a data source id which is not valid
     *     (not a String form of a UUID)
     */
    public List<MedicalResource> readMedicalResourcesByIdsWithPermissionChecks(
            List<MedicalResourceId> medicalResourceIds,
            Set<Integer> grantedReadMedicalResourceTypes,
            String callingPackageName,
            boolean hasWritePermission,
            boolean isCalledFromBgWithoutBgRead)
            throws SQLiteException {

        Pair<String, String[]> sqlAndArgs =
                getSqlAndArgsBasedOnPermissionFilters(
                        medicalResourceIds,
                        grantedReadMedicalResourceTypes,
                        callingPackageName,
                        hasWritePermission,
                        isCalledFromBgWithoutBgRead);
        return mTransactionManager.runAsTransaction(
                db -> {
                    List<MedicalResource> medicalResources;
                    try (Cursor cursor = db.rawQuery(sqlAndArgs.first, sqlAndArgs.second)) {
                        medicalResources = getMedicalResources(cursor);
                    }
                    // If the app is called from background but without background read permission,
                    // the most the app can do, is to read their own data. Same when the
                    // grantedReadMedicalResourceTypes is empty. And we don't need to add access
                    // logs when an app intends to access their own data.
                    // If medicalResources is empty, it means that we haven't read any resources
                    // out, so no need to add access logs either.
                    if (!isCalledFromBgWithoutBgRead
                            && !grantedReadMedicalResourceTypes.isEmpty()
                            && !medicalResources.isEmpty()) {
                        // We do this to get resourceTypes that are read due to the calling app
                        // having a read permission for it. If the resources returned, were read
                        // due to selfRead only, no access logs should be created.
                        // However if the resources read were written by the app itself, but the
                        // app also had read permissions for those resources, we don't record
                        // this as selfRead and access log is added.
                        Set<Integer> resourceTypes =
                                getIntersectionOfResourceTypesReadAndGrantedReadPermissions(
                                        getResourceTypesRead(medicalResources),
                                        grantedReadMedicalResourceTypes);
                        if (!resourceTypes.isEmpty()) {
                            mAccessLogsHelper.addAccessLog(
                                    db,
                                    callingPackageName,
                                    resourceTypes,
                                    OPERATION_TYPE_READ,
                                    /* accessedMedicalDataSource= */ false);
                        }
                    }
                    return medicalResources;
                });
    }

    /**
     * Reads from the storage and creates a map between {@link MedicalResourceType}s and all its
     * contributing {@link MedicalDataSource}s.
     *
     * <p>This map does not guarantee to contain all the valid {@link MedicalResourceType}s we
     * support, but only contain those we have data for in the storage.
     */
    public Map<Integer, Set<MedicalDataSource>>
            getMedicalResourceTypeToContributingDataSourcesMap() {
        return mTransactionManager.runAsTransaction(
                db -> {
                    Map<Long, MedicalDataSource> allRowIdToDataSourceMap =
                            mMedicalDataSourceHelper.getAllRowIdToDataSourceMap(db);
                    Map<Integer, List<Long>> resourceTypeToDataSourceIdsMap =
                            getMedicalResourceTypeToDataSourceIdsMap(db);
                    return resourceTypeToDataSourceIdsMap.keySet().stream()
                            .collect(
                                    toMap(
                                            medicalResourceType -> medicalResourceType,
                                            medicalResourceType ->
                                                    resourceTypeToDataSourceIdsMap
                                                            .getOrDefault(
                                                                    medicalResourceType, List.of())
                                                            .stream()
                                                            .map(allRowIdToDataSourceMap::get)
                                                            // This should not happen, but we
                                                            // filter out nulls for extra safe.
                                                            .filter(Objects::nonNull)
                                                            .collect(toSet())));
                });
    }

    private Map<Integer, List<Long>> getMedicalResourceTypeToDataSourceIdsMap(SQLiteDatabase db) {
        String readMainTableQuery = getReadQueryForMedicalResourceTypeToDataSourceIdsMap();
        Map<Integer, List<Long>> resourceTypeToDataSourceIdsMap = new HashMap<>();
        try (Cursor cursor = db.rawQuery(readMainTableQuery, /* selectionArgs= */ null)) {
            if (cursor.moveToFirst()) {
                do {
                    int medicalResourceType =
                            getCursorInt(cursor, getMedicalResourceTypeColumnName());
                    List<Long> dataSourceIds =
                            getCursorLongList(cursor, DATA_SOURCE_ID_COLUMN_NAME, DELIMITER);
                    resourceTypeToDataSourceIdsMap.put(medicalResourceType, dataSourceIds);
                } while (cursor.moveToNext());
            }
        }
        return resourceTypeToDataSourceIdsMap;
    }

    static Set<Integer> getIntersectionOfResourceTypesReadAndGrantedReadPermissions(
            Set<Integer> resourceTypesRead, Set<Integer> grantedReadPerms) {
        Set<Integer> intersection = new HashSet<>(resourceTypesRead);
        intersection.retainAll(grantedReadPerms);
        return intersection;
    }

    private static Set<Integer> getResourceTypesRead(List<MedicalResource> resources) {
        return resources.stream().map(MedicalResource::getType).collect(Collectors.toSet());
    }

    /**
     * Returns an SQL query and the selection arguments for that query to get medical resources,
     * based on permission values.
     *
     * @throws IllegalArgumentException if any of the ids has a data source id which is not valid
     *     (not a String form of a UUID)
     */
    private Pair<String, String[]> getSqlAndArgsBasedOnPermissionFilters(
            List<MedicalResourceId> medicalResourceIds,
            Set<Integer> grantedReadMedicalResourceTypes,
            String callingPackageName,
            boolean hasWritePermission,
            boolean isCalledFromBgWithoutBgRead) {
        if (!hasWritePermission && grantedReadMedicalResourceTypes.isEmpty()) {
            throw new IllegalStateException("no read or write permission");
        }
        long appId = mAppInfoHelper.getAppInfoId(callingPackageName);
        // App is calling the API from background without backgroundReadPermission.
        if (isCalledFromBgWithoutBgRead) {
            // App has writePermission.
            // App can read all data they wrote themselves.
            if (hasWritePermission) {
                return readAllIdsWrittenByCallingPackage(medicalResourceIds, appId);
            }
            // App does not have writePermission.
            // App has normal read permission for some medicalResourceTypes.
            // App can read the ids that belong to those medicalResourceTypes and was written by the
            // app itself.
            return readResourcesByIdsAppIdResourceTypes(
                    medicalResourceIds,
                    appId,
                    LogicalOperator.AND,
                    grantedReadMedicalResourceTypes);
        }

        // App is in background with backgroundReadPermission or in foreground.
        // App has writePermission.
        if (hasWritePermission) {
            // App does not have any read permissions for any medicalResourceType.
            // App can read all data they wrote themselves.
            if (grantedReadMedicalResourceTypes.isEmpty()) {
                return readAllIdsWrittenByCallingPackage(medicalResourceIds, appId);
            }
            // App has some read permissions for medicalResourceTypes.
            // App can read all data they wrote themselves and the medicalResourceTypes they have
            // read permission for.
            return readResourcesByIdsAppIdResourceTypes(
                    medicalResourceIds, appId, LogicalOperator.OR, grantedReadMedicalResourceTypes);
        }
        // App is in background with backgroundReadPermission or in foreground.
        // App has some read permissions for medicalResourceTypes.
        // App does not have writePermission.
        // App can read all data of the granted medicalResourceType read permissions.
        return readResourcesByIdsAppIdResourceTypes(
                medicalResourceIds,
                /* appId= */ null,
                LogicalOperator.AND,
                grantedReadMedicalResourceTypes);
    }

    private static Pair<String, String[]> readAllIdsWrittenByCallingPackage(
            List<MedicalResourceId> medicalResourceIds, long appId) {
        Pair<String, String[]> paramsAndArgs = makeParametersAndArgs(medicalResourceIds, appId);
        return Pair.create(
                "SELECT "
                        + getMedicalResourceColumns()
                        + " FROM "
                        + RESOURCES_JOIN_DATA_SOURCES_JOIN_INDICES
                        + " WHERE "
                        + SELECT_ON_IDS_WHERE_CLAUSE
                        + paramsAndArgs.first,
                paramsAndArgs.second);
    }

    /**
     * Reads the {@link MedicalResource}s stored in the HealthConnect database by {@code request}.
     *
     * @param pageTokenWrapper a {@link PhrPageTokenWrapper}.
     * @return a {@link ReadMedicalResourcesInternalResponse}.
     */
    public ReadMedicalResourcesInternalResponse
            readMedicalResourcesByRequestWithoutPermissionChecks(
                    PhrPageTokenWrapper pageTokenWrapper, int pageSize) {
        ReadTableRequest request =
                getReadTableRequestUsingRequestFilters(pageTokenWrapper, pageSize);

        return mTransactionManager.runAsTransaction(
                (db) -> {
                    return getMedicalResources(db, request, pageTokenWrapper, pageSize);
                });
    }

    /**
     * Reads the {@link MedicalResource}s stored in the HealthConnect database by {@code request}
     * filtering based on {@code callingPackageName}'s permissions.
     *
     * @return a {@link ReadMedicalResourcesInternalResponse}.
     */
    // TODO(b/360352345): Add cts tests for access logs being created per API call.

    public ReadMedicalResourcesInternalResponse readMedicalResourcesByRequestWithPermissionChecks(
            PhrPageTokenWrapper pageTokenWrapper,
            int pageSize,
            String callingPackageName,
            boolean enforceSelfRead) {
        ReadMedicalResourcesInitialRequest request = pageTokenWrapper.getRequest();
        if (request == null) {
            throw new IllegalStateException("The pageTokenWrapper's request can not be null.");
        }
        return mTransactionManager.runAsTransaction(
                db -> {
                    ReadMedicalResourcesInternalResponse response;
                    ReadTableRequest readTableRequest =
                            getReadTableRequestUsingRequestBasedOnPermissionFilters(
                                    pageTokenWrapper,
                                    pageSize,
                                    callingPackageName,
                                    enforceSelfRead);
                    response =
                            getMedicalResources(db, readTableRequest, pageTokenWrapper, pageSize);
                    if (!enforceSelfRead) {
                        mAccessLogsHelper.addAccessLog(
                                db,
                                callingPackageName,
                                Set.of(request.getMedicalResourceType()),
                                OPERATION_TYPE_READ,
                                /* accessedMedicalDataSource= */ false);
                    }
                    return response;
                });
    }

    private ReadTableRequest getReadTableRequestUsingRequestBasedOnPermissionFilters(
            PhrPageTokenWrapper pageTokenWrapper,
            int pageSize,
            String callingPackageName,
            boolean enforceSelfRead) {
        // If this is true, app can only read its own data of the given filters set in the request.
        if (enforceSelfRead) {
            long appId = mAppInfoHelper.getAppInfoId(callingPackageName);
            return getReadTableRequestUsingRequestFiltersAndAppId(
                    pageTokenWrapper, pageSize, appId);
        }
        // Otherwise, app can read all data of the given filters.
        return getReadTableRequestUsingRequestFilters(pageTokenWrapper, pageSize);
    }

    /** Creates {@link ReadTableRequest} for the given {@link PhrPageTokenWrapper}. */
    public static ReadTableRequest getReadTableRequestUsingRequestFilters(
            PhrPageTokenWrapper pageTokenWrapper, int pageSize) {
        // The INNER_QUERY_ALIAS refers to the medical_resource_table.
        List<String> allColumns = new ArrayList<>(sMedicalResourceColumns);
        allColumns.add(sLastModifiedTimeInInnerQuery);
        ReadTableRequest readTableRequest =
                getReadTableRequestUsingPageSizeAndLastRowId(
                                pageSize, pageTokenWrapper.getLastRowId())
                        .setColumnNames(allColumns);
        ReadMedicalResourcesInitialRequest request = pageTokenWrapper.getRequest();
        SqlJoin joinClause;
        if (request == null) {
            // If request is null, it means the request is to read out all the data without
            // any filters applied. So we just join the tables without any filtering on them.
            joinClause =
                    joinWithMedicalResourceIndicesTable()
                            .attachJoin(joinWithMedicalDataSourceTable());
        } else if (request.getDataSourceIds().isEmpty()) {
            joinClause =
                    getJoinWithIndicesAndDataSourceTablesFilterOnMedicalResourceTypes(
                            Set.of(request.getMedicalResourceType()));
        } else {
            List<UUID> dataSourceUuids = StorageUtils.toUuids(request.getDataSourceIds());
            joinClause =
                    getJoinWithIndicesAndDataSourceTablesFilterOnMedicalResourceTypesAndSourceIds(
                            Set.of(request.getMedicalResourceType()), dataSourceUuids);
        }
        return readTableRequest.setJoinClause(joinClause);
    }

    /**
     * Creates {@link ReadTableRequest} for the given {@link PhrPageTokenWrapper} and {@code
     * callingPackageName}.
     */
    private static ReadTableRequest getReadTableRequestUsingRequestFiltersAndAppId(
            PhrPageTokenWrapper pageTokenWrapper, int pageSize, long appId) {
        ReadMedicalResourcesInitialRequest request = pageTokenWrapper.getRequest();
        if (request == null) {
            throw new IllegalArgumentException("Request can't be null when doing a filtered read.");
        }
        List<String> allColumns = new ArrayList<>(sMedicalResourceColumns);
        allColumns.add(sLastModifiedTimeInInnerQuery);
        ReadTableRequest readTableRequest =
                getReadTableRequestUsingPageSizeAndLastRowId(
                                pageSize, pageTokenWrapper.getLastRowId())
                        .setColumnNames(allColumns);
        SqlJoin joinClause;
        if (request.getDataSourceIds().isEmpty()) {
            joinClause =
                    getJoinWithIndicesAndDataSourceTablesFilterOnMedicalResourceTypesAndAppId(
                            Set.of(request.getMedicalResourceType()), appId);
        } else {
            List<UUID> dataSourceUuids = StorageUtils.toUuids(request.getDataSourceIds());
            joinClause =
                    getJoinWithIndicesAndDataSourceTablesFilterOnTypesAndSourceIdsAndAppId(
                            Set.of(request.getMedicalResourceType()), dataSourceUuids, appId);
        }
        return readTableRequest.setJoinClause(joinClause);
    }

    private static ReadTableRequest getReadTableRequestUsingPageSizeAndLastRowId(
            int pageSize, long lastRowId) {
        // The limit is set to pageSize + 1, so that we know if there are more resources
        // than the pageSize for creating the pageToken.
        ReadTableRequest request =
                new ReadTableRequest(getMainTableName())
                        .setWhereClause(getReadByLastRowIdWhereClause(lastRowId));

        if (Flags.phrReadMedicalResourcesFixQueryLimit()) {
            request.setFinalOrderBy(getOrderByClause()).setFinalLimit(pageSize + 1);
        } else {
            request.setOrderBy(getOrderByClause()).setLimit(pageSize + 1);
        }

        return request;
    }

    static ReadTableRequest getReadRequestForDistinctResourceTypesBelongingToDataSourceIds(
            List<UUID> dataSourceIds) {
        return new ReadTableRequest(getMainTableName())
                .setDistinctClause(true)
                .setColumnNames(
                        List.of(MedicalResourceIndicesHelper.getMedicalResourceTypeColumnName()))
                .setJoinClause(
                        getJoinWithMedicalDataSourceFilterOnDataSourceIds(
                                dataSourceIds, joinWithMedicalResourceIndicesTable()));
    }

    @VisibleForTesting
    static ReadTableRequest getFilteredReadRequestForDistinctResourceTypes(
            List<UUID> dataSourceIds, Set<Integer> medicalResourceTypes, long appId) {
        return new ReadTableRequest(getMainTableName())
                .setDistinctClause(true)
                .setColumnNames(
                        List.of(MedicalResourceIndicesHelper.getMedicalResourceTypeColumnName()))
                .setJoinClause(
                        getJoinWithMedicalDataSourceFilterOnDataSourceIdsAndAppId(
                                dataSourceIds,
                                appId,
                                getJoinWithIndicesTableFilterOnMedicalResourceTypes(
                                        medicalResourceTypes)));
    }

    /**
     * Creates raw SQL query for {@link
     * MedicalResourceHelper#getMedicalResourceTypeToDataSourceIdsMap}.
     *
     * <p>"GROUP BY" is not supported in {@link ReadTableRequest} and should be achieved via {@link
     * AggregateRecordRequest}. But the {@link AggregateRecordRequest} is too complicated for our
     * simple use case here (requiring {@link RecordHelper}). Thus we just build and return raw SQL
     * query which appends the "GROUP BY" clause directly.
     */
    @VisibleForTesting
    static String getReadQueryForMedicalResourceTypeToDataSourceIdsMap() {
        ReadTableRequest readDistinctResourceTypeToDataSourceIdRequest =
                new ReadTableRequest(getMainTableName())
                        .setDistinctClause(true)
                        .setColumnNames(
                                List.of(
                                        getMedicalResourceTypeColumnName(),
                                        DATA_SOURCE_ID_COLUMN_NAME))
                        .setJoinClause(joinWithMedicalResourceIndicesTable());

        return String.format(
                "SELECT %1$s, GROUP_CONCAT(%2$s, '%3$s') AS %4$s FROM (%5$s) GROUP BY %6$s",
                /* 1 */ getMedicalResourceTypeColumnName(),
                /* 2 */ DATA_SOURCE_ID_COLUMN_NAME,
                /* 3 */ DELIMITER,
                /* 4 */ DATA_SOURCE_ID_COLUMN_NAME,
                /* 5 */ readDistinctResourceTypeToDataSourceIdRequest.getReadCommand(),
                /* 6 */ getMedicalResourceTypeColumnName());
    }

    /**
     * Creates {@link SqlJoin} that is an inner join from medical_resource_table to
     * medical_resource_indices_table followed by another inner join from medical_resource_table to
     * medical_data_source_table.
     */
    private static SqlJoin getJoinWithIndicesAndDataSourceTables() {
        return joinWithMedicalResourceIndicesTable().attachJoin(joinWithMedicalDataSourceTable());
    }

    /**
     * Creates {@link SqlJoin} that is an inner join from medical_resource_table to
     * medical_resource_indices_table filtering on {@code medicalResourceTypes} followed by another
     * inner join from medical_resource_table to medical_data_source_table.
     */
    private static SqlJoin getJoinWithIndicesAndDataSourceTablesFilterOnMedicalResourceTypes(
            Set<Integer> medicalResourceTypes) {
        return getJoinWithIndicesTableFilterOnMedicalResourceTypes(medicalResourceTypes)
                .attachJoin(joinWithMedicalDataSourceTable());
    }

    /**
     * Creates {@link SqlJoin} that is an inner join from medical_resource_table to
     * medical_resource_indices_table filtering on {@code medicalResourceTypes} followed by another
     * inner join from medical_resource_table to medical_data_source_table filtering on appId.
     */
    private static SqlJoin
            getJoinWithIndicesAndDataSourceTablesFilterOnMedicalResourceTypesAndAppId(
                    Set<Integer> medicalResourceTypes, long appId) {
        return getJoinWithIndicesTableFilterOnMedicalResourceTypes(medicalResourceTypes)
                .attachJoin(joinWithMedicalDataSourceTableFilterOnAppId(appId));
    }

    /**
     * Creates {@link SqlJoin} that is an inner join from medical_resource_table to
     * medical_resource_indices_table filtering on {@code medicalResourceTypes} followed by another
     * inner join from medical_resource_table to medical_data_source_table filtering on {@code
     * dataSourceIds}.
     */
    private static SqlJoin
            getJoinWithIndicesAndDataSourceTablesFilterOnMedicalResourceTypesAndSourceIds(
                    Set<Integer> medicalResourceTypes, List<UUID> dataSourceUuids) {
        return getJoinWithIndicesTableFilterOnMedicalResourceTypes(medicalResourceTypes)
                .attachJoin(joinWithMedicalDataSourceTableFilterOnDataSourceIds(dataSourceUuids));
    }

    /**
     * Creates {@link SqlJoin} that is an inner join from medical_resource_table to
     * medical_resource_indices_table filtering on {@code medicalResourceTypes} followed by another
     * inner join from medical_resource_table to medical_data_source_table filtering on {@code
     * dataSourceIds} and appId.
     */
    private static SqlJoin getJoinWithIndicesAndDataSourceTablesFilterOnTypesAndSourceIdsAndAppId(
            Set<Integer> medicalResourceTypes, List<UUID> dataSourceUuids, long appId) {
        return getJoinWithIndicesTableFilterOnMedicalResourceTypes(medicalResourceTypes)
                .attachJoin(
                        joinWithMedicalDataSourceTableFilterOnDataSourceIdsAndAppId(
                                dataSourceUuids, appId));
    }

    /**
     * Creates {@link SqlJoin} that is an inner join from medical_resource_table to
     * medical_resource_indices_table filtering on {@code medicalResourceTypes} followed by {@code
     * extraJoin} attached to it.
     *
     * <p>If the list of {@code medicalResourceTypes} is empty, then the {@link WhereClauses} will
     * be empty.
     */
    static SqlJoin getJoinWithIndicesTableFilterOnMedicalResourceTypes(
            Set<Integer> medicalResourceTypes) {
        WhereClauses medicalResourceTypeWhereClause =
                new WhereClauses(AND)
                        .addWhereInIntsClause(
                                getMedicalResourceTypeColumnName(),
                                new ArrayList<>(medicalResourceTypes));
        return joinWithMedicalResourceIndicesTable()
                .setSecondTableWhereClause(medicalResourceTypeWhereClause);
    }

    static SqlJoin getJoinWithMedicalDataSourceFilterOnDataSourceIdsAndAppId(
            List<UUID> dataSourceIds, long appId, SqlJoin extraJoin) {
        return joinWithMedicalDataSourceTable()
                .setSecondTableWhereClause(
                        getDataSourceIdsAndAppIdWhereClause(dataSourceIds, appId))
                .attachJoin(extraJoin);
    }

    static SqlJoin getJoinWithMedicalDataSourceFilterOnDataSourceIds(
            List<UUID> dataSourceIds, SqlJoin extraJoin) {
        return joinWithMedicalDataSourceTable()
                .setSecondTableWhereClause(getDataSourceIdsWhereClause(dataSourceIds))
                .attachJoin(extraJoin);
    }

    static SqlJoin joinWithMedicalResourceIndicesTable() {
        return new SqlJoin(
                        MEDICAL_RESOURCE_TABLE_NAME,
                        MedicalResourceIndicesHelper.getTableName(),
                        MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME,
                        MedicalResourceIndicesHelper.getParentColumnReference())
                .setJoinType(SQL_JOIN_INNER);
    }

    private static SqlJoin joinWithMedicalDataSourceTable() {
        return new SqlJoin(
                        MEDICAL_RESOURCE_TABLE_NAME,
                        MedicalDataSourceHelper.getMainTableName(),
                        DATA_SOURCE_ID_COLUMN_NAME,
                        MedicalDataSourceHelper.getPrimaryColumnName())
                .setJoinType(SQL_JOIN_INNER);
    }

    private static SqlJoin joinWithMedicalDataSourceTableFilterOnAppId(long appId) {
        SqlJoin join = joinWithMedicalDataSourceTable();
        join.setSecondTableWhereClause(getAppIdWhereClause(appId));
        return join;
    }

    private static SqlJoin joinWithMedicalDataSourceTableFilterOnDataSourceIds(
            List<UUID> dataSourceUuids) {
        SqlJoin join = joinWithMedicalDataSourceTable();
        join.setSecondTableWhereClause(getReadTableWhereClause(dataSourceUuids));
        return join;
    }

    private static SqlJoin joinWithMedicalDataSourceTableFilterOnDataSourceIdsAndAppId(
            List<UUID> dataSourceUuids, long appId) {
        SqlJoin join = joinWithMedicalDataSourceTable();
        join.setSecondTableWhereClause(
                getReadTableWhereClause(dataSourceUuids)
                        .addWhereEqualsClause(
                                MedicalDataSourceHelper.getAppInfoIdColumnName(),
                                String.valueOf(appId)));
        return join;
    }

    private static WhereClauses getAppIdWhereClause(long appId) {
        return new WhereClauses(AND)
                .addWhereEqualsClause(
                        MedicalDataSourceHelper.getAppInfoIdColumnName(), String.valueOf(appId));
    }

    private static WhereClauses getDataSourceIdsAndAppIdWhereClause(
            List<UUID> dataSourceIds, long appId) {
        WhereClauses whereClauses = getAppIdWhereClause(appId);
        whereClauses.addWhereInClauseWithoutQuotes(
                getDataSourceUuidColumnName(), StorageUtils.getListOfHexStrings(dataSourceIds));
        return whereClauses;
    }

    private static WhereClauses getDataSourceIdsWhereClause(List<UUID> dataSourceIds) {
        return new WhereClauses(AND)
                .addWhereInClauseWithoutQuotes(
                        getDataSourceUuidColumnName(), getListOfHexStrings(dataSourceIds));
    }

    static WhereClauses getAppIdsWhereClause(Set<Long> appIds) {
        return new WhereClauses(AND)
                .addWhereInLongsClause(
                        MedicalDataSourceHelper.getAppInfoIdColumnName(), appIds.stream().toList());
    }

    private static OrderByClause getOrderByClause() {
        return new OrderByClause()
                .addOrderByClause(MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME, /* isAscending= */ true);
    }

    private static WhereClauses getReadByLastRowIdWhereClause(long lastRowId) {
        WhereClauses whereClauses = new WhereClauses(AND);

        if (lastRowId == DEFAULT_LONG) {
            return whereClauses;
        }

        whereClauses.addWhereGreaterThanClause(MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME, lastRowId);
        return whereClauses;
    }

    /**
     * Upserts (insert/update) a list of {@link MedicalResource}s created based on the given list of
     * {@link UpsertMedicalResourceInternalRequest}s into the HealthConnect database.
     *
     * @param upsertMedicalResourceInternalRequests a list of {@link
     *     UpsertMedicalResourceInternalRequest}.
     * @return List of {@link MedicalResource}s that were upserted into the database, in the same
     *     order as their associated {@link UpsertMedicalResourceInternalRequest}s.
     * @throws IllegalArgumentException if the data source id does not exist, or if a resource's
     *     FHIR version does not match the data source's FHIR version.
     */
    public List<MedicalResource> upsertMedicalResources(
            String callingPackageName,
            List<UpsertMedicalResourceInternalRequest> upsertMedicalResourceInternalRequests)
            throws SQLiteException {
        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Upserting "
                            + upsertMedicalResourceInternalRequests.size()
                            + " "
                            + UpsertMedicalResourceInternalRequest.class.getSimpleName()
                            + "(s).");
        }
        return mTransactionManager.runAsTransaction(
                (RunnableWithReturn<List<MedicalResource>, RuntimeException>)
                        db ->
                                readDataSourcesAndUpsertMedicalResources(
                                        db,
                                        callingPackageName,
                                        upsertMedicalResourceInternalRequests));
    }

    private List<MedicalResource> readDataSourcesAndUpsertMedicalResources(
            SQLiteDatabase db,
            String callingPackageName,
            List<UpsertMedicalResourceInternalRequest> upsertRequests) {
        List<String> dataSourceUuids =
                upsertRequests.stream()
                        .map(UpsertMedicalResourceInternalRequest::getDataSourceId)
                        .toList();
        long appInfoIdRestriction = mAppInfoHelper.getAppInfoId(callingPackageName);
        Map<String, Pair<Long, FhirVersion>> dataSourceUuidToRowIdAndVersion =
                mMedicalDataSourceHelper.getUuidToRowIdAndVersionMap(
                        db, appInfoIdRestriction, StorageUtils.toUuids(dataSourceUuids));

        // Standard Upsert code cannot be used as it uses a query with inline values to look for
        // existing data. The FHIR id is a user supplied string, and so vulnerable to SQL injection.
        // The Insert itself uses ContentValues (and so is safe) but there is also a read which is
        // not.
        // SQLite supports UPSERT https://www.sqlite.org/lang_upsert.html with ON CONFLICT DO UPDATE
        // This was added in SQLite version 3.24.0. This has been supported since Android API 30.
        // https://developer.android.com/reference/android/database/sqlite/package-summary.html
        // So we use this.
        for (UpsertMedicalResourceInternalRequest upsertRequest : upsertRequests) {
            Pair<Long, FhirVersion> dataSourceRowIdAndVersion =
                    dataSourceUuidToRowIdAndVersion.get(upsertRequest.getDataSourceId());
            if (dataSourceRowIdAndVersion == null) {
                throw new IllegalArgumentException(
                        "Invalid data source id: " + upsertRequest.getDataSourceId());
            }
            Long dataSourceRowId = dataSourceRowIdAndVersion.first;
            String dataSourceFhirVersion = dataSourceRowIdAndVersion.second.toString();
            if (!upsertRequest.getFhirVersion().equals(dataSourceFhirVersion)) {
                throw new IllegalArgumentException(
                        "Invalid fhir version: "
                                + upsertRequest.getFhirVersion()
                                + ". It did not match the data source's fhir version");
            }
            ContentValues contentValues =
                    getContentValues(dataSourceRowId, upsertRequest, mTimeSource.getInstantNow());
            long rowId =
                    db.insertWithOnConflict(
                            MEDICAL_RESOURCE_TABLE_NAME,
                            /* nullColumnHack= */ null,
                            contentValues,
                            SQLiteDatabase.CONFLICT_REPLACE);
            int medicalResourceType = upsertRequest.getMedicalResourceType();
            db.insertWithOnConflict(
                    MedicalResourceIndicesHelper.getTableName(),
                    /* nullColumnHack= */ null,
                    MedicalResourceIndicesHelper.getContentValues(rowId, medicalResourceType),
                    SQLiteDatabase.CONFLICT_REPLACE);
        }

        List<MedicalResource> upsertedMedicalResources = new ArrayList<>();
        Set<Integer> resourceTypes = new HashSet<>();
        for (UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest :
                upsertRequests) {
            MedicalResource medicalResource =
                    buildMedicalResource(upsertMedicalResourceInternalRequest);
            resourceTypes.add(medicalResource.getType());
            upsertedMedicalResources.add(medicalResource);
        }

        mAccessLogsHelper.addAccessLog(
                db,
                callingPackageName,
                resourceTypes,
                OPERATION_TYPE_UPSERT,
                /* accessedMedicalDataSource= */ false);

        return upsertedMedicalResources;
    }

    @VisibleForTesting
    static ContentValues getContentValues(
            long dataSourceRowId,
            UpsertMedicalResourceInternalRequest upsertMedicalResourceInternalRequest,
            Instant instant) {
        ContentValues resourceContentValues = new ContentValues();
        resourceContentValues.put(DATA_SOURCE_ID_COLUMN_NAME, dataSourceRowId);
        resourceContentValues.put(
                FHIR_DATA_COLUMN_NAME, upsertMedicalResourceInternalRequest.getData());
        resourceContentValues.put(
                FHIR_RESOURCE_TYPE_COLUMN_NAME,
                upsertMedicalResourceInternalRequest.getFhirResourceType());
        resourceContentValues.put(
                FHIR_RESOURCE_ID_COLUMN_NAME,
                upsertMedicalResourceInternalRequest.getFhirResourceId());
        resourceContentValues.put(LAST_MODIFIED_TIME_COLUMN_NAME, instant.toEpochMilli());
        return resourceContentValues;
    }

    /**
     * Create {@link ContentValues} for the given {@code dataSourceRowId}, {@code lastModifiedTime},
     * {@code appInfoId} and {@link MedicalResource}.
     *
     * <p>This is only used in DatabaseMerger code, where we want to provide a lastModifiedTimestamp
     * from the source database rather than based on the current time.
     */
    public static ContentValues getContentValues(
            long dataSourceRowId, long lastModifiedTime, MedicalResource resource) {
        FhirResource fhirResource = resource.getFhirResource();
        ContentValues resourceContentValues = new ContentValues();
        resourceContentValues.put(DATA_SOURCE_ID_COLUMN_NAME, dataSourceRowId);
        resourceContentValues.put(FHIR_DATA_COLUMN_NAME, fhirResource.getData());
        resourceContentValues.put(FHIR_RESOURCE_TYPE_COLUMN_NAME, fhirResource.getType());
        resourceContentValues.put(FHIR_RESOURCE_ID_COLUMN_NAME, fhirResource.getId());
        resourceContentValues.put(LAST_MODIFIED_TIME_COLUMN_NAME, lastModifiedTime);
        return resourceContentValues;
    }

    /**
     * Creates a {@link MedicalResource} for the given {@code uuid} and {@link
     * UpsertMedicalResourceInternalRequest}.
     */
    private static MedicalResource buildMedicalResource(
            UpsertMedicalResourceInternalRequest internalRequest) {
        FhirResource fhirResource =
                new FhirResource.Builder(
                                internalRequest.getFhirResourceType(),
                                internalRequest.getFhirResourceId(),
                                internalRequest.getData())
                        .build();
        return new MedicalResource.Builder(
                        internalRequest.getMedicalResourceType(),
                        internalRequest.getDataSourceId(),
                        parseFhirVersion(internalRequest.getFhirVersion()),
                        fhirResource)
                .build();
    }

    /**
     * Returns a {@link ReadMedicalResourcesInternalResponse}.
     *
     * <p>This should be run within a transaction as it does multiple requests using the db passed
     * for the transaction.
     *
     * @param request the specification for the rows to read
     * @param pageSize the number of results to return in this page
     * @param pageTokenWrapper the page token for the query
     * @throws IllegalArgumentException if the cursor contains more than @link
     *     MAXIMUM_ALLOWED_CURSOR_COUNT} records.
     */
    public static ReadMedicalResourcesInternalResponse getMedicalResources(
            SQLiteDatabase db,
            ReadTableRequest request,
            PhrPageTokenWrapper pageTokenWrapper,
            int pageSize) {
        ReadMedicalResourcesInternalResponse response;
        // Get the count from a requests with no limit,
        int totalRowCount;
        if (Flags.phrReadMedicalResourcesFixQueryLimit()) {
            Integer originalLimit = request.getFinalLimit();
            request.setFinalLimit(null);
            totalRowCount = TransactionManager.count(db, request);
            request.setFinalLimit(originalLimit);
        } else {
            Integer originalLimit = request.getLimit();
            request.setLimit(null);
            totalRowCount = TransactionManager.count(db, request);
            request.setLimit(originalLimit);
        }
        try (Cursor cursor = db.rawQuery(request.getReadCommand(), null)) {
            response = getMedicalResources(cursor, pageTokenWrapper, pageSize, totalRowCount);
        }
        return response;
    }

    /**
     * Returns a {@link ReadMedicalResourcesInternalResponse}.
     *
     * @param pageSize the number of results to return in this page
     * @param totalRowCount the number of rows that would have been returned if this query was
     *     executed with no limit
     * @throws IllegalArgumentException if the cursor contains more than @link
     *     MAXIMUM_ALLOWED_CURSOR_COUNT} records.
     */
    private static ReadMedicalResourcesInternalResponse getMedicalResources(
            Cursor cursor, PhrPageTokenWrapper pageTokenWrapper, int pageSize, int totalRowCount) {
        // TODO(b/356613483): remove these checks in the helpers and instead validate pageSize
        // in the service.
        if (cursor.getCount() > MAXIMUM_ALLOWED_CURSOR_COUNT) {
            throw new IllegalArgumentException(
                    "Too many resources in the cursor. Max allowed: "
                            + MAXIMUM_ALLOWED_CURSOR_COUNT);
        }
        List<MedicalResource> medicalResources = new ArrayList<>();
        String nextPageToken = null;
        long lastRowId = DEFAULT_LONG;
        if (cursor.moveToFirst()) {
            do {
                if (medicalResources.size() >= pageSize) {
                    nextPageToken = pageTokenWrapper.cloneWithNewLastRowId(lastRowId).encode();
                    break;
                }
                medicalResources.add(getMedicalResource(cursor));
                lastRowId = getCursorLong(cursor, MEDICAL_RESOURCE_PRIMARY_COLUMN_NAME);
            } while (cursor.moveToNext());
        }

        int remainingCount = totalRowCount - medicalResources.size();
        return new ReadMedicalResourcesInternalResponse(
                medicalResources, nextPageToken, remainingCount);
    }

    /**
     * Returns List of {@code MedicalResource}s from the cursor. If the cursor contains more than
     * {@link Constants#MAXIMUM_ALLOWED_CURSOR_COUNT} records, it throws {@link
     * IllegalArgumentException}.
     */
    private static List<MedicalResource> getMedicalResources(Cursor cursor) {
        if (cursor.getCount() > MAXIMUM_ALLOWED_CURSOR_COUNT) {
            throw new IllegalArgumentException(
                    "Too many resources in the cursor. Max allowed: "
                            + MAXIMUM_ALLOWED_CURSOR_COUNT);
        }
        List<MedicalResource> medicalResources = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                medicalResources.add(getMedicalResource(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return medicalResources;
    }

    /**
     * Deletes a list of {@link MedicalResource}s created based on the given list of {@link
     * MedicalResourceId}s into the HealthConnect database.
     *
     * @param medicalResourceIds list of {@link MedicalResourceId} to delete
     */
    public void deleteMedicalResourcesByIdsWithoutPermissionChecks(
            List<MedicalResourceId> medicalResourceIds) {
        if (medicalResourceIds.isEmpty()) {
            throw new IllegalArgumentException("Nothing to delete specified");
        }
        Pair<String, String[]> paramsAndArgs =
                makeParametersAndArgs(medicalResourceIds, /* appId= */ null);
        String whereClause = DELETE_ON_IDS_WHERE_CLAUSE + paramsAndArgs.first + ")";
        mTransactionManager.runAsTransaction(
                db -> {
                    db.delete(MEDICAL_RESOURCE_TABLE_NAME, whereClause, paramsAndArgs.second);
                });
    }

    /**
     * Deletes a list of {@link MedicalResource}s created based on the given list of {@link
     * MedicalResourceId}s into the HealthConnect database.
     *
     * @param medicalResourceIds list of {@link MedicalResourceId} to delete
     * @param callingPackageName Only allows deletions of resources whose owning datasource belongs
     *     to the given appInfoId.
     * @throws IllegalArgumentException if no appId exists for the given {@code packageName} in the
     *     {@link AppInfoHelper#TABLE_NAME}.
     */
    public void deleteMedicalResourcesByIdsWithPermissionChecks(
            List<MedicalResourceId> medicalResourceIds, String callingPackageName)
            throws SQLiteException {

        long appId = mAppInfoHelper.getAppInfoId(callingPackageName);
        if (appId == Constants.DEFAULT_LONG) {
            throw new IllegalArgumentException(
                    "Deletion not permitted as app has inserted no data.");
        }

        Pair<String, String[]> paramsAndArgs = makeParametersAndArgs(medicalResourceIds, appId);
        String whereClause = DELETE_ON_IDS_WHERE_CLAUSE + paramsAndArgs.first + ")";
        String[] args = paramsAndArgs.second;

        mTransactionManager.runAsTransaction(
                db -> {
                    // Getting the distinct resource types that will be deleted, to add
                    // access logs.
                    Set<Integer> resourcesTypes =
                            readMedicalResourcesTypes(db, medicalResourceIds, appId);

                    db.delete(MEDICAL_RESOURCE_TABLE_NAME, whereClause, args);

                    if (!resourcesTypes.isEmpty()) {
                        mAccessLogsHelper.addAccessLog(
                                db,
                                callingPackageName,
                                resourcesTypes,
                                OPERATION_TYPE_DELETE,
                                /* accessedMedicalDataSource= */ false);
                    }
                });
    }

    private Set<Integer> readMedicalResourcesTypes(
            SQLiteDatabase db, List<MedicalResourceId> medicalResourceIds, long appId) {
        Pair<String, String[]> paramsAndArgs = makeParametersAndArgs(medicalResourceIds, appId);
        String sql =
                "SELECT DISTINCT "
                        + MedicalResourceIndicesHelper.getMedicalResourceTypeColumnName()
                        + " FROM "
                        + RESOURCES_JOIN_DATA_SOURCES_JOIN_INDICES
                        + " WHERE "
                        + SELECT_ON_IDS_WHERE_CLAUSE
                        + paramsAndArgs.first;
        Set<Integer> resourceTypes = new HashSet<>();
        try (Cursor cursor = db.rawQuery(sql, paramsAndArgs.second)) {
            if (cursor.moveToFirst()) {
                do {
                    resourceTypes.add(getCursorInt(cursor, getMedicalResourceTypeColumnName()));
                } while (cursor.moveToNext());
            }
        }
        return resourceTypes;
    }

    /**
     * Deletes all {@link MedicalResource}s that are part of the given datasource.
     *
     * <p>No error occurs if any of the ids are not present because the ids are just a part of the
     * filters.
     *
     * @param request which resources to delete.
     */
    public void deleteMedicalResourcesByRequestWithoutPermissionChecks(
            DeleteMedicalResourcesRequest request) throws SQLiteException {
        Set<String> dataSourceIds = request.getDataSourceIds();
        Set<Integer> medicalResourceTypes = request.getMedicalResourceTypes();
        List<UUID> dataSourceUuids = StorageUtils.toUuids(dataSourceIds);
        if (dataSourceUuids.isEmpty() && !dataSourceIds.isEmpty()) {
            // The request came in with no valid UUIDs. Do nothing.
            return;
        }
        mTransactionManager.delete(
                getFilteredDeleteRequest(dataSourceUuids, medicalResourceTypes, /* appId= */ null));
    }

    /**
     * Deletes all {@link MedicalResource}s that are part of the given datasource.
     *
     * <p>No error occurs if any of the ids are not present because the ids are just a part of the
     * filters.
     *
     * @param request which resources to delete.
     * @param callingPackageName only allows deletions of data sources belonging to the given app
     * @throws IllegalArgumentException if the {@code callingPackageName} does not exist in the
     *     {@link AppInfoHelper#TABLE_NAME}. This can happen if the app has never written any data
     *     sources.
     */
    public void deleteMedicalResourcesByRequestWithPermissionChecks(
            DeleteMedicalResourcesRequest request, String callingPackageName)
            throws SQLiteException {
        Set<String> dataSourceIds = request.getDataSourceIds();
        Set<Integer> medicalResourceTypes = request.getMedicalResourceTypes();
        List<UUID> dataSourceUuids = StorageUtils.toUuids(dataSourceIds);
        if (dataSourceUuids.isEmpty() && !dataSourceIds.isEmpty()) {
            // The request came in with no valid UUIDs. Do nothing.
            return;
        }

        long appId = mAppInfoHelper.getAppInfoId(callingPackageName);
        if (appId == Constants.DEFAULT_LONG) {
            throw new IllegalArgumentException(
                    "Deletion not permitted as app has inserted no data.");
        }

        mTransactionManager.runAsTransaction(
                db -> {
                    // Getting the distinct resource types that will be deleted, to add
                    // access logs.
                    ReadTableRequest readRequest =
                            getFilteredReadRequestForDistinctResourceTypes(
                                    dataSourceUuids, medicalResourceTypes, appId);
                    Set<Integer> resourceTypes =
                            readMedicalResourcesTypesByReadRequest(db, readRequest);

                    mTransactionManager.delete(
                            db,
                            getFilteredDeleteRequest(dataSourceUuids, medicalResourceTypes, appId));

                    if (!resourceTypes.isEmpty()) {
                        mAccessLogsHelper.addAccessLog(
                                db,
                                callingPackageName,
                                resourceTypes,
                                OPERATION_TYPE_DELETE,
                                /* accessedMedicalDataSource= */ false);
                    }
                });
    }

    private Set<Integer> readMedicalResourcesTypesByReadRequest(
            SQLiteDatabase db, ReadTableRequest request) {
        Set<Integer> resourceTypes = new HashSet<>();
        try (Cursor cursor = mTransactionManager.read(db, request)) {
            if (cursor.moveToFirst()) {
                do {
                    resourceTypes.add(getCursorInt(cursor, getMedicalResourceTypeColumnName()));
                } while (cursor.moveToNext());
            }
        }
        return resourceTypes;
    }

    private DeleteTableRequest getFilteredDeleteRequest(
            List<UUID> dataSourceUuids, Set<Integer> medicalResourceTypes, @Nullable Long appId) {
        /*
           SQLite does not allow deletes with joins. So the following code does a select with
           appropriate joins, and then deletes the result. This is doing the following SQL code:

           DELETE FROM medical_resource_table
           WHERE medical_resource_row_id IN (
             SELECT medical_resource_row_id FROM medical_resource_table
             JOIN medical_indices_table ...
             JOIN medical_datasource_table ...
             WHERE data_source_uuid IN (uuid1, uuid2, ...)
             AND app_info_id IN (id1, id2, ...)
           )

           The ReadTableRequest does the inner select, and the DeleteTableRequest does the outer
           delete. The foreign key between medical_resource_table and medical_data_source_table is
           (datasource) PRIMARY_COLUMN_NAME = (resource) DATA_SOURCE_ID_COLUMN_NAME.
        */

        WhereClauses dataSourceWhereClauses =
                MedicalDataSourceHelper.getWhereClauses(dataSourceUuids, appId);
        SqlJoin dataSourceJoin = joinWithMedicalDataSourceTable();
        dataSourceJoin.setSecondTableWhereClause(dataSourceWhereClauses);

        SqlJoin indexJoin =
                getJoinWithIndicesTableFilterOnMedicalResourceTypes(medicalResourceTypes);
        indexJoin.attachJoin(dataSourceJoin);

        ReadTableRequest innerRead =
                new ReadTableRequest(getMainTableName())
                        .setJoinClause(indexJoin)
                        .setColumnNames(List.of(getPrimaryColumn()));

        return new DeleteTableRequest(getMainTableName())
                .addExtraWhereClauses(
                        new WhereClauses(AND)
                                .addWhereInSQLRequestClause(getPrimaryColumn(), innerRead));
    }

    private static MedicalResource getMedicalResource(Cursor cursor) {
        int fhirResourceTypeInt = getCursorInt(cursor, FHIR_RESOURCE_TYPE_COLUMN_NAME);
        FhirResource fhirResource =
                new FhirResource.Builder(
                                fhirResourceTypeInt,
                                getCursorString(cursor, FHIR_RESOURCE_ID_COLUMN_NAME),
                                getCursorString(cursor, FHIR_DATA_COLUMN_NAME))
                        .build();
        FhirVersion fhirVersion =
                parseFhirVersion(getCursorString(cursor, getFhirVersionColumnName()));
        long lastModifiedTimestamp =
                getCursorLong(cursor, LAST_MODIFIED_TIMESTAMP_MEDICAL_RESOURCE_ALIAS);
        return new MedicalResource(
                getCursorInt(cursor, getMedicalResourceTypeColumnName()),
                getCursorUUID(cursor, getDataSourceUuidColumnName()).toString(),
                fhirVersion,
                fhirResource,
                lastModifiedTimestamp);
    }

    /**
     * Creates sql and arguments suitable for appending to a WHERE clause specifying medical
     * resource ids.
     *
     * @param medicalResourceIds a non-empty list of ids to specify in the where clause.
     * @param appId if not null an app id which should be AND combined in the where clause.
     * @return a pair where the first element is SQL that can be appended to a relevant where clause
     *     with some values parameterised. The second element is the values for those parameters
     * @throws IllegalArgumentException if any of the ids have a data source id that is not valid
     *     (not a String form of a UUID)
     */
    private static Pair<String, String[]> makeParametersAndArgs(
            List<MedicalResourceId> medicalResourceIds, @Nullable Long appId) {
        if (medicalResourceIds.isEmpty()) {
            throw new IllegalArgumentException("No ids provided");
        }
        StringBuilder parameters = new StringBuilder();
        // Data source id is not passed as a parameter as the rawQuery API does not allow
        // BLOBs as strings. So unfortunately we inline the datasource id. This means there
        // are only 2 parameters per medical resource id, not 3.
        // One potential future improvement is to keep a Data source id map in memory as is
        // done for App id.
        String[] selectionArgs =
                new String[2 * medicalResourceIds.size() + (appId == null ? 0 : 1)];
        int index = 0;
        parameters.append('(');
        for (MedicalResourceId id : medicalResourceIds) {
            index = appendMedicalResourceId(id, parameters, selectionArgs, index);
        }
        // replace a trailing comma with a )
        parameters.setCharAt(parameters.length() - 1, ')');
        if (appId != null) {
            parameters
                    .append(" AND ")
                    .append(MedicalDataSourceHelper.getAppInfoIdColumnName())
                    .append("=?");
            selectionArgs[index] = String.valueOf(appId);
        }

        return new Pair<>(parameters.toString(), selectionArgs);
    }

    /**
     * Creates SQL and selection arguments for the given {@link MedicalResourceId}s joining with
     * medical_resource_indices table and medical_data_source table and filtering on appId of the
     * {@code callingPackageName} and {@code medicalResourceTypes}.
     *
     * @param medicalResourceTypes a non-empty set of medical resource types to include.
     * @param appId an app id to filter to, or if null all app ids will be included
     * @throws IllegalArgumentException if any of the medical resource ids is not valid (has a data
     *     source if which is not a valid string form of a UUID)
     */
    private static Pair<String, String[]> readResourcesByIdsAppIdResourceTypes(
            List<MedicalResourceId> medicalResourceIds,
            @Nullable Long appId,
            LogicalOperator howToCombineAppIdAndResourceTypes,
            Set<Integer> medicalResourceTypes) {
        StringBuilder sql =
                new StringBuilder(
                        "SELECT "
                                + getMedicalResourceColumns()
                                + " FROM "
                                + RESOURCES_JOIN_DATA_SOURCES_JOIN_INDICES
                                + " WHERE "
                                + SELECT_ON_IDS_WHERE_CLAUSE);

        String[] selectionArgs =
                new String
                        [2 * medicalResourceIds.size()
                                + (appId == null ? 0 : 1)
                                + medicalResourceTypes.size()];
        int index = 0;
        sql.append('(');
        for (MedicalResourceId id : medicalResourceIds) {
            index = appendMedicalResourceId(id, sql, selectionArgs, index);
        }
        // replace a trailing comma with a )
        sql.setCharAt(sql.length() - 1, ')');

        sql.append(" AND (");
        if (appId != null) {
            sql.append(MedicalDataSourceHelper.getAppInfoIdColumnName())
                    .append("=?")
                    .append(
                            howToCombineAppIdAndResourceTypes.equals(LogicalOperator.AND)
                                    ? " AND "
                                    : " OR ");
            selectionArgs[index++] = String.valueOf(appId);
        }
        sql.append(MedicalResourceIndicesHelper.getMedicalResourceTypeColumnName()).append(" IN (");

        for (Integer type : medicalResourceTypes) {
            sql.append("?,");
            selectionArgs[index++] = String.valueOf(type);
        }
        // Replace closing comma with closing bracket for IN
        sql.setCharAt(sql.length() - 1, ')');
        sql.append(")");
        return new Pair<>(sql.toString(), selectionArgs);
    }

    /**
     * Appends a medical resource id to both an SQL {@code StringBuilder} as a parameter and to an
     * array of arguments.
     *
     * @param id the id to append
     * @param sql the SQL string being built
     * @param selectionArgs the array holding the arguments for the SQL parameters
     * @param index the index to put the argument into {@code selectionArgs}
     * @return the new index for the next insert
     * @throws IllegalArgumentException if the data source id is not a valid UUID
     */
    private static int appendMedicalResourceId(
            MedicalResourceId id, StringBuilder sql, String[] selectionArgs, int index) {
        // Data source id is not passed as a parameter as the rawQuery API does not allow
        // BLOBs as strings. So unfortunately we inline the datasource id.
        // One potential future improvement is to keep a Data source id map in memory as is
        // done for App id.
        sql.append("(")
                .append(StorageUtils.getHexString(UUID.fromString(id.getDataSourceId())))
                .append(",?,?),");
        selectionArgs[index++] = String.valueOf(id.getFhirResourceType());
        selectionArgs[index++] = id.getFhirResourceId();
        return index;
    }

    private enum LogicalOperator {
        AND,
        OR
    }
}
