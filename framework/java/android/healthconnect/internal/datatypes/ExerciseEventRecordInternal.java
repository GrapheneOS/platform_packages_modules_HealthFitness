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
import android.healthconnect.datatypes.ExerciseEventRecord;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

/**
 * @see ExerciseEventRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_EXERCISE_EVENT)
public final class ExerciseEventRecordInternal extends IntervalRecordInternal<ExerciseEventRecord> {
    private int mEventType;

    @ExerciseEventRecord.ExerciseEventType.ExerciseEventTypes
    public int getEventType() {
        return mEventType;
    }

    /** returns this object with the specified eventType */
    @NonNull
    public ExerciseEventRecordInternal setEventType(int eventType) {
        this.mEventType = eventType;
        return this;
    }

    @NonNull
    @Override
    public ExerciseEventRecord toExternalRecord() {
        return new ExerciseEventRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getEventType())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .build();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mEventType = parcel.readInt();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull ExerciseEventRecord exerciseEventRecord) {
        mEventType = exerciseEventRecord.getEventType();
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mEventType);
    }
}
