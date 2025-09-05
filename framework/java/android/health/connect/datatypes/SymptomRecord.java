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

import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.internal.datatypes.SymptomRecordInternal;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Captures a description of a user's symptom. Each record represents a particular symptom
 * experienced one or more times over a time period.
 */
@FlaggedApi(FLAG_SYMPTOMS)
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SYMPTOM)
public final class SymptomRecord extends IntervalRecord {

    @SymptomType private final int mSymptomType;
    @Nullable private final String mNotes;
    @SymptomSeverity private final int mSeverity;
    private final int mCount;
    @SymptomRecordTemporalType private final int mTemporalType;

    /**
     * @param symptomType The type of symptom, from {@link SymptomType}.
     * @param notes A description of the symptom.
     * @param severity The severity of the symptom.
     * @param count The number of occurrences of the symptom.
     * @param temporalType The temporal type of the record.
     * @param startTime The start time of the record.
     * @param startZoneOffset The start zone offset of the record.
     * @param endTime The end time of the record.
     * @param endZoneOffset The end zone offset of the record.
     * @param metadata The metadata of the record.
     * @param skipValidation Whether to skip validation of the record.
     * @hide
     */
    public SymptomRecord(
            @SymptomType int symptomType,
            @Nullable String notes,
            @SymptomSeverity int severity,
            int count,
            @SymptomRecordTemporalType int temporalType,
            @NonNull Instant startTime,
            @NonNull ZoneOffset startZoneOffset,
            @NonNull Instant endTime,
            @NonNull ZoneOffset endZoneOffset,
            @NonNull Metadata metadata,
            boolean skipValidation) {
        super(
                metadata,
                startTime,
                startZoneOffset,
                endTime,
                endZoneOffset,
                skipValidation,
                /* enforceFutureTimeRestrictions= */ true);
        mSymptomType = symptomType;
        mNotes = notes;
        mSeverity = severity;
        mCount = count;
        mTemporalType = temporalType;
    }

    /** Returns the type of symptom for this record. */
    @SymptomType
    public int getSymptomType() {
        return mSymptomType;
    }

    /** Returns the notes for this record. */
    @Nullable
    public String getNotes() {
        return mNotes;
    }

    /** Returns the severity of the symptom for this record. */
    @SymptomSeverity
    public int getSeverity() {
        return mSeverity;
    }

    /** Returns the number of occurrences of the symptom for this record. */
    public int getCount() {
        return mCount;
    }

    /**
     * Returns the temporal type of this record, indicating whether it represents an instant, an
     * interval, or a local date.
     *
     * @return The temporal type, as one of {@link #RECORD_TEMPORAL_TYPE_INSTANT}, {@link
     *     #RECORD_TEMPORAL_TYPE_INTERVAL}, or {@link #RECORD_TEMPORAL_TYPE_LOCAL_DATE}.
     */
    @SymptomRecordTemporalType
    public int getTemporalType() {
        return mTemporalType;
    }

    /** Returns the date of the record, or null if the record is not a local date record. */
    @Nullable
    public LocalDate getDate() {
        if (getTemporalType() == RECORD_TEMPORAL_TYPE_LOCAL_DATE) {
            return getStartTime().atOffset(getStartZoneOffset()).toLocalDate();
        }
        return null;
    }

