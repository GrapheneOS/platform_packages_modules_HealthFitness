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

/**
 * Captures pause or rest events within an exercise. Each record contains the start / stop time of
 * the event.
 *
 * <p>For pause events, resume state can be assumed from the end time of the pause or rest event.
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_EXERCISE_EVENT)
public final class ExerciseEventRecord extends IntervalRecord {
    private final int mEventType;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param eventType Type of event. Required field. Allowed values: {@link
     *     ExerciseEventType.ExerciseEventTypes}.
     */
    private ExerciseEventRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @ExerciseEventType.ExerciseEventTypes int eventType) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        mEventType = eventType;
    }

    /**
     * @return eventType of this activity.
     */
    @NonNull
    @ExerciseEventType.ExerciseEventTypes
    public int getEventType() {
        return mEventType;
    }

    /** Builder class for {@link ExerciseEventRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final int mEventType;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param eventType EventType of this activity
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @ExerciseEventType.ExerciseEventTypes int eventType) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEventType = eventType;
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
         * @return Object of {@link ExerciseEventRecord}
         */
        @NonNull
        public ExerciseEventRecord build() {
            return new ExerciseEventRecord(
                    mMetadata, mStartTime, mStartZoneOffset, mEndTime, mEndZoneOffset, mEventType);
        }
    }

    /**
     * Types of exercise event as returned by {@link ExerciseEventRecord#getEventType()}.
     *
     * <p>They can be either explicitly requested by a user or auto-detected by a tracking app.
     */
    public static final class ExerciseEventType {
        public static final int EXERCISE_EVENT_TYPE_UNKNOWN = 0;
        /**
         * Explicit pause during a workout, requested by the user (by clicking a pause button in the
         * session UI). Movement happening during pause should not contribute to session metrics.
         */
        public static final int EXERCISE_EVENT_TYPE_PAUSE = 1;
        /**
         * Auto-detected periods of rest during a workout. There should be no user movement detected
         * during rest and any movement detected should finish rest event.
         */
        public static final int EXERCISE_EVENT_TYPE_REST = 2;

        private ExerciseEventType() {}

        /** @hide */
        @IntDef({EXERCISE_EVENT_TYPE_UNKNOWN, EXERCISE_EVENT_TYPE_PAUSE, EXERCISE_EVENT_TYPE_REST})
        @Retention(RetentionPolicy.SOURCE)
        public @interface ExerciseEventTypes {}
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
        ExerciseEventRecord that = (ExerciseEventRecord) o;
        return getEventType() == that.getEventType();
    }

    /**
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getEventType());
    }
}
