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

package com.android.server.healthconnect.fitness.aggregation;

import android.annotation.Nullable;
import android.health.connect.Constants;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.TimeRangeFilter;
import android.health.connect.TimeRangeFilterHelper;
import android.util.Pair;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class to encapsulate the logic around time intervals to aggregate over.
 *
 * <p>When dealing with these intervals there are two concepts to consider:
 *
 * <ul>
 *   <li>The "Splits" which are like fence posts marking the starts and ends of intervals
 *   <li>The "intervals" which are like fence panels in between two splits
 * </ul>
 *
 * @hide
 */
public class TimeSplits {
    private static final int MAX_NUMBER_OF_GROUPS = Constants.MAXIMUM_PAGE_SIZE;

    private final long mStartTime;
    private final long mEndTime;
    private final boolean mShouldGroupBy;
    // A list of point s for time intervals to aggregate in.
    // These are "fenceposts" so one more than the number of intervals.
    // The first and last points are start and end points, the middle points are both start and
    // end points for interval.
    private final List<Long> mTimeSplits;

    private TimeSplits(long startTime, long endTime, boolean shouldGroupBy, List<Long> timeSplits) {
        mStartTime = startTime;
        mEndTime = endTime;
        mShouldGroupBy = shouldGroupBy;
        mTimeSplits = timeSplits;
    }

    /** {@return the start time (inclusive) for finding records that overlap these time splits}. */
    public long getStartTime() {
        return mStartTime;
    }

    /** {@return the start time (exclusive) for finding records that overlap these time splits}. */
    public long getEndTime() {
        return mEndTime;
    }

    /** {@return true if these splits should be used to group results during aggregation} */
    public boolean shouldGroupBy() {
        return mShouldGroupBy;
    }

    /** {@return the number of intervals (which is the number of "fenceposts" -1)} */
    public int size() {
        return mTimeSplits.size() - 1;
    }

    /** {@return a list of the splits or "fence posts" marking the edges of the intervals} */
    public List<Long> getSplits() {
        return mTimeSplits;
    }

    /** {@return a list of the intervals or "fence panels" between the splits} */
    public List<Pair<Long, Long>> getIntervals() {
        List<Pair<Long, Long>> groupIntervals = new ArrayList<>();
        long previous = mTimeSplits.get(0);
        for (int i = 1; i < mTimeSplits.size(); i++) {
            Pair<Long, Long> pair = new Pair<>(previous, mTimeSplits.get(i));
            groupIntervals.add(pair);
            previous = mTimeSplits.get(i);
        }

        return groupIntervals;
    }

    /**
     * Make a {@link TimeSplits} from the information provided in an {@link
     * android.health.connect.aidl.AggregateDataRequestParcel}.
     *
     * <p>Exactly one of Period and duration should be non-null. If both are then period will take
     * precedence, but this should not be the case for correctly constructed parcels.
     *
     * @param startTime the start time (inclusive) for finding records that overlap these time
     *     splits, so should be aggregated
     * @param endTime the end time (exclusive) for finding records that overlap these time splits,
     *     so should be aggregated
     * @param period if the splits happen every "period" this should be non-null, it specifies the
     *     sizes of the intervals
     * @param duration if the splits happen every "duration" this should be non-null, it specifies
     *     the sizes of the intervals
     */
    public static TimeSplits makeTimeSplitsFromParcelData(
            long startTime,
            long endTime,
            @Nullable Duration duration,
            @Nullable Period period,
            TimeRangeFilter timeRangeFilter) {
        boolean shouldGroupBy;
        List<Long> timeSplits;
        if (period != null) {
            timeSplits = getGroupSplitsForPeriod(timeRangeFilter, period);
            shouldGroupBy = true;
        } else if (duration != null) {
            timeSplits = getGroupSplitsForDuration(timeRangeFilter, duration);
            shouldGroupBy = true;
        } else {
            timeSplits = List.of(startTime, endTime);
            shouldGroupBy = false;
        }
        return new TimeSplits(startTime, endTime, shouldGroupBy, timeSplits);
    }

    private static List<Long> getGroupSplitsForPeriod(TimeRangeFilter timeFilter, Period period) {
        LocalDateTime filterStart = ((LocalTimeRangeFilter) timeFilter).getStartTime();
        LocalDateTime filterEnd = ((LocalTimeRangeFilter) timeFilter).getEndTime();

        List<Long> splits = new ArrayList<>();
        splits.add(TimeRangeFilterHelper.getMillisOfLocalTime(filterStart));

        LocalDateTime currentEnd = filterStart.plus(period);
        while (!currentEnd.isAfter(filterEnd)) {
            splits.add(TimeRangeFilterHelper.getMillisOfLocalTime(currentEnd));
            currentEnd = currentEnd.plus(period);

            if (splits.size() > MAX_NUMBER_OF_GROUPS) {
                throw new IllegalArgumentException(
                        "Number of groups must not exceed " + MAX_NUMBER_OF_GROUPS);
            }
        }

        // If the last group doesn't fit the rest of the window, we cut it up to filterEnd
        if (splits.get(splits.size() - 1) < TimeRangeFilterHelper.getMillisOfLocalTime(filterEnd)) {
            splits.add(TimeRangeFilterHelper.getMillisOfLocalTime(filterEnd));
        }
        return splits;
    }

    private static List<Long> getGroupSplitsForDuration(
            TimeRangeFilter timeRangeFilter, Duration duration) {
        long groupByStart = TimeRangeFilterHelper.getFilterStartTimeMillis(timeRangeFilter);
        long groupByEnd = TimeRangeFilterHelper.getFilterEndTimeMillis(timeRangeFilter);
        long groupDurationMillis = duration.toMillis();

        if ((groupByEnd - groupByStart) / groupDurationMillis > MAX_NUMBER_OF_GROUPS) {
            throw new IllegalArgumentException(
                    "Number of buckets must not exceed " + MAX_NUMBER_OF_GROUPS);
        }

        List<Long> splits = new ArrayList<>();
        splits.add(groupByStart);
        long currentEnd = groupByStart + groupDurationMillis;
        while (currentEnd <= groupByEnd) {
            splits.add(currentEnd);
            currentEnd += groupDurationMillis;
        }

        // If the last group doesn't fit the rest of the window, we cut it up to filterEnd
        if (splits.get(splits.size() - 1) < groupByEnd) {
            splits.add(groupByEnd);
        }
        return splits;
    }
}
