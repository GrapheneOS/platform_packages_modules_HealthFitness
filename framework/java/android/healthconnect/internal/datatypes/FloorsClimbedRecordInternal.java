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
import android.healthconnect.datatypes.FloorsClimbedRecord;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

/**
 * @see FloorsClimbedRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED)
public final class FloorsClimbedRecordInternal extends IntervalRecordInternal<FloorsClimbedRecord> {
    private int mFloors;

    public int getFloors() {
        return mFloors;
    }

    /** returns this object with the specified floors */
    @NonNull
    public FloorsClimbedRecordInternal setFloors(int floors) {
        this.mFloors = floors;
        return this;
    }

    @NonNull
    @Override
    public FloorsClimbedRecord toExternalRecord() {
        return new FloorsClimbedRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getFloors())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .build();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mFloors = parcel.readInt();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull FloorsClimbedRecord floorsClimbedRecord) {
        mFloors = floorsClimbedRecord.getFloors();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mFloors);
    }
}
