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

import com.android.server.healthconnect.common.accesslog.ReadAccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.ReadAccessLogsHelper.ReadAccessLog;
import com.android.server.healthconnect.common.accesslog.ReadAccessLogsHelper.ReadAccessLogsResponse;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to process ReadAccessLogs and ChangeLogs to collect Ecosystem Metrics.
 *
 * @hide
 */
public class EcosystemStatsCollector {

    private static final int COLLECTION_WINDOW_DAYS = 30;

    private final ReadAccessLogsHelper mReadAccessLogsHelper;
    private final ChangeLogsHelper mChangeLogsHelper;
    private final Map<String, Map<Integer, Set<String>>> mReaderPackageToWriterPackagesPerDataType;
    private final Map<String, Set<String>> mReaderPackageToWriterPackages;
    private final Set<Integer> mDataTypesReadOrWritten;
    private final Set<Integer> mDataTypesWritten;
    private final Set<Integer> mDataTypesRead;
    private final Set<Integer> mDataTypeShared;

    public EcosystemStatsCollector(
            ReadAccessLogsHelper readAccessLogsHelper, ChangeLogsHelper changeLogsHelper) {
        mReadAccessLogsHelper = readAccessLogsHelper;
        mChangeLogsHelper = changeLogsHelper;
        mReaderPackageToWriterPackagesPerDataType = new HashMap<>();
        mDataTypesReadOrWritten = new HashSet<>();
        mDataTypesRead = new HashSet<>();
        mDataTypesWritten = new HashSet<>();
        mDataTypeShared = new HashSet<>();
        mReaderPackageToWriterPackages = new HashMap<>();
    }

    public void processReadAccessLogs() {
        long collectionWindowStart =
                Instant.now().minus(COLLECTION_WINDOW_DAYS, ChronoUnit.DAYS).toEpochMilli();
        Set<Integer> dataTypesWrittenInPast30Days =
                mChangeLogsHelper.getRecordTypesWrittenInPast30Days();
        mDataTypesReadOrWritten.addAll(dataTypesWrittenInPast30Days);
        mDataTypesWritten.addAll(dataTypesWrittenInPast30Days);
        ReadAccessLogsResponse readAccessLogsResponse;
        int nextRowId = 0;

        do {
            readAccessLogsResponse =
                    mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ nextRowId);
            nextRowId = readAccessLogsResponse.getNextRowId();
            processReadAccessLogsResponse(
                    readAccessLogsResponse.getReadAccessLogs(), collectionWindowStart);
        } while (readAccessLogsResponse.hasMoreRecords());
    }

    private void processReadAccessLogsResponse(
            List<ReadAccessLog> readAccessLogList, long thirtyDaysAgoTimestampMillis) {
        for (ReadAccessLog readAccessLog : readAccessLogList) {
            String readerPackage = readAccessLog.getReaderPackage();
            String writerPackage = readAccessLog.getWriterPackage();

            // Ignore reads more than 30 days ago
            if (thirtyDaysAgoTimestampMillis >= readAccessLog.getReadTimeStamp()) {
                continue;
            }

            // We count even if the records being read here were written more than 30 days ago.
            // We just care they were read within 30 days.
            mDataTypesReadOrWritten.add(readAccessLog.getDataType());
            mDataTypesRead.add(readAccessLog.getDataType());

            // Ignore reads for data with timestamps more than 30 days before the read.
            // For next batch of metrics, we only count if the records being read here were
            // written
            // less than 30 days ago.
            if (!readAccessLog.getRecordWithinPast30Days()) {
                continue;
            }
            mDataTypeShared.add(readAccessLog.getDataType());
            mReaderPackageToWriterPackages
                    .computeIfAbsent(readerPackage, k -> new HashSet<>())
                    .add(writerPackage);

            Map<Integer, Set<String>> dataTypeToWriterPackageNames =
                    mReaderPackageToWriterPackagesPerDataType.computeIfAbsent(
                            readerPackage, k -> new HashMap<>());

            dataTypeToWriterPackageNames
                    .computeIfAbsent(readAccessLog.getDataType(), k -> new HashSet<>())
                    .add(writerPackage);
        }
    }

    public Set<Integer> getDataTypesReadOrWritten() {
        return mDataTypesReadOrWritten;
    }

    public Set<Integer> getDataTypeShared() {
        return mDataTypeShared;
    }

    public Set<Integer> getDataTypesRead() {
        return mDataTypesRead;
    }

    public Map<String, Map<Integer, Set<String>>> getDirectionalAppPairingsPerDataType() {
        return mReaderPackageToWriterPackagesPerDataType;
    }

    public Map<String, Set<String>> getDirectionalAppPairings() {
        return mReaderPackageToWriterPackages;
    }

    /** Returns the number of directional app pairings. */
    public int getNumberOfAppPairings() {
        int count = 0;
        for (Set<String> writingPackages : mReaderPackageToWriterPackages.values()) {
            count += writingPackages.size();
        }
        return count;
    }

    public Set<Integer> getDataTypesWritten() {
        return mDataTypesWritten;
    }
}
