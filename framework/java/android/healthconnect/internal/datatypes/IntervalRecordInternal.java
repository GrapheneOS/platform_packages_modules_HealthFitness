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
package android.healthconnect.internal.datatypes;

import static android.healthconnect.internal.datatypes.utils.BundleUtils.requireLong;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_END_TIME;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_END_ZONE_OFFSET;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_START_TIME;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_START_ZONE_OFFSET;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.healthconnect.datatypes.IntervalRecord;
import android.os.Bundle;
import android.os.Parcel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Base class for all health connect datatype records that require a start and end time.
 *
 * @hide
 */
public abstract class IntervalRecordInternal<T extends IntervalRecord> extends RecordInternal<T> {
    private long mStartTime;
    private int mStartZoneOffset;
    private long mEndTime;
    private int mEndZoneOffset;

    IntervalRecordInternal() {
        super();
    }

    public long getStartTimeInMillis() {
        return mStartTime;
    }

    public int getStartZoneOffsetInSeconds() {
        return mStartZoneOffset;
    }

    public long getEndTimeInMillis() {
        return mEndTime;
    }

    public int getEndZoneOffsetInSeconds() {
        return mEndZoneOffset;
    }

    @Override
    void populateRecordTo(@NonNull Parcel parcel) {
        parcel.writeLong(mStartTime);
        parcel.writeInt(mStartZoneOffset);
        parcel.writeLong(mEndTime);
        parcel.writeInt(mEndZoneOffset);

        populateIntervalRecordTo(parcel);
    }

    @Override
    void populateRecordFrom(@NonNull Parcel parcel) {
        mStartTime = parcel.readLong();
        mStartZoneOffset = parcel.readInt();
        mEndTime = parcel.readLong();
        mEndZoneOffset = parcel.readInt();

        populateIntervalRecordFrom(parcel);
    }

    @Override
    void populateRecordFrom(@NonNull T intervalRecord) {
        mStartTime = intervalRecord.getStartTime().toEpochMilli();
        mStartZoneOffset = intervalRecord.getStartZoneOffset().getTotalSeconds();
        mEndTime = intervalRecord.getEndTime().toEpochMilli();
        mEndZoneOffset = intervalRecord.getEndZoneOffset().getTotalSeconds();

        populateIntervalRecordFrom(intervalRecord);
    }

    @Override
    final void populateRecordFrom(@NonNull Bundle payload) {
        requireNonNull(payload);

        mStartTime = requireLong(payload, DM_RECORD_START_TIME);
        mStartZoneOffset = payload.getInt(DM_RECORD_START_ZONE_OFFSET);
        mEndTime = requireLong(payload, DM_RECORD_END_TIME);
        mEndZoneOffset = payload.getInt(DM_RECORD_END_ZONE_OFFSET);

        populateIntervalRecordFrom(payload);
    }

    Instant getStartTime() {
        return Instant.ofEpochMilli(mStartTime);
    }

    @NonNull
    public IntervalRecordInternal<T> setStartTime(long startTime) {
        mStartTime = startTime;
        return this;
    }

    ZoneOffset getStartZoneOffset() {
        return ZoneOffset.ofTotalSeconds(mStartZoneOffset);
    }

    @NonNull
    public IntervalRecordInternal<T> setStartZoneOffset(int startZoneOffset) {
        mStartZoneOffset = startZoneOffset;
        return this;
    }

    Instant getEndTime() {
        return Instant.ofEpochMilli(mEndTime);
    }

    @NonNull
    public IntervalRecordInternal<T> setEndTime(long endTime) {
        mEndTime = endTime;
        return this;
    }

    ZoneOffset getEndZoneOffset() {
        return ZoneOffset.ofTotalSeconds(mEndZoneOffset);
    }

    @NonNull
    public IntervalRecordInternal<T> setEndZoneOffset(int endZoneOffset) {
        mEndZoneOffset = endZoneOffset;
        return this;
    }

    /**
     * @return the {@link LocalDate} object of this activity start time.
     */
    @Override
    @NonNull
    public LocalDate getLocalDate() {
        return LocalDate.ofInstant(this.getStartTime(), this.getStartZoneOffset());
    }

    /**
     * Child class must implement this method and populates itself with the data present in {@param
     * bundle}. Reads should be in the same order as write
     */
    abstract void populateIntervalRecordFrom(@NonNull Parcel parcel);

    /**
     * Child class must implement this method and populates itself with the data present in {@param
     * record}
     */
    abstract void populateIntervalRecordFrom(@NonNull T intervalRecord);

    /**
     * Populate {@param bundle} with the data required to un-bundle self. This is used during IPC
     * transmissions
     */
    abstract void populateIntervalRecordTo(@NonNull Parcel parcel);

    /** Populates the record using the provided data migration payload. */
    void populateIntervalRecordFrom(@NonNull Bundle payload) {
        // TODO(b/263571058): Make abstract when implemented for all records
    }
}
