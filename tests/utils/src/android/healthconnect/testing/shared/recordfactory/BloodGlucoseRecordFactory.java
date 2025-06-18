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

import android.health.connect.datatypes.BloodGlucoseRecord;
import android.health.connect.datatypes.MealType;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.BloodGlucose;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class BloodGlucoseRecordFactory extends RecordFactory<BloodGlucoseRecord> {
    private static final String KEY_LEVEL = PREFIX + "LEVEL";
    private static final String KEY_SPECIMEN_SOURCE = PREFIX + "SPECIMEN_SOURCE";
    private static final String KEY_MEAL_TYPE = PREFIX + "MEAL_TYPE";
    private static final String KEY_RELATION_TO_MEAL = PREFIX + "RELATION_TO_MEAL";

    @Override
    public BloodGlucoseRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new BloodGlucoseRecord.Builder(
                        metadata,
                        time,
                        BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                        BloodGlucose.fromMillimolesPerLiter(5.0),
                        BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_GENERAL,
                        MealType.MEAL_TYPE_BREAKFAST)
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public BloodGlucoseRecord anotherFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new BloodGlucoseRecord.Builder(
                        metadata,
                        time,
                        BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_CAPILLARY_BLOOD,
                        BloodGlucose.fromMillimolesPerLiter(6.0),
                        BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_FASTING,
                        MealType.MEAL_TYPE_DINNER)
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public BloodGlucoseRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new BloodGlucoseRecord.Builder(
                        metadata,
                        time,
                        BloodGlucoseRecord.SpecimenSource.SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                        BloodGlucose.fromMillimolesPerLiter(5.0),
                        BloodGlucoseRecord.RelationToMealType.RELATION_TO_MEAL_GENERAL,
                        MealType.MEAL_TYPE_UNKNOWN)
                .build();
    }

    @Override
    protected BloodGlucoseRecord recordWithMetadata(BloodGlucoseRecord record, Metadata metadata) {
        return new BloodGlucoseRecord.Builder(
                        metadata,
                        record.getTime(),
                        record.getSpecimenSource(),
                        record.getLevel(),
                        record.getRelationToMeal(),
                        record.getMealType())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(BloodGlucoseRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_LEVEL, record.getLevel().getInMillimolesPerLiter());
        values.putInt(KEY_SPECIMEN_SOURCE, record.getSpecimenSource());
        values.putInt(KEY_MEAL_TYPE, record.getMealType());
        values.putInt(KEY_RELATION_TO_MEAL, record.getRelationToMeal());
        return values;
    }

    @Override
    public BloodGlucoseRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new BloodGlucoseRecord.Builder(
                        metadata,
                        time,
                        bundle.getInt(KEY_SPECIMEN_SOURCE),
                        BloodGlucose.fromMillimolesPerLiter(bundle.getDouble(KEY_LEVEL)),
                        bundle.getInt(KEY_RELATION_TO_MEAL),
                        bundle.getInt(KEY_MEAL_TYPE))
                .setZoneOffset(zoneOffset)
                .build();
    }

    @Override
    public String recordToString(BloodGlucoseRecord record) {
        return "BloodGlucoseRecord{"
                + "\n\ttime = "
                + record.getTime()
                + ",\n\tzoneOffset = "
                + record.getZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tlevel = "
                + record.getLevel()
                + ",\n\tspecimenSource = "
                + record.getSpecimenSource()
                + ",\n\tmealType = "
                + record.getMealType()
                + ",\n\trelationToMeal = "
                + record.getRelationToMeal()
                + "\n}";
    }
}
