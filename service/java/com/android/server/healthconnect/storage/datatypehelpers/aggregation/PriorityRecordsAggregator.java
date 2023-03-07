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

package com.android.server.healthconnect.storage.datatypehelpers.aggregation;

import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.EXERCISE_SESSION_DURATION_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.SLEEP_SESSION_DURATION_TOTAL;

import android.database.Cursor;
import android.health.connect.Constants;
import android.health.connect.datatypes.AggregationType;
import android.util.ArrayMap;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.storage.datatypehelpers.ExerciseSegmentRecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.SleepStageRecordHelper;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Aggregates records with priorities.
 *
 * @hide
 */
public class PriorityRecordsAggregator implements Comparator<AggregationRecordData> {
    static final String TAG = "HealthPriorityRecordsAggregator";

    private final List<Long> mGroupSplits;
    private final Map<Long, Integer> mAppIdToPriority;
    private final Map<Integer, Double> mGroupToAggregationResult;
    private final Map<Integer, ZoneOffset> mGroupToFirstZoneOffset;
    private final int mNumberOfGroups;
    private int mCurrentGroup = -1;

    private long mLatestPopulatedStart = -1;
    @AggregationType.AggregationTypeIdentifier private final int mAggregationType;

    private final TreeSet<AggregationTimestamp> mTimestampsBuffer;
    private final TreeSet<AggregationRecordData> mOpenIntervals;

