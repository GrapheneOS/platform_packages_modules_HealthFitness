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

package android.healthconnect;

import android.annotation.NonNull;

import java.time.Instant;
import java.util.Objects;

/** Specification of time range for read and delete requests. */
public final class TimeRangeFilter {
    /**
     * @see TimeRangeFilter
     */
    public static final class Builder {
        private final Instant mStartTime;
        private final Instant mEndTime;

        /**
         * @param startTime represents start time of this filter
         * @param endTime end time of this filter
         */
        public Builder(@NonNull Instant startTime, @NonNull Instant endTime) {
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);

            mStartTime = startTime;
            mEndTime = endTime;
        }

        @NonNull
        public TimeRangeFilter build() {
            return new TimeRangeFilter(mStartTime, mEndTime);
        }
    }

    private final Instant mStartTime;
    private final Instant mEndTime;

    /**
     * @param startTime represents start time of this filter
     * @param endTime end time of this filter
     */
    private TimeRangeFilter(@NonNull Instant startTime, @NonNull Instant endTime) {
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endTime);

        mStartTime = startTime;
        mEndTime = endTime;
    }

    /**
     * @return start time of this filter
     */
    @NonNull
    public Instant getStartTime() {
        return mStartTime;
    }

    /**
     * @return end time of this filter
     */
    @NonNull
    public Instant getEndTime() {
        return mEndTime;
    }
}
