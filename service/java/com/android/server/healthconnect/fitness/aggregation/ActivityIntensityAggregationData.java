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

import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE;
import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.ACTIVITY_INTENSITY_DURATION_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.ACTIVITY_INTENSITY_MINUTES_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.ACTIVITY_INTENSITY_MODERATE_DURATION_TOTAL;
import static android.health.connect.datatypes.AggregationType.AggregationTypeIdentifier.ACTIVITY_INTENSITY_VIGOROUS_DURATION_TOTAL;

import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;

import android.database.Cursor;
import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.AggregationType;

import com.android.server.healthconnect.fitness.recordhelpers.ActivityIntensityRecordHelper;

import java.util.concurrent.TimeUnit;

/**
 * Helper class to aggregate {@link ActivityIntensityRecord} data.
 *
 * <p>Calculates the duration of the overlap between the underlying {@link ActivityIntensityRecord}
 * and the requested window, and applies the multiplier depending on intensity type of the record.
 *
 * @hide
 */
final class ActivityIntensityAggregationData extends AggregationRecordData {

    private static final long MILLIS_IN_A_MINUTE = TimeUnit.MINUTES.toMillis(1);

    @AggregationType.AggregationTypeIdentifier private final int mAggregationType;
    @ActivityIntensityRecord.ActivityIntensityType private int mActivityIntensityType;

    ActivityIntensityAggregationData(
            @AggregationType.AggregationTypeIdentifier int aggregationType) {
        mAggregationType = aggregationType;
    }

    @Override
    double getResultOnInterval(AggregationTimestamp windowStart, AggregationTimestamp windowEnd) {
        double overlapDurationMillis =
                calculateIntervalOverlapDuration(
                        getStartTime(), windowStart.getTime(), getEndTime(), windowEnd.getTime());

        // Zero multiplier is used for cases when the intensity type of the underlying record is
        // different to the intensity type being aggregated, in which case the record duration must
        // not be included in the aggregation.
        int multiplier =
                switch (mAggregationType) {
                    case ACTIVITY_INTENSITY_MODERATE_DURATION_TOTAL ->
                            mActivityIntensityType == ACTIVITY_INTENSITY_TYPE_MODERATE ? 1 : 0;
                    case ACTIVITY_INTENSITY_VIGOROUS_DURATION_TOTAL ->
                            mActivityIntensityType == ACTIVITY_INTENSITY_TYPE_VIGOROUS ? 1 : 0;
                    case ACTIVITY_INTENSITY_DURATION_TOTAL -> 1;
                    case ACTIVITY_INTENSITY_MINUTES_TOTAL ->
                            mActivityIntensityType == ACTIVITY_INTENSITY_TYPE_MODERATE ? 1 : 2;
                    default ->
                            throw new IllegalStateException(
                                    "Unsupported aggregation type: " + mAggregationType);
                };

        // TODO(b/373585917): round the resulting INTENSITY_MINUTES to the nearest minute instead
        // of rounding down to avoid floating point inaccuracy.
        return overlapDurationMillis
                * multiplier
                / (mAggregationType == ACTIVITY_INTENSITY_MINUTES_TOTAL ? MILLIS_IN_A_MINUTE : 1);
    }

    @Override
    void populateSpecificAggregationData(Cursor cursor, boolean useLocalTime) {
        mActivityIntensityType =
                getCursorInt(cursor, ActivityIntensityRecordHelper.TYPE_COLUMN_NAME);
    }
}
