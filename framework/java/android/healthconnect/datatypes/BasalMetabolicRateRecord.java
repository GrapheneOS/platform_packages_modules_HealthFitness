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

import static android.healthconnect.datatypes.AggregationType.AggregationTypeIdentifier.BMR_RECORD_BASAL_CALORIES_TOTAL;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;

import android.annotation.NonNull;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.datatypes.units.Power;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the BMR of a user. Each record represents the energy a user would burn if at rest all
 * day, based on their height and weight.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE)
public final class BasalMetabolicRateRecord extends InstantRecord {
    private final Power mBasalMetabolicRate;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param basalMetabolicRate BasalMetabolicRate of this activity
     */
    private BasalMetabolicRateRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull Power basalMetabolicRate) {
        super(metadata, time, zoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(basalMetabolicRate);
        mBasalMetabolicRate = basalMetabolicRate;
    }

    /**
     * @return basalMetabolicRate
     */
    @NonNull
    public Power getBasalMetabolicRate() {
        return mBasalMetabolicRate;
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
        BasalMetabolicRateRecord that = (BasalMetabolicRateRecord) o;
        return getBasalMetabolicRate().equals(that.getBasalMetabolicRate());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getBasalMetabolicRate());
    }

    /** Builder class for {@link BasalMetabolicRateRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final Power mBasalMetabolicRate;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param basalMetabolicRate Basal metabolic rate, in {@link Power} unit. Required field.
         *     Valid range: 0-10000 kcal/day.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @NonNull Power basalMetabolicRate) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(basalMetabolicRate);
            mMetadata = metadata;
            mTime = time;
            mBasalMetabolicRate = basalMetabolicRate;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(time);
        }

        /** Sets the zone offset of the user when the activity happened */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /** Sets the zone offset of this record to system default. */
        @NonNull
        public Builder clearZoneOffset() {
            mZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /**
         * @return Object of {@link BasalMetabolicRateRecord}
         */
        @NonNull
        public BasalMetabolicRateRecord build() {
            return new BasalMetabolicRateRecord(mMetadata, mTime, mZoneOffset, mBasalMetabolicRate);
        }
    }

    /**
     * Metric identifier get total basal calories burnt using aggregate APIs in {@link
     * HealthConnectManager}
     */
    @NonNull
    public static final AggregationType<Power> BASAL_CALORIES_TOTAL =
            new AggregationType<>(
                    BMR_RECORD_BASAL_CALORIES_TOTAL,
                    AggregationType.SUM,
                    RECORD_TYPE_BASAL_METABOLIC_RATE,
                    Power.class);
}
