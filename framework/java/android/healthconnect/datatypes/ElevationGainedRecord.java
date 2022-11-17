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
import android.healthconnect.datatypes.units.Length;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures the elevation gained by the user since the last reading. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED)
public final class ElevationGainedRecord extends IntervalRecord {
    /** Builder class for {@link ElevationGainedRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final Length mElevation;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param elevation Elevation in {@link Length} unit. Required field. Valid range:
         *     -1000000-1000000 meters.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull Length elevation) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(elevation);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mElevation = elevation;
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
         * @return Object of {@link ElevationGainedRecord}
         */
        @NonNull
        public ElevationGainedRecord build() {
            return new ElevationGainedRecord(
                    mMetadata, mStartTime, mStartZoneOffset, mEndTime, mEndZoneOffset, mElevation);
        }
    }

    private final Length mElevation;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param elevation Elevation of this activity
     */
    private ElevationGainedRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull Length elevation) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(startZoneOffset);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endZoneOffset);
        Objects.requireNonNull(elevation);
        mElevation = elevation;
    }

    /**
     * @return elevation in {@link Length} unit.
     */
    @NonNull
    public Length getElevation() {
        return mElevation;
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
        if (super.equals(object) && object instanceof ElevationGainedRecord) {
            ElevationGainedRecord other = (ElevationGainedRecord) object;
            return this.getElevation().equals(other.getElevation());
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getElevation());
    }
}
