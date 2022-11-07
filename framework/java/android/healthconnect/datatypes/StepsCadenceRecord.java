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
import java.util.List;
import java.util.Objects;

/** Captures the user's steps cadence. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE)
public final class StepsCadenceRecord extends IntervalRecord {
    private final List<StepsCadenceRecordSample> mStepsCadenceRecordSamples;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param stepsCadenceRecordSamples Samples of recorded StepsCadenceRecord
     */
    private StepsCadenceRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull List<StepsCadenceRecordSample> stepsCadenceRecordSamples) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(startZoneOffset);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endZoneOffset);
        Objects.requireNonNull(stepsCadenceRecordSamples);
        mStepsCadenceRecordSamples = stepsCadenceRecordSamples;
    }

    /**
     * @return StepsCadenceRecord samples corresponding to this record
     */
    @NonNull
    public List<StepsCadenceRecordSample> getSamples() {
        return mStepsCadenceRecordSamples;
    }

    /** Represents a single measurement of the steps cadence. */
    public static final class StepsCadenceRecordSample {
        private final double mRate;
        private final Instant mTime;

        /**
         * StepsCadenceRecord sample for entries of {@link StepsCadenceRecord}
         *
         * @param rate Rate in steps per minute.
         * @param time The point in time when the measurement was taken.
         */
        public StepsCadenceRecordSample(double rate, @NonNull Instant time) {
            Objects.requireNonNull(time);
            mTime = time;
            mRate = rate;
        }

        /**
         * @return Rate for this sample
         */
        public double getRate() {
            return mRate;
        }

        /**
         * @return time at which this sample was recorded
         */
        @NonNull
        public Instant getTime() {
            return mTime;
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param object the reference object with which to compare.
         * @return {@code true} if this object is the same as the obj
         */
        @Override
        public boolean equals(@NonNull Object object) {
            if (super.equals(object) && object instanceof StepsCadenceRecordSample) {
                StepsCadenceRecordSample other = (StepsCadenceRecordSample) object;
                return this.getRate() == other.getRate() && this.getTime().equals(other.getTime());
            }
            return false;
        }

        /**
         * Returns a hash code value for the object.
         *
         * @return a hash code value for this object.
         */
        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), this.getRate(), this.getTime());
        }
    }

    /** Builder class for {@link StepsCadenceRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private final List<StepsCadenceRecordSample> mStepsCadenceRecordSamples;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param stepsCadenceRecordSamples Samples of recorded StepsCadenceRecord
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull List<StepsCadenceRecordSample> stepsCadenceRecordSamples) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(stepsCadenceRecordSamples);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mStepsCadenceRecordSamples = stepsCadenceRecordSamples;
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
         * @return Object of {@link StepsCadenceRecord}
         */
        @NonNull
        public StepsCadenceRecord build() {
            return new StepsCadenceRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mStepsCadenceRecordSamples);
        }
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (super.equals(object) && object instanceof StepsCadenceRecord) {
            StepsCadenceRecord other = (StepsCadenceRecord) object;
            return this.getSamples().equals(other.getSamples());
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getSamples());
    }
}
