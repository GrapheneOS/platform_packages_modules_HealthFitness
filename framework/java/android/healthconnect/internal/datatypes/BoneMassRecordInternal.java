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
import android.healthconnect.datatypes.BoneMassRecord;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.units.Mass;
import android.os.Parcel;

/**
 * @see BoneMassRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BONE_MASS)
public final class BoneMassRecordInternal extends InstantRecordInternal<BoneMassRecord> {
    private double mMass;

    public double getMass() {
        return mMass;
    }

    /** returns this object with the specified mass */
    @NonNull
    public BoneMassRecordInternal setMass(double mass) {
        this.mMass = mass;
        return this;
    }

    @NonNull
    @Override
    public BoneMassRecord toExternalRecord() {
        return new BoneMassRecord.Builder(buildMetaData(), getTime(), Mass.fromKilograms(getMass()))
                .setZoneOffset(getZoneOffset())
                .build();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mMass = parcel.readDouble();
    }

    @Override
    void populateInstantRecordFrom(@NonNull BoneMassRecord boneMassRecord) {
        mMass = boneMassRecord.getMass().getInKilograms();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mMass);
    }
}
