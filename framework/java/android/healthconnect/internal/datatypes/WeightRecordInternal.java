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
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.WeightRecord;
import android.healthconnect.datatypes.units.Mass;
import android.os.Parcel;

/**
 * @see WeightRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_WEIGHT)
public final class WeightRecordInternal extends InstantRecordInternal<WeightRecord> {
    private double mWeight;

    public double getWeight() {
        return mWeight;
    }

    /** returns this object with the specified weight */
    @NonNull
    public WeightRecordInternal setWeight(double weight) {
        this.mWeight = weight;
        return this;
    }

    @NonNull
    @Override
    public WeightRecord toExternalRecord() {
        return new WeightRecord.Builder(buildMetaData(), getTime(), Mass.fromKilograms(getWeight()))
                .setZoneOffset(getZoneOffset())
                .build();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mWeight = parcel.readDouble();
    }

    @Override
    void populateInstantRecordFrom(@NonNull WeightRecord weightRecord) {
        mWeight = weightRecord.getWeight().getInKilograms();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mWeight);
    }
}
