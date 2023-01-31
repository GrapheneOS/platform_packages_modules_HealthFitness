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

import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.FLOORS_CLIMBED_RECORD_FLOORS_CLIMBED_TOTAL;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED;

import android.annotation.IntRange;
import android.annotation.NonNull;
import android.healthconnect.HealthConnectManager;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures the number of floors climbed by the user between the start and end time. */
@Identifier(recordIdentifier = RECORD_TYPE_FLOORS_CLIMBED)
public final class FloorsClimbedRecord extends IntervalRecord {
    /**
     * Metric identifier to get total floors climbed using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Long> FLOORS_CLIMBED_TOTAL =
            new AggregationType<>(
                    FLOORS_CLIMBED_RECORD_FLOORS_CLIMBED_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_FLOORS_CLIMBED,
                    Long.class);

    private final int mFloors;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param floors Number of floors of this activity. Valid range: 0-1000000.
     */
    private FloorsClimbedRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @IntRange(from = 0, to = 1000000) int floors) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        ValidationUtils.requireInRange(floors, 0, 1000000, "floors");
        mFloors = floors;
    }

    /**
     * @return number of floors climbed.
     */
    @IntRange(from = 0, to = 1000000)
    public int getFloors() {
        return mFloors;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        FloorsClimbedRecord that = (FloorsClimbedRecord) o;
        return getFloors() == that.getFloors();
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getFloors());
    }

    /** Builder class for {@link FloorsClimbedRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final int mFloors;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param floors Number of floors. Required field. Valid range: 0-1000000.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                int floors) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mFloors = floors;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
        }

        /** Sets the zone offset of the user when the activity started */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);

            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Sets the zone offset of the user when the activity ended */
        @NonNull
        public Builder setEndZoneOffset(@NonNull ZoneOffset endZoneOffset) {
            Objects.requireNonNull(endZoneOffset);

            mEndZoneOffset = endZoneOffset;
            return this;
        }

        /** Sets the start zone offset of this record to system default. */
        @NonNull
        public Builder clearStartZoneOffset() {
            mStartZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /** Sets the start zone offset of this record to system default. */
        @NonNull
        public Builder clearEndZoneOffset() {
            mEndZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /**
         * @return Object of {@link FloorsClimbedRecord}
         */
        @NonNull
        public FloorsClimbedRecord build() {
            return new FloorsClimbedRecord(
                    mMetadata, mStartTime, mStartZoneOffset, mEndTime, mEndZoneOffset, mFloors);
        }
    }
}
