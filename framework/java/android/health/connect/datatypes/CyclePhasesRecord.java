/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static android.health.connect.Constants.DEFAULT_INT;

import static com.android.healthfitness.flags.Flags.FLAG_CYCLE_PHASES_FLAG;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.internal.datatypes.CyclePhasesRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Represents a user's menstrual cycle phase for a specific day.
 *
 * <p>This record is designed to capture the current phase of the menstrual cycle (e.g., follicular,
 * luteal) for a given instant in time. It is not intended for predictive use cases.
 *
 * <p>The {@code phase} field indicates the current cycle phase, and {@code dayOfCycle} represents
 * the day within the menstrual cycle.
 *
 * @hide
 */
// TODO(b/452289293): Unhide this when API implementation is done
@FlaggedApi(FLAG_CYCLE_PHASES_FLAG)
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_CYCLE_PHASES)
public final class CyclePhasesRecord extends InstantRecord {
    /** Represents an unknown menstrual cycle phase. */
    public static final int PHASE_UNKNOWN = 0;

    /**
     * Represents the follicular phase of the menstrual cycle. This phase begins with menstruation
     * and ends with ovulation.
     */
    public static final int PHASE_FOLLICULAR = 1;

    /**
     * Represents the luteal phase of the menstrual cycle. This phase begins after ovulation and
     * ends just before the next menstrual period.
     */
    public static final int PHASE_LUTEAL = 2;

    /**
     * The phase of the menstrual cycle.
     *
     * <p>Possible values include {@link #PHASE_UNKNOWN}, {@link #PHASE_FOLLICULAR}, and {@link
     * #PHASE_LUTEAL}.
     */
    @CyclePhase private final int mPhase;

    /**
     * The day within the menstrual cycle.
     *
     * <p>This field is optional. If not provided, a value of -1 is used.
     */
    private final int mDayOfCycle;

    /** @hide */
    @IntDef({PHASE_UNKNOWN, PHASE_FOLLICULAR, PHASE_LUTEAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CyclePhase {}

    // TODO(b/452288913): Add VALID_CYCLE_PHASES, without unknown phase.

    /** Returns the phase of the menstrual cycle. */
    @CyclePhase
    public int getPhase() {
        return mPhase;
    }

    /**
     * Returns the day within the menstrual cycle.
     *
     * <p>Returns -1 if the day of cycle was not set.
     */
    public int getDayOfCycle() {
        return mDayOfCycle;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CyclePhasesRecord that = (CyclePhasesRecord) o;
        return mPhase == that.mPhase && mDayOfCycle == that.mDayOfCycle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mPhase, mDayOfCycle);
    }

    /** Builder class for {@link CyclePhasesRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mTime;
        @CyclePhase private final int mPhase;
        private ZoneOffset mZoneOffset = RecordUtils.getDefaultZoneOffset();
        private int mDayOfCycle = DEFAULT_INT;

        public Builder(Metadata metadata, Instant time, @CyclePhase int phase) {
            mMetadata = metadata;
            mTime = time;
            mPhase = phase;
        }

        /**
         * Sets the day of cycle for this data.
         *
         * <p>If unset, the value will be -1.
         */
        @NonNull
        public Builder setDayOfCycle(int dayOfCycle) {
            mDayOfCycle = dayOfCycle;
            return this;
        }

        /** Sets the zone offset of the user when the data was logged. */
        @NonNull
        public Builder setZoneOffset(@NonNull ZoneOffset zoneOffset) {
            requireNonNull(zoneOffset);
            mZoneOffset = zoneOffset;
            return this;
        }

        /** Clears the zone offset of the user when the data was logged. */
        @NonNull
        public Builder clearZoneOffset() {
            mZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /**
         * @return Object of {@link CyclePhasesRecord} without validating the values.
         * @hide
         */
        @NonNull
        public CyclePhasesRecord buildWithoutValidation() {
            return new CyclePhasesRecord(
                    mMetadata, mTime, mZoneOffset, mPhase, mDayOfCycle, /* skipValidation= */ true);
        }

        /**
         * @return Object of {@link CyclePhasesRecord}
         */
        @NonNull
        public CyclePhasesRecord build() {
            return new CyclePhasesRecord(
                    mMetadata,
                    mTime,
                    mZoneOffset,
                    mPhase,
                    mDayOfCycle,
                    /* skipValidation= */ false);
        }
    }

    private CyclePhasesRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            @CyclePhase int phase,
            int dayOfCycle,
            boolean skipValidation) {
        super(metadata, time, zoneOffset, skipValidation);
        // TODO(b/452288913): Add validation

        mPhase = phase;
        mDayOfCycle = dayOfCycle;
    }

    /** @hide */
    @Override
    public RecordInternal<?> toRecordInternal() {
        CyclePhasesRecordInternal recordInternal =
                (CyclePhasesRecordInternal)
                        new CyclePhasesRecordInternal().setMetaData(getMetadata());
        recordInternal
                .setTime(getTime().toEpochMilli())
                .setZoneOffset(getZoneOffset().getTotalSeconds());
        recordInternal.setPhase(mPhase).setDayOfCycle(mDayOfCycle);
        return recordInternal;
    }
}
