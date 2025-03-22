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

package com.android.server.healthconnect.fitness.aggregation;

import android.database.Cursor;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.AggregateResult;
import android.health.connect.TimeRangeFilter;
import android.health.connect.TimeRangeFilterHelper;
import android.health.connect.aidl.AggregateDataRequestParcel;
import android.health.connect.aidl.AggregateDataResponseParcel;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.util.ArrayMap;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregate FitnessRecords from the database based on the given request, using the
 * TransactionManager.
 *
 * <p>FitnessRecord refers to any of the record types defined in {@link
 * android.health.connect.datatypes.RecordTypeIdentifier};
 *
 * @hide
 */
public final class FitnessRecordAggregateHelper {
    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;
    private final HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private final AccessLogsHelper mAccessLogsHelper;
    private final ReadAccessLogsHelper mReadAccessLogsHelper;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;
    private final AggregationTypeIdMapper mAggregationTypeIdMapper;

    public FitnessRecordAggregateHelper(
            TransactionManager transactionManager,
            AppInfoHelper appInfoHelper,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            AccessLogsHelper accessLogsHelper,
            ReadAccessLogsHelper readAccessLogsHelper,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mHealthDataCategoryPriorityHelper = healthDataCategoryPriorityHelper;
        mAccessLogsHelper = accessLogsHelper;
        mReadAccessLogsHelper = readAccessLogsHelper;
        mInternalHealthConnectMappings = internalHealthConnectMappings;
        mAggregationTypeIdMapper = AggregationTypeIdMapper.getInstance();
    }

    /**
     * Aggregate and return results for the given request.
     *
     * @param callingPackageName The package name making the request.
     * @param request The aggregation request.
     * @param startDateAccess The start time since when the caller is allowed to read data.
     * @param shouldRecordAccessLog Whether access logs should be recorded.
     * @return The response containing the aggregations result.
     */
    public AggregateDataResponseParcel aggregateRecords(
            String callingPackageName,
            AggregateDataRequestParcel request,
            long startDateAccess,
            boolean shouldRecordAccessLog) {
        // Common request time for all access logs.
        long requestTime = Instant.now().toEpochMilli();

        List<AggregateRecordRequest> mAggregateRecordRequests =
                new ArrayList<>(request.getAggregateIds().length);
        Set<Integer> recordTypeIds = new HashSet<>();
        for (int id : request.getAggregateIds()) {
            AggregationType<?> aggregationType = mAggregationTypeIdMapper.getAggregationTypeFor(id);
            int recordTypeId = aggregationType.getApplicableRecordTypeId();
            recordTypeIds.add(recordTypeId);
            RecordHelper<?> recordHelper =
                    mInternalHealthConnectMappings.getRecordHelper(recordTypeId);
            AggregateRecordRequest aggregateRecordRequest =
                    recordHelper.getAggregateRecordRequest(
                            aggregationType,
                            callingPackageName,
                            request.getPackageFilters(),
                            mHealthDataCategoryPriorityHelper,
                            mInternalHealthConnectMappings,
                            mAppInfoHelper,
                            mTransactionManager,
                            request.getStartTime(),
                            request.getEndTime(),
                            startDateAccess,
                            request.useLocalTimeFilter());

            if (request.getDuration() != null || request.getPeriod() != null) {
                aggregateRecordRequest.setGroupBy(
                        recordHelper.getDurationGroupByColumnName(),
                        request.getPeriod(),
                        request.getDuration(),
                        request.getTimeRangeFilter());
            }
            mAggregateRecordRequests.add(aggregateRecordRequest);
        }

        Map<AggregationType<?>, List<AggregateResult<?>>> results = new ArrayMap<>();
        for (AggregateRecordRequest aggregateRecordRequest : mAggregateRecordRequests) {
            populateRequestWithResults(
                    callingPackageName,
                    aggregateRecordRequest,
                    recordTypeIds,
                    request.getTimeRangeFilter(),
                    shouldRecordAccessLog,
                    requestTime);
            results.put(
                    aggregateRecordRequest.getAggregationType(),
                    aggregateRecordRequest.getAggregateResults());
        }

        int responseSize =
                mAggregateRecordRequests.isEmpty()
                        ? 0
                        : mAggregateRecordRequests.get(0).getAggregateResults().size();
        List<AggregateRecordsResponse<?>> aggregateRecordsResponses = new ArrayList<>(responseSize);
        for (int i = 0; i < responseSize; i++) {
            Map<Integer, AggregateResult<?>> aggregateResultMap = new ArrayMap<>();
            for (AggregationType<?> aggregationType : results.keySet()) {
                aggregateResultMap.put(
                        (mAggregationTypeIdMapper.getIdFor(aggregationType)),
                        Objects.requireNonNull(results.get(aggregationType)).get(i));
            }
            aggregateRecordsResponses.add(new AggregateRecordsResponse<>(aggregateResultMap));
        }

        AggregateDataResponseParcel aggregateDataResponseParcel =
                new AggregateDataResponseParcel(aggregateRecordsResponses);
        if (request.getPeriod() != null) {
            aggregateDataResponseParcel.setPeriod(
                    request.getPeriod(), request.getTimeRangeFilter());
        } else if (request.getDuration() != null) {
            aggregateDataResponseParcel.setDuration(
                    request.getDuration(), request.getTimeRangeFilter());
        }

        return aggregateDataResponseParcel;
    }

    // Computes aggregations and record read access log
    private void populateRequestWithResults(
            String callingPackageName,
            AggregateRecordRequest aggregateRecordRequest,
            Set<Integer> recordTypeIds,
            TimeRangeFilter timeRangeFilter,
            boolean shouldRecordAccessLog,
            long requestTime) {
        mTransactionManager.runWithoutTransaction(
                db -> {
                    try (Cursor cursor =
                                    db.rawQuery(
                                            aggregateRecordRequest.getAggregationCommand(), null);
                            Cursor metaDataCursor =
                                    db.rawQuery(
                                            aggregateRecordRequest
                                                    .getCommandToFetchAggregateMetadata(),
                                            null)) {
                        // processResultsAndReturnContributingPackages stores the aggregation result
                        // in the aggregateTableRequest.
                        List<String> contributingPackages =
                                aggregateRecordRequest.processResultsAndReturnContributingPackages(
                                        cursor, metaDataCursor);
                        if (AconfigFlagHelper.isEcosystemMetricsEnabled()
                                && shouldRecordAccessLog) {
                            mReadAccessLogsHelper.recordAccessLogForAggregationReads(
                                    db,
                                    callingPackageName,
                                    /* readTimeStamp= */ requestTime,
                                    aggregateRecordRequest.getRecordTypeId(),
                                    /* endTimeStamp= */ TimeRangeFilterHelper
                                            .getFilterEndTimeMillis(timeRangeFilter),
                                    contributingPackages);
                        }
                    }
                    if (Flags.addMissingAccessLogs() && shouldRecordAccessLog) {
                        mAccessLogsHelper.recordReadAccessLog(
                                db, callingPackageName, recordTypeIds);
                    }
                });
    }
}
