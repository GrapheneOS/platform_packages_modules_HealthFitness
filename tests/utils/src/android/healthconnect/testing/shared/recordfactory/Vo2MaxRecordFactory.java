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
import android.health.connect.datatypes.Vo2MaxRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class Vo2MaxRecordFactory extends RecordFactory<Vo2MaxRecord> {
    private static final String KEY_VO2_MAX = PREFIX + "VO2_MAX";
    private static final String KEY_MEASUREMENT_METHOD = PREFIX + "MEASUREMENT_METHOD";

    @Override
    public Vo2MaxRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new Vo2MaxRecord.Builder(
                        metadata,
                        time,
                        Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_METABOLIC_CART,
                        45.0)
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public Vo2MaxRecord anotherFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new Vo2MaxRecord.Builder(
                        metadata,
                        time,
                        Vo2MaxRecord.Vo2MaxMeasurementMethod
                                .MEASUREMENT_METHOD_ROCKPORT_FITNESS_TEST,
                        50.0)
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public Vo2MaxRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new Vo2MaxRecord.Builder(
                        metadata,
                        time,
                        Vo2MaxRecord.Vo2MaxMeasurementMethod.MEASUREMENT_METHOD_METABOLIC_CART,
                        45.0)
                .build();
    }

    @Override
    protected Vo2MaxRecord recordWithMetadata(Vo2MaxRecord record, Metadata metadata) {
        return new Vo2MaxRecord.Builder(
                        metadata,
                        record.getTime(),
                        record.getMeasurementMethod(),
                        record.getVo2MillilitersPerMinuteKilogram())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(Vo2MaxRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_VO2_MAX, record.getVo2MillilitersPerMinuteKilogram());
        values.putInt(KEY_MEASUREMENT_METHOD, record.getMeasurementMethod());
        return values;
    }

    @Override
    public Vo2MaxRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new Vo2MaxRecord.Builder(
                        metadata,
                        time,
                        bundle.getInt(KEY_MEASUREMENT_METHOD),
                        bundle.getDouble(KEY_VO2_MAX))
                .setZoneOffset(zoneOffset)
                .build();
    }

    @Override
    public String recordToString(Vo2MaxRecord record) {
        return "Vo2MaxRecord{"
                + "\n\ttime = "
                + record.getTime()
                + ",\n\tzoneOffset = "
                + record.getZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tvo2MillilitersPerMinuteKilogram = "
                + record.getVo2MillilitersPerMinuteKilogram()
                + ",\n\tmeasurementMethod = "
                + record.getMeasurementMethod()
                + "\n}";
    }
}
