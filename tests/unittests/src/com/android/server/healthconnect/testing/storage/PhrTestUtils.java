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

package com.android.server.healthconnect.testing.storage;

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_VERSION_R4;

import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.net.Uri;
import android.util.Pair;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.phr.PhrPageTokenWrapper;
import com.android.server.healthconnect.phr.ReadMedicalResourcesInternalResponse;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertMedicalResourceInternalRequest;

import com.google.common.truth.Correspondence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PhrTestUtils {
    public static final Correspondence<AccessLog, AccessLog> ACCESS_LOG_EQUIVALENCE =
            Correspondence.from(PhrTestUtils::isAccessLogEqual, "isAccessLogEqual");

    private final MedicalDataSourceHelper mMedicalDataSourceHelper;
    private final MedicalResourceHelper mMedicalResourceHelper;
    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;

    public PhrTestUtils(HealthConnectInjector healthConnectInjector) {
        mMedicalResourceHelper = healthConnectInjector.getMedicalResourceHelper();
        mMedicalDataSourceHelper = healthConnectInjector.getMedicalDataSourceHelper();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
    }

    /**
     * Upsert a {@link MedicalResource} using the given {@link MedicalResourceCreator} and the
     * {@link MedicalDataSource}.
     */
    public MedicalResource upsertResource(
            MedicalResourceCreator creator, MedicalDataSource dataSource) {
        MedicalResource medicalResource = creator.create(dataSource.getId());
        return mMedicalResourceHelper
                .upsertMedicalResources(
                        dataSource.getPackageName(), List.of(makeUpsertRequest(medicalResource)))
                .get(0);
    }

    /**
     * Upsert {@link MedicalResource}s using the given {@link MedicalResourcesCreator}, the {@code
     * numOfResources} and {@link MedicalDataSource}.
     */
    public List<MedicalResource> upsertResources(
            MedicalResourcesCreator creator, int numOfResources, MedicalDataSource dataSource) {
        List<MedicalResource> medicalResources = creator.create(numOfResources, dataSource.getId());
        return mMedicalResourceHelper.upsertMedicalResources(
                dataSource.getPackageName(),
                medicalResources.stream().map(PhrTestUtils::makeUpsertRequest).toList());
    }

    /** Returns a request to upsert the given {@link MedicalResource}. */
    public static UpsertMedicalResourceInternalRequest makeUpsertRequest(MedicalResource resource) {
        return makeUpsertRequest(
                resource.getFhirResource(),
                resource.getType(),
                resource.getFhirVersion(),
                resource.getDataSourceId());
    }

    /**
     * Returns a request to upsert the given {@link FhirResource}, along with required source
     * information.
     */
    public static UpsertMedicalResourceInternalRequest makeUpsertRequest(
            FhirResource resource,
            int medicalResourceType,
            FhirVersion fhirVersion,
            String datasourceId) {
        return new UpsertMedicalResourceInternalRequest()
                .setMedicalResourceType(medicalResourceType)
                .setFhirResourceId(resource.getId())
                .setFhirResourceType(resource.getType())
                .setFhirVersion(fhirVersion)
                .setData(resource.getData())
                .setDataSourceId(datasourceId);
    }

    /**
     * Insert and return a {@link MedicalDataSource} where the display name, and URI will contain
     * the given name.
     *
     * <p>The FHIR version is set to R4.
     */
    public MedicalDataSource insertR4MedicalDataSource(String name, String packageName) {
        Uri uri = Uri.parse(String.format("%s/%s", DATA_SOURCE_FHIR_BASE_URI, name));
        String displayName = String.format("%s %s", DATA_SOURCE_DISPLAY_NAME, name);

        CreateMedicalDataSourceRequest createMedicalDataSourceRequest =
                new CreateMedicalDataSourceRequest.Builder(uri, displayName, FHIR_VERSION_R4)
                        .build();
        return mMedicalDataSourceHelper.createMedicalDataSource(
                createMedicalDataSourceRequest, packageName);
    }

    /** Interface for a {@link MedicalResource} creator. */
    public interface MedicalResourceCreator {
        /** Creates a {@link MedicalResource} using the given {@code dataSourceId}. */
        MedicalResource create(String dataSourceId);
    }

    /** Interface for multiple {@link MedicalResource}s creator. */
    public interface MedicalResourcesCreator {
        /**
         * Creates multiple {@link MedicalResource}s based on the {@code num} and the given {@code
         * dataSourceId}.
         */
        List<MedicalResource> create(int num, String dataSourceId);
    }

    /** Reads the last_modified_time column for the given {@code tableName}. */
    public long readLastModifiedTimestamp(String tableName) {
        long timestamp = DEFAULT_LONG;
        ReadTableRequest readTableRequest = new ReadTableRequest(tableName);
        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            if (cursor.moveToFirst()) {
                do {
                    timestamp = getCursorLong(cursor, LAST_MODIFIED_TIME_COLUMN_NAME);
                } while (cursor.moveToNext());
            }
            return timestamp;
        }
    }

    /**
     * Given two {@link AccessLog}s, compare whether they are equal or not. This ignores the {@link
     * AccessLog#getAccessTime()}.
     */
    public static boolean isAccessLogEqual(AccessLog actual, AccessLog expected) {
        return Objects.equals(actual.getPackageName(), expected.getPackageName())
                && actual.getOperationType() == expected.getOperationType()
                && Objects.equals(
                        actual.getMedicalResourceTypes(), expected.getMedicalResourceTypes())
                && Objects.equals(actual.getRecordTypes(), expected.getRecordTypes())
                && actual.isMedicalDataSourceAccessed() == expected.isMedicalDataSourceAccessed();
    }

    /**
     * Inserts a {@link MedicalDataSource} into the given {@link HealthConnectDatabase} using the
     * given {@code name}, and {@code packageName}. It returns a pair of rowId of the inserted row
     * and the generated uuid string of the {@link MedicalDataSource}.
     */
    public Pair<Long, String> insertMedicalDataSource(
            HealthConnectDatabase healthConnectDatabase,
            Context context,
            String name,
            String packageName,
            Instant instant) {
        SQLiteDatabase db = healthConnectDatabase.getWritableDatabase();
        long appInfoId = mAppInfoHelper.getOrInsertAppInfoId(db, packageName);
        if (appInfoId == DEFAULT_LONG) {
            throw new IllegalStateException("App id does not exist");
        }
        MedicalDataSource dataSource =
                new MedicalDataSource.Builder(
                                UUID.randomUUID().toString(),
                                packageName,
                                Uri.parse(String.format("%s/%s", DATA_SOURCE_FHIR_BASE_URI, name)),
                                String.format("%s %s", DATA_SOURCE_DISPLAY_NAME, name),
                                FHIR_VERSION_R4)
                        .build();
        long rowId =
                db.insertWithOnConflict(
                        MedicalDataSourceHelper.getMainTableName(),
                        /* nullColumnHack= */ null,
                        MedicalDataSourceHelper.getContentValues(
                                dataSource, appInfoId, instant.toEpochMilli()),
                        SQLiteDatabase.CONFLICT_IGNORE);
        return new Pair<>(rowId, dataSource.getId());
    }

    /**
     * Inserts a {@code numOfResources} of {@link MedicalResource}s into the given {@link
     * HealthConnectDatabase} using the given {@link MedicalResourcesCreator}, {@code
     * dataSourceUuid}, and {@code dataSourceRowId}.
     */
    public void insertMedicalResources(
            HealthConnectDatabase healthConnectDatabase,
            MedicalResourcesCreator creator,
            String dataSourceUuid,
            long dataSourceRowId,
            Instant instant,
            int numOfResources) {
        List<MedicalResource> medicalResources = creator.create(numOfResources, dataSourceUuid);
        SQLiteDatabase db = healthConnectDatabase.getWritableDatabase();
        for (MedicalResource medicalResource : medicalResources) {
            insertResource(db, medicalResource, dataSourceRowId, instant);
        }
    }

    /**
     * Inserts a {@link MedicalResource} into the given {@link HealthConnectDatabase} using the
     * given {@link MedicalResourceCreator}, {@code dataSourceUuid}, and {@code dataSourceRowId}.
     */
    public void insertMedicalResource(
            HealthConnectDatabase healthConnectDatabase,
            MedicalResourceCreator creator,
            String dataSourceUuid,
            long dataSourceRowId,
            Instant instant) {
        MedicalResource medicalResource = creator.create(dataSourceUuid);
        SQLiteDatabase db = healthConnectDatabase.getWritableDatabase();
        insertResource(db, medicalResource, dataSourceRowId, instant);
    }

    private void insertResource(
            SQLiteDatabase db,
            MedicalResource medicalResource,
            long dataSourceRowId,
            Instant instant) {
        long rowId =
                db.insertWithOnConflict(
                        MedicalResourceHelper.getMainTableName(),
                        /* nullColumnHack= */ null,
                        MedicalResourceHelper.getContentValues(
                                dataSourceRowId, instant.toEpochMilli(), medicalResource),
                        SQLiteDatabase.CONFLICT_REPLACE);
        db.insertWithOnConflict(
                MedicalResourceIndicesHelper.getTableName(),
                /* nullColumnHack= */ null,
                MedicalResourceIndicesHelper.getContentValues(rowId, medicalResource.getType()),
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static ReadMedicalResourcesInternalResponse readMedicalResources(
            SQLiteDatabase db, PhrPageTokenWrapper pageTokenWrapper) {
        ReadTableRequest readTableRequest =
                MedicalResourceHelper.getReadTableRequestUsingRequestFilters(
                        pageTokenWrapper, MAXIMUM_PAGE_SIZE);
        return MedicalResourceHelper.getMedicalResources(
                db, readTableRequest, pageTokenWrapper, MAXIMUM_PAGE_SIZE);
    }

    /**
     * Reads all the {@link MedicalResource}s and their associated last_modified_timestamp from the
     * database.
     */
    public List<Pair<MedicalResource, Long>> readAllMedicalResources() {
        return mTransactionManager.runWithoutTransaction(
                (SQLiteDatabase db) -> readAllMedicalResources(db));
    }

    /**
     * Reads all the {@link MedicalResource}s and their associated last_modified_timestamp from the
     * given {@link HealthConnectDatabase}.
     */
    public static List<Pair<MedicalResource, Long>> readAllMedicalResources(
            HealthConnectDatabase stagedDatabase) {
        return readAllMedicalResources(stagedDatabase.getReadableDatabase());
    }

    private static List<Pair<MedicalResource, Long>> readAllMedicalResources(SQLiteDatabase db) {
        List<Pair<MedicalResource, Long>> result = new ArrayList<>();
        String nextPageToken = null;
        do {
            PhrPageTokenWrapper phrPageTokenWrapper =
                    PhrPageTokenWrapper.fromPageTokenAllowingNull(nextPageToken);
            ReadMedicalResourcesInternalResponse response =
                    readMedicalResources(db, phrPageTokenWrapper);

            result.addAll(
                    response.getMedicalResources().stream()
                            .map(
                                    medicalResource ->
                                            new Pair<>(
                                                    medicalResource,
                                                    medicalResource.getLastModifiedTimestamp()))
                            .toList());
            nextPageToken = response.getPageToken();

        } while (nextPageToken != null);
        return result;
    }

    /**
     * Reads {@link MedicalDataSource}s and their associated last_modified_timestamp and returns it
     * as a list of {@link Pair}s with the first element of the pair being {@link MedicalDataSource}
     * and the second element last_modified_timestamp.
     */
    public List<Pair<MedicalDataSource, Long>> readMedicalDataSources() {
        try (Cursor cursor =
                mTransactionManager.rawQuery(
                        MedicalDataSourceHelper.getReadQueryForDataSources(), null)) {
            return MedicalDataSourceHelper.getMedicalDataSourcesWithTimestamps(cursor);
        }
    }

    /**
     * Reads {@link MedicalDataSource}s and their associated last_modified_timestamp and returns it
     * as a list of {@link Pair}s with the first element of the pair being {@link MedicalDataSource}
     * and the second element last_modified_timestamp.
     */
    public static List<Pair<MedicalDataSource, Long>> readMedicalDataSources(
            HealthConnectDatabase stagedDatabase) {
        try (Cursor cursor =
                stagedDatabase
                        .getReadableDatabase()
                        .rawQuery(MedicalDataSourceHelper.getReadQueryForDataSources(), null)) {
            return MedicalDataSourceHelper.getMedicalDataSourcesWithTimestamps(cursor);
        }
    }
}
