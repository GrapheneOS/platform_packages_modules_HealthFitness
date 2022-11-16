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
import android.healthconnect.datatypes.RestingHeartRateRecord;
import android.os.Parcel;

import androidx.annotation.NonNull;

/**
 * @see RestingHeartRateRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE)
public final class RestingHeartRateRecordInternal
        extends InstantRecordInternal<RestingHeartRateRecord> {
    private long mBeatsPerMinute;

    public long getBeatsPerMinute() {
        return mBeatsPerMinute;
    }

    /** returns this object with the specified beatsPerMinute */
    @NonNull
    public RestingHeartRateRecordInternal setBeatsPerMinute(long beatsPerMinute) {
        this.mBeatsPerMinute = beatsPerMinute;
        return this;
    }

    @NonNull
    @Override
    public RestingHeartRateRecord toExternalRecord() {
        return new RestingHeartRateRecord.Builder(buildMetaData(), getTime(), getBeatsPerMinute())
                .setZoneOffset(getZoneOffset())
                .build();
    }

    @Override
    void populateInstantRecordFrom(@NonNull Parcel parcel) {
        mBeatsPerMinute = parcel.readLong();
    }

    @Override
    void populateInstantRecordFrom(@NonNull RestingHeartRateRecord restingHeartRateRecord) {
        mBeatsPerMinute = restingHeartRateRecord.getBeatsPerMinute();
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeLong(mBeatsPerMinute);
    }
}
