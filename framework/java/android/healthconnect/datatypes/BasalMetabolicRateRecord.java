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

import android.healthconnect.datatypes.units.Power;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the BMR of a user. Each record represents the energy a user would burn if at rest all
 * day, based on their height and weight.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE)
public class BasalMetabolicRateRecord extends InstantRecord {
    /** BasalMetabolicRateRecord builder */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private final Power mBasalMetabolicRate;
        private ZoneOffset mZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());

        /**
         * @see BasalMetabolicRateRecord
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @NonNull Power basalMetabolicRate) {
            mMetadata = metadata;
            mTime = time;
            mBasalMetabolicRate = basalMetabolicRate;
        }

        /** Sets the zone offset of the user when the activity happened */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
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

    private final Power mBasalMetabolicRate;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param basalMetabolicRate Basal metabolic rate, in Power unit.
     */
    public BasalMetabolicRateRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull Power basalMetabolicRate) {
        super(metadata, time, zoneOffset);
        Objects.requireNonNull(basalMetabolicRate);
        mBasalMetabolicRate = basalMetabolicRate;
    }

    /**
     * @return Basal metabolic rate, in Power unit
     */
    @NonNull
    public Power getBasalMetabolicRate() {
        return mBasalMetabolicRate;
    }
}
