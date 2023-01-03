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
import android.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures any other exercise a user does for which there is not a custom record type. This can be
 * common fitness exercise like running or different sports.
 *
 * <p>Each record needs a start time and end time. Records don't need to be back-to-back or directly
 * after each other, there can be gaps in between.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION)
public final class ExerciseSessionRecord extends IntervalRecord {
    private final int mExerciseType;

    private final CharSequence mNotes;
    private final CharSequence mTitle;
    private final ExerciseRoute mRoute;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param notes Notes for this activity
     * @param exerciseType Type of exercise (e.g. walking, swimming). Required field. Allowed
     *     values: {@link ExerciseSessionType.ExerciseSessionTypes }
     * @param title Title of this activity
     */
    private ExerciseSessionRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @Nullable CharSequence notes,
            @NonNull @ExerciseSessionType.ExerciseSessionTypes int exerciseType,
            @Nullable CharSequence title,
            @Nullable ExerciseRoute route) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        mNotes = notes;
        mExerciseType = exerciseType;
        mTitle = title;
        mRoute = route;
    }

    /** Returns exerciseType of this session. */
    @ExerciseSessionType.ExerciseSessionTypes
    public int getExerciseType() {
        return mExerciseType;
    }

    /** Returns notes for this activity. Returns null if the session doesn't have notes. */
    @Nullable
    public CharSequence getNotes() {
        return mNotes;
    }

    /** Returns title of this session. Returns null if the session doesn't have title. */
    @Nullable
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Returns route of this session. Returns null if the session doesn't have route. */
    @Nullable
    public ExerciseRoute getRoute() {
        return mRoute;
    }

    /** Returns if this session has route. */
    @NonNull
    public boolean hasRoute() {
        return mRoute != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciseSessionRecord)) return false;
        if (!super.equals(o)) return false;
        ExerciseSessionRecord that = (ExerciseSessionRecord) o;
        return getExerciseType() == that.getExerciseType()
                && RecordUtils.isEqualNullableCharSequences(getNotes(), that.getNotes())
                && RecordUtils.isEqualNullableCharSequences(getTitle(), that.getTitle())
                && Objects.equals(getRoute(), that.getRoute());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(), getExerciseType(), getNotes(), getTitle(), getRoute());
    }

    /** Builder class for {@link ExerciseSessionRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final int mExerciseType;
        private CharSequence mNotes;
        private CharSequence mTitle;
        private ExerciseRoute mRoute;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param exerciseType Type of exercise (e.g. walking, swimming). Required field. Allowed
         *     values: {@link ExerciseSessionType}
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @ExerciseSessionType.ExerciseSessionTypes int exerciseType) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mExerciseType = exerciseType;
        }

        /** Sets the zone offset of the user when the session started */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            Objects.requireNonNull(startZoneOffset);

            mStartZoneOffset = startZoneOffset;
            return this;
        }

        /** Sets the zone offset of the user when the session ended */
        @NonNull
        public Builder setEndZoneOffset(@NonNull ZoneOffset endZoneOffset) {
            Objects.requireNonNull(endZoneOffset);

            mEndZoneOffset = endZoneOffset;
            return this;
        }

        /**
         * Sets notes for this activity
         *
         * @param notes Notes for this activity
         */
        @NonNull
        public Builder setNotes(@Nullable CharSequence notes) {
            mNotes = notes;
            return this;
        }

        /**
         * Sets a title of this activity
         *
         * @param title Title of this activity
         */
        @NonNull
        public Builder setTitle(@Nullable CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets route for this activity
         *
         * @param route ExerciseRoute for this activity
         */
        @NonNull
        public Builder setRoute(@Nullable ExerciseRoute route) {
            mRoute = route;
            return this;
        }

        /** Returns {@link ExerciseSessionRecord} */
        @NonNull
        public ExerciseSessionRecord build() {
            return new ExerciseSessionRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mNotes,
                    mExerciseType,
                    mTitle,
                    mRoute);
        }
    }
}
