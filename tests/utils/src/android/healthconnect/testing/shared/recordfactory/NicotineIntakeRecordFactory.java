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

package android.healthconnect.testing.shared.recordfactory;

import static android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_CIGARETTE;
import static android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_VAPE;

import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.NicotineIntakeRecord;
import android.health.connect.datatypes.units.Mass;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

public final class NicotineIntakeRecordFactory extends RecordFactory<NicotineIntakeRecord> {

    private static final String KEY_NICOTINE_INTAKE_TYPE = PREFIX + "NICOTINE_INTAKE_TYPE";
    private static final String KEY_QUANTITY = PREFIX + "QUANTITY";
    private static final String KEY_NICOTINE_INTAKE = PREFIX + "NICOTINE_INTAKE";

    @Override
    public NicotineIntakeRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new NicotineIntakeRecord.Builder(
                        metadata, startTime, endTime, /* quantity= */ 50, NICOTINE_INTAKE_TYPE_VAPE)
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .setNicotineIntake(Mass.fromGrams(0.005))
                .build();
    }

    @Override
    public NicotineIntakeRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new NicotineIntakeRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        /* quantity= */ 5,
                        NICOTINE_INTAKE_TYPE_CIGARETTE)
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .setNicotineIntake(Mass.fromGrams(0.12))
                .build();
    }

    @Override
    public NicotineIntakeRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new NicotineIntakeRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        /* quantity= */ 3,
                        NICOTINE_INTAKE_TYPE_CIGARETTE)
                .build();
    }

    @Override
    protected NicotineIntakeRecord recordWithMetadata(
            NicotineIntakeRecord record, Metadata metadata) {
        return new NicotineIntakeRecord.Builder(
                        metadata,
                        record.getStartTime(),
                        record.getEndTime(),
                        record.getQuantity(),
                        record.getNicotineIntakeType())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .setNicotineIntake(record.getNicotineIntake())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(NicotineIntakeRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_NICOTINE_INTAKE_TYPE, record.getNicotineIntakeType());
        values.putInt(KEY_QUANTITY, record.getQuantity());

        if (record.getNicotineIntake() != null) {
            values.putDouble(KEY_NICOTINE_INTAKE, record.getNicotineIntake().getInGrams());
        }
        return values;
    }

    @Override
    public NicotineIntakeRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        int nicotineIntakeType = bundle.getInt(KEY_NICOTINE_INTAKE_TYPE);
        int quantity = bundle.getInt(KEY_QUANTITY);

        NicotineIntakeRecord.Builder record =
                new NicotineIntakeRecord.Builder(
                                metadata, startTime, endTime, quantity, nicotineIntakeType)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset);
        if (bundle.containsKey(KEY_NICOTINE_INTAKE)) {
            record.setNicotineIntake(Mass.fromGrams(bundle.getDouble(KEY_NICOTINE_INTAKE)));
        }
        return record.build();
    }
}
