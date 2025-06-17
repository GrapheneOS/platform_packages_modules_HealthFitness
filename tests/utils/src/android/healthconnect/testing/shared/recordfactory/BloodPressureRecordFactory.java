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

import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Pressure;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class BloodPressureRecordFactory extends RecordFactory<BloodPressureRecord> {
    private static final String KEY_SYSTOLIC = PREFIX + "SYSTOLIC";
    private static final String KEY_DIASTOLIC = PREFIX + "DIASTOLIC";
    private static final String KEY_BODY_POSITION = PREFIX + "BODY_POSITION";
    private static final String KEY_MEASUREMENT_LOCATION = PREFIX + "MEASUREMENT_LOCATION";

    @Override
    public BloodPressureRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new BloodPressureRecord.Builder(
                        metadata,
                        time,
                        BloodPressureRecord.BloodPressureMeasurementLocation
                                .BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST,
                        Pressure.fromMillimetersOfMercury(120.0),
                        Pressure.fromMillimetersOfMercury(80.0),
                        BloodPressureRecord.BodyPosition.BODY_POSITION_SITTING_DOWN)
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public BloodPressureRecord anotherFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new BloodPressureRecord.Builder(
                        metadata,
                        time,
                        BloodPressureRecord.BloodPressureMeasurementLocation
                                .BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST,
                        Pressure.fromMillimetersOfMercury(125.0),
                        Pressure.fromMillimetersOfMercury(85.0),
                        BloodPressureRecord.BodyPosition.BODY_POSITION_STANDING_UP)
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public BloodPressureRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new BloodPressureRecord.Builder(
                        metadata,
                        time,
                        BloodPressureRecord.BloodPressureMeasurementLocation
                                .BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST,
                        Pressure.fromMillimetersOfMercury(120.0),
                        Pressure.fromMillimetersOfMercury(80.0),
                        BloodPressureRecord.BodyPosition.BODY_POSITION_SITTING_DOWN)
                .build();
    }

    @Override
    protected BloodPressureRecord recordWithMetadata(
            BloodPressureRecord record, Metadata metadata) {
        return new BloodPressureRecord.Builder(
                        metadata,
                        record.getTime(),
                        record.getMeasurementLocation(),
                        record.getSystolic(),
                        record.getDiastolic(),
                        record.getBodyPosition())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(BloodPressureRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_SYSTOLIC, record.getSystolic().getInMillimetersOfMercury());
        values.putDouble(KEY_DIASTOLIC, record.getDiastolic().getInMillimetersOfMercury());
        values.putInt(KEY_BODY_POSITION, record.getBodyPosition());
        values.putInt(KEY_MEASUREMENT_LOCATION, record.getMeasurementLocation());
        return values;
    }

    @Override
    public BloodPressureRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new BloodPressureRecord.Builder(
                        metadata,
                        time,
                        bundle.getInt(KEY_MEASUREMENT_LOCATION),
                        Pressure.fromMillimetersOfMercury(bundle.getDouble(KEY_SYSTOLIC)),
                        Pressure.fromMillimetersOfMercury(bundle.getDouble(KEY_DIASTOLIC)),
                        bundle.getInt(KEY_BODY_POSITION))
                .setZoneOffset(zoneOffset)
                .build();
    }
}