    /** @hide */
    @Override
    @NonNull
    public SymptomRecordInternal toRecordInternal() {
        SymptomRecordInternal recordInternal =
                (SymptomRecordInternal) new SymptomRecordInternal().setMetaData(getMetadata());
        recordInternal.setTimeInterval(this);
        recordInternal.setSymptomType(mSymptomType);
        if (mNotes != null) {
            recordInternal.setNotes(mNotes);
        }
        recordInternal.setSeverity(mSeverity);
        recordInternal.setCount(mCount);
        recordInternal.setTemporalType(mTemporalType);
        return recordInternal;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof SymptomRecord that)) return false;
        if (!super.equals(o)) return false;
        return mSymptomType == that.mSymptomType
                && mSeverity == that.mSeverity
                && mCount == that.mCount
                && mTemporalType == that.mTemporalType
                && Objects.equals(mNotes, that.mNotes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(), mSymptomType, mNotes, mSeverity, mCount, mTemporalType);
    }

    /** @hide */
    @IntDef({
        SYMPTOM_TYPE_UNKNOWN,
        SYMPTOM_TYPE_COUGH,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SymptomType {}

    /** Unknown symptom type. */
    public static final int SYMPTOM_TYPE_UNKNOWN = 0;

    /** Cough symptom. */
    public static final int SYMPTOM_TYPE_COUGH = 1;

    // TODO(b/438675118): Add remaining symptom types once permissions are added.

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SEVERITY_UNSPECIFIED, SEVERITY_MILD, SEVERITY_MODERATE, SEVERITY_SEVERE})
    public @interface SymptomSeverity {}

    /** Unspecified severity. */
    public static final int SEVERITY_UNSPECIFIED = 0;

    /** Mild severity. */
    public static final int SEVERITY_MILD = 1;

    /** Moderate severity. */
    public static final int SEVERITY_MODERATE = 2;

    /** Severe severity. */
    public static final int SEVERITY_SEVERE = 3;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
        RECORD_TEMPORAL_TYPE_INSTANT,
        RECORD_TEMPORAL_TYPE_INTERVAL,
        RECORD_TEMPORAL_TYPE_LOCAL_DATE
    })
    public @interface SymptomRecordTemporalType {}

    /** The record represents an instantaneous event. */
    public static final int RECORD_TEMPORAL_TYPE_INSTANT = 0;

    /** The record represents an event over an interval. */
    public static final int RECORD_TEMPORAL_TYPE_INTERVAL = 1;

    /** The record represents an event that occurred on a specific date. */
    public static final int RECORD_TEMPORAL_TYPE_LOCAL_DATE = 2;

    /** Builder class for {@link SymptomRecord}. */
    public static final class Builder {
        @SymptomType private final int mSymptomType;
        @NonNull private final Metadata mMetadata;
        @NonNull private final Instant mStartTime;
        @NonNull private final Instant mEndTime;
        @Nullable private String mNotes;
        @NonNull private ZoneOffset mStartZoneOffset;
        @NonNull private ZoneOffset mEndZoneOffset;
        @SymptomSeverity private int mSeverity = SEVERITY_UNSPECIFIED;
        private int mCount = 1;
        @SymptomRecordTemporalType private final int mTemporalType;

        /**
         * Builder for a symptom that occurs at a specific instant time. The temporal type of the
         * record will be set to {@link #RECORD_TEMPORAL_TYPE_INSTANT}.
         *
         * @param symptomType The type of symptom.
         * @param time The time of the record.
         * @param metadata The metadata of the record.
         */
        public Builder(
                @SymptomType int symptomType, @NonNull Instant time, @NonNull Metadata metadata) {
            this.mSymptomType = symptomType;
            this.mStartTime = Objects.requireNonNull(time, "Time cannot be null");
            this.mEndTime = Objects.requireNonNull(time, "Time cannot be null");
            this.mMetadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
            this.mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(time);
            this.mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(time);
            this.mTemporalType = RECORD_TEMPORAL_TYPE_INSTANT;
        }

        /**
         * Builder for a symptom that occurs over a time interval. The temporal type of the record
         * will be set to {@link #RECORD_TEMPORAL_TYPE_INTERVAL}.
         *
         * @param symptomType The type of symptom.
         * @param startTime The start time of the record.
         * @param endTime The end time of the record.
         * @param metadata The metadata of the record.
         */
        public Builder(
                @SymptomType int symptomType,
                @NonNull Instant startTime,
                @NonNull Instant endTime,
                @NonNull Metadata metadata) {
            if (!startTime.isBefore(endTime)) {
                throw new IllegalArgumentException("startTime must be before endTime");
            }
            this.mSymptomType = symptomType;
            this.mStartTime = Objects.requireNonNull(startTime, "StartTime cannot be null");
            this.mEndTime = Objects.requireNonNull(endTime, "EndTime cannot be null");
            this.mMetadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
            this.mStartZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(startTime);
            this.mEndZoneOffset = ZoneOffset.systemDefault().getRules().getOffset(endTime);
            this.mTemporalType = RECORD_TEMPORAL_TYPE_INTERVAL;
        }

        /**
         * Builder for a symptom that falls on a specific local date. The temporal type of the
         * record will be set to {@link #RECORD_TEMPORAL_TYPE_LOCAL_DATE}.
         *
         * @param symptomType The type of symptom.
         * @param date The date of the record.
         * @param metadata The metadata of the record.
         */
        public Builder(
                @SymptomType int symptomType, @NonNull LocalDate date, @NonNull Metadata metadata) {
            Objects.requireNonNull(date, "Date cannot be null");
            this.mSymptomType = symptomType;
            this.mMetadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
            this.mStartZoneOffset =
                    ZoneId.systemDefault().getRules().getOffset(date.atStartOfDay());
            this.mEndZoneOffset =
                    ZoneId.systemDefault().getRules().getOffset(LocalTime.MAX.atDate(date));
            this.mStartTime = date.atStartOfDay().toInstant(mStartZoneOffset);
            this.mEndTime = LocalTime.MAX.atDate(date).toInstant(mEndZoneOffset);
            this.mTemporalType = RECORD_TEMPORAL_TYPE_LOCAL_DATE;
        }

        /**
         * Sets the notes for this record.
         *
         * @param notes A description of the symptom.
         * @return This builder.
         */
        @NonNull
        public Builder setNotes(@Nullable String notes) {
            this.mNotes = notes;
            return this;
        }

        /**
         * Sets the severity of the symptom for this record.
         *
         * @param severity The severity of the symptom.
         * @return This builder.
         */
        @NonNull
        public Builder setSeverity(@SymptomSeverity int severity) {
            this.mSeverity = severity;
            return this;
        }

        /**
         * Sets the number of occurrences of the symptom for this record.
         *
         * @param count The number of occurrences of the symptom.
         * @return This builder.
         * @throws IllegalStateException if the builder is for an instant record.
         */
        @NonNull
        public Builder setCount(int count) {
            if (mTemporalType == RECORD_TEMPORAL_TYPE_INSTANT) {
                throw new IllegalStateException(
                        "Count is not supported for instant symptom records.");
            }
            this.mCount = count;
            return this;
        }

        /**
         * Sets the start zone offset of this record.
         *
         * @param startZoneOffset The start zone offset of the record.
         * @return This builder.
         * @throws IllegalStateException if the builder is for a local date record.
         */
        @NonNull
        public Builder setStartZoneOffset(@NonNull ZoneOffset startZoneOffset) {
            if (mTemporalType == RECORD_TEMPORAL_TYPE_LOCAL_DATE) {
                throw new IllegalStateException(
                        "Zone offset is not supported for local date symptom records.");
            }
            this.mStartZoneOffset = startZoneOffset;
            return this;
        }

        /**
         * Sets the end zone offset of this record.
         *
         * @param endZoneOffset The end zone offset of the record.
         * @return This builder.
         * @throws IllegalStateException if the builder is for a local date record.
         */
        @NonNull
        public Builder setEndZoneOffset(@NonNull ZoneOffset endZoneOffset) {
            if (mTemporalType == RECORD_TEMPORAL_TYPE_LOCAL_DATE) {
                throw new IllegalStateException(
                        "Zone offset is not supported for local date symptom records.");
            }
            this.mEndZoneOffset = endZoneOffset;
            return this;
        }

        /**
         * @return A {@link SymptomRecord} with the specified parameters.
         */
        @NonNull
        public SymptomRecord build() {
            return build(false);
        }

        /**
         * @return Object of {@link SymptomRecord} without validating the values.
         * @hide
         */
        @NonNull
        public SymptomRecord buildWithoutValidation() {
            return build(true);
        }

        @NonNull
        private SymptomRecord build(boolean skipValidation) {
            return new SymptomRecord(
                    mSymptomType,
                    mNotes,
                    mSeverity,
                    mCount,
                    mTemporalType,
                    mStartTime,
                    mStartZoneOffset,
                    mEndTime,
                    mEndZoneOffset,
                    mMetadata,
                    skipValidation);
        }
    }
}
