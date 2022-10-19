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
import android.healthconnect.datatypes.StepsRecord;
import android.os.Parcel;

/** @hide */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_STEPS)
public final class StepsRecordInternal extends IntervalRecordInternal<StepsRecord> {
    private long mCount = INT_DEFAULT;

    public long getCount() {
        return mCount;
    }

    @NonNull
    public StepsRecordInternal setCount(long count) {
        this.mCount = count;
        return this;
    }

    @NonNull
    @Override
    public StepsRecord toExternalRecord() {
        return new StepsRecord.Builder(buildMetaData(), getStartTime(), getEndTime(), getCount())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .build();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mCount = parcel.readLong();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull StepsRecord stepsRecord) {
        mCount = stepsRecord.getCount();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeLong(mCount);
    }
}
