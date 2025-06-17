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

import android.health.connect.datatypes.BasalBodyTemperatureRecord;
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Temperature;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class BasalBodyTemperatureRecordFactory
        extends RecordFactory<BasalBodyTemperatureRecord> {
    private static final String KEY_TEMPERATURE = PREFIX + "TEMPERATURE";
    private static final String KEY_MEASUREMENT_LOCATION = PREFIX + "MEASUREMENT_LOCATION";

    @Override
    public BasalBodyTemperatureRecord newFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new BasalBodyTemperatureRecord.Builder(
                        metadata,
                        time,
                        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN,
                        Temperature.fromCelsius(37.0))
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public BasalBodyTemperatureRecord anotherFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new BasalBodyTemperatureRecord.Builder(
                        metadata,
                        time,
                        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_ARMPIT,
                        Temperature.fromCelsius(38.0))
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public BasalBodyTemperatureRecord newEmptyRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new BasalBodyTemperatureRecord.Builder(
                        metadata,
                        time,
                        BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_UNKNOWN,
                        Temperature.fromCelsius(37.0))
                .build();
    }

    @Override
    protected BasalBodyTemperatureRecord recordWithMetadata(
            BasalBodyTemperatureRecord record, Metadata metadata) {
        return new BasalBodyTemperatureRecord.Builder(
                        metadata,
                        record.getTime(),
                        record.getMeasurementLocation(),
                        record.getTemperature())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(BasalBodyTemperatureRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_TEMPERATURE, record.getTemperature().getInCelsius());
        values.putInt(KEY_MEASUREMENT_LOCATION, record.getMeasurementLocation());
        return values;
    }

    @Override
    public BasalBodyTemperatureRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new BasalBodyTemperatureRecord.Builder(
                        metadata,
                        time,
                        bundle.getInt(KEY_MEASUREMENT_LOCATION),
                        Temperature.fromCelsius(bundle.getDouble(KEY_TEMPERATURE)))
                .setZoneOffset(zoneOffset)
                .build();
    }
}
