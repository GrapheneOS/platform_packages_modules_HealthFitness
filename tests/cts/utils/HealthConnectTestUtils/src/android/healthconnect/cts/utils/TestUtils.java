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

package android.healthconnect.cts.utils;

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.HealthDataCategory.BODY_MEASUREMENTS;
import static android.health.connect.HealthDataCategory.CYCLE_TRACKING;
import static android.health.connect.HealthDataCategory.NUTRITION;
import static android.health.connect.HealthDataCategory.SLEEP;
import static android.health.connect.HealthDataCategory.VITALS;
import static android.health.connect.HealthPermissionCategory.BASAL_METABOLIC_RATE;
import static android.health.connect.HealthPermissionCategory.EXERCISE;
import static android.health.connect.HealthPermissionCategory.HEART_RATE;
import static android.health.connect.HealthPermissionCategory.PLANNED_EXERCISE;
import static android.health.connect.HealthPermissionCategory.STEPS;
import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.healthconnect.cts.utils.DataFactory.NOW;
import static android.healthconnect.cts.utils.DataFactory.getDataOrigin;
import static android.healthconnect.cts.utils.HealthConnectReceiver.callAndGetResponseWithShellPermissionIdentity;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;
import static com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import android.Manifest;
import android.app.UiAutomation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.AggregateRecordsGroupedByDurationResponse;
import android.health.connect.AggregateRecordsGroupedByPeriodResponse;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.ApplicationInfoResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.FetchDataOriginsPriorityOrderResponse;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissionCategory;
import android.health.connect.InsertRecordsResponse;
import android.health.connect.ReadRecordsRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.RecordIdFilter;
import android.health.connect.RecordTypeInfoResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.UpdateDataOriginPriorityOrderRequest;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.AppInfo;
import android.health.connect.datatypes.BasalBodyTemperatureRecord;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.BloodGlucoseRecord;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.BodyFatRecord;
import android.health.connect.datatypes.BodyTemperatureRecord;
import android.health.connect.datatypes.BodyWaterMassRecord;
import android.health.connect.datatypes.BoneMassRecord;
import android.health.connect.datatypes.CervicalMucusRecord;
import android.health.connect.datatypes.CyclingPedalingCadenceRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ElevationGainedRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.FloorsClimbedRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.HeartRateVariabilityRmssdRecord;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.IntermenstrualBleedingRecord;
import android.health.connect.datatypes.LeanBodyMassRecord;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MenstruationFlowRecord;
import android.health.connect.datatypes.MenstruationPeriodRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.OvulationTestRecord;
import android.health.connect.datatypes.OxygenSaturationRecord;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RespiratoryRateRecord;
import android.health.connect.datatypes.RestingHeartRateRecord;
import android.health.connect.datatypes.SexualActivityRecord;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.StepsCadenceRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.Vo2MaxRecord;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.WheelchairPushesRecord;
import android.health.connect.migration.MigrationException;
import android.os.OutcomeReceiver;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.healthfitness.flags.Flags;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class TestUtils {
    private static final String TAG = "HCTestUtils";

    public static ChangeLogTokenResponse getChangeLogToken(ChangeLogTokenRequest request)
            throws InterruptedException {
        return getChangeLogToken(request, ApplicationProvider.getApplicationContext());
    }

    public static ChangeLogTokenResponse getChangeLogToken(
            ChangeLogTokenRequest request, Context context) throws InterruptedException {
        HealthConnectReceiver<ChangeLogTokenResponse> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager(context)
                .getChangeLogToken(request, Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse();
    }

    public static String insertRecordAndGetId(Record record) throws InterruptedException {
        return insertRecords(Collections.singletonList(record)).get(0).getMetadata().getId();
    }

    public static String insertRecordAndGetId(Record record, Context context)
            throws InterruptedException {
        return insertRecords(Collections.singletonList(record), context)
                .get(0)
                .getMetadata()
                .getId();
    }

    /**
     * Insert record to the database.
     *
     * @param record record to insert
     * @return inserted record
     */
    public static Record insertRecord(Record record) throws InterruptedException {
        return insertRecords(Collections.singletonList(record)).get(0);
    }

    /**
     * Inserts records to the database.
     *
     * @param records records to insert
     * @return inserted records
     */
    public static List<Record> insertRecords(List<? extends Record> records)
            throws InterruptedException {
        return insertRecords(records, ApplicationProvider.getApplicationContext());
    }

    /**
     * Inserts records to the database.
     *
     * @param records records to insert
     * @return inserted records
     */
    public static List<Record> insertRecords(Record... records) throws InterruptedException {
        return insertRecords(Arrays.asList(records), ApplicationProvider.getApplicationContext());
    }

    /**
     * Inserts records to the database.
     *
     * @param records records to insert.
     * @param context a {@link Context} to obtain {@link HealthConnectManager}.
     * @return inserted records.
     */
    public static List<Record> insertRecords(List<? extends Record> records, Context context)
            throws InterruptedException {
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager(context)
                .insertRecords(
                        unmodifiableList(records), Executors.newSingleThreadExecutor(), receiver);
        List<Record> returnedRecords = receiver.getResponse().getRecords();
        assertThat(returnedRecords).hasSize(records.size());
        return returnedRecords;
    }

    /**
     * Returns all records from the `records` list in their original order, but distinct by UUID.
     */
    public static <T extends Record> List<T> distinctByUuid(List<T> records) {
        return records.stream().filter(distinctByUuid()).toList();
    }

    private static Predicate<? super Record> distinctByUuid() {
        Set<String> seen = ConcurrentHashMap.newKeySet();
        return record -> seen.add(record.getMetadata().getId());
    }

    /** Updates the provided records in the database. */
    public static void updateRecords(List<? extends Record> records) throws InterruptedException {
        updateRecords(records, ApplicationProvider.getApplicationContext());
    }

    /** Synchronously updates records in HC. */
    public static void updateRecords(List<? extends Record> records, Context context)
            throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager(context)
                .updateRecords(
                        unmodifiableList(records), Executors.newSingleThreadExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();
    }

    public static ChangeLogsResponse getChangeLogs(ChangeLogsRequest changeLogsRequest)
            throws InterruptedException {
        return getChangeLogs(changeLogsRequest, ApplicationProvider.getApplicationContext());
    }

    public static ChangeLogsResponse getChangeLogs(
            ChangeLogsRequest changeLogsRequest, Context context) throws InterruptedException {
        HealthConnectReceiver<ChangeLogsResponse> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager(context)
                .getChangeLogs(changeLogsRequest, Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse();
    }

    /**
     * Given {@link android.health.connect.HealthPermissions#MANAGE_HEALTH_DATA_PERMISSION}, invokes
     * {@link HealthConnectManager#aggregate} with the given {@code request}.
     */
    public static <T> AggregateRecordsResponse<T> getAggregateResponseWithManagePermission(
            AggregateRecordsRequest<T> request) throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);

        try {
            return getAggregateResponse(request);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static <T> AggregateRecordsResponse<T> getAggregateResponse(
            AggregateRecordsRequest<T> request) throws InterruptedException {
        HealthConnectReceiver<AggregateRecordsResponse<T>> receiver =
                new HealthConnectReceiver<AggregateRecordsResponse<T>>();
        getHealthConnectManager().aggregate(request, Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse();
    }

    public static <T> AggregateRecordsResponse<T> getAggregateResponse(
            AggregateRecordsRequest<T> request, List<Record> recordsToInsert)
            throws InterruptedException {
        if (recordsToInsert != null) {
            insertRecords(recordsToInsert);
        }

        HealthConnectReceiver<AggregateRecordsResponse<T>> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager().aggregate(request, Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse();
    }

    public static <T>
            List<AggregateRecordsGroupedByDurationResponse<T>> getAggregateResponseGroupByDuration(
                    AggregateRecordsRequest<T> request, Duration duration)
                    throws InterruptedException {
        HealthConnectReceiver<List<AggregateRecordsGroupedByDurationResponse<T>>> receiver =
                new HealthConnectReceiver<>();
        getHealthConnectManager()
                .aggregateGroupByDuration(
                        request, duration, Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse();
    }

    public static <T>
            List<AggregateRecordsGroupedByPeriodResponse<T>> getAggregateResponseGroupByPeriod(
                    AggregateRecordsRequest<T> request, Period period) throws InterruptedException {
        HealthConnectReceiver<List<AggregateRecordsGroupedByPeriodResponse<T>>> receiver =
                new HealthConnectReceiver<>();
        getHealthConnectManager()
                .aggregateGroupByPeriod(
                        request, period, Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse();
    }

    /**
     * Given {@link android.health.connect.HealthPermissions#MANAGE_HEALTH_DATA_PERMISSION}, invokes
     * {@link HealthConnectManager#readRecords} with the given {@code request}.
     */
    public static <T extends Record> List<T> readRecordsWithManagePermission(
            ReadRecordsRequest<T> request) throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);

        try {
            return readRecords(request);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static <T extends Record> List<T> readRecords(ReadRecordsRequest<T> request)
            throws InterruptedException {
        return getReadRecordsResponse(request).getRecords();
    }

    public static <T extends Record> List<T> readRecords(
            ReadRecordsRequest<T> request, Context context) throws InterruptedException {
        return getReadRecordsResponse(request, context).getRecords();
    }

    public static <T extends Record> ReadRecordsResponse<T> getReadRecordsResponse(
            ReadRecordsRequest<T> request) throws InterruptedException {
        return getReadRecordsResponse(request, ApplicationProvider.getApplicationContext());
    }

    public static <T extends Record> ReadRecordsResponse<T> getReadRecordsResponse(
            ReadRecordsRequest<T> request, Context context) throws InterruptedException {
        assertThat(request.getRecordType()).isNotNull();
        HealthConnectReceiver<ReadRecordsResponse<T>> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager(context)
                .readRecords(request, Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse();
    }

    public static <T extends Record> void assertRecordNotFound(String uuid, Class<T> recordType)
            throws InterruptedException {
        assertThat(
                        readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(recordType)
                                        .addId(uuid)
                                        .build()))
                .isEmpty();
    }

    public static <T extends Record> void assertRecordFound(String uuid, Class<T> recordType)
            throws InterruptedException {
        assertThat(
                        readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(recordType)
                                        .addId(uuid)
                                        .build()))
                .isNotEmpty();
    }

    /** Reads all records in the DB for a given {@code recordClass}. */
    public static <T extends Record> List<T> readAllRecords(Class<T> recordClass)
            throws InterruptedException {
        List<T> records = new ArrayList<>();
        ReadRecordsResponse<T> readRecordsResponse =
                readRecordsWithPagination(
                        new ReadRecordsRequestUsingFilters.Builder<>(recordClass).build());
        while (true) {
            records.addAll(readRecordsResponse.getRecords());
            long pageToken = readRecordsResponse.getNextPageToken();
            if (pageToken == -1) {
                break;
            }
            readRecordsResponse =
                    readRecordsWithPagination(
                            new ReadRecordsRequestUsingFilters.Builder<>(recordClass)
                                    .setPageToken(pageToken)
                                    .build());
        }
        return records;
    }

    public static <T extends Record> ReadRecordsResponse<T> readRecordsWithPagination(
            ReadRecordsRequest<T> request) throws InterruptedException {
        HealthConnectReceiver<ReadRecordsResponse<T>> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager()
                .readRecords(request, Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse();
    }

    public static void setAutoDeletePeriod(int period) throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
            getHealthConnectManager()
                    .setRecordRetentionPeriodInDays(
                            period, Executors.newSingleThreadExecutor(), receiver);
            receiver.verifyNoExceptionOrThrow();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static void verifyDeleteRecords(DeleteUsingFiltersRequest request)
            throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
            getHealthConnectManager()
                    .deleteRecords(request, Executors.newSingleThreadExecutor(), receiver);
            receiver.verifyNoExceptionOrThrow();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    /**
     * Delete all health records (datasources, resources etc) stored in the Health Connect database.
     */
    public static void deleteAllMedicalData() throws InterruptedException {
        if (!isPersonalHealthRecordEnabled()) {
            return;
        }
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();
            HealthConnectManager manager = getHealthConnectManager();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            manager.getMedicalDataSources(
                    new GetMedicalDataSourcesRequest.Builder().build(), executor, receiver);
            List<MedicalDataSource> dataSources = receiver.getResponse();
            for (MedicalDataSource dataSource : dataSources) {
                HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
                manager.deleteMedicalDataSourceWithData(dataSource.getId(), executor, callback);
                callback.verifyNoExceptionOrThrow();
            }
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static void verifyDeleteRecords(List<RecordIdFilter> request)
            throws InterruptedException {
        verifyDeleteRecords(request, ApplicationProvider.getApplicationContext());
    }

    public static void verifyDeleteRecords(List<RecordIdFilter> request, Context context)
            throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager(context)
                .deleteRecords(request, Executors.newSingleThreadExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();
    }

    public static void verifyDeleteRecords(
            Class<? extends Record> recordType, TimeInstantRangeFilter timeRangeFilter)
            throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager()
                .deleteRecords(
                        recordType, timeRangeFilter, Executors.newSingleThreadExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();
    }

    /** Helper function to delete records from the DB using HealthConnectManager. */
    public static void deleteRecords(List<? extends Record> records) throws InterruptedException {
        List<RecordIdFilter> recordIdFilters =
                records.stream()
                        .map(
                                (record ->
                                        RecordIdFilter.fromId(
                                                record.getClass(), record.getMetadata().getId())))
                        .collect(Collectors.toList());
        verifyDeleteRecords(recordIdFilters);
    }

    /** Helper function to delete records from the DB, using HealthConnectManager. */
    public static void deleteRecordsByIdFilter(List<RecordIdFilter> recordIdFilters)
            throws InterruptedException {
        verifyDeleteRecords(recordIdFilters);
    }

    public static List<AccessLog> queryAccessLogs() throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            HealthConnectReceiver<List<AccessLog>> receiver = new HealthConnectReceiver<>();
            getHealthConnectManager()
                    .queryAccessLogs(Executors.newSingleThreadExecutor(), receiver);
            return receiver.getResponse();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static Map<Class<? extends Record>, RecordTypeInfoResponse> queryAllRecordTypesInfo()
            throws InterruptedException, NullPointerException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            HealthConnectReceiver<Map<Class<? extends Record>, RecordTypeInfoResponse>> receiver =
                    new HealthConnectReceiver<>();
            getHealthConnectManager()
                    .queryAllRecordTypesInfo(Executors.newSingleThreadExecutor(), receiver);
            return receiver.getResponse();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    public static List<LocalDate> getActivityDates(List<Class<? extends Record>> recordTypes)
            throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            HealthConnectReceiver<List<LocalDate>> receiver = new HealthConnectReceiver<>();
            getHealthConnectManager()
                    .queryActivityDates(recordTypes, Executors.newSingleThreadExecutor(), receiver);
            return receiver.getResponse();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    /** Calls {@link HealthConnectManager#startMigration} ()} with shell permission identity. */
    public static void startMigrationWithShellPermissionIdentity() throws InterruptedException {
        callAndGetResponseWithShellPermissionIdentity(
                MigrationException.class,
                getHealthConnectManager()::startMigration,
                Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
    }

    /** Calls {@link HealthConnectManager#finishMigration} with shell permission identity. */
    public static void finishMigrationWithShellPermissionIdentity() throws InterruptedException {
        callAndGetResponseWithShellPermissionIdentity(
                MigrationException.class,
                getHealthConnectManager()::finishMigration,
                Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
    }

    /** Calls insertMinDataMigrationSdkExtensionVersion with shell permission identity. */
    public static void insertMinDataMigrationSdkExtensionVersionWithShellPermissionIdentity(
            int version) throws InterruptedException {
        Void unused =
                callAndGetResponseWithShellPermissionIdentity(
                        MigrationException.class,
                        (executor, receiver) ->
                                getHealthConnectManager()
                                        .insertMinDataMigrationSdkExtensionVersion(
                                                version, executor, receiver),
                        Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
    }

    public static void deleteAllStagedRemoteData() {
        deleteAllStagedRemoteData(getHealthConnectManager());
    }

    public static void deleteAllStagedRemoteData(HealthConnectManager service) {
        runWithShellPermissionIdentity(
                () ->
                        // TODO(b/241542162): Avoid reflection once TestApi can be called from CTS
                        service.getClass().getMethod("deleteAllStagedRemoteData").invoke(service),
                "android.permission.DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA");
    }

    /** Set lower rate limits for testing */
    public static boolean setLowerRateLimitsForTesting(boolean enabled) {
        HealthConnectManager service = getHealthConnectManager();
        try {
            runWithShellPermissionIdentity(
                    () ->
                            // TODO(b/241542162): Avoid reflection once TestApi can be called from
                            // CTS
                            service.getClass()
                                    .getMethod("setLowerRateLimitsForTesting", boolean.class)
                                    .invoke(service, enabled),
                    "android.permission.DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA");
            return true;
        } catch (RuntimeException e) {
            // Old versions of the module don't have this API.
            Log.e(TAG, "Couldn't override quota for testing", e);
            return false;
        }
    }

    public static int getHealthConnectDataMigrationState() throws InterruptedException {
        return callAndGetResponseWithShellPermissionIdentity(
                        getHealthConnectManager()::getHealthConnectDataState,
                        MANAGE_HEALTH_DATA_PERMISSION)
                .getDataMigrationState();
    }

    public static int getHealthConnectDataRestoreState() throws InterruptedException {
        return callAndGetResponseWithShellPermissionIdentity(
                        getHealthConnectManager()::getHealthConnectDataState,
                        MANAGE_HEALTH_DATA_PERMISSION)
                .getDataRestoreState();
    }

    public static List<AppInfo> getApplicationInfo() throws InterruptedException {
        HealthConnectReceiver<ApplicationInfoResponse> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager()
                .getContributorApplicationsInfo(Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse().getApplicationInfoList();
    }

    public static <T extends Record> T getRecordById(List<T> list, String id) {
        for (T record : list) {
            if (record.getMetadata().getId().equals(id)) {
                return record;
            }
        }

        throw new AssertionError("Record not found with id: " + id);
    }

    public static void populateAndResetExpectedResponseMap(
            HashMap<Class<? extends Record>, RecordTypeInfoTestResponse> expectedResponseMap) {
        expectedResponseMap.put(
                ElevationGainedRecord.class,
                new RecordTypeInfoTestResponse(
                        ACTIVITY, HealthPermissionCategory.ELEVATION_GAINED, new ArrayList<>()));
        expectedResponseMap.put(
                OvulationTestRecord.class,
                new RecordTypeInfoTestResponse(
                        CYCLE_TRACKING,
                        HealthPermissionCategory.OVULATION_TEST,
                        new ArrayList<>()));
        expectedResponseMap.put(
                DistanceRecord.class,
                new RecordTypeInfoTestResponse(
                        ACTIVITY, HealthPermissionCategory.DISTANCE, new ArrayList<>()));
        expectedResponseMap.put(
                SpeedRecord.class,
                new RecordTypeInfoTestResponse(
                        ACTIVITY, HealthPermissionCategory.SPEED, new ArrayList<>()));

        expectedResponseMap.put(
                Vo2MaxRecord.class,
                new RecordTypeInfoTestResponse(
                        ACTIVITY, HealthPermissionCategory.VO2_MAX, new ArrayList<>()));
        expectedResponseMap.put(
                OxygenSaturationRecord.class,
                new RecordTypeInfoTestResponse(
                        VITALS, HealthPermissionCategory.OXYGEN_SATURATION, new ArrayList<>()));
        expectedResponseMap.put(
                TotalCaloriesBurnedRecord.class,
                new RecordTypeInfoTestResponse(
                        ACTIVITY,
                        HealthPermissionCategory.TOTAL_CALORIES_BURNED,
                        new ArrayList<>()));
        expectedResponseMap.put(
                HydrationRecord.class,
                new RecordTypeInfoTestResponse(
                        NUTRITION, HealthPermissionCategory.HYDRATION, new ArrayList<>()));
        expectedResponseMap.put(
                StepsRecord.class,
                new RecordTypeInfoTestResponse(ACTIVITY, STEPS, new ArrayList<>()));
        expectedResponseMap.put(
                CervicalMucusRecord.class,
                new RecordTypeInfoTestResponse(
                        CYCLE_TRACKING,
                        HealthPermissionCategory.CERVICAL_MUCUS,
                        new ArrayList<>()));
        expectedResponseMap.put(
                ExerciseSessionRecord.class,
                new RecordTypeInfoTestResponse(ACTIVITY, EXERCISE, new ArrayList<>()));
        expectedResponseMap.put(
                HeartRateRecord.class,
                new RecordTypeInfoTestResponse(VITALS, HEART_RATE, new ArrayList<>()));
        expectedResponseMap.put(
                RespiratoryRateRecord.class,
                new RecordTypeInfoTestResponse(
                        VITALS, HealthPermissionCategory.RESPIRATORY_RATE, new ArrayList<>()));
        expectedResponseMap.put(
                BasalBodyTemperatureRecord.class,
                new RecordTypeInfoTestResponse(
                        VITALS,
                        HealthPermissionCategory.BASAL_BODY_TEMPERATURE,
                        new ArrayList<>()));
        expectedResponseMap.put(
                WheelchairPushesRecord.class,
                new RecordTypeInfoTestResponse(
                        ACTIVITY, HealthPermissionCategory.WHEELCHAIR_PUSHES, new ArrayList<>()));
        expectedResponseMap.put(
                PowerRecord.class,
                new RecordTypeInfoTestResponse(
                        ACTIVITY, HealthPermissionCategory.POWER, new ArrayList<>()));
        expectedResponseMap.put(
                BodyWaterMassRecord.class,
                new RecordTypeInfoTestResponse(
                        BODY_MEASUREMENTS,
                        HealthPermissionCategory.BODY_WATER_MASS,
                        new ArrayList<>()));
        expectedResponseMap.put(
                WeightRecord.class,
                new RecordTypeInfoTestResponse(
                        BODY_MEASUREMENTS, HealthPermissionCategory.WEIGHT, new ArrayList<>()));
        expectedResponseMap.put(
                BoneMassRecord.class,
                new RecordTypeInfoTestResponse(
                        BODY_MEASUREMENTS, HealthPermissionCategory.BONE_MASS, new ArrayList<>()));
        expectedResponseMap.put(
                RestingHeartRateRecord.class,
                new RecordTypeInfoTestResponse(
                        VITALS, HealthPermissionCategory.RESTING_HEART_RATE, new ArrayList<>()));
        expectedResponseMap.put(
                SkinTemperatureRecord.class,
                new RecordTypeInfoTestResponse(
                        VITALS, HealthPermissionCategory.SKIN_TEMPERATURE, new ArrayList<>()));
        expectedResponseMap.put(
                ActiveCaloriesBurnedRecord.class,
                new RecordTypeInfoTestResponse(
                        ACTIVITY,
                        HealthPermissionCategory.ACTIVE_CALORIES_BURNED,
                        new ArrayList<>()));
        expectedResponseMap.put(
                BodyFatRecord.class,
                new RecordTypeInfoTestResponse(
                        BODY_MEASUREMENTS, HealthPermissionCategory.BODY_FAT, new ArrayList<>()));
        expectedResponseMap.put(
                BodyTemperatureRecord.class,
                new RecordTypeInfoTestResponse(
                        VITALS, HealthPermissionCategory.BODY_TEMPERATURE, new ArrayList<>()));
        expectedResponseMap.put(
                NutritionRecord.class,
                new RecordTypeInfoTestResponse(
                        NUTRITION, HealthPermissionCategory.NUTRITION, new ArrayList<>()));
        expectedResponseMap.put(
                LeanBodyMassRecord.class,
                new RecordTypeInfoTestResponse(
                        BODY_MEASUREMENTS,
                        HealthPermissionCategory.LEAN_BODY_MASS,
                        new ArrayList<>()));
        expectedResponseMap.put(
                HeartRateVariabilityRmssdRecord.class,
                new RecordTypeInfoTestResponse(
                        VITALS,
                        HealthPermissionCategory.HEART_RATE_VARIABILITY,
                        new ArrayList<>()));
        expectedResponseMap.put(
                MenstruationFlowRecord.class,
                new RecordTypeInfoTestResponse(
                        CYCLE_TRACKING, HealthPermissionCategory.MENSTRUATION, new ArrayList<>()));
        expectedResponseMap.put(
                BloodGlucoseRecord.class,
                new RecordTypeInfoTestResponse(
                        VITALS, HealthPermissionCategory.BLOOD_GLUCOSE, new ArrayList<>()));
        expectedResponseMap.put(
                BloodPressureRecord.class,
                new RecordTypeInfoTestResponse(
                        VITALS, HealthPermissionCategory.BLOOD_PRESSURE, new ArrayList<>()));
        expectedResponseMap.put(
                CyclingPedalingCadenceRecord.class,
                new RecordTypeInfoTestResponse(ACTIVITY, EXERCISE, new ArrayList<>()));
        expectedResponseMap.put(
                IntermenstrualBleedingRecord.class,
                new RecordTypeInfoTestResponse(
                        CYCLE_TRACKING,
                        HealthPermissionCategory.INTERMENSTRUAL_BLEEDING,
                        new ArrayList<>()));
        expectedResponseMap.put(
                FloorsClimbedRecord.class,
                new RecordTypeInfoTestResponse(
                        ACTIVITY, HealthPermissionCategory.FLOORS_CLIMBED, new ArrayList<>()));
        expectedResponseMap.put(
                StepsCadenceRecord.class,
                new RecordTypeInfoTestResponse(ACTIVITY, STEPS, new ArrayList<>()));
        expectedResponseMap.put(
                HeightRecord.class,
                new RecordTypeInfoTestResponse(
                        BODY_MEASUREMENTS, HealthPermissionCategory.HEIGHT, new ArrayList<>()));
        expectedResponseMap.put(
                SexualActivityRecord.class,
                new RecordTypeInfoTestResponse(
                        CYCLE_TRACKING,
                        HealthPermissionCategory.SEXUAL_ACTIVITY,
                        new ArrayList<>()));
        expectedResponseMap.put(
                MenstruationPeriodRecord.class,
                new RecordTypeInfoTestResponse(
                        CYCLE_TRACKING, HealthPermissionCategory.MENSTRUATION, new ArrayList<>()));
        expectedResponseMap.put(
                SleepSessionRecord.class,
                new RecordTypeInfoTestResponse(
                        SLEEP, HealthPermissionCategory.SLEEP, new ArrayList<>()));
        expectedResponseMap.put(
                BasalMetabolicRateRecord.class,
                new RecordTypeInfoTestResponse(
                        BODY_MEASUREMENTS, BASAL_METABOLIC_RATE, new ArrayList<>()));
        expectedResponseMap.put(
                PlannedExerciseSessionRecord.class,
                new RecordTypeInfoTestResponse(ACTIVITY, PLANNED_EXERCISE, new ArrayList<>()));
    }

    public static FetchDataOriginsPriorityOrderResponse fetchDataOriginsPriorityOrder(
            int dataCategory) throws InterruptedException {
        HealthConnectReceiver<FetchDataOriginsPriorityOrderResponse> receiver =
                new HealthConnectReceiver<>();
        getHealthConnectManager()
                .fetchDataOriginsPriorityOrder(
                        dataCategory, Executors.newSingleThreadExecutor(), receiver);
        return receiver.getResponse();
    }

    public static void updateDataOriginPriorityOrder(UpdateDataOriginPriorityOrderRequest request)
            throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        getHealthConnectManager()
                .updateDataOriginPriorityOrder(
                        request, Executors.newSingleThreadExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();
    }

    public static void deleteTestData() throws InterruptedException {
        verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.now().plus(10, ChronoUnit.DAYS))
                                        .build())
                        .addRecordType(ExerciseSessionRecord.class)
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .addRecordType(BasalMetabolicRateRecord.class)
                        .build());
    }

    public static String runShellCommand(String command) throws IOException {
        UiAutomation uiAutomation =
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                        .getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity();
        final ParcelFileDescriptor stdout = uiAutomation.executeShellCommand(command);
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(new FileInputStream(stdout.getFileDescriptor())))) {
            char[] buffer = new char[4096];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                output.append(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }

        return output.toString();
    }

    @NonNull
    public static HealthConnectManager getHealthConnectManager() {
        return getHealthConnectManager(ApplicationProvider.getApplicationContext());
    }

    @NonNull
    public static HealthConnectManager getHealthConnectManager(Context context) {
        return requireNonNull(context.getSystemService(HealthConnectManager.class));
    }

    /** Sets up the priority list for aggregation tests. */
    public static void setupAggregation(String packageName, int dataCategory) {
        try {
            setupAggregation(
                    record -> insertRecords(Collections.singletonList(record)),
                    packageName,
                    dataCategory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Sets up the priority list for aggregation tests. */
    public static void setupAggregation(
            ThrowingConsumer<Record> inserter, String packageName, int dataCategory)
            throws Exception {
        inserter.acceptOrThrow(getAnUnaggregatableRecord(packageName));
        setupAggregation(List.of(packageName), dataCategory);
    }

    /**
     * Sets up the priority list for aggregation tests.
     *
     * <p>In order for this method to work, eac of the {@code packageNames} needs to have at least
     * one record of any type in the HC DB before this method is called.
     *
     * <p>This is mainly used to setup priority list for a test app, so a test can read aggregation
     * of data inserted by a test app. It would be nicer if this method take an instance of a test
     * app such as {@code TestAppProxy}, however, it would requires this TestUtils class depends on
     * the dependency where the TestAppProxy comes from, which then would create a dependency cycle
     * because TestAppProxy's dependency is already using this TestUtils class.
     */
    public static void setupAggregation(List<String> packageNames, int dataCategory)
            throws Exception {
        // Add the packageNames inserting the records to the priority list manually
        // Since CTS tests get their permissions granted at install time and skip
        // the Health Connect APIs that would otherwise add the packageName to the priority list
        updatePriorityWithManageHealthDataPermission(dataCategory, packageNames);
        FetchDataOriginsPriorityOrderResponse newPriority =
                getPriorityWithManageHealthDataPermission(dataCategory);
        List<String> newPriorityString =
                newPriority.getDataOriginsPriorityOrder().stream()
                        .map(DataOrigin::getPackageName)
                        .toList();
        assertThat(newPriorityString).isEqualTo(packageNames);
    }

    /** Inserts a record that does not support aggregation to enable the priority list. */
    public static void insertRecordsForPriority(String packageName) throws InterruptedException {
        // Insert records that do not support aggregation so that the AppInfoTable is initialised
        insertRecords(List.of(getAnUnaggregatableRecord(packageName)));
    }

    /** Returns a {@link Record} that does not support aggregation. */
    private static Record getAnUnaggregatableRecord(String packageName) {
        return new MenstruationPeriodRecord.Builder(
                        new Metadata.Builder()
                                .setDataOrigin(
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName)
                                                .build())
                                .build(),
                        NOW,
                        NOW.plusMillis(1000))
                .build();
    }

    /** Updates the priority list after getting the MANAGE_HEALTH_DATA permission. */
    public static void updatePriorityWithManageHealthDataPermission(
            int permissionCategory, List<String> packageNames) throws InterruptedException {
        UiAutomation uiAutomation =
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                        .getUiAutomation();

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            updatePriority(permissionCategory, packageNames);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    /** Updates the priority list without getting the MANAGE_HEALTH_DATA permission. */
    public static void updatePriority(int permissionCategory, List<String> packageNames)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        List<DataOrigin> dataOrigins =
                packageNames.stream()
                        .map(
                                (packageName) ->
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName)
                                                .build())
                        .collect(Collectors.toList());

        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        UpdateDataOriginPriorityOrderRequest updateDataOriginPriorityOrderRequest =
                new UpdateDataOriginPriorityOrderRequest(dataOrigins, permissionCategory);
        service.updateDataOriginPriorityOrder(
                updateDataOriginPriorityOrderRequest,
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(updateDataOriginPriorityOrderRequest.getDataCategory())
                .isEqualTo(permissionCategory);
        assertThat(updateDataOriginPriorityOrderRequest.getDataOriginInOrder()).isNotNull();
        receiver.verifyNoExceptionOrThrow(3);
    }

    public static boolean areHealthPermissionsSupported() {
        return areHealthPermissionsSupported(ApplicationProvider.getApplicationContext());
    }

    /** returns true if the hardware is supported by HealthConnect. */
    public static boolean areHealthPermissionsSupported(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean isWatchEnabled =
                pm.hasSystemFeature(PackageManager.FEATURE_WATCH)
                        && Flags.replaceBodySensorPermissionEnabled();
        return DeviceSupportUtils.isHealthConnectFullySupported(context) || isWatchEnabled;
    }

    /** Gets the priority list after getting the MANAGE_HEALTH_DATA permission. */
    public static FetchDataOriginsPriorityOrderResponse getPriorityWithManageHealthDataPermission(
            int permissionCategory) throws InterruptedException {
        UiAutomation uiAutomation =
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                        .getUiAutomation();

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        FetchDataOriginsPriorityOrderResponse response;

        try {
            response = getPriority(permissionCategory);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }

        return response;
    }

    /** Gets the priority list without requesting the MANAGE_HEALTH_DATA permission. */
    public static FetchDataOriginsPriorityOrderResponse getPriority(int permissionCategory)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        AtomicReference<FetchDataOriginsPriorityOrderResponse> response = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();
        service.fetchDataOriginsPriorityOrder(
                permissionCategory,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(FetchDataOriginsPriorityOrderResponse result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        healthConnectExceptionAtomicReference.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        if (healthConnectExceptionAtomicReference.get() != null) {
            throw healthConnectExceptionAtomicReference.get();
        }

        return response.get();
    }

    /**
     * Marks apps with any granted health permissions as connected to HC.
     *
     * <p>Test apps in CTS get their permissions auto granted without going through the HC
     * connection flow which prevents the HC service from recording the app info in the database.
     *
     * <p>This method calls "getCurrentPriority" API behind the scenes which has a side effect of
     * adding all the apps on the device with at least one health permission granted to the
     * database.
     */
    public static void connectAppsWithGrantedPermissions() {
        try {
            getPriorityWithManageHealthDataPermission(1);
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Zips given id and records lists to create a list of {@link RecordIdFilter}. */
    public static List<RecordIdFilter> getRecordIdFilters(
            List<String> recordIds, List<Record> records) {
        return IntStream.range(0, recordIds.size())
                .mapToObj(
                        i -> {
                            Class<? extends Record> recordClass = records.get(i).getClass();
                            String id = recordIds.get(i);
                            return RecordIdFilter.fromId(recordClass, id);
                        })
                .toList();
    }

    /** Creates an {@link Instant} representing the given local time yesterday at UTC. */
    public static Instant yesterdayAt(String localTime) {
        return LocalTime.parse(localTime)
                .atDate(LocalDate.now().minusDays(1))
                .toInstant(ZoneOffset.UTC);
    }

    /** Extracts and returns ids of the provided records. */
    public static List<String> getRecordIds(List<? extends Record> records) {
        return records.stream().map(Record::getMetadata).map(Metadata::getId).toList();
    }

    /**
     * Creates a {@link ReadRecordsRequestUsingFilters} with the filters being a {@code clazz} and a
     * list of package names.
     */
    public static <T extends Record>
            ReadRecordsRequestUsingFilters<T> createReadRecordsRequestUsingFilters(
                    Class<T> clazz, Collection<String> packageNameFilters) {
        ReadRecordsRequestUsingFilters.Builder<T> builder =
                new ReadRecordsRequestUsingFilters.Builder<>(clazz);
        for (String packageName : packageNameFilters) {
            builder.addDataOrigins(getDataOrigin(packageName));
        }
        return builder.build();
    }

    /** Copies record ids from the one list to another in order. Workaround for b/328228842. */
    // TODO(b/328228842): Avoid using reflection once we have Builder(Record) constructors
    public static void copyRecordIdsViaReflection(
            List<? extends Record> from, List<? extends Record> to) {
        assertThat(from).hasSize(to.size());

        for (int i = 0; i < from.size(); i++) {
            copyRecordIdViaReflection(from.get(i), to.get(i));
        }
    }

    /**
     * Sets value for a field using reflection. This can be used to set fields for immutable class.
     *
     * <p>This method recursively looks for the field in the object's class and its superclasses.
     */
    public static void setFieldValueUsingReflection(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findFieldUsingReflection(object.getClass(), fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static Field findFieldUsingReflection(Class<?> type, String fieldName) {
        try {
            return type.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            // If field isn't present, recursively look for it in the class's superclass.
            Class<?> superClass = type.getSuperclass();
            if (superClass != null) {
                return findFieldUsingReflection(superClass, fieldName);
            }
        }
        throw new IllegalArgumentException("Could not find field " + fieldName);
    }

    // TODO(b/328228842): Avoid using reflection once we have Builder(Record) constructors
    private static void copyRecordIdViaReflection(Record from, Record to) {
        setRecordIdViaReflection(to.getMetadata(), from.getMetadata().getId());
    }

    // TODO(b/328228842): Avoid using reflection once we have Builder(Record) constructors
    private static void setRecordIdViaReflection(Metadata metadata, String id) {
        try {
            Field field = Metadata.class.getDeclaredField("mId");
            boolean isAccessible = field.isAccessible();
            field.setAccessible(true);
            field.set(metadata, id);
            field.setAccessible(isAccessible);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class RecordTypeInfoTestResponse {
        private final int mRecordTypePermission;
        private final ArrayList<String> mContributingPackages;
        private final int mRecordTypeCategory;

        RecordTypeInfoTestResponse(
                int recordTypeCategory,
                int recordTypePermission,
                ArrayList<String> contributingPackages) {
            mRecordTypeCategory = recordTypeCategory;
            mRecordTypePermission = recordTypePermission;
            mContributingPackages = contributingPackages;
        }

        public int getRecordTypeCategory() {
            return mRecordTypeCategory;
        }

        public int getRecordTypePermission() {
            return mRecordTypePermission;
        }

        public ArrayList<String> getContributingPackages() {
            return mContributingPackages;
        }
    }

    /**
     * A {@link Consumer} that allows throwing checked exceptions from its single abstract method.
     */
    @FunctionalInterface
    @SuppressWarnings("FunctionalInterfaceMethodChanged")
    public interface ThrowingConsumer<T> extends Consumer<T> {
        /** Implementations of this method might throw exception. */
        void acceptOrThrow(T t) throws Exception;

        @Override
        default void accept(T t) {
            try {
                acceptOrThrow(t);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
