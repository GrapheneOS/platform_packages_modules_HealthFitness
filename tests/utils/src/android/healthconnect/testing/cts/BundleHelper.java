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

package android.healthconnect.testing.cts;

import static java.util.Objects.requireNonNull;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.ReadMedicalResourcesPageRequest;
import android.health.connect.ReadMedicalResourcesRequest;
import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.UpsertMedicalResourceRequest;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.InstantRecord;
import android.health.connect.datatypes.IntervalRecord;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.testing.shared.ToStringUtils;
import android.healthconnect.testing.shared.recordfactory.RecordFactory;
import android.os.Bundle;
import android.util.Log;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/** Converters from/to bundles for HC request, response, and record types. */
public final class BundleHelper {
    private static final String TAG = "TestApp-BundleHelper";
    static final String PREFIX = "android.healthconnect.cts.";
    public static final String QUERY_TYPE = PREFIX + "QUERY_TYPE";
    public static final String INSERT_RECORDS_QUERY = PREFIX + "INSERT_RECORDS_QUERY";
    public static final String READ_RECORDS_QUERY = PREFIX + "READ_RECORDS_QUERY";
    public static final String READ_RECORDS_USING_IDS_QUERY =
            PREFIX + "READ_RECORDS_USING_IDS_QUERY";
    public static final String AGGREGATE_STEPS_COUNT_TOTAL_QUERY =
            PREFIX + "AGGREGATE_STEPS_COUNT_TOTAL_QUERY";
    public static final String READ_CHANGE_LOGS_QUERY = PREFIX + "READ_CHANGE_LOGS_QUERY";
    public static final String DELETE_RECORDS_QUERY = PREFIX + "DELETE_RECORDS_QUERY";
    public static final String UPDATE_RECORDS_QUERY = PREFIX + "UPDATE_RECORDS_QUERY";
    public static final String GET_CHANGE_LOG_TOKEN_QUERY = PREFIX + "GET_CHANGE_LOG_TOKEN_QUERY";
    public static final String CREATE_MEDICAL_DATA_SOURCE_QUERY =
            PREFIX + "CREATE_MEDICAL_DATA_SOURCE_QUERY";
    public static final String GET_MEDICAL_DATA_SOURCES_USING_IDS_QUERY =
            PREFIX + "GET_MEDICAL_DATA_SOURCES_USING_IDS_QUERY";
    public static final String GET_MEDICAL_DATA_SOURCES_USING_REQUEST_QUERY =
            PREFIX + "GET_MEDICAL_DATA_SOURCES_USING_REQUEST_QUERY";
    public static final String UPSERT_MEDICAL_RESOURCES_QUERY =
            PREFIX + "UPSERT_MEDICAL_RESOURCE_QUERY";
    public static final String READ_MEDICAL_RESOURCES_BY_REQUEST_QUERY =
            PREFIX + "READ_MEDICAL_RESOURCES_BY_REQUEST_QUERY";
    public static final String READ_MEDICAL_RESOURCES_BY_IDS_QUERY =
            PREFIX + "READ_MEDICAL_RESOURCES_BY_IDS_QUERY";
    public static final String DELETE_MEDICAL_RESOURCES_BY_REQUEST_QUERY =
            PREFIX + "DELETE_MEDICAL_RESOURCES_BY_REQUEST_QUERY";
    public static final String DELETE_MEDICAL_RESOURCES_BY_IDS_QUERY =
            PREFIX + "DELETE_MEDICAL_RESOURCES_BY_IDS_QUERY";
    public static final String DELETE_MEDICAL_DATA_SOURCE_WITH_DATA_QUERY =
            PREFIX + "DELETE_MEDICAL_DATA_SOURCE_WITH_DATA_QUERY";

    private static final String CREATE_MEDICAL_DATA_SOURCE_REQUEST =
            PREFIX + "CREATE_MEDICAL_DATA_SOURCE_REQUEST";
    private static final String GET_MEDICAL_DATA_SOURCES_REQUEST =
            PREFIX + "GET_MEDICAL_DATA_SOURCES_REQUEST";
    public static final String MEDICAL_DATA_SOURCE_RESPONSE =
            PREFIX + "MEDICAL_DATA_SOURCE_RESPONSE";
    public static final String MEDICAL_DATA_SOURCES_RESPONSE =
            PREFIX + "MEDICAL_DATA_SOURCE_RESPONSE";
    private static final String UPSERT_MEDICAL_RESOURCE_REQUESTS =
            PREFIX + "UPSERT_MEDICAL_RESOURCE_REQUEST";
    private static final String READ_MEDICAL_RESOURCES_REQUEST_IS_PAGE_REQUEST =
            PREFIX + "READ_MEDICAL_RESOURCES_REQUEST_IS_PAGE_REQUEST";
    private static final String READ_MEDICAL_RESOURCES_REQUEST_MEDICAL_RESOURCE_TYPE =
            PREFIX + "READ_MEDICAL_RESOURCES_REQUEST_MEDICAL_RESOURCE_TYPE";
    private static final String READ_MEDICAL_RESOURCES_REQUEST_DATA_SOURCE_IDS =
            PREFIX + "READ_MEDICAL_RESOURCES_REQUEST_DATA_SOURCE_IDS";
    private static final String READ_MEDICAL_RESOURCES_REQUEST_PAGE_TOKEN =
            PREFIX + "READ_MEDICAL_RESOURCES_REQUEST_PAGE_TOKEN";
    private static final String READ_MEDICAL_RESOURCES_REQUEST_PAGE_SIZE =
            PREFIX + "READ_MEDICAL_RESOURCES_REQUEST_PAGE_SIZE";
    private static final String MEDICAL_RESOURCE_IDS = PREFIX + "MEDICAL_RESOURCE_IDS";
    public static final String MEDICAL_RESOURCES_RESPONSE = PREFIX + "MEDICAL_RESOURCE_RESPONSE";
    public static final String READ_MEDICAL_RESOURCES_RESPONSE =
            PREFIX + "READ_MEDICAL_RESOURCES_RESPONSE";
    private static final String DELETE_MEDICAL_RESOURCES_REQUEST =
            PREFIX + "DELETE_MEDICAL_RESOURCES_REQUEST";

