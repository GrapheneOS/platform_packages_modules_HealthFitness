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
import android.annotation.IntRange;
import android.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** Captures the number of swimming strokes. Type of swimming stroke must be provided. */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SWIMMING_STROKES)
public final class SwimmingStrokesRecord extends IntervalRecord {

    private final long mCount;
    private final int mType;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}.
     * @param startTime Start time of this activity
     * @param startZoneOffset Zone offset of the user when the activity started
     * @param endTime End time of this activity
     * @param endZoneOffset Zone offset of the user when the activity finished
     * @param count Count of swimming strokes during this activity
     * @param type Swimming stroke type used in this activity.
     */
    private SwimmingStrokesRecord(
            @NonNull Metadata metadata,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            long count,
            @SwimmingStrokesType.SwimmingStrokesTypes int type) {
        super(metadata, startTime, startZoneOffset, endTime, endZoneOffset);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(startZoneOffset);
        Objects.requireNonNull(startTime);
        Objects.requireNonNull(endZoneOffset);
        mCount = count;
        mType = type;
    }

    /** Identifier for swimming types, as returned by {@link SwimmingStrokesRecord#getType()}. */
    public static final class SwimmingStrokesType {
        public static final int SWIMMING_STROKES_TYPE_OTHER = 0;
        public static final int SWIMMING_STROKES_TYPE_FREESTYLE = 1;
        public static final int SWIMMING_STROKES_TYPE_BACKSTROKE = 2;
        public static final int SWIMMING_STROKES_TYPE_BREASTSTROKE = 3;
        public static final int SWIMMING_STROKES_TYPE_BUTTERFLY = 4;
        public static final int SWIMMING_STROKES_TYPE_MIXED = 5;

        private SwimmingStrokesType() {}

        /** @hide */
        @IntDef({
            SWIMMING_STROKES_TYPE_FREESTYLE,
            SWIMMING_STROKES_TYPE_BACKSTROKE,
            SWIMMING_STROKES_TYPE_BREASTSTROKE,
            SWIMMING_STROKES_TYPE_BUTTERFLY,
            SWIMMING_STROKES_TYPE_MIXED,
            SWIMMING_STROKES_TYPE_OTHER,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface SwimmingStrokesTypes {}
    }

    /**
     * @return Count of strokes during this activity.
     */
    public long getCount() {
        return mCount;
    }

    /**
     * @return Swimming stroke type used in this activity.
     */
    @SwimmingStrokesType.SwimmingStrokesTypes
    public int getType() {
        return mType;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the object argument; {@code false}
     *     otherwise.
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (super.equals(object) && object instanceof SwimmingStrokesRecord) {
            SwimmingStrokesRecord other = (SwimmingStrokesRecord) object;
            return this.getCount() == other.getCount() && this.getType() == other.getType();
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getCount(), this.getType());
    }

    /** Builder class for {@link SwimmingStrokesRecord} */
    public static final class Builder {
        private final Metadata mMetadata;
        private final Instant mStartTime;
        private final Instant mEndTime;
        private ZoneOffset mStartZoneOffset;
        private ZoneOffset mEndZoneOffset;
        private final long mCount;
        private final int mType;

        /**
         * @param metadata Metadata to be associated with the record. See {@link Metadata}.
         * @param startTime Start time of this activity
         * @param endTime End time of this activity
         * @param count Count of strokes. Optional field. Valid range: 1-1000000.
         * @param type Swimming style. Required field. Allowed values: {@link SwimmingStrokesType}.
         */
        public Builder(
                @NonNull Metadata metadata,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @IntRange(from = 1, to = 1000000) long count,
                @SwimmingStrokesType.SwimmingStrokesTypes int type) {
            Objects.requireNonNull(metadata);
            Objects.requireNonNull(startTime);
            Objects.requireNonNull(endTime);
            mMetadata = metadata;
            mStartTime = startTime;
            mEndTime = endTime;
            mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
            mCount = count;
            mType = type;
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
         * @return Object of {@link SwimmingStrokesRecord}
         */
        @NonNull
        public SwimmingStrokesRecord build() {
            return new SwimmingStrokesRecord(
                    mMetadata,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mCount,
                    mType);
        }
    }
}
