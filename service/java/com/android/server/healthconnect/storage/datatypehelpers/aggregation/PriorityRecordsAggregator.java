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

import com.android.server.healthconnect.storage.datatypehelpers.ExerciseSegmentRecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.SleepStageRecordHelper;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    @AggregationType.AggregationTypeIdentifier private final int mAggregationType;

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

        mGroupToAggregationResult = new ArrayMap<>(mGroupSplits.size());
        for (int i = 0; i < mGroupSplits.size() - 1; i++) {
            mGroupToAggregationResult.put(i, 0.0);
        }
        mNumberOfGroups = mGroupSplits.size();
        mGroupToFirstZoneOffset = new ArrayMap<>(mNumberOfGroups);

        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Aggregation request for splits: "
                            + mGroupSplits
                            + " with priorities: "
                            + appIdPriorityList);
        }
    }

    // TODO(b/270051633): handle overlaps using priorities.
    /** Calculates aggregation result for each group. */
    public Map<Integer, Double> calculateAggregation(Cursor cursor) {
        AggregationRecordData data = createAggregationRecordData();
        int currentGroup = 0;
        while (cursor.moveToNext()) {
            data.populateAggregationData(cursor);
            while (currentGroup < mNumberOfGroups
                    && data.getStartTime() > getGroupEnd(currentGroup)) {
                currentGroup += 1;
            }

            // Update group score until current record overlaps with group interval.
            int recordGroup = currentGroup;
            while (recordGroup < mNumberOfGroups - 1
                    && data.getEndTime() > getGroupStart(recordGroup)) {
                updateZoneOffsetForGroup(recordGroup, data.getStartTimeZoneOffset());
                updateResultForGroup(recordGroup, data);
                recordGroup += 1;
            }
        }

        if (Constants.DEBUG) {
            Slog.d(TAG, "Aggregation result: " + mGroupToAggregationResult.toString());
        }
        return mGroupToAggregationResult;
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

    private void updateZoneOffsetForGroup(int recordGroup, ZoneOffset startTimeZoneOffset) {
        if (!mGroupToFirstZoneOffset.containsKey(recordGroup)) {
            mGroupToFirstZoneOffset.put(recordGroup, startTimeZoneOffset);
        }
    }

    private void updateResultForGroup(int groupIndex, AggregationRecordData data) {
        mGroupToAggregationResult.put(
                groupIndex,
                mGroupToAggregationResult.get(groupIndex)
                        + data.getResultOnInterval(
                                getGroupStart(groupIndex), getGroupEnd(groupIndex)));
    }

    private long getGroupStart(int index) {
        return mGroupSplits.get(index);
    }

    private long getGroupEnd(int index) {
        return mGroupSplits.get(index + 1);
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
