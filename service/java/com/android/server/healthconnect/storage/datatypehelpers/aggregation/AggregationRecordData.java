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

import static com.android.server.healthconnect.storage.datatypehelpers.IntervalRecordHelper.END_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.IntervalRecordHelper.LOCAL_DATE_TIME_END_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.IntervalRecordHelper.LOCAL_DATE_TIME_START_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.IntervalRecordHelper.START_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.IntervalRecordHelper.START_ZONE_OFFSET_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;

import android.database.Cursor;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Represents priority aggregation data.
 *
 * @hide
 */
public abstract class AggregationRecordData {
    private long mRecordStartTime;
    private long mRecordEndTime;
    private long mAppId;
    private long mLastModifiedTime;
    private ZoneOffset mStartTimeZoneOffset;

    long getStartTime() {
        return mRecordStartTime;
    }

    long getEndTime() {
        return mRecordEndTime;
    }

    long getAppId() {
        return mAppId;
    }

    long getLastModifiedTime() {
        return mLastModifiedTime;
    }

    ZoneOffset getStartTimeZoneOffset() {
        return mStartTimeZoneOffset;
    }

    protected UUID readUuid(Cursor cursor) {
        return StorageUtils.getCursorUUID(cursor, UUID_COLUMN_NAME);
    }

    void populateAggregationData(Cursor cursor, boolean useLocalTime) {
        mRecordStartTime =
                StorageUtils.getCursorLong(
                        cursor,
                        useLocalTime
                                ? LOCAL_DATE_TIME_START_TIME_COLUMN_NAME
                                : START_TIME_COLUMN_NAME);
        mRecordEndTime =
                StorageUtils.getCursorLong(
                        cursor,
                        useLocalTime ? LOCAL_DATE_TIME_END_TIME_COLUMN_NAME : END_TIME_COLUMN_NAME);
        mAppId = StorageUtils.getCursorLong(cursor, APP_INFO_ID_COLUMN_NAME);
        mLastModifiedTime = StorageUtils.getCursorLong(cursor, LAST_MODIFIED_TIME_COLUMN_NAME);
        mStartTimeZoneOffset = StorageUtils.getZoneOffset(cursor, START_ZONE_OFFSET_COLUMN_NAME);
        populateSpecificAggregationData(cursor, useLocalTime);
    }

    AggregationTimestamp getStartTimestamp() {
        return new AggregationTimestamp(AggregationTimestamp.INTERVAL_START, getStartTime())
                .setParentData(this);
    }

    AggregationTimestamp getEndTimestamp() {
        return new AggregationTimestamp(AggregationTimestamp.INTERVAL_END, getEndTime())
                .setParentData(this);
    }

    @VisibleForTesting
    AggregationRecordData setData(long startTime, long endTime, long appId, long lastModifiedTime) {
        mRecordStartTime = startTime;
        mRecordEndTime = endTime;
        mAppId = appId;
        mLastModifiedTime = lastModifiedTime;
        return this;
    }

    /**
     * Calculates aggregation result given start and end time of the target interval. Implementation
     * may assume that it's will be called with non overlapping intervals. So (start time, end time)
     * input intervals of all calls will not overlap.
     */
    abstract double getResultOnInterval(long startTime, long endTime);

    abstract void populateSpecificAggregationData(Cursor cursor, boolean useLocalTime);

    @Override
    public String toString() {
        return "AggregData{startTime=" + mRecordStartTime + ", endTime=" + mRecordEndTime + "}";
    }

    /** Calculates overlap between two intervals */
    static long calculateIntervalOverlapDuration(
            long intervalStart1, long intervalStart2, long intervalEnd1, long intervalEnd2) {
        return Math.max(
                Math.min(intervalEnd1, intervalEnd2) - Math.max(intervalStart1, intervalStart2), 0);
    }
}