    public static final String SELF_REVOKE_PERMISSION_REQUEST =
            PREFIX + "SELF_REVOKE_PERMISSION_REQUEST";

    public static final String KILL_SELF_REQUEST = PREFIX + "KILL_SELF_REQUEST";

    public static final String INTENT_EXCEPTION = PREFIX + "INTENT_EXCEPTION";

    private static final String CHANGE_LOGS_RESPONSE = PREFIX + "CHANGE_LOGS_RESPONSE";
    private static final String CHANGE_LOG_TOKEN = PREFIX + "CHANGE_LOG_TOKEN";
    private static final String RECORD_CLASS_NAME = PREFIX + "RECORD_CLASS_NAME";
    private static final String START_TIME_MILLIS = PREFIX + "START_TIME_MILLIS";
    private static final String END_TIME_MILLIS = PREFIX + "END_TIME_MILLIS";
    private static final String EXERCISE_SESSION_TYPE = PREFIX + "EXERCISE_SESSION_TYPE";
    private static final String RECORD_LIST = PREFIX + "RECORD_LIST";
    private static final String PACKAGE_NAME = PREFIX + "PACKAGE_NAME";
    private static final String CLIENT_ID = PREFIX + "CLIENT_ID";
    private static final String RECORD_ID = PREFIX + "RECORD_ID";
    private static final String AGGREGATE_STEPS_COUNT_TOTAL_RESULT =
            PREFIX + "AGGREGATE_STEPS_COUNT_TOTAL_RESULT";
    private static final String MEDICAL_DATA_SOURCE_ID = PREFIX + "MEDICAL_DATA_SOURCE_ID";
    private static final String METADATA = PREFIX + "METADATA";
    private static final String DEVICE = PREFIX + "DEVICE";
    private static final String DEVICE_TYPE = PREFIX + "DEVICE_TYPE";
    private static final String MANUFACTURER = PREFIX + "MANUFACTURER";
    private static final String MODEL = PREFIX + "MODEL";
    private static final String VALUES = PREFIX + "VALUES";
    private static final String COUNT = PREFIX + "COUNT";
    private static final String LENGTH_IN_METERS = PREFIX + "LENGTH_IN_METERS";
    private static final String ENERGY_IN_CALORIES = PREFIX + "ENERGY_IN_CALORIES";
    private static final String WEIGHT_IN_GRAMS = PREFIX + "WEIGHT_IN_GRAMS";
    private static final String IRON_IN_GRAMS = PREFIX + "IRON_IN_GRAMS";
    private static final String SAMPLE_TIMES = PREFIX + "SAMPLE_TIMES";
    private static final String SAMPLE_VALUES = PREFIX + "SAMPLE_VALUES";
    private static final String EXERCISE_ROUTE_TIMESTAMPS = PREFIX + "EXERCISE_ROUTE_TIMESTAMPS";
    private static final String EXERCISE_ROUTE_LATITUDES = PREFIX + "EXERCISE_ROUTE_LATITUDES";
    private static final String EXERCISE_ROUTE_LONGITUDES = PREFIX + "EXERCISE_ROUTE_LONGITUDES";
    private static final String EXERCISE_ROUTE_ALTITUDES = PREFIX + "EXERCISE_ROUTE_ALTITUDES";
    private static final String EXERCISE_ROUTE_HACCS = PREFIX + "EXERCISE_ROUTE_HACCS";
    private static final String EXERCISE_ROUTE_VACCS = PREFIX + "EXERCISE_ROUTE_VACCS";
    private static final String EXERCISE_HAS_ROUTE = PREFIX + "EXERCISE_HAS_ROUTE";
    private static final String EXERCISE_LAPS = PREFIX + "EXERCISE_LAPS";
    private static final String POWER_WATTS = PREFIX + "POWER_WATTS";
    private static final String TIME_INSTANT_RANGE_FILTER = PREFIX + "TIME_INSTANT_RANGE_FILTER";
    private static final String CHANGE_LOGS_REQUEST = PREFIX + "CHANGE_LOGS_REQUEST";
    private static final String CHANGE_LOG_TOKEN_REQUEST = PREFIX + "CHANGE_LOG_TOKEN_REQUEST";
    private static final String PERMISSION_NAME = PREFIX + "PERMISSION_NAME";
    private static final String START_TIMES = PREFIX + "START_TIMES";
    private static final String END_TIMES = PREFIX + "END_TIMES";
    private static final String EXERCISE_SEGMENT_TYPES = PREFIX + "EXERCISE_SEGMENT_TYPES";
    private static final String EXERCISE_SEGMENT_REP_COUNTS =
            PREFIX + "EXERCISE_SEGMENT_REP_COUNTS";
    private static final String NOTES = PREFIX + "NOTES";
    private static final String TITLE = PREFIX + "TITLE";
    private static final String PLANNED_EXERCISE_SESSION_ID =
            PREFIX + "PLANNED_EXERCISE_SESSION_ID";
    private static final String START_ZONE_OFFSET = PREFIX + "START_ZONE_OFFSET";
    private static final String END_ZONE_OFFSET = PREFIX + "END_ZONE_OFFSET";

