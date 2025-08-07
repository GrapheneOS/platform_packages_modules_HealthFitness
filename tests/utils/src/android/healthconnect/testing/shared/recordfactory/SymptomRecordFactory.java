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

import static android.health.connect.datatypes.SymptomRecord.SYMPTOM_TYPE_COUGH;

import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.SymptomRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

public class SymptomRecordFactory extends RecordFactory<SymptomRecord> {
    private static final String NOTES_KEY = PREFIX + ".notes";
    private static final String SEVERITY_KEY = PREFIX + ".severity";
    private static final String COUNT_KEY = PREFIX + ".count";
    private static final String TEMPORAL_TYPE_KEY = PREFIX + ".temporal_type";

    @Override
    public SymptomRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, startTime, endTime, metadata)
                .setNotes("notes")
                .setSeverity(SymptomRecord.SEVERITY_MILD)
                .setCount(2)
                .build();
    }

    @Override
    public SymptomRecord anotherFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, startTime, endTime, metadata)
                .setNotes("another_notes")
                .setSeverity(SymptomRecord.SEVERITY_MODERATE)
                .setCount(3)
                .build();
    }

    @Override
    public SymptomRecord newEmptyRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, startTime, endTime, metadata).build();
    }

    @Override
    protected SymptomRecord recordWithMetadata(SymptomRecord record, Metadata metadata) {
        return new SymptomRecord.Builder(
                        record.getSymptomType(),
                        record.getStartTime(),
                        record.getEndTime(),
                        metadata)
                .setNotes(record.getNotes())
                .setSeverity(record.getSeverity())
                .setCount(record.getCount())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(SymptomRecord record) {
        Bundle bundle = new Bundle();
        bundle.putString(NOTES_KEY, record.getNotes());
        bundle.putInt(SEVERITY_KEY, record.getSeverity());
        bundle.putInt(COUNT_KEY, record.getCount());
        bundle.putInt(TEMPORAL_TYPE_KEY, record.getTemporalType());
        return bundle;
    }

    @Override
    public SymptomRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new SymptomRecord.Builder(SYMPTOM_TYPE_COUGH, startTime, endTime, metadata)
                .setNotes(bundle.getString(NOTES_KEY))
                .setSeverity(bundle.getInt(SEVERITY_KEY))
                .setCount(bundle.getInt(COUNT_KEY))
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }

    @Override
    public String recordToString(SymptomRecord record) {
        return "SymptomRecord: " + record.getSymptomType();
    }
}
