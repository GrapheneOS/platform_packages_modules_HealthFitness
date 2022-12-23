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

import android.annotation.NonNull;
import android.healthconnect.datatypes.CyclingPedalingCadenceRecord;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @see CyclingPedalingCadenceRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE)
public class CyclingPedalingCadenceRecordInternal
        extends SeriesRecordInternal<
                CyclingPedalingCadenceRecord,
                CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample> {
    private List<CyclingPedalingCadenceRecordSample> mCyclingPedalingCadenceRecordSamples =
            new ArrayList<>();

    @Override
    void populateIntervalRecordFrom(
            @NonNull CyclingPedalingCadenceRecord cyclingPedalingCadenceRecord) {
        mCyclingPedalingCadenceRecordSamples =
                new ArrayList<>(cyclingPedalingCadenceRecord.getSamples().size());
        for (CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
                cyclingPedalingCadenceRecordSample : cyclingPedalingCadenceRecord.getSamples()) {
            mCyclingPedalingCadenceRecordSamples.add(
                    new CyclingPedalingCadenceRecordSample(
                            cyclingPedalingCadenceRecordSample.getRevolutionsPerMinute(),
                            cyclingPedalingCadenceRecordSample.getTime().toEpochMilli()));
        }
    }

    @Override
    @NonNull
    public List<CyclingPedalingCadenceRecordSample> getSamples() {
        return mCyclingPedalingCadenceRecordSamples;
    }

    @NonNull
    @Override
    public CyclingPedalingCadenceRecordInternal setSamples(List<? extends Sample> samples) {
        Objects.requireNonNull(samples);
        this.mCyclingPedalingCadenceRecordSamples =
                (List<CyclingPedalingCadenceRecordSample>) samples;
        return this;
    }

    @Override
    @NonNull
    public CyclingPedalingCadenceRecord toExternalRecord() {
        return new CyclingPedalingCadenceRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getExternalSamples())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .build();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        int size = parcel.readInt();
        mCyclingPedalingCadenceRecordSamples = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mCyclingPedalingCadenceRecordSamples.add(
                    new CyclingPedalingCadenceRecordSample(parcel.readDouble(), parcel.readLong()));
        }
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mCyclingPedalingCadenceRecordSamples.size());
        for (CyclingPedalingCadenceRecordSample cyclingPedalingCadenceRecordSample :
                mCyclingPedalingCadenceRecordSamples) {
            parcel.writeDouble(cyclingPedalingCadenceRecordSample.getRevolutionsPerMinute());
            parcel.writeLong(cyclingPedalingCadenceRecordSample.getEpochMillis());
        }
    }

    private List<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
            getExternalSamples() {
        List<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
                cyclingPedalingCadenceRecords =
                        new ArrayList<>(mCyclingPedalingCadenceRecordSamples.size());
        for (CyclingPedalingCadenceRecordSample cyclingPedalingCadenceRecordSample :
                mCyclingPedalingCadenceRecordSamples) {
            cyclingPedalingCadenceRecords.add(
                    new CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample(
                            cyclingPedalingCadenceRecordSample.getRevolutionsPerMinute(),
                            Instant.ofEpochMilli(
                                    cyclingPedalingCadenceRecordSample.getEpochMillis())));
        }
        return cyclingPedalingCadenceRecords;
    }

    /**
     * @see CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
     */
    public static final class CyclingPedalingCadenceRecordSample implements Sample {
        private final double mRevolutionsPerMinute;
        private final long mEpochMillis;

        public CyclingPedalingCadenceRecordSample(double revolutionsPerMinute, long epochMillis) {
            mRevolutionsPerMinute = revolutionsPerMinute;
            mEpochMillis = epochMillis;
        }

        public double getRevolutionsPerMinute() {
            return mRevolutionsPerMinute;
        }

        public long getEpochMillis() {
            return mEpochMillis;
        }
    }
}
