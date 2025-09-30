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

import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_OTHER;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_SERVING_SIZE_GLASS;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;

import android.health.connect.datatypes.AlcoholConsumptionRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Percentage;
import android.health.connect.datatypes.units.Volume;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

public final class AlcoholConsumptionRecordFactory extends RecordFactory<AlcoholConsumptionRecord> {

    private static final String KEY_SERVING_COUNT = PREFIX + "SERVING_COUNT";
    private static final String KEY_SERVING_SIZE = PREFIX + "SERVING_SIZE";
    private static final String KEY_TYPE = PREFIX + "TYPE";
    private static final String KEY_ALCOHOL_BY_VOLUME = PREFIX + "ALCOHOL_BY_VOLUME";
    private static final String KEY_SERVING_VOLUME = PREFIX + "SERVING_VOLUME";
    private static final String KEY_NOTE = PREFIX + "NOTE";

    @Override
    public AlcoholConsumptionRecord newFullRecord(
            Metadata metadata, Instant time, Instant endTime) {

        return new AlcoholConsumptionRecord.Builder(
                        metadata,
                        time, /* servingCount */
                        3,
                        ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER)
                .setServingSize(ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT)
                .setServingVolume(Volume.fromLiters(330.0 / 1000))
                .setAlcoholByVolume(Percentage.fromValue(6.7))
                .setNote("Pub crawl")
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(-1))
                .build();
    }

    @Override
    public AlcoholConsumptionRecord anotherFullRecord(
            Metadata metadata, Instant time, Instant endTime) {

        return new AlcoholConsumptionRecord.Builder(
                        metadata,
                        time, /* servingCount */
                        2,
                        ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE)
                .setServingSize(ALCOHOL_CONSUMPTION_SERVING_SIZE_GLASS)
                .setServingVolume(Volume.fromLiters(250.0 / 1000))
                .setAlcoholByVolume(Percentage.fromValue(15.2))
                .setNote("Celebration")
                .setStartZoneOffset(ZoneOffset.ofHours(1))
                .setEndZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public AlcoholConsumptionRecord newEmptyRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new AlcoholConsumptionRecord.Builder(
                        metadata,
                        time, /* servingCount */
                        1,
                        ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_OTHER)
                .build();
    }

    @Override
    protected AlcoholConsumptionRecord recordWithMetadata(
            AlcoholConsumptionRecord record, Metadata metadata) {
        return new AlcoholConsumptionRecord.Builder(
                        metadata,
                        record.getStartTime(),
                        record.getEndTime(),
                        record.getServingCount(),
                        record.getBeverageType())
                .setServingSize(record.getServingSize())
                .setServingVolume(record.getServingVolume())
                .setAlcoholByVolume(record.getAlcoholByVolume())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .setNote(record.getNote())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(AlcoholConsumptionRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_SERVING_COUNT, record.getServingCount());
        values.putInt(KEY_TYPE, record.getBeverageType());
        values.putInt(KEY_SERVING_SIZE, record.getServingSize());
        if (record.getServingVolume() != null) {
            values.putDouble(KEY_SERVING_VOLUME, record.getServingVolume().getInLiters());
        }
        if (record.getAlcoholByVolume() != null) {
            values.putDouble(KEY_ALCOHOL_BY_VOLUME, record.getAlcoholByVolume().getValue());
        }
        if (record.getNote() != null) {
            values.putString(KEY_NOTE, record.getNote().toString());
        }
        return values;
    }

    @Override
    public AlcoholConsumptionRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        AlcoholConsumptionRecord.Builder record =
                new AlcoholConsumptionRecord.Builder(
                                metadata,
                                time,
                                bundle.getInt(KEY_SERVING_COUNT),
                                bundle.getInt(KEY_TYPE))
                        .setServingSize(bundle.getInt(KEY_SERVING_SIZE))
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(endZoneOffset);

        if (bundle.containsKey(KEY_SERVING_VOLUME)) {
            record.setServingVolume(Volume.fromLiters(bundle.getDouble(KEY_SERVING_VOLUME)));
        }
        if (bundle.containsKey(KEY_ALCOHOL_BY_VOLUME)) {
            record.setAlcoholByVolume(
                    Percentage.fromValue(bundle.getDouble(KEY_ALCOHOL_BY_VOLUME)));
        }
        if (bundle.containsKey(KEY_NOTE)) {
            record.setNote(bundle.getString(KEY_NOTE));
        }

        return record.build();
    }

    @Override
    public String recordToString(AlcoholConsumptionRecord record) {
        return "AlcoholConsumptionRecord{"
                + "\n\tstartTime = "
                + record.getStartTime()
                + ",\n\tendTime = "
                + record.getEndTime()
                + ",\n\tstartZoneOffset = "
                + record.getStartZoneOffset()
                + ",\n\tendZoneOffset = "
                + record.getEndZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tservingCount = "
                + record.getServingCount()
                + ",\n\tbeverageType = "
                + record.getBeverageType()
                + ",\n\tservingSize = "
                + record.getServingSize()
                + ",\n\tservingVolume = "
                + record.getServingVolume()
                + ",\n\talcoholByVolume = "
                + record.getAlcoholByVolume()
                + ",\n\tnote = "
                + record.getNote()
                + "\n}";
    }
}
