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
import android.healthconnect.datatypes.units.Percentage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the body fat percentage of a user. Each record represents a person's total body fat as a
 * percentage of their total body mass.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BODY_FAT)
public final class BodyFatRecord extends InstantRecord {

    private final Percentage mPercentage;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param percentage Percentage of this activity
     */
    private BodyFatRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull Percentage percentage) {
        super(metadata, time, zoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(percentage);
        mPercentage = percentage;
    }
    /**
     * @return percentage in {@link Percentage} unit.
     */
    @NonNull
    public Percentage getPercentage() {
        return mPercentage;
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
        BodyFatRecord that = (BodyFatRecord) o;
        return getPercentage().equals(that.getPercentage());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getPercentage());
    }

    /** Builder class for {@link BodyFatRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final Percentage mPercentage;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param percentage Percentage in {@link Percentage} unit. Required field. Valid range:
         *     0-100.
         */
        public Builder(
                @NonNull Metadata metadata, @NonNull Instant time, @NonNull Percentage percentage) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(percentage);
            mMetadata = metadata;
            mTime = time;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mPercentage = percentage;
        }

        /** Sets the zone offset of the user when the activity happened */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /**
         * @return Object of {@link BodyFatRecord}
         */
        @NonNull
        public BodyFatRecord build() {
            return new BodyFatRecord(mMetadata, mTime, mZoneOffset, mPercentage);
        }
    }
}
