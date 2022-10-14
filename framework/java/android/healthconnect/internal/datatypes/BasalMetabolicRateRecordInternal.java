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

import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.units.Power;
import android.os.Parcel;

import androidx.annotation.NonNull;

/** @hide */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE)
public final class BasalMetabolicRateRecordInternal
        extends InstantRecordInternal<BasalMetabolicRateRecord> {
    private double mBasalMetabolicRate = 0.0;

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mBasalMetabolicRate = parcel.readDouble();
    }

    @Override
    void populateInstantRecordFrom(@NonNull BasalMetabolicRateRecord basalMetabolicRateRecord) {
        mBasalMetabolicRate = basalMetabolicRateRecord.getBasalMetabolicRate().getInWatts();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mBasalMetabolicRate);
    }

    public double getBasalMetabolicRate() {
        return mBasalMetabolicRate;
    }

    public void setBasalMetabolicRate(double basalMetabolicRate) {
        mBasalMetabolicRate = basalMetabolicRate;
    }

    @NonNull
    @Override
    public BasalMetabolicRateRecord toExternalRecord() {
        return new BasalMetabolicRateRecord.Builder(
                        buildMetaData(), getTime(), Power.fromWatts(getBasalMetabolicRate()))
                .setZoneOffset(getZoneOffset())
                .build();
    }
}
