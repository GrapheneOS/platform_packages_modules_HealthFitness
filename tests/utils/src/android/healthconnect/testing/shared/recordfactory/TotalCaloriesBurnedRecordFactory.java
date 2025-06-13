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

import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.units.Energy;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class TotalCaloriesBurnedRecordFactory
        extends RecordFactory<TotalCaloriesBurnedRecord> {
    private static final String KEY_ENERGY = PREFIX + "ENERGY";

    @Override
    public TotalCaloriesBurnedRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new TotalCaloriesBurnedRecord.Builder(
                        metadata, startTime, endTime, Energy.fromCalories(100.0))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public TotalCaloriesBurnedRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new TotalCaloriesBurnedRecord.Builder(
                        metadata, startTime, endTime, Energy.fromCalories(200.0))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public TotalCaloriesBurnedRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new TotalCaloriesBurnedRecord.Builder(
                        metadata, startTime, endTime, Energy.fromCalories(100.0))
                .build();
    }

    @Override
    protected TotalCaloriesBurnedRecord recordWithMetadata(
            TotalCaloriesBurnedRecord record, Metadata metadata) {
        return new TotalCaloriesBurnedRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime(), record.getEnergy())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(TotalCaloriesBurnedRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_ENERGY, record.getEnergy().getInCalories());
        return values;
    }

    @Override
    public TotalCaloriesBurnedRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new TotalCaloriesBurnedRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Energy.fromCalories(bundle.getDouble(KEY_ENERGY, 100.0)))
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
