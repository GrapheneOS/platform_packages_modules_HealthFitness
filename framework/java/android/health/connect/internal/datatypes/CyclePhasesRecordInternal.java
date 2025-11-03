/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.health.connect.internal.datatypes;

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.datatypes.CyclePhasesRecord.PHASE_UNKNOWN;

import static com.android.healthfitness.flags.Flags.FLAG_CYCLE_PHASES_FLAG;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.health.connect.datatypes.CyclePhasesRecord;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

/**
 * @see android.health.connect.datatypes.CyclePhasesRecord
 * @hide
 */
@FlaggedApi(FLAG_CYCLE_PHASES_FLAG)
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_CYCLE_PHASES)
public final class CyclePhasesRecordInternal extends InstantRecordInternal<CyclePhasesRecord> {
    @CyclePhasesRecord.CyclePhase private int mPhase = PHASE_UNKNOWN;
    private int mDayOfCycle = DEFAULT_INT;

    public CyclePhasesRecordInternal() {
        super();
    }

    public CyclePhasesRecordInternal(Parcel parcel) {
        super(parcel);
        mPhase = parcel.readInt();
        mDayOfCycle = parcel.readInt();
    }

    @CyclePhasesRecord.CyclePhase
    public int getPhase() {
        return mPhase;
    }

    public int getDayOfCycle() {
        return mDayOfCycle;
    }

    /** returns this object with the specified {@code phase} */
    public CyclePhasesRecordInternal setPhase(@CyclePhasesRecord.CyclePhase int phase) {
        mPhase = phase;
        return this;
    }

    /** returns this object with the specified {@code dayOfCycle} */
    public CyclePhasesRecordInternal setDayOfCycle(int dayOfCycle) {
        mDayOfCycle = dayOfCycle;
        return this;
    }

    @Override
    void populateInstantRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mPhase);
        parcel.writeInt(mDayOfCycle);
    }

    @Override
    public CyclePhasesRecord toExternalRecord() {
        return new CyclePhasesRecord.Builder(buildMetaData(), getTime(), mPhase)
                .setDayOfCycle(mDayOfCycle)
                .setZoneOffset(getZoneOffset())
                .buildWithoutValidation();
    }
}
