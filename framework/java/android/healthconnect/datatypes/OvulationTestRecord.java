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

import android.annotation.IntDef;
import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Each record represents the result of an ovulation test. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST)
public final class OvulationTestRecord extends InstantRecord {

    private final int mResult;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param time Start time of this activity
     * @param zoneOffset Zone offset of the user when the activity started
     * @param result Result of this activity
     */
    private OvulationTestRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @OvulationTestResult.OvulationTestResults int result) {
        super(metadata, time, zoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        mResult = result;
    }

    /**
     * @return test result
     */
    @OvulationTestResult.OvulationTestResults
    public int getResult() {
        return mResult;
    }

    /** Identifier for Ovulation Test Result */
    public static final class OvulationTestResult {
        /**
         * Inconclusive result. Refers to ovulation test results that are indeterminate (e.g. may be
         * testing malfunction, user error, etc.). ". Any unknown value will also be returned as
         * [RESULT_INCONCLUSIVE].
         */
        public static final int RESULT_INCONCLUSIVE = 0;

        /**
         * Positive fertility (may also be referred as "peak" fertility). Refers to the peak of the
         * luteinizing hormone (LH) surge and ovulation is expected to occur in 10-36 hours.
         */
        public static final int RESULT_POSITIVE = 1;

        /**
         * High fertility. Refers to a rise in estrogen or luteinizing hormone that may signal the
         * fertile window (time in the menstrual cycle when conception is likely to occur).
         */
        public static final int RESULT_HIGH = 2;

        /**
         * Negative fertility (may also be referred as "low" fertility). Refers to the time in the
         * cycle where fertility/conception is expected to be low.
         */
        public static final int RESULT_NEGATIVE = 3;

        OvulationTestResult() {}

        /** @hide */
        @IntDef({RESULT_INCONCLUSIVE, RESULT_POSITIVE, RESULT_HIGH, RESULT_NEGATIVE})
        @Retention(RetentionPolicy.SOURCE)
        public @interface OvulationTestResults {}
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
        OvulationTestRecord that = (OvulationTestRecord) o;
        return getResult() == that.getResult();
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getResult());
    }

    /** Builder class for {@link OvulationTestRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        private ZoneOffset mZoneOffset;
        private final int mResult;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param time Start time of this activity
         * @param result The result of a user's ovulation test, which shows if they're ovulating or
         *     not. Required field. Allowed values: {@link OvulationTestResult}.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant time,
                @OvulationTestResult.OvulationTestResults int result) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(time);
            mMetadata = metadata;
            mTime = time;
            mZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mResult = result;
        }

        /** Sets the zone offset of the user when the activity happened */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            Objects.requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /**
         * @return Object of {@link OvulationTestRecord}
         */
        @NonNull
        public OvulationTestRecord build() {
            return new OvulationTestRecord(mMetadata, mTime, mZoneOffset, mResult);
        }
    }
}
