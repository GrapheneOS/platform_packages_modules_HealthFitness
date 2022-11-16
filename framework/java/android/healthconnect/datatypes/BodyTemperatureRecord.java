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

import android.healthconnect.datatypes.BodyTemperatureMeasurementLocation.BodyTemperatureMeasurementLocations;
import android.healthconnect.datatypes.units.Temperature;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the body temperature of a user. Each record represents a single instantaneous body
 * temperature measuremen
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE)
public final class BodyTemperatureRecord extends InstantRecord {

    private final int mMeasurementLocation;
    private final Temperature mTemperature;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param measurementLocation MeasurementLocation of this activity
     * @param temperature Temperature of this activity
     */
    private BodyTemperatureRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @BodyTemperatureMeasurementLocation.BodyTemperatureMeasurementLocations
                    int measurementLocation,
            @NonNull Temperature temperature) {
        super(metadata, time, zoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        Objects.requireNonNull(temperature);
        mMeasurementLocation = measurementLocation;
        mTemperature = temperature;
    }
    /**
     * @return measurementLocation
     */
    @BodyTemperatureMeasurementLocations
    public int getMeasurementLocation() {
        return mMeasurementLocation;
    }
    /**
     * @return temperature in {@link Temperature} unit.
     */
    @NonNull
    public Temperature getTemperature() {
        return mTemperature;
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
        BodyTemperatureRecord that = (BodyTemperatureRecord) o;
        return getMeasurementLocation() == that.getMeasurementLocation()
                && getTemperature().equals(that.getTemperature());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMeasurementLocation(), getTemperature());
    }

    /** Builder class for {@link BodyTemperatureRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final int mMeasurementLocation;
        private final Temperature mTemperature;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param measurementLocation Where on the user's body the temperature measurement was taken
         *     from. Optional field. Allowed values: {@link BodyTemperatureMeasurementLocation}.
         * @param temperature Temperature in {@link Temperature} unit. Required field. Valid range:
         *     0-100 Celsius degrees.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @BodyTemperatureMeasurementLocation.BodyTemperatureMeasurementLocations
                        int measurementLocation,
                @NonNull Temperature temperature) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(temperature);
            mMetadata = metadata;
            mTime = time;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mMeasurementLocation = measurementLocation;
            mTemperature = temperature;
        }

        /** Sets the zone offset of the user when the activity happened */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /**
         * @return Object of {@link BodyTemperatureRecord}
         */
        @NonNull
        public BodyTemperatureRecord build() {
            return new BodyTemperatureRecord(
                    mMetadata, mTime, mZoneOffset, mMeasurementLocation, mTemperature);
        }
    }
}
