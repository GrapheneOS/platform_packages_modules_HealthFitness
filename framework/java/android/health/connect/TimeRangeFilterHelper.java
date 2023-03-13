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

package android.health.connect;

import android.annotation.NonNull;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * A helper class for {@link TimeRangeFilter} to handle possible time filter types.
 *
 * @hide
 */
public final class TimeRangeFilterHelper {
    /**
     * @return start time period.
     */
    public static long getPeriodStart(TimeRangeFilter timeRangeFilter) {
        LocalDate localDate;
        if (timeRangeFilter instanceof TimeInstantRangeFilter) {
            localDate =
                    LocalDate.ofInstant(
                            ((TimeInstantRangeFilter) timeRangeFilter).getStartTime(),
                            ZoneOffset.MIN);
        } else if (timeRangeFilter instanceof LocalTimeRangeFilter) {
            localDate = LocalDate.from(((LocalTimeRangeFilter) timeRangeFilter).getStartTime());
        } else {
            throw new IllegalArgumentException(
                    "Invalid time filter object. Object should be either "
                            + "TimeInstantRangeFilter or LocalTimeRangeFilter.");
        }
        return ChronoUnit.DAYS.between(LocalDate.ofEpochDay(0), localDate);
    }

    /**
     * @return end time period.
     */
    public static long getPeriodEnd(TimeRangeFilter timeRangeFilter) {
        LocalDate localDate;
        if (timeRangeFilter instanceof TimeInstantRangeFilter) {
            localDate =
                    LocalDate.ofInstant(
                            ((TimeInstantRangeFilter) timeRangeFilter).getEndTime(),
                            ZoneOffset.MAX);
        } else if (timeRangeFilter instanceof LocalTimeRangeFilter) {
            localDate = LocalDate.from(((LocalTimeRangeFilter) timeRangeFilter).getEndTime());
        } else {
            throw new IllegalArgumentException(
                    "Invalid time filter object. Object should be either "
                            + "TimeInstantRangeFilter or LocalTimeRangeFilter.");
        }
        return ChronoUnit.DAYS.between(LocalDate.ofEpochDay(0), localDate);
    }
    /**
     * @return duration start time epoch milli.
     */
    public static long getDurationStart(@NonNull TimeRangeFilter timeRangeFilter) {
        if (timeRangeFilter instanceof TimeInstantRangeFilter) {
            return ((TimeInstantRangeFilter) timeRangeFilter).getStartTime().toEpochMilli();
        } else if (timeRangeFilter instanceof LocalTimeRangeFilter) {
            return ((LocalTimeRangeFilter) timeRangeFilter)
                    .getStartTime()
                    .toInstant(ZoneOffset.MAX)
                    .toEpochMilli();
        } else {
            throw new IllegalArgumentException(
                    "Invalid time filter object. Object should be either "
                            + "TimeInstantRangeFilter or LocalTimeRangeFilter.");
        }
    }

    /**
     * @return duration end time epoch milli.
     */
    public static long getDurationEnd(@NonNull TimeRangeFilter timeRangeFilter) {
        if (timeRangeFilter instanceof TimeInstantRangeFilter) {
            return ((TimeInstantRangeFilter) timeRangeFilter).getEndTime().toEpochMilli();
        } else if (timeRangeFilter instanceof LocalTimeRangeFilter) {
            return ((LocalTimeRangeFilter) timeRangeFilter)
                    .getEndTime()
                    .toInstant(ZoneOffset.MIN)
                    .toEpochMilli();
        } else {
            throw new IllegalArgumentException(
                    "Invalid time filter object. Object should be either "
                            + "TimeInstantRangeFilter or LocalTimeRangeFilter.");
        }
    }
}
