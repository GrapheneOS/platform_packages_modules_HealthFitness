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
import android.healthconnect.datatypes.CervicalMucusRecord;
import android.healthconnect.datatypes.CervicalMucusRecord.CervicalMucusAppearance;
import android.healthconnect.datatypes.CervicalMucusRecord.CervicalMucusSensation;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

/**
 * @see CervicalMucusRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS)
public final class CervicalMucusRecordInternal extends InstantRecordInternal<CervicalMucusRecord> {
    private int mSensation;
    private int mAppearance;

    @CervicalMucusSensation.CervicalMucusSensations
    public int getSensation() {
        return mSensation;
    }

    /** returns this object with the specified sensation */
    @NonNull
    public CervicalMucusRecordInternal setSensation(int sensation) {
        this.mSensation = sensation;
        return this;
    }

    @CervicalMucusAppearance.CervicalMucusAppearances
    public int getAppearance() {
        return mAppearance;
    }

    /** returns this object with the specified appearance */
    @NonNull
    public CervicalMucusRecordInternal setAppearance(int appearance) {
        this.mAppearance = appearance;
        return this;
    }

    @NonNull
    @Override
    public CervicalMucusRecord toExternalRecord() {
        return new CervicalMucusRecord.Builder(
                        buildMetaData(), getTime(), getSensation(), getAppearance())
                .setZoneOffset(getZoneOffset())
                .build();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mSensation = parcel.readInt();
        mAppearance = parcel.readInt();
    }

    @Override
    void populateInstantRecordFrom(@NonNull CervicalMucusRecord cervicalMucusRecord) {
        mSensation = cervicalMucusRecord.getSensation();
        mAppearance = cervicalMucusRecord.getAppearance();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mSensation);
        parcel.writeInt(mAppearance);
    }
}
