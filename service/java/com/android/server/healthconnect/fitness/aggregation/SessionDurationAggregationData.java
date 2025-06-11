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

package com.android.server.healthconnect.fitness.aggregation;

import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.isNullValue;

import android.annotation.Nullable;
import android.database.Cursor;
import android.health.connect.Constants;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class to aggregate Sleep, Exercise, and Mindfulness sessions.
 *
 * <p>Calculates the duration of the overlap between the underlying session record and the requested
 * window and subtracts the so called excluded intervals if any (e.g. AWAKE stages for sleep, PAUSE
 * and REST segments for exercise).
 *
 * @hide
 */
class SessionDurationAggregationData extends AggregationRecordData {
    private static final String TAG = "HealthSessionPriorityAggregation";
    private static final long MILLIS_IN_SECOND = 1000L;
    @Nullable private final List<Long> mExcludeStarts;
    @Nullable private final List<Long> mExcludeEnds;

    @VisibleForTesting
    SessionDurationAggregationData(
            long recordStartTime,
            long recordEndTime,
            int priority,
            long lastModifiedTime,
            List<Long> mExcludeStarts,
            List<Long> mExcludeEnds) {
        super(
                recordStartTime,
                recordEndTime,
                priority,
                lastModifiedTime,
                /* startTimeZoneOffset= */ null);
        this.mExcludeStarts = mExcludeStarts;
        this.mExcludeEnds = mExcludeEnds;
    }

    SessionDurationAggregationData(
            Cursor cursor,
            boolean useLocalTime,
            Map<Long, Integer> appIdToPriority,
            @Nullable String excludeIntervalStartTimeColumn,
            @Nullable String excludeIntervalEndTimeColumn) {
        super(cursor, useLocalTime, appIdToPriority);

        UUID currentSessionUuid = readUuid(cursor);
        List<Long> excludeStarts = new ArrayList<>();
        List<Long> excludeEnds = new ArrayList<>();
        do {
            // Populate stages from each row.
            Long start =
                    getExcludeStart(
                            cursor,
                            useLocalTime,
                            excludeIntervalStartTimeColumn,
                            getStartTimeZoneOffset());
            if (start != null) {
                Long end =
                        getExcludeEnd(
                                cursor,
                                useLocalTime,
                                excludeIntervalEndTimeColumn,
                                getStartTimeZoneOffset());
                if (end != null) {
                    excludeStarts.add(start);
                    excludeEnds.add(end);
                }
            }
        } while (cursor.moveToNext() && currentSessionUuid.equals(readUuid(cursor)));
        // In case we hit another record, move the cursor back to read next record in outer
        // RecordHelper#getInternalRecords loop.
        cursor.moveToPrevious();

        if (!excludeStarts.isEmpty()) {
            excludeStarts.sort(Comparator.naturalOrder());
            excludeEnds.sort(Comparator.naturalOrder());
            mExcludeStarts = excludeStarts;
            mExcludeEnds = excludeEnds;
            if (Constants.DEBUG) {
                Slog.d(TAG, "Exclude intervals: " + excludeStarts + " ends: " + excludeEnds);
            }
        } else {
            mExcludeStarts = null;
            mExcludeEnds = null;
        }
    }

    @Override
    double getResultOnInterval(AggregationTimestamp startPoint, AggregationTimestamp endPoint) {
        return AggregationRecordData.calculateIntervalOverlapDuration(
                        getStartTime(), startPoint.getTime(), getEndTime(), endPoint.getTime())
                - calculateDurationToExclude(startPoint.getTime(), endPoint.getTime());
    }

    private static @Nullable Long getExcludeStart(
            Cursor cursor,
            boolean useLocalTime,
            @Nullable String excludeIntervalStartTimeColumn,
            @Nullable ZoneOffset zoneOffset) {
        if (excludeIntervalStartTimeColumn == null) {
            return null;
        }
        if (isNullValue(cursor, excludeIntervalStartTimeColumn)) {
            return null;
        }
        if (useLocalTime) {
            return calculateLocalTime(cursor, excludeIntervalStartTimeColumn, zoneOffset);
        } else {
            return getCursorLong(cursor, excludeIntervalStartTimeColumn);
        }
    }

    @Nullable
    private static Long getExcludeEnd(
            Cursor cursor,
            boolean useLocalTime,
            @Nullable String excludeIntervalEndTimeColumn,
            @Nullable ZoneOffset zoneOffset) {
        if (excludeIntervalEndTimeColumn == null) {
            return null;
        }
        if (useLocalTime) {
            return calculateLocalTime(cursor, excludeIntervalEndTimeColumn, zoneOffset);
        } else {
            return getCursorLong(cursor, excludeIntervalEndTimeColumn);
        }
    }

    private static Long calculateLocalTime(
            Cursor cursor, String physicalColumnName, @Nullable ZoneOffset zoneOffset) {
        long offsetMillis;
        if (zoneOffset == null) {
            // This should not happen, but if we get asked to use local time without getting a zone
            // offset, treat as no offset.
            Slog.w(TAG, "Asked to calculate a local time without a zone offset");
            offsetMillis = 0;
        } else {
            offsetMillis = MILLIS_IN_SECOND * zoneOffset.getTotalSeconds();
        }
        return getCursorLong(cursor, physicalColumnName) + offsetMillis;
    }

    private long calculateDurationToExclude(long startTime, long endTime) {
        if (mExcludeStarts == null || mExcludeEnds == null) {
            // No intervals to exclude for this record data.
            return 0;
        }

        long durationToExclude = 0;
        // Find intervals to exclude which potentially can overlap with the given interval.

        // Find the latest start timestamp index such that intervalStart <= startTime
        int lowerBoundStartIndex = Collections.binarySearch(mExcludeStarts, startTime);
        if (lowerBoundStartIndex < 0) {
            // startTime not found in mExcludeStarts, bin search output = -(insertionIndex + 1)
            int insertionIndex = -lowerBoundStartIndex - 1;
            lowerBoundStartIndex = Math.max(insertionIndex - 1, 0);
        }

        // Find the earliest end timestamp index such that intervalEnd >= endTime
        int upperBoundEndIndex = Collections.binarySearch(mExcludeEnds, endTime);
        if (upperBoundEndIndex < 0) {
            // endTime not found, bin search output = - (insertionIndex + 1) = -upper bound
            upperBoundEndIndex = -upperBoundEndIndex;
        }

        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Excluding overlaps with intervals within indexes "
                            + lowerBoundStartIndex
                            + " and "
                            + upperBoundEndIndex);
        }

        for (int index = lowerBoundStartIndex;
                index < Math.min(upperBoundEndIndex + 1, mExcludeStarts.size());
                index++) {
            durationToExclude +=
                    AggregationRecordData.calculateIntervalOverlapDuration(
                            mExcludeStarts.get(index), startTime,
                            mExcludeEnds.get(index), endTime);
        }

        return durationToExclude;
    }
}
