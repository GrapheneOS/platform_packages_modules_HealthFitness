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
import android.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Captures exercise or a sequence of exercises. This can be a playing game like football or a
 * sequence of fitness exercises.
 *
 * <p>Each record needs a start time, end time and session type. In addition, each record has two
 * optional independent lists of time intervals: {@link ExerciseSegment} represents particular
 * exercise within session, {@link ExerciseLap} represents a lap time within session.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION)
public final class ExerciseSessionRecord extends IntervalRecord {
    private final int mExerciseType;

    private final CharSequence mNotes;
    private final CharSequence mTitle;
    private final ExerciseRoute mRoute;

    private final List<ExerciseSegment> mSegments;
    private final List<ExerciseLap> mLaps;

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
            @Nullable ExerciseRoute route,
            @NonNull List<ExerciseSegment> segments,
            @NonNull List<ExerciseLap> laps) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        mNotes = notes;
        mExerciseType = exerciseType;
        mTitle = title;
        mRoute = route;
        mSegments = Collections.unmodifiableList(segments);
        mLaps = Collections.unmodifiableList(laps);
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

    /**
     * Returns segments of this session. Returns empty list if the session doesn't have exercise
     * segments.
     */
    @NonNull
    public List<ExerciseSegment> getSegments() {
        return mSegments;
    }

    /**
     * Returns laps of this session. Returns empty list if the session doesn't have exercise laps.
     */
    @NonNull
    public List<ExerciseLap> getLaps() {
        return mLaps;
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
                && Objects.equals(getRoute(), that.getRoute())
                && Objects.equals(getSegments(), that.getSegments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                getExerciseType(),
                getNotes(),
                getTitle(),
                getRoute(),
                getSegments());
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
        private List<ExerciseSegment> mSegments;
        private List<ExerciseLap> mLaps;

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
            mExerciseType = exerciseType;
            mSegments = new ArrayList<>();
            mLaps = new ArrayList<>();
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
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

        /** Sets the start zone offset of this record to system default. */
        @NonNull
        public Builder clearStartZoneOffset() {
            mStartZoneOffset = RecordUtils.getDefaultZoneOffset();
            return this;
        }

        /** Sets the start zone offset of this record to system default. */
        @NonNull
        public Builder clearEndZoneOffset() {
            mEndZoneOffset = RecordUtils.getDefaultZoneOffset();
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

        /**
         * Sets segments for this session.
         *
         * @param laps list of {@link ExerciseLap} of this session
         */
        @NonNull
        public Builder setLaps(@NonNull List<ExerciseLap> laps) {
            Objects.requireNonNull(laps);
            mLaps.clear();
            mLaps.addAll(laps);
            return this;
        }

        /**
         * Sets segments for this session.
         *
         * @param segments list of {@link ExerciseSegment} of this session
         */
        @NonNull
        public Builder setSegments(@NonNull List<ExerciseSegment> segments) {
            Objects.requireNonNull(segments);
            mSegments.clear();
            mSegments.addAll(segments);
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
                    mRoute,
                    mSegments,
                    mLaps);
        }
    }
}
