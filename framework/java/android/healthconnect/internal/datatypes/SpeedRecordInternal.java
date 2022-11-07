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

import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.SpeedRecord;
import android.healthconnect.datatypes.units.Velocity;
import android.os.Parcel;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @see SpeedRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SPEED)
public class SpeedRecordInternal
        extends SeriesRecordInternal<SpeedRecord, SpeedRecord.SpeedRecordSample> {
    private List<SpeedRecordSample> mSpeedRecordSamples = new ArrayList<>();

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        int size = parcel.readInt();
        mSpeedRecordSamples = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mSpeedRecordSamples.add(new SpeedRecordSample(parcel.readDouble(), parcel.readLong()));
        }
    }

    @Override
    @NonNull
    public List<SpeedRecordSample> getSamples() {
        return mSpeedRecordSamples;
    }

    @NonNull
    @Override
    public SpeedRecordInternal setSamples(List<? extends Sample> samples) {
        Objects.requireNonNull(samples);
        this.mSpeedRecordSamples = (List<SpeedRecordSample>) samples;
        return this;
    }

    @Override
    @NonNull
    public SpeedRecord toExternalRecord() {
        return new SpeedRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getExternalSamples())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .build();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull SpeedRecord speedRecord) {
        mSpeedRecordSamples = new ArrayList<>(speedRecord.getSamples().size());
        for (SpeedRecord.SpeedRecordSample speedRecordSample : speedRecord.getSamples()) {
            mSpeedRecordSamples.add(
                    new SpeedRecordSample(
                            speedRecordSample.getSpeed().getInMetersPerSecond(),
                            speedRecordSample.getTime().toEpochMilli()));
        }
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mSpeedRecordSamples.size());
        for (SpeedRecordSample speedRecordSample : mSpeedRecordSamples) {
            parcel.writeDouble(speedRecordSample.getSpeed());
            parcel.writeLong(speedRecordSample.getEpochMillis());
        }
    }

    private List<SpeedRecord.SpeedRecordSample> getExternalSamples() {
        List<SpeedRecord.SpeedRecordSample> speedRecords =
                new ArrayList<>(mSpeedRecordSamples.size());
        for (SpeedRecordSample speedRecordSample : mSpeedRecordSamples) {
            speedRecords.add(
                    new SpeedRecord.SpeedRecordSample(
                            Velocity.fromMetersPerSecond(speedRecordSample.getSpeed()),
                            Instant.ofEpochMilli(speedRecordSample.getEpochMillis())));
        }
        return speedRecords;
    }

    /**
     * @see android.healthconnect.datatypes.SpeedRecord.SpeedRecordSample
     */
    public static final class SpeedRecordSample implements Sample {
        private final double mSpeed;
        private final long mEpochMillis;

        public SpeedRecordSample(double speed, long epochMillis) {
            mSpeed = speed;
            mEpochMillis = epochMillis;
        }

        public double getSpeed() {
            return mSpeed;
        }

        public long getEpochMillis() {
            return mEpochMillis;
        }
    }
}
