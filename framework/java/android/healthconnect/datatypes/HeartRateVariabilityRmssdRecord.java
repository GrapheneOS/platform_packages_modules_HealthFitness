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
import java.util.Objects;

/**
 * Captures the user's heart rate variability RMSSD. Each record represents a single instantaneous
 * measurement.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD)
public final class HeartRateVariabilityRmssdRecord extends InstantRecord {
    private final double mHeartRateVariabilityMillis;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time time for this record
     * @param zoneOffset Zone offset for the record
     * @param heartRateVariabilityMillis heartRateVariability in milliseconds
     */
    private HeartRateVariabilityRmssdRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            double heartRateVariabilityMillis) {
        super(metadata, time, zoneOffset);
        mHeartRateVariabilityMillis = heartRateVariabilityMillis;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return {@code true} if this object is the same as the object
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        HeartRateVariabilityRmssdRecord that = (HeartRateVariabilityRmssdRecord) o;
        return getHeartRateVariabilityMillis() == that.getHeartRateVariabilityMillis();
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getHeartRateVariabilityMillis());
    }

    /** Returns heartRateVariabilityMillis for the record. */
    public double getHeartRateVariabilityMillis() {
        return mHeartRateVariabilityMillis;
    }

    /** Builder class for {@link HeartRateVariabilityRmssdRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private final double mHeartRateVariabilityMillis;
        private ZoneOffset mZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time time for this record
         * @param heartRateVariabilityMillis heartRateVariability in milliseconds
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                double heartRateVariabilityMillis) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            mMetadata = metadata;
            mTime = time;
            mHeartRateVariabilityMillis = heartRateVariabilityMillis;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(mTime);
        }

        /** Sets the zone offset of the record entry. */
        @NonNull
        public HeartRateVariabilityRmssdRecord.Builder setZoneOffset(
                @NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /** Clears zone offset. */
        @NonNull
        public HeartRateVariabilityRmssdRecord.Builder clearZoneOffset() {
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            return this;
        }

        /** Builds {@link HeartRateVariabilityRmssdRecord} */
        @NonNull
        public HeartRateVariabilityRmssdRecord build() {
            return new HeartRateVariabilityRmssdRecord(
                    mMetadata, mTime, mZoneOffset, mHeartRateVariabilityMillis);
        }
    }
}
