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
import android.healthconnect.datatypes.PowerRecord;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.units.Power;
import android.os.Parcel;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @see PowerRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_POWER)
public class PowerRecordInternal
        extends SeriesRecordInternal<PowerRecord, PowerRecord.PowerRecordSample> {
    private List<PowerRecordSample> mPowerRecordSamples = new ArrayList<>();

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        int size = parcel.readInt();
        mPowerRecordSamples = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mPowerRecordSamples.add(new PowerRecordSample(parcel.readDouble(), parcel.readLong()));
        }
    }

    @Override
    @NonNull
    public List<PowerRecordSample> getSamples() {
        return mPowerRecordSamples;
    }

    @NonNull
    @Override
    public PowerRecordInternal setSamples(List<? extends Sample> samples) {
        Objects.requireNonNull(samples);
        this.mPowerRecordSamples = (List<PowerRecordSample>) samples;
        return this;
    }

    @Override
    @NonNull
    public PowerRecord toExternalRecord() {
        return new PowerRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getExternalSamples())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .build();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull PowerRecord powerRecord) {
        mPowerRecordSamples = new ArrayList<>(powerRecord.getSamples().size());
        for (PowerRecord.PowerRecordSample powerRecordSample : powerRecord.getSamples()) {
            mPowerRecordSamples.add(
                    new PowerRecordSample(
                            powerRecordSample.getPower().getInWatts(),
                            powerRecordSample.getTime().toEpochMilli()));
        }
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mPowerRecordSamples.size());
        for (PowerRecordSample powerRecordSample : mPowerRecordSamples) {
            parcel.writeDouble(powerRecordSample.getPower());
            parcel.writeLong(powerRecordSample.getEpochMillis());
        }
    }

    private List<PowerRecord.PowerRecordSample> getExternalSamples() {
        List<PowerRecord.PowerRecordSample> powerRecords =
                new ArrayList<>(mPowerRecordSamples.size());
        for (PowerRecordSample powerRecordSample : mPowerRecordSamples) {
            powerRecords.add(
                    new PowerRecord.PowerRecordSample(
                            Power.fromWatts(powerRecordSample.getPower()),
                            Instant.ofEpochMilli(powerRecordSample.getEpochMillis())));
        }
        return powerRecords;
    }

    /**
     * @see android.healthconnect.datatypes.PowerRecord.PowerRecordSample
     */
    public static final class PowerRecordSample implements Sample {
        private final double mPower;
        private final long mEpochMillis;

        public PowerRecordSample(double power, long epochMillis) {
            mPower = power;
            mEpochMillis = epochMillis;
        }

        public double getPower() {
            return mPower;
        }

        public long getEpochMillis() {
            return mEpochMillis;
        }
    }
}
