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

package android.health.connect.internal.datatypes;

import android.annotation.NonNull;
import android.os.Parcel;

import java.time.Instant;
import java.util.Objects;

/**
 * Internal time interval.
 *
 * @hide
 */
public class TimeIntervalInternal {
    private final long mStartTime;
    private final long mEndTime;

    /** Reads interval from parcel. */
    public static TimeIntervalInternal readFromParcel(Parcel parcel) {
        return new TimeIntervalInternal(
                Instant.ofEpochMilli(parcel.readLong()), Instant.ofEpochMilli(parcel.readLong()));
    }

    public TimeIntervalInternal(@NonNull Instant startTime, @NonNull Instant endTime) {
        this(startTime.toEpochMilli(), endTime.toEpochMilli());
    }

    public TimeIntervalInternal(long startTime, long endTime) {
        mStartTime = startTime;
        mEndTime = endTime;
    }

    /*
     * Returns start time of the interval.
     */
    @NonNull
    public long getStartTime() {
        return mStartTime;
    }

    /*
     * Returns end time of the interval.
     */
    @NonNull
    public long getEndTime() {
        return mEndTime;
    }

    /** Writes interval to parcel. */
    public void writeToParcel(Parcel parcel) {
        parcel.writeLong(mStartTime);
        parcel.writeLong(mEndTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeIntervalInternal)) return false;
        TimeIntervalInternal that = (TimeIntervalInternal) o;
        return getStartTime() == that.getStartTime() && getEndTime() == that.getEndTime();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStartTime(), getEndTime());
    }
}