    /** Converts an insert records request to a bundle. */
    public static Bundle fromInsertRecordsRequest(List<? extends Record> records) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORDS_QUERY);
        bundle.putParcelableArrayList(RECORD_LIST, new ArrayList<>(fromRecordList(records)));
        return bundle;
    }

    /** Converts a bundle to an insert records request. */
    public static List<? extends Record> toInsertRecordsRequest(Bundle bundle) {
        return toRecordList(bundle.getParcelableArrayList(RECORD_LIST, Bundle.class));
    }

    /** Converts an update records request to a bundle. */
    public static Bundle fromUpdateRecordsRequest(List<Record> records) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, UPDATE_RECORDS_QUERY);
        bundle.putParcelableArrayList(RECORD_LIST, new ArrayList<>(fromRecordList(records)));
        return bundle;
    }

    /** Converts a bundle to an update records request. */
    public static List<? extends Record> toUpdateRecordsRequest(Bundle bundle) {
        return toRecordList(bundle.getParcelableArrayList(RECORD_LIST, Bundle.class));
    }

    /** Converts an insert records response to a bundle. */
    public static Bundle fromInsertRecordsResponse(List<String> recordIds) {
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(RECORD_ID, new ArrayList<>(recordIds));
        return bundle;
    }

    /** Converts a bundle to an insert records response. */
    public static List<String> toInsertRecordsResponse(Bundle bundle) {
        return bundle.getStringArrayList(RECORD_ID);
    }

    /** Converts a ReadRecordsRequestUsingFilters to a bundle. */
    public static <T extends Record> Bundle fromReadRecordsRequestUsingFilters(
            ReadRecordsRequestUsingFilters<T> request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_RECORDS_QUERY);
        bundle.putString(RECORD_CLASS_NAME, request.getRecordType().getName());
        bundle.putStringArrayList(
                PACKAGE_NAME,
                new ArrayList<>(
                        request.getDataOrigins().stream()
                                .map(DataOrigin::getPackageName)
                                .toList()));

        if (request.getTimeRangeFilter() instanceof TimeInstantRangeFilter filter) {
            bundle.putBoolean(TIME_INSTANT_RANGE_FILTER, true);

            Long startTime = transformOrNull(filter.getStartTime(), Instant::toEpochMilli);
            Long endTime = transformOrNull(filter.getEndTime(), Instant::toEpochMilli);

            bundle.putSerializable(START_TIME_MILLIS, startTime);
            bundle.putSerializable(END_TIME_MILLIS, endTime);
        } else if (request.getTimeRangeFilter() != null) {
            throw new IllegalArgumentException("Unsupported time range filter");
        }

        return bundle;
    }

    /** Converts a bundle to a ReadRecordsRequestUsingFilters. */
    public static ReadRecordsRequestUsingFilters<? extends Record> toReadRecordsRequestUsingFilters(
            Bundle bundle) {
        String recordClassName = bundle.getString(RECORD_CLASS_NAME);

        Class<? extends Record> recordClass = recordClassForName(recordClassName);

        ReadRecordsRequestUsingFilters.Builder<? extends Record> request =
                new ReadRecordsRequestUsingFilters.Builder<>(recordClass);

        if (bundle.getBoolean(TIME_INSTANT_RANGE_FILTER)) {
            Long startTimeMillis = bundle.getSerializable(START_TIME_MILLIS, Long.class);
            Long endTimeMillis = bundle.getSerializable(END_TIME_MILLIS, Long.class);

            Instant startTime = transformOrNull(startTimeMillis, Instant::ofEpochMilli);
            Instant endTime = transformOrNull(endTimeMillis, Instant::ofEpochMilli);

            TimeInstantRangeFilter timeInstantRangeFilter =
                    new TimeInstantRangeFilter.Builder()
                            .setStartTime(startTime)
                            .setEndTime(endTime)
                            .build();

            request.setTimeRangeFilter(timeInstantRangeFilter);
        }
        List<String> packageNames = bundle.getStringArrayList(PACKAGE_NAME);

        if (packageNames != null) {
            for (String packageName : packageNames) {
                request.addDataOrigins(
                        new DataOrigin.Builder().setPackageName(packageName).build());
            }
        }

        return request.build();
    }

    /** Converts a ReadRecordsRequestUsingFilters to a bundle. */
    public static <T extends Record> Bundle fromReadRecordsRequestUsingIds(
            ReadRecordsRequestUsingIds<T> request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_RECORDS_USING_IDS_QUERY);
        bundle.putString(RECORD_CLASS_NAME, request.getRecordType().getName());

        var recordIdFilters = request.getRecordIdFilters();
        bundle.putStringArrayList(
                RECORD_ID,
                new ArrayList<>(
                        recordIdFilters.stream()
                                .map(RecordIdFilter::getId)
                                .filter(Objects::nonNull)
                                .toList()));
        bundle.putStringArrayList(
                CLIENT_ID,
                new ArrayList<>(
                        recordIdFilters.stream()
                                .map(RecordIdFilter::getClientRecordId)
                                .filter(Objects::nonNull)
                                .toList()));

        return bundle;
    }

    /** Converts a bundle to a ReadRecordsRequestUsingFilters. */
    public static ReadRecordsRequestUsingIds<? extends Record> toReadRecordsRequestUsingIds(
            Bundle bundle) {
        String recordClassName = bundle.getString(RECORD_CLASS_NAME);
        var request = new ReadRecordsRequestUsingIds.Builder<>(recordClassForName(recordClassName));
        var recordIds = bundle.getStringArrayList(RECORD_ID);
        if (recordIds != null) {
            for (String id : recordIds) {
                request.addId(id);
            }
        }
        var clientRecordIds = bundle.getStringArrayList(CLIENT_ID);
        if (clientRecordIds != null) {
            for (String clientId : clientRecordIds) {
                request.addClientRecordId(clientId);
            }
        }

        return request.build();
    }

    /** Converts a read records response to a bundle. */
    public static Bundle fromReadRecordsResponse(List<? extends Record> records) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(RECORD_LIST, new ArrayList<>(fromRecordList(records)));
        return bundle;
    }

    /** Converts a bundle to a read records response. */
    public static <T extends Record> List<T> toReadRecordsResponse(Bundle bundle) {
        return (List<T>) toRecordList(bundle.getParcelableArrayList(RECORD_LIST, Bundle.class));
    }

    /** Converts an aggregate steps count total request to a bundle. */
    public static Bundle fromAggregateStepsCountTotalRequest(
            Instant startTime, Instant endTime, List<String> packageNames) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, AGGREGATE_STEPS_COUNT_TOTAL_QUERY);
        bundle.putLong(START_TIME_MILLIS, startTime.toEpochMilli());
        bundle.putLong(END_TIME_MILLIS, endTime.toEpochMilli());
        bundle.putStringArrayList(PACKAGE_NAME, new ArrayList<>(packageNames));
        return bundle;
    }

    /** Converts a bundle to an aggregate steps count total request. */
    public static AggregateRecordsRequest<Long> toAggregateStepsCountTotalRequest(Bundle bundle) {
        Instant startTime = Instant.ofEpochMilli(bundle.getLong(START_TIME_MILLIS));
        Instant endTime = Instant.ofEpochMilli(bundle.getLong(END_TIME_MILLIS));
        TimeInstantRangeFilter timeInstantRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(startTime)
                        .setEndTime(endTime)
                        .build();

        AggregateRecordsRequest.Builder<Long> request =
                new AggregateRecordsRequest.Builder<Long>(timeInstantRangeFilter)
                        .addAggregationType(StepsRecord.STEPS_COUNT_TOTAL);

        List<String> packageNames = requireNonNull(bundle.getStringArrayList(PACKAGE_NAME));
        for (String packageName : packageNames) {
            request.addDataOriginsFilter(
                    new DataOrigin.Builder().setPackageName(packageName).build());
        }

        return request.build();
    }

    /** Converts an aggregate steps count total response to a bundle. */
    public static Bundle fromAggregateStepsCountTotalResponse(
            AggregateRecordsResponse<Long> response) {
        Bundle bundle = new Bundle();
        Long result = response.get(StepsRecord.STEPS_COUNT_TOTAL);
        if (result != null) {
            bundle.putLong(AGGREGATE_STEPS_COUNT_TOTAL_RESULT, result);
        }
        return bundle;
    }

    /** Converts a bundle to an aggregate steps count total response. */
    public static Long toAggregateStepsCountTotalResponse(Bundle bundle) {
        return bundle.containsKey(AGGREGATE_STEPS_COUNT_TOTAL_RESULT)
                ? bundle.getLong(AGGREGATE_STEPS_COUNT_TOTAL_RESULT)
                : null;
    }

    /** Converts a delete records request to a bundle. */
    public static Bundle fromDeleteRecordsByIdsRequest(List<RecordIdFilter> recordIdFilters) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, DELETE_RECORDS_QUERY);

        List<String> recordClassNames =
                recordIdFilters.stream()
                        .map(RecordIdFilter::getRecordType)
                        .map(Class::getName)
                        .toList();
        List<String> recordIds = recordIdFilters.stream().map(RecordIdFilter::getId).toList();

        bundle.putStringArrayList(RECORD_CLASS_NAME, new ArrayList<>(recordClassNames));
        bundle.putStringArrayList(RECORD_ID, new ArrayList<>(recordIds));

        return bundle;
    }

    /** Converts a bundle to a delete records request. */
    public static List<RecordIdFilter> toDeleteRecordsByIdsRequest(Bundle bundle) {
        List<String> recordClassNames = bundle.getStringArrayList(RECORD_CLASS_NAME);
        List<String> recordIds = bundle.getStringArrayList(RECORD_ID);

        return IntStream.range(0, recordClassNames.size())
                .mapToObj(
                        i -> {
                            String recordClassName = recordClassNames.get(i);
                            Class<? extends Record> recordClass =
                                    recordClassForName(recordClassName);
                            String recordId = recordIds.get(i);
                            return RecordIdFilter.fromId(recordClass, recordId);
                        })
                .toList();
    }

    /** Converts a ChangeLogTokenRequest to a bundle. */
    public static Bundle fromChangeLogTokenRequest(ChangeLogTokenRequest request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, GET_CHANGE_LOG_TOKEN_QUERY);
        bundle.putParcelable(CHANGE_LOG_TOKEN_REQUEST, request);
        return bundle;
    }

    /** Converts a self-revoke permission request to a bundle. */
    public static Bundle forSelfRevokePermissionRequest(String permission) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, SELF_REVOKE_PERMISSION_REQUEST);
        bundle.putString(PERMISSION_NAME, permission);
        return bundle;
    }

    /** Converts a bundle to a self-revoke permission request. */
    public static String toPermissionToSelfRevoke(Bundle bundle) {
        return bundle.getString(PERMISSION_NAME);
    }

    /** Converts a bundle to a ChangeLogTokenRequest. */
    public static ChangeLogTokenRequest toChangeLogTokenRequest(Bundle bundle) {
        return bundle.getParcelable(CHANGE_LOG_TOKEN_REQUEST, ChangeLogTokenRequest.class);
    }

    /** Converts a changelog token response to a bundle. */
    public static Bundle fromChangeLogTokenResponse(String token) {
        Bundle bundle = new Bundle();
        bundle.putString(CHANGE_LOG_TOKEN, token);
        return bundle;
    }

    /** Converts a bundle to a change log token response. */
    public static String toChangeLogTokenResponse(Bundle bundle) {
        return bundle.getString(CHANGE_LOG_TOKEN);
    }

    /** Converts a ChangeLogsRequest to a bundle. */
    public static Bundle fromChangeLogsRequest(ChangeLogsRequest request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_CHANGE_LOGS_QUERY);
        bundle.putParcelable(CHANGE_LOGS_REQUEST, request);
        return bundle;
    }

    /** Converts a bundle to a ChangeLogsRequest. */
    public static ChangeLogsRequest toChangeLogsRequest(Bundle bundle) {
        return bundle.getParcelable(CHANGE_LOGS_REQUEST, ChangeLogsRequest.class);
    }

    /** Converts a ChangeLogsResponse to a bundle. */
    public static Bundle fromChangeLogsResponse(ChangeLogsResponse response) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(CHANGE_LOGS_RESPONSE, response);
        return bundle;
    }

    /** Converts a bundle to a ChangeLogsResponse. */
    public static ChangeLogsResponse toChangeLogsResponse(Bundle bundle) {
        return bundle.getParcelable(CHANGE_LOGS_RESPONSE, ChangeLogsResponse.class);
    }

    /** Converts a {@link CreateMedicalDataSourceRequest} from a bundle. */
    public static CreateMedicalDataSourceRequest toCreateMedicalDataSourceRequest(Bundle bundle) {
        return bundle.getParcelable(
                CREATE_MEDICAL_DATA_SOURCE_REQUEST, CreateMedicalDataSourceRequest.class);
    }

    /** Converts a {@link CreateMedicalDataSourceRequest} into a bundle. */
    public static Bundle fromCreateMedicalDataSourceRequest(
            CreateMedicalDataSourceRequest request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, CREATE_MEDICAL_DATA_SOURCE_QUERY);
        bundle.putParcelable(CREATE_MEDICAL_DATA_SOURCE_REQUEST, request);
        return bundle;
    }

    /** Converts one UUID string into a bundle. */
    public static Bundle fromMedicalDataSourceId(String id) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, DELETE_MEDICAL_DATA_SOURCE_WITH_DATA_QUERY);
        bundle.putString(MEDICAL_DATA_SOURCE_ID, id);
        return bundle;
    }

    /** Converts one UUID strings back from a bundle. */
    public static String toMedicalDataSourceId(Bundle bundle) {
        return bundle.getString(MEDICAL_DATA_SOURCE_ID);
    }

    /** Converts a list of UUID strings into a bundle. */
    public static Bundle fromMedicalDataSourceIds(List<String> ids) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, GET_MEDICAL_DATA_SOURCES_USING_IDS_QUERY);
        bundle.putStringArrayList(MEDICAL_DATA_SOURCE_ID, new ArrayList<>(ids));
        return bundle;
    }

    /** Converts a list of UUID strings back from a bundle. */
    public static List<String> toMedicalDataSourceIds(Bundle bundle) {
        return bundle.getStringArrayList(MEDICAL_DATA_SOURCE_ID);
    }

    /** Converts a {@link GetMedicalDataSourcesRequest} into a bundle. */
    public static Bundle fromMedicalDataSourceRequest(GetMedicalDataSourcesRequest request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, GET_MEDICAL_DATA_SOURCES_USING_REQUEST_QUERY);
        bundle.putParcelable(GET_MEDICAL_DATA_SOURCES_REQUEST, request);
        return bundle;
    }

    /** Converts a {@link GetMedicalDataSourcesRequest} into a bundle. */
    public static GetMedicalDataSourcesRequest toMedicalDataSourceRequest(Bundle bundle) {
        return bundle.getParcelable(
                GET_MEDICAL_DATA_SOURCES_REQUEST, GetMedicalDataSourcesRequest.class);
    }

    /** Converts a list of {@link MedicalDataSource}s into a bundle. */
    public static Bundle fromMedicalDataSources(List<MedicalDataSource> medicalDataSources) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(
                MEDICAL_DATA_SOURCES_RESPONSE, new ArrayList<>(medicalDataSources));
        return bundle;
    }

    /** Converts a list of {@link MedicalDataSource}s back from a bundle. */
    public static List<MedicalDataSource> toMedicalDataSources(Bundle bundle) {
        return bundle.getParcelableArrayList(
                MEDICAL_DATA_SOURCES_RESPONSE, MedicalDataSource.class);
    }

    /**
     * Converts a {@link MedicalDataSource} to a bundle for sending to another app.
     *
     * <p>To convert back, use {@link #toMedicalDataSource(Bundle)}.
     */
    public static Bundle fromMedicalDataSource(MedicalDataSource medicalDataSource) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(MEDICAL_DATA_SOURCE_RESPONSE, medicalDataSource);
        return bundle;
    }

    /**
     * Converts a {@link MedicalDataSource} back from a bundle.
     *
     * <p>To create, use {@link #fromMedicalDataSource(MedicalDataSource)}.
     */
    public static MedicalDataSource toMedicalDataSource(Bundle bundle) {
        return bundle.getParcelable(MEDICAL_DATA_SOURCE_RESPONSE, MedicalDataSource.class);
    }

    /** Converts a {@link CreateMedicalDataSourceRequest} from a bundle. */
    public static List<UpsertMedicalResourceRequest> toUpsertMedicalResourceRequests(
            Bundle bundle) {
        return bundle.getParcelableArrayList(
                UPSERT_MEDICAL_RESOURCE_REQUESTS, UpsertMedicalResourceRequest.class);
    }

    /** Converts a {@link CreateMedicalDataSourceRequest} into a bundle. */
    public static Bundle fromUpsertMedicalResourceRequests(
            List<UpsertMedicalResourceRequest> requests) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, UPSERT_MEDICAL_RESOURCES_QUERY);
        bundle.putParcelableArrayList(UPSERT_MEDICAL_RESOURCE_REQUESTS, new ArrayList<>(requests));
        return bundle;
    }

    /** Converts a {@link ReadMedicalResourcesRequest} into a bundle. */
    public static Bundle fromReadMedicalResourcesRequest(ReadMedicalResourcesRequest request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_MEDICAL_RESOURCES_BY_REQUEST_QUERY);
        bundle.putInt(READ_MEDICAL_RESOURCES_REQUEST_PAGE_SIZE, request.getPageSize());

        if (request instanceof ReadMedicalResourcesPageRequest) {
            bundle.putBoolean(READ_MEDICAL_RESOURCES_REQUEST_IS_PAGE_REQUEST, true);
            bundle.putString(
                    READ_MEDICAL_RESOURCES_REQUEST_PAGE_TOKEN,
                    ((ReadMedicalResourcesPageRequest) request).getPageToken());
        } else if (request instanceof ReadMedicalResourcesInitialRequest) {
            ReadMedicalResourcesInitialRequest initialRequest =
                    (ReadMedicalResourcesInitialRequest) request;
            bundle.putBoolean(READ_MEDICAL_RESOURCES_REQUEST_IS_PAGE_REQUEST, false);
            bundle.putInt(
                    READ_MEDICAL_RESOURCES_REQUEST_MEDICAL_RESOURCE_TYPE,
                    initialRequest.getMedicalResourceType());
            bundle.putStringArrayList(
                    READ_MEDICAL_RESOURCES_REQUEST_DATA_SOURCE_IDS,
                    new ArrayList<>(initialRequest.getDataSourceIds()));
        } else {
            throw new IllegalArgumentException(
                    "Request was not of type ReadMedicalResourcesInitialRequest or"
                            + " ReadMedicalResourcesPageRequest");
        }

        // Check that no data was lost and that the request can be restored again. This could happen
        // if new fields are added to the ReadMedicalResourcesRequest without including them here.
        if (!toReadMedicalResourcesRequest(bundle).equals(request)) {
            throw new IllegalStateException("Data may be lost when converting to/from Bundle");
        }

        return bundle;
    }

    /** Converts a {@link ReadMedicalResourcesRequest} from a bundle. */
    public static ReadMedicalResourcesRequest toReadMedicalResourcesRequest(Bundle bundle) {
        boolean isPageRequest = bundle.getBoolean(READ_MEDICAL_RESOURCES_REQUEST_IS_PAGE_REQUEST);
        int pageSize = bundle.getInt(READ_MEDICAL_RESOURCES_REQUEST_PAGE_SIZE);

        if (isPageRequest) {
            String pageToken = bundle.getString(READ_MEDICAL_RESOURCES_REQUEST_PAGE_TOKEN);
            return new ReadMedicalResourcesPageRequest.Builder(pageToken)
                    .setPageSize(pageSize)
                    .build();
        } else {
            int medicalResourceType =
                    bundle.getInt(READ_MEDICAL_RESOURCES_REQUEST_MEDICAL_RESOURCE_TYPE);
            Set<String> dataSourceIds =
                    new HashSet<>(
                            bundle.getStringArrayList(
                                    READ_MEDICAL_RESOURCES_REQUEST_DATA_SOURCE_IDS));
            return new ReadMedicalResourcesInitialRequest.Builder(medicalResourceType)
                    .addDataSourceIds(dataSourceIds)
                    .setPageSize(pageSize)
                    .build();
        }
    }

    /** Converts a list of {@link MedicalResourceId}s from a bundle. */
    public static List<MedicalResourceId> toMedicalResourceIds(Bundle bundle) {
        return bundle.getParcelableArrayList(MEDICAL_RESOURCE_IDS, MedicalResourceId.class);
    }

    /**
     * Converts a list of {@link MedicalResourceId}s into a bundle with QUERY_TYPE set to
     * READ_MEDICAL_RESOURCES_BY_IDS_QUERY
     */
    public static Bundle fromMedicalResourceIdsForRead(List<MedicalResourceId> ids) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_MEDICAL_RESOURCES_BY_IDS_QUERY);
        bundle.putParcelableArrayList(MEDICAL_RESOURCE_IDS, new ArrayList<>(ids));
        return bundle;
    }

    /**
     * Converts a list of {@link MedicalResourceId}s into a bundle with QUERY_TYPE set to
     * DELETE_MEDICAL_RESOURCES_BY_IDS_QUERY
     */
    public static Bundle fromMedicalResourceIdsForDelete(List<MedicalResourceId> ids) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, DELETE_MEDICAL_RESOURCES_BY_IDS_QUERY);
        bundle.putParcelableArrayList(MEDICAL_RESOURCE_IDS, new ArrayList<>(ids));
        return bundle;
    }

    /**
     * Converts a list of {@link MedicalResource}s to a bundle for sending to another app.
     *
     * <p>To convert back, use {@link #toMedicalResources(Bundle)}.
     */
    public static Bundle fromMedicalResources(List<MedicalResource> medicalResources) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(
                MEDICAL_RESOURCES_RESPONSE, new ArrayList<>(medicalResources));
        return bundle;
    }

    /**
     * Converts a list of {@link MedicalResource}s back from a bundle.
     *
     * <p>To create, use {@link #fromMedicalResources(List)}.
     */
    public static List<MedicalResource> toMedicalResources(Bundle bundle) {
        return bundle.getParcelableArrayList(MEDICAL_RESOURCES_RESPONSE, MedicalResource.class);
    }

    /** Converts a {@link ReadMedicalResourcesResponse} from a bundle. */
    public static ReadMedicalResourcesResponse toReadMedicalResourcesResponse(Bundle bundle) {
        return bundle.getParcelable(
                READ_MEDICAL_RESOURCES_RESPONSE, ReadMedicalResourcesResponse.class);
    }

    /** Converts a {@link ReadMedicalResourcesResponse} to a bundle for sending to another app. */
    public static Bundle fromReadMedicalResourcesResponse(ReadMedicalResourcesResponse response) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(READ_MEDICAL_RESOURCES_RESPONSE, response);
        return bundle;
    }

    /** Converts a {@link DeleteMedicalResourcesRequest} from a bundle. */
    public static DeleteMedicalResourcesRequest toDeleteMedicalResourcesRequest(Bundle bundle) {
        return bundle.getParcelable(
                DELETE_MEDICAL_RESOURCES_REQUEST, DeleteMedicalResourcesRequest.class);
    }

    /** Converts a {@link DeleteMedicalResourcesRequest} into a bundle. */
    public static Bundle fromDeleteMedicalResourcesRequest(DeleteMedicalResourcesRequest request) {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, DELETE_MEDICAL_RESOURCES_BY_REQUEST_QUERY);
        bundle.putParcelable(DELETE_MEDICAL_RESOURCES_REQUEST, request);
        return bundle;
    }

    private static List<Bundle> fromRecordList(List<? extends Record> records) {
        return records.stream().map(BundleHelper::fromRecord).toList();
    }

    private static List<? extends Record> toRecordList(List<Bundle> bundles) {
        return bundles.stream().map(BundleHelper::toRecord).toList();
    }

    private static Bundle fromRecord(Record record) {
        Bundle bundle = new Bundle();
        bundle.putString(RECORD_CLASS_NAME, record.getClass().getName());
        bundle.putBundle(METADATA, fromMetadata(record.getMetadata()));

        if (record instanceof IntervalRecord intervalRecord) {
            bundle.putLong(START_TIME_MILLIS, intervalRecord.getStartTime().toEpochMilli());
            bundle.putLong(END_TIME_MILLIS, intervalRecord.getEndTime().toEpochMilli());
            bundle.putInt(START_ZONE_OFFSET, intervalRecord.getStartZoneOffset().getTotalSeconds());
            bundle.putInt(END_ZONE_OFFSET, intervalRecord.getEndZoneOffset().getTotalSeconds());
        } else if (record instanceof InstantRecord instantRecord) {
            bundle.putLong(START_TIME_MILLIS, instantRecord.getTime().toEpochMilli());
            bundle.putInt(START_ZONE_OFFSET, instantRecord.getZoneOffset().getTotalSeconds());
        } else {
            throw new IllegalArgumentException("Unsupported record type: ");
        }

        Bundle values;
        RecordFactory<? extends Record> recordFactory =
                RecordFactory.forDataType(record.getClass());
        values = recordFactory.getValuesBundle(record);
        bundle.putBundle(VALUES, values);

        Record decodedRecord = toRecord(bundle);
        if (!record.equals(decodedRecord)) {
            Log.e(
                    TAG,
                    BundleHelper.class.getSimpleName()
                            + ".java - record = "
                            + ToStringUtils.recordToString(record));
            Log.e(
                    TAG,
                    BundleHelper.class.getSimpleName()
                            + ".java - decoded = "
                            + ToStringUtils.recordToString(decodedRecord));
            throw new IllegalArgumentException(
                    "Some fields are incorrectly encoded in " + record.getClass().getSimpleName());
        }

        return bundle;
    }

    private static Record toRecord(Bundle bundle) {
        Metadata metadata = toMetadata(bundle.getBundle(METADATA));

        String recordClassName = bundle.getString(RECORD_CLASS_NAME);

        Instant startTime = Instant.ofEpochMilli(bundle.getLong(START_TIME_MILLIS));
        Instant endTime = Instant.ofEpochMilli(bundle.getLong(END_TIME_MILLIS));
        ZoneOffset startZoneOffset = ZoneOffset.ofTotalSeconds(bundle.getInt(START_ZONE_OFFSET));
        ZoneOffset endZoneOffset = ZoneOffset.ofTotalSeconds(bundle.getInt(END_ZONE_OFFSET));

        Bundle values = bundle.getBundle(VALUES);

        Class<? extends Record> recordClass = recordClassForName(recordClassName);
        RecordFactory<? extends Record> recordFactory = RecordFactory.forDataType(recordClass);
        return recordFactory.newRecordFromValuesBundle(
                metadata, startTime, endTime, startZoneOffset, endZoneOffset, values);
    }

    private static Bundle fromMetadata(Metadata metadata) {
        Bundle bundle = new Bundle();
        bundle.putString(RECORD_ID, metadata.getId());
        bundle.putString(PACKAGE_NAME, metadata.getDataOrigin().getPackageName());
        bundle.putString(CLIENT_ID, metadata.getClientRecordId());
        bundle.putBundle(DEVICE, fromDevice(metadata.getDevice()));
        return bundle;
    }

    private static Bundle fromDevice(Device device) {
        Bundle bundle = new Bundle();
        bundle.putString(MANUFACTURER, device.getManufacturer());
        bundle.putString(MODEL, device.getModel());
        bundle.putInt(DEVICE_TYPE, device.getType());
        return bundle;
    }

    private static Metadata toMetadata(Bundle bundle) {
        Metadata.Builder metadata = new Metadata.Builder();

        ifNotNull(bundle.getString(RECORD_ID), metadata::setId);
        ifNotNull(
                bundle.getString(PACKAGE_NAME),
                packageName ->
                        metadata.setDataOrigin(
                                new DataOrigin.Builder().setPackageName(packageName).build()));
        metadata.setClientRecordId(bundle.getString(CLIENT_ID));

        Bundle deviceBundle = bundle.getBundle(DEVICE);
        ifNotNull(
                deviceBundle,
                nonNullDeviceBundle -> {
                    Device.Builder deviceBuilder = new Device.Builder();
                    ifNotNull(
                            nonNullDeviceBundle.getString(MANUFACTURER),
                            deviceBuilder::setManufacturer);
                    ifNotNull(nonNullDeviceBundle.getString(MODEL), deviceBuilder::setModel);
                    deviceBuilder.setType(
                            nonNullDeviceBundle.getInt(DEVICE_TYPE, Device.DEVICE_TYPE_UNKNOWN));
                    metadata.setDevice(deviceBuilder.build());
                });

        return metadata.build();
    }

    private static <T> void ifNotNull(T obj, Consumer<T> consumer) {
        if (obj == null) {
            return;
        }
        consumer.accept(obj);
    }

    private static <T, R> R transformOrNull(T obj, Function<T, R> transform) {
        if (obj == null) {
            return null;
        }
        return transform.apply(obj);
    }

    private static Class<? extends Record> recordClassForName(String className) {
        try {
            return (Class<? extends Record>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private BundleHelper() {}
}
