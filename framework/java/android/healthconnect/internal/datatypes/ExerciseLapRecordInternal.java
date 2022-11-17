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
import android.healthconnect.datatypes.ExerciseLapRecord;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.units.Length;
import android.os.Parcel;

/**
 * @see ExerciseLapRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_EXERCISE_LAP)
public final class ExerciseLapRecordInternal extends IntervalRecordInternal<ExerciseLapRecord> {
    private double mLength;

    public double getLength() {
        return mLength;
    }

    /** returns this object with the specified length */
    @NonNull
    public ExerciseLapRecordInternal setLength(double length) {
        this.mLength = length;
        return this;
    }

    @NonNull
    @Override
    public ExerciseLapRecord toExternalRecord() {
        return new ExerciseLapRecord.Builder(buildMetaData(), getStartTime(), getEndTime())
                .setLength(Length.fromMeters(getLength()))
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .build();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mLength = parcel.readDouble();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull ExerciseLapRecord exerciseLapRecord) {
        if (exerciseLapRecord.getLength() != null) {
            mLength = exerciseLapRecord.getLength().getInMeters();
        }
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeDouble(mLength);
    }
}
