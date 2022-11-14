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
import android.healthconnect.datatypes.SwimmingStrokesRecord;
import android.os.Parcel;

/**
 * @see SwimmingStrokesRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_SWIMMING_STROKES)
public final class SwimmingStrokesRecordInternal
        extends IntervalRecordInternal<SwimmingStrokesRecord> {
    private long mCount;
    private int mType;

    public long getCount() {
        return mCount;
    }

    /** returns this object with the specified count */
    @NonNull
    public SwimmingStrokesRecordInternal setCount(long count) {
        this.mCount = count;
        return this;
    }

    @SwimmingStrokesRecord.SwimmingStrokesType.SwimmingStrokesTypes
    public int getType() {
        return mType;
    }

    /** returns this object with the specified type */
    @NonNull
    public SwimmingStrokesRecordInternal setType(
            @SwimmingStrokesRecord.SwimmingStrokesType.SwimmingStrokesTypes int type) {
        this.mType = type;
        return this;
    }

    @NonNull
    @Override
    public SwimmingStrokesRecord toExternalRecord() {
        return new SwimmingStrokesRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getCount(), getType())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .build();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mCount = parcel.readLong();
        mType = parcel.readInt();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull SwimmingStrokesRecord swimmingStrokesRecord) {
        mCount = swimmingStrokesRecord.getCount();
        mType = swimmingStrokesRecord.getType();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeLong(mCount);
        parcel.writeInt(mType);
    }
}
