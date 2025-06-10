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

package android.healthconnect.testing.shared;

import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.TimeInstantRangeFilter;

import java.time.Instant;
import java.time.LocalDateTime;

/** Test data factory for time range filters to be used in aggregations. */
public final class TimeFilterFactory {

    /** Returns a {@link TimeInstantRangeFilter} with given {@code start} and {@code end} time. */
    public static TimeInstantRangeFilter getTimeFilter(Instant start, Instant end) {
        return new TimeInstantRangeFilter.Builder().setStartTime(start).setEndTime(end).build();
    }

    /** Returns a {@link LocalTimeRangeFilter} with given {@code start} and {@code end} time. */
    public static LocalTimeRangeFilter getTimeFilter(LocalDateTime start, LocalDateTime end) {
        return new LocalTimeRangeFilter.Builder().setStartTime(start).setEndTime(end).build();
    }

    /** Returns an open start interval {@link TimeInstantRangeFilter} ending at {@code end} time. */
    public static TimeInstantRangeFilter getOpenStartTimeFilter(Instant end) {
        return new TimeInstantRangeFilter.Builder().setEndTime(end).build();
    }

    /** Returns an open start interval {@link LocalTimeRangeFilter} ending at {@code end} time. */
    public static LocalTimeRangeFilter getOpenStartTimeFilter(LocalDateTime end) {
        return new LocalTimeRangeFilter.Builder().setEndTime(end).build();
    }

    /**
     * Returns an open end interval {@link TimeInstantRangeFilter} starting from {@code start} time.
     */
    public static TimeInstantRangeFilter getOpenEndTimeFilter(Instant start) {
        return new TimeInstantRangeFilter.Builder().setStartTime(start).build();
    }

    /**
     * Returns an open end interval {@link TimeInstantRangeFilter} starting from {@code start} time.
     */
    public static LocalTimeRangeFilter getOpenEndTimeFilter(LocalDateTime start) {
        return new LocalTimeRangeFilter.Builder().setStartTime(start).build();
    }
}
