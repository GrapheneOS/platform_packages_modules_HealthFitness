/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.health.connect.datatypes;

import android.annotation.NonNull;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.internal.datatypes.BodyWaterMassRecordInternal;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures the user's body water mass. Each record represents a single instantaneous measurement.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS)
public final class BodyWaterMassRecord extends InstantRecord {
    private final Mass mBodyWaterMass;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time time for this record
     * @param zoneOffset Zone offset for this record
     * @param bodyWaterMass bodyWaterMass, in Mass unit
     */
    private BodyWaterMassRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @NonNull Mass bodyWaterMass) {
        super(metadata, time, zoneOffset);
        Objects.requireNonNull(bodyWaterMass);
        mBodyWaterMass = bodyWaterMass;
    }

    /** Returns body water mass, in Mass unit. */
    @NonNull
    public Mass getBodyWaterMass() {
        return mBodyWaterMass;
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
        BodyWaterMassRecord that = (BodyWaterMassRecord) o;
        return getBodyWaterMass().equals(that.getBodyWaterMass());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getBodyWaterMass());
    }

    /** Builder class for {@link BodyWaterMassRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private final Mass mBodyWaterMass;
        private ZoneOffset mZoneOffset;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time time for this record
         * @param bodyWaterMass Body water mass, in {@link Mass} unit.
         */
        public Builder(
                @NonNull Metadata metadata, @NonNull Instant time, @NonNull Mass bodyWaterMass) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            Objects.requireNonNull(bodyWaterMass);
            mMetadata = metadata;
            mTime = time;
            mBodyWaterMass = bodyWaterMass;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(time);
        }

        /** Sets the zone offset for the record. */
        @NonNull
        public BodyWaterMassRecord.Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /** Clears zone offset. */
        @NonNull
        public BodyWaterMassRecord.Builder clearZoneOffset() {
            mZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /** Builds {@link BodyWaterMassRecord} */
        @NonNull
        public BodyWaterMassRecord build() {
            return new BodyWaterMassRecord(mMetadata, mTime, mZoneOffset, mBodyWaterMass);
        }
    }

    /** @hide */
    @Override
    public BodyWaterMassRecordInternal toRecordInternal() {
        BodyWaterMassRecordInternal recordInternal =
                (BodyWaterMassRecordInternal)
                        new BodyWaterMassRecordInternal()
                                .setUuid(getMetadata().getId())
                                .setPackageName(getMetadata().getDataOrigin().getPackageName())
                                .setLastModifiedTime(
                                        getMetadata().getLastModifiedTime().toEpochMilli())
                                .setClientRecordId(getMetadata().getClientRecordId())
                                .setClientRecordVersion(getMetadata().getClientRecordVersion())
                                .setManufacturer(getMetadata().getDevice().getManufacturer())
                                .setModel(getMetadata().getDevice().getModel())
                                .setDeviceType(getMetadata().getDevice().getType());
        recordInternal.setTime(getTime().toEpochMilli());
        recordInternal.setZoneOffset(getZoneOffset().getTotalSeconds());
        recordInternal.setBodyWaterMass(mBodyWaterMass.getInKilograms());
        return recordInternal;
    }
}
