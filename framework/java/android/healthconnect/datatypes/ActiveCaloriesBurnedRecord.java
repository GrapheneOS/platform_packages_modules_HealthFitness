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
import android.healthconnect.datatypes.units.Energy;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the estimated active energy burned by the user (in kilocalories), excluding basal
 * metabolic rate (BMR).
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED)
public final class ActiveCaloriesBurnedRecord extends IntervalRecord {
    /** Builder class for {@link ActiveCaloriesBurnedRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final Energy mEnergy;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param energy Energy in {@link Energy} unit. Required field. Valid range: 0-1000000 kcal.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull Energy energy) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(energy);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEnergy = energy;
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
         * @return Object of {@link ActiveCaloriesBurnedRecord}
         */
        @NonNull
        public ActiveCaloriesBurnedRecord build() {
            return new ActiveCaloriesBurnedRecord(
                    mMetadata, mStartTime, mStartZoneOffset, mEndTime, mEndZoneOffset, mEnergy);
        }
    }

    private final Energy mEnergy;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param energy Energy of this activity
     */
    private ActiveCaloriesBurnedRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull Energy energy) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(startZoneOffset);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endZoneOffset);
        Objects.requireNonNull(energy);
        mEnergy = energy;
    }

    /**
     * @return energy in {@link Energy} unit.
     */
    @NonNull
    public Energy getEnergy() {
        return mEnergy;
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
        if (super.equals(object) && object instanceof ActiveCaloriesBurnedRecord) {
            ActiveCaloriesBurnedRecord other = (ActiveCaloriesBurnedRecord) object;
            return this.getEnergy().equals(other.getEnergy());
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getEnergy());
    }
}
