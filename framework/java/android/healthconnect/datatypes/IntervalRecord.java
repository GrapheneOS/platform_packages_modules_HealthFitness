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

package android.healthconnect.datatypes;

import android.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneOffset;

/** A record that contains a measurement with a time interval. */
public abstract class IntervalRecord extends Record {
    private final Instant mStartTime;
    private final ZoneOffset mStartZoneOffset;
    private final Instant mEndTime;
    private final ZoneOffset mEndZoneOffset;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     */
    IntervalRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset) {
        super(metadata);
        mStartTime = startTime;
        mStartZoneOffset = startZoneOffset;
        mEndTime = endTime;
        mEndZoneOffset = endZoneOffset;
    }

    /**
     * @return Start time of the activity
     */
    @NonNull
    public Instant getStartTime() {
        return mStartTime;
    }

    /**
     * @return Start time's zone offset of the activity
     */
    @NonNull
    public ZoneOffset getStartZoneOffset() {
        return mStartZoneOffset;
    }

    /**
     * @return End time of the activity
     */
    @NonNull
    public Instant getEndTime() {
        return mEndTime;
    }

    /**
     * @return End time's zone offset of the activity
     */
    @NonNull
    public ZoneOffset getEndZoneOffset() {
        return mEndZoneOffset;
    }
}
