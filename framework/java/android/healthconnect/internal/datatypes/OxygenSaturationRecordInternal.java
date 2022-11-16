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
import android.healthconnect.datatypes.OxygenSaturationRecord;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.units.Percentage;
import android.os.Parcel;

import androidx.annotation.NonNull;

/**
 * @see OxygenSaturationRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION)
public final class OxygenSaturationRecordInternal
        extends InstantRecordInternal<OxygenSaturationRecord> {
    private double mPercentage;

    public double getPercentage() {
        return mPercentage;
    }

    /** returns this object with the specified percentage */
    @NonNull
    public OxygenSaturationRecordInternal setPercentage(double percentage) {
        this.mPercentage = percentage;
        return this;
    }

    @NonNull
    @Override
    public OxygenSaturationRecord toExternalRecord() {
        return new OxygenSaturationRecord.Builder(
                        buildMetaData(), getTime(), Percentage.fromValue(getPercentage()))
                .setZoneOffset(getZoneOffset())
                .build();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mPercentage = parcel.readDouble();
    }

    @Override
    void populateInstantRecordFrom(@NonNull OxygenSaturationRecord oxygenSaturationRecord) {
        mPercentage = oxygenSaturationRecord.getPercentage().getValue();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mPercentage);
    }
}
