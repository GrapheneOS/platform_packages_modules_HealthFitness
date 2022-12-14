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

import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.DISTANCE_RECORD_DISTANCE_TOTAL;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;

import android.annotation.NonNull;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.datatypes.units.Length;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures distance travelled by the user since the last reading. The total distance over an
 * interval can be calculated by adding together all the values during the interval. The start time
 * of each record should represent the start of the interval in which the distance was covered.
 *
 * <p>If break downs are preferred in scenario of a long workout, consider writing multiple distance
 * records. The start time of each record should be equal to or greater than the end time of the
 * previous record.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_DISTANCE)
public final class DistanceRecord extends IntervalRecord {
    /** Builder class for {@link DistanceRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final Length mDistance;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param distance Distance in {@link Length} unit. Required field. Valid range: 0-1000000
         *     meters.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull Length distance) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(distance);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mDistance = distance;
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

        /**
         * @return Object of {@link DistanceRecord}
         */
        @NonNull
        public DistanceRecord build() {
            return new DistanceRecord(
                    mMetadata, mStartTime, mStartZoneOffset, mEndTime, mEndZoneOffset, mDistance);
        }
    }

    /**
     * Metric identifier to get total distance using aggregate APIs in {@link HealthConnectManager}
     */
    @android.annotation.NonNull
    public static final AggregationType<Length> DISTANCE_TOTAL =
            new AggregationType<>(
                    DISTANCE_RECORD_DISTANCE_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_DISTANCE,
                    Length.class);

    private final Length mDistance;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param distance Distance of this activity
     */
    private DistanceRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull Length distance) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(startZoneOffset);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endZoneOffset);
        Objects.requireNonNull(distance);
        mDistance = distance;
    }

    /**
     * @return distance of this activity in {@link Length} unit
     */
    @NonNull
    public Length getDistance() {
        return mDistance;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the object argument; {@code false}
     *     otherwise.
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (super.equals(object) && object instanceof DistanceRecord) {
            DistanceRecord other = (DistanceRecord) object;
            return this.getDistance().equals(other.getDistance());
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getDistance());
    }
}
