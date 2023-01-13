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
import android.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Captures user sleep session. Each session requires start and end time and a list of {@link
 * Stage}.
 *
 * <p>Each {@link Stage} interval should be between the start time and the end time of the session.
 * Stages within one session must not overlap.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION)
public final class SleepSessionRecord extends IntervalRecord {
    private final List<Stage> mStages;
    private final CharSequence mNotes;
    private final CharSequence mTitle;
    /**
     * Builds {@link SleepSessionRecord} instance
     *
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the session started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the session finished
     * @param stages list of {@link Stage} of the sleep sessions.
     * @param notes Additional notes for the session. Optional field.
     * @param title Title of the session. Optional field.
     */
    private SleepSessionRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull List<Stage> stages,
            @Nullable CharSequence notes,
            @Nullable CharSequence title) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        Objects.requireNonNull(stages);
        validateStages(stages, startTime, endTime);
        mStages = stages;
        mNotes = notes;
        mTitle = title;
    }

    /** Returns notes for the sleep session. Returns null if no notes was specified. */
    @Nullable
    public CharSequence getNotes() {
        return mNotes;
    }

    /** Returns title of the sleep session. Returns null if no notes was specified. */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns stages of the sleep session. */
    @NonNull
    public List<Stage> getStages() {
        return mStages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SleepSessionRecord)) return false;
        if (!super.equals(o)) return false;
        SleepSessionRecord that = (SleepSessionRecord) o;
        return CharSequence.compare(getNotes(), that.getNotes()) == 0
                && CharSequence.compare(getTitle(), that.getTitle()) == 0
                && Objects.equals(getStages(), that.getStages());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getNotes(), getTitle(), getStages());
    }

    /**
     * Captures the user's length and type of sleep. Each record represents a time interval for a
     * stage of sleep.
     *
     * <p>The start time of the record represents the start and end time of the sleep stage and
     * always need to be included.
     */
    public static class Stage {
        @NonNull private final Instant mStartTime;
        @NonNull private final Instant mEndTime;
        @StageType.StageTypes private final int mStageType;

        /**
         * Builds {@link Stage} instance
         *
         * @param startTime start time of the stage
         * @param endTime end time of the stage. Must not be earlier than start time.
         * @param stageType type of the stage. One of {@link StageType}
         */
        public Stage(
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @StageType.StageTypes int stageType) {
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            if (!endTime.isAfter(startTime)) {
                throw new IllegalArgumentException("End time must be after start time.");
            }
            this.mStartTime = startTime;
            this.mEndTime = endTime;
            this.mStageType = stageType;
        }

        /** Returns start time of this stage. */
        @NonNull
        public Instant getStartTime() {
            return mStartTime;
        }

        /** Returns end time of this stage. */
        @NonNull
        public Instant getEndTime() {
            return mEndTime;
        }

        /** Returns stage type. */
        @StageType.StageTypes
        public int getType() {
            return mStageType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Stage)) return false;
            Stage that = (Stage) o;
            return getType() == that.getType()
                    && Objects.equals(getStartTime(), that.getStartTime())
                    && Objects.equals(getEndTime(), that.getEndTime());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getStartTime(), getEndTime(), mStageType);
        }
    }

    /** Identifier for sleeping stage, as returned by {@link Stage#getType()}. */
    public static final class StageType {
        /** Use this type if the stage of sleep is unknown. */
        public static final int STAGE_TYPE_UNKNOWN = 0;

        /**
         * The user is awake and either known to be in bed, or it is unknown whether they are in bed
         * or not.
         */
        public static final int STAGE_TYPE_AWAKE = 1;

        /** The user is asleep but the particular stage of sleep (light, deep or REM) is unknown. */
        public static final int STAGE_TYPE_SLEEPING = 2;

        /** The user is out of bed and assumed to be awake. */
        public static final int STAGE_TYPE_AWAKE_OUT_OF_BED = 3;

        /** The user is in a light sleep stage. */
        public static final int STAGE_TYPE_SLEEPING_LIGHT = 4;

        /** The user is in a deep sleep stage. */
        public static final int STAGE_TYPE_SLEEPING_DEEP = 5;

        /** The user is in a REM sleep stage. */
        public static final int STAGE_TYPE_SLEEPING_REM = 6;

        /** The user is awake and in bed. */
        public static final int STAGE_TYPE_AWAKE_IN_BED = 7;

        private StageType() {}

        /** @hide */
        @IntDef({
            STAGE_TYPE_UNKNOWN,
            STAGE_TYPE_AWAKE,
            STAGE_TYPE_SLEEPING,
            STAGE_TYPE_AWAKE_OUT_OF_BED,
            STAGE_TYPE_SLEEPING_LIGHT,
            STAGE_TYPE_SLEEPING_DEEP,
            STAGE_TYPE_SLEEPING_REM,
            STAGE_TYPE_AWAKE_IN_BED
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface StageTypes {}
    }

    /** Builder class for {@link SleepSessionRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private List<Stage> mStages;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private CharSequence mNotes;
        private CharSequence mTitle;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull List<Stage> stages) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            Objects.requireNonNull(stages);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStages = new ArrayList<>(stages);
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
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
         * Sets notes for this activity
         *
         * @param notes Additional notes for the session. Optional field.
         */
        @NonNull
        public Builder setNotes(@Nullable CharSequence notes) {
            mNotes = notes;
            return this;
        }

        /**
         * Sets a title of this activity
         *
         * @param title Title of the session. Optional field.
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Adds stage to the existing list of sleep stages. Returns Object with updated stages.
         *
         * @param stage stage to add
         */
        @NonNull
        public Builder addStage(@NonNull Stage stage) {
            Objects.requireNonNull(stage);
            mStages.add(stage);
            return this;
        }

        /** Returns {@link SleepSessionRecord} */
        @NonNull
        public SleepSessionRecord build() {
            return new SleepSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mStages,
                    mNotes,
                    mTitle);
        }
    }

    private void validateStages(
            @NonNull List<Stage> stages,
            @NonNull Instant sessionStartTime,
            @NonNull Instant sessionEndTime) {
        // Sort stages by start times.
        List<Stage> sortedStages = new ArrayList<>(stages);
        sortedStages.sort(Comparator.comparing(Stage::getStartTime));
        for (int i = 0; i < sortedStages.size(); i++) {
            Instant stageStartTime = sortedStages.get(i).getStartTime();
            Instant stageEndTime = sortedStages.get(i).getEndTime();
            if (stageStartTime.isBefore(sessionStartTime) || stageEndTime.isAfter(sessionEndTime)) {
                throw new IllegalArgumentException(
                        "Sleep stage time interval must be between within sleep session interval");
            }

            if (i != 0) {
                Instant previousEndTime = sortedStages.get(i - 1).getEndTime();
                if (previousEndTime.isAfter(stageStartTime)) {
                    throw new IllegalArgumentException("Sleep stages must not overlap");
                }
            }
        }
    }
}