    public PriorityRecordsAggregator(
            List<Long> groupSplits,
            List<Long> appIdPriorityList,
            @AggregationType.AggregationTypeIdentifier int aggregationType) {
        mGroupSplits = groupSplits;
        mAggregationType = aggregationType;
        mAppIdToPriority = new ArrayMap<>();
        for (int i = 0; i < appIdPriorityList.size(); i++) {
            // Add to the map with -index, so app with higher priority has higher value in the map.
            mAppIdToPriority.put(appIdPriorityList.get(i), appIdPriorityList.size() - i);
        }

        mTimestampsBuffer = new TreeSet<>();
        mNumberOfGroups = mGroupSplits.size() - 1;
        mGroupToFirstZoneOffset = new ArrayMap<>(mNumberOfGroups);
        mOpenIntervals = new TreeSet<>(this);
        mGroupToAggregationResult = new ArrayMap<>(mGroupSplits.size());
        for (int i = 0; i < mGroupSplits.size() - 1; i++) {
            mGroupToAggregationResult.put(i, 0.0);
        }

        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Aggregation request for splits: "
                            + mGroupSplits
                            + " with priorities: "
                            + appIdPriorityList);
        }
    }

    /** Calculates aggregation result for each group. */
    public void calculateAggregation(Cursor cursor) {
        initialiseTimestampsBuffer(cursor);
        populateTimestampBuffer(cursor);
        AggregationTimestamp scanPoint, nextPoint;
        while (mTimestampsBuffer.size() > 1) {
            scanPoint = mTimestampsBuffer.pollFirst();
            nextPoint = mTimestampsBuffer.first();
            if (scanPoint.getType() == AggregationTimestamp.GROUP_BORDER) {
                mCurrentGroup += 1;
            } else if (scanPoint.getType() == AggregationTimestamp.INTERVAL_START) {
                mOpenIntervals.add(scanPoint.getParentData());
                recordFirstInGroupZoneOffset(scanPoint.getParentData().getStartTimeZoneOffset());
            } else if (scanPoint.getType() == AggregationTimestamp.INTERVAL_END) {
                mOpenIntervals.remove(scanPoint.getParentData());
            } else {
                throw new UnsupportedOperationException(
                        "Unknown aggregation timestamp type: " + scanPoint.getType());
            }
            updateAggregationResult(scanPoint, nextPoint);
            populateTimestampBuffer(cursor);
        }

        if (Constants.DEBUG) {
            Slog.d(TAG, "Aggregation result: " + mGroupToAggregationResult.toString());
        }
    }

    private void populateTimestampBuffer(Cursor cursor) {
        // Buffer populating strategy guarantees that at the moment we start to process the earliest
        // record, we added to the buffer later overlapping records and the first non-overlapping
        // record. It guarantees that the aggregation score can be calculated correctly for any
        // timestamp within the earliest record interval.
        if (mTimestampsBuffer.first().getType() != AggregationTimestamp.INTERVAL_START) {
            return;
        }

        // Add record timestamps to buffer until latest buffer record do not overlap with earliest
        // buffer record.
        long expansionBorder = mTimestampsBuffer.first().getParentData().getEndTime();
        while (mLatestPopulatedStart < expansionBorder && cursor.moveToNext()) {
            AggregationRecordData data = readNewDataAndAddToBuffer(cursor);
            mLatestPopulatedStart = data.getStartTime();
        }

        if (Constants.DEBUG) {
            Slog.d(TAG, "Timestamps buffer: " + mTimestampsBuffer);
        }
    }

    private void initialiseTimestampsBuffer(Cursor cursor) {
        for (Long groupSplit : mGroupSplits) {
            mTimestampsBuffer.add(
                    new AggregationTimestamp(AggregationTimestamp.GROUP_BORDER, groupSplit));
        }

        if (cursor.moveToNext()) {
            readNewDataAndAddToBuffer(cursor);
        }

        if (Constants.DEBUG) {
            Slog.d(TAG, "Initialised aggregation buffer: " + mTimestampsBuffer);
        }
    }

    private AggregationRecordData readNewDataAndAddToBuffer(Cursor cursor) {
        AggregationRecordData data = readNewData(cursor);
        mTimestampsBuffer.add(data.getStartTimestamp());
        mTimestampsBuffer.add(data.getEndTimestamp());
        return data;
    }

    @VisibleForTesting
    AggregationRecordData readNewData(Cursor cursor) {
        AggregationRecordData data = createAggregationRecordData();
        data.populateAggregationData(cursor);
        return data;
    }

    /** Returns result for the given group */
    public Double getResultForGroup(Integer groupNumber) {
        return mGroupToAggregationResult.get(groupNumber);
    }

    /** Returns start time zone offset for the given group */
    public ZoneOffset getZoneOffsetForGroup(Integer groupNumber) {
        return mGroupToFirstZoneOffset.get(groupNumber);
    }

    private AggregationRecordData createAggregationRecordData() {
        switch (mAggregationType) {
            case SLEEP_SESSION_DURATION_TOTAL:
                return new SessionDurationAggregationData(
                        SleepStageRecordHelper.getStartTimeColumnName(),
                        SleepStageRecordHelper.getEndTimeColumnName());
            case EXERCISE_SESSION_DURATION_TOTAL:
                return new SessionDurationAggregationData(
                        ExerciseSegmentRecordHelper.getStartTimeColumnName(),
                        ExerciseSegmentRecordHelper.getEndTimeColumnName());
            default:
                throw new UnsupportedOperationException(
                        "Priority aggregation do not support type: " + mAggregationType);
        }
    }

    private void recordFirstInGroupZoneOffset(ZoneOffset startTimeZoneOffset) {
        if (mCurrentGroup == -1) {
            return;
        }

        if (!mGroupToFirstZoneOffset.containsKey(mCurrentGroup)) {
            mGroupToFirstZoneOffset.put(mCurrentGroup, startTimeZoneOffset);
        }
    }

    private void updateAggregationResult(
            AggregationTimestamp startPoint, AggregationTimestamp endPoint) {
        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Updating result for group "
                            + mCurrentGroup
                            + " for interval: ("
                            + startPoint.getTime()
                            + ", "
                            + endPoint.getTime()
                            + ")");
        }

        if (mOpenIntervals.isEmpty() || mCurrentGroup < 0 || mCurrentGroup >= mNumberOfGroups) {
            return;
        }

        mGroupToAggregationResult.put(
                mCurrentGroup,
                mGroupToAggregationResult.get(mCurrentGroup)
                        + mOpenIntervals
                                .last()
                                .getResultOnInterval(startPoint.getTime(), endPoint.getTime()));
    }

    // Compared aggregation data by data source priority, then by last modified time.
    @Override
    public int compare(AggregationRecordData o1, AggregationRecordData o2) {
        if (!Objects.equals(
                mAppIdToPriority.get(o1.getAppId()), mAppIdToPriority.get(o2.getAppId()))) {
            return Integer.compare(
                    mAppIdToPriority.get(o1.getAppId()), mAppIdToPriority.get(o2.getAppId()));
        }

        // The later the last modified time, the higher priority this record has.
        return Long.compare(o2.getLastModifiedTime(), o1.getLastModifiedTime());
    }
}
