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
import android.healthconnect.datatypes.RespiratoryRateRecord;
import android.os.Parcel;

import androidx.annotation.NonNull;

/**
 * @see RespiratoryRateRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE)
public final class RespiratoryRateRecordInternal
        extends InstantRecordInternal<RespiratoryRateRecord> {
    private double mRate;

    public double getRate() {
        return mRate;
    }

    /** returns this object with the specified rate */
    @NonNull
    public RespiratoryRateRecordInternal setRate(double rate) {
        this.mRate = rate;
        return this;
    }

    @NonNull
    @Override
    public RespiratoryRateRecord toExternalRecord() {
        return new RespiratoryRateRecord.Builder(buildMetaData(), getTime(), getRate())
                .setZoneOffset(getZoneOffset())
                .build();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mRate = parcel.readDouble();
    }

    @Override
    void populateInstantRecordFrom(@NonNull RespiratoryRateRecord respiratoryRateRecord) {
        mRate = respiratoryRateRecord.getRate();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mRate);
    }
}
