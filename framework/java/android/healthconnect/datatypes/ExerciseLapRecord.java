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
import android.annotation.Nullable;
import android.healthconnect.datatypes.units.Length;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the time of a lap within an exercise. A lap is explicitly marked segment within an
 * exercise session (such as pool length while swimming or a track lap while running). Each record
 * contains the start / stop time of the lap.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_EXERCISE_LAP)
public final class ExerciseLapRecord extends IntervalRecord {
    /** Builder class for {@link ExerciseLapRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private Length mLength;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         */
        public Builder(
                @NonNull Metadata metadata, @NonNull Instant startTime, @NonNull Instant endTime) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        }

        /**
         * Sets the length of this activity
         *
         * @param length Length of the lap, in {@link Length} unit. Optional field. Valid range:
         *     0-1000000 meters.
         */
        @NonNull
        public Builder setLength(@Nullable Length length) {
            mLength = length;
            return this;
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
         * @return Object of {@link ExerciseLapRecord}
         */
        @NonNull
        public ExerciseLapRecord build() {
            return new ExerciseLapRecord(
                    mMetadata, mStartTime, mStartZoneOffset, mEndTime, mEndZoneOffset, mLength);
        }
    }

    private final Length mLength;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param length Length of this activity
     */
    private ExerciseLapRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @Nullable Length length) {

        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        mLength = length;
    }

    /**
     * @return length in {@link Length} unit.
     */
    @Nullable
    public Length getLength() {
        return mLength;
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
        ExerciseLapRecord that = (ExerciseLapRecord) o;
        return getLength().equals(that.getLength());
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getLength());
    }
}
