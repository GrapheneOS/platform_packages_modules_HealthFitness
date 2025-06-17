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

import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Power;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class BasalMetabolicRateRecordFactory extends RecordFactory<BasalMetabolicRateRecord> {
    private static final String KEY_BASAL_METABOLIC_RATE = PREFIX + "BASAL_METABOLIC_RATE";

    @Override
    public BasalMetabolicRateRecord newFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new BasalMetabolicRateRecord.Builder(metadata, time, Power.fromWatts(1500.0))
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public BasalMetabolicRateRecord anotherFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new BasalMetabolicRateRecord.Builder(metadata, time, Power.fromWatts(1600.0))
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public BasalMetabolicRateRecord newEmptyRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new BasalMetabolicRateRecord.Builder(metadata, time, Power.fromWatts(1500.0))
                .build();
    }

    @Override
    protected BasalMetabolicRateRecord recordWithMetadata(
            BasalMetabolicRateRecord record, Metadata metadata) {
        return new BasalMetabolicRateRecord.Builder(
                        metadata, record.getTime(), record.getBasalMetabolicRate())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(BasalMetabolicRateRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_BASAL_METABOLIC_RATE, record.getBasalMetabolicRate().getInWatts());
        return values;
    }

    @Override
    public BasalMetabolicRateRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new BasalMetabolicRateRecord.Builder(
                        metadata, time, Power.fromWatts(bundle.getDouble(KEY_BASAL_METABOLIC_RATE)))
                .setZoneOffset(zoneOffset)
                .build();
    }
}
