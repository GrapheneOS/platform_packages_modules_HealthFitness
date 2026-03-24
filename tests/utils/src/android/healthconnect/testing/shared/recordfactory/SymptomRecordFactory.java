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

import static java.time.Instant.now;

import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.SymptomRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Utility class to create {@link SymptomRecord} for testing. */
public final class SymptomRecordFactory extends RecordFactory<SymptomRecord> {
    private static final String KEY_SYMPTOM_TYPE = "symptom_type";
    private static final String KEY_NOTES = "notes";
    private static final String KEY_SEVERITY = "severity";
    private static final String KEY_COUNT = "count";
    private static final String KEY_TEMPORAL_TYPE = "temporal_type";

    @Override
    public SymptomRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new SymptomRecord.Builder(
                        SymptomRecord.SYMPTOM_TYPE_COUGH, startTime, endTime, metadata)
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .setNotes("Persistent cough")
                .setSeverity(SymptomRecord.SEVERITY_MODERATE)
                .setCount(5)
                .build();
    }

    @Override
    public SymptomRecord anotherFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new SymptomRecord.Builder(
                        SymptomRecord.SYMPTOM_TYPE_COUGH, startTime, endTime, metadata)
                .setStartZoneOffset(ZoneOffset.ofHours(-2))
                .setEndZoneOffset(ZoneOffset.ofHours(-1))
                .setNotes("Persistent cough updated note")
                .setSeverity(SymptomRecord.SEVERITY_SEVERE)
                .setCount(1)
                .build();
    }

    /** Creates a new SymptomRecord with an instant temporal type and given symptom. */
    public static SymptomRecord newInstantRecord(int symptomType) {
        return newInstantRecord(newEmptyMetadata(), symptomType);
    }

    /** Creates a new SymptomRecord with an instant temporal type, given metadata and symptom. */
    public static SymptomRecord newInstantRecord(Metadata metadata, int symptomType) {
        return newInstantRecord(metadata, /* time= */ now(), symptomType);
    }

    /** Creates a new SymptomRecord with an instant temporal type, given time and symptom. */
    public static SymptomRecord newInstantRecord(Metadata metadata, Instant time, int symptomType) {
        return new SymptomRecord.Builder(symptomType, time, metadata)
                .setNotes("Sudden dizziness")
                .setSeverity(SymptomRecord.SEVERITY_MILD)
                .build();
    }

    /** Creates a new SymptomRecord with a local date temporal type. */
    public SymptomRecord newLocalDateRecord(Metadata metadata, LocalDate date) {
        return new SymptomRecord.Builder(SymptomRecord.SYMPTOM_TYPE_FEVER, date, metadata)
                .setNotes("Fever all day")
                .setSeverity(SymptomRecord.SEVERITY_MODERATE)
                .setCount(3)
                .build();
    }

    @Override
    public SymptomRecord newEmptyRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new SymptomRecord.Builder(
                        SymptomRecord.SYMPTOM_TYPE_COUGH, startTime, endTime, metadata)
                .build();
    }

    @Override
    public SymptomRecord recordWithMetadata(SymptomRecord record, Metadata metadata) {
        SymptomRecord.Builder builder;
        switch (record.getTemporalType()) {
            case SymptomRecord.RECORD_TEMPORAL_TYPE_INSTANT:
                builder =
                        new SymptomRecord.Builder(
                                record.getSymptomType(), record.getStartTime(), metadata);
                break;
            case SymptomRecord.RECORD_TEMPORAL_TYPE_LOCAL_DATE:
                builder =
                        new SymptomRecord.Builder(
                                record.getSymptomType(), record.getDate(), metadata);
                builder.setCount(record.getCount());
                break;
            case SymptomRecord.RECORD_TEMPORAL_TYPE_INTERVAL:
            default:
                builder =
                        new SymptomRecord.Builder(
                                record.getSymptomType(),
                                record.getStartTime(),
                                record.getEndTime(),
                                metadata);
                builder.setStartZoneOffset(record.getStartZoneOffset());
                builder.setEndZoneOffset(record.getEndZoneOffset());
                builder.setCount(record.getCount());
                break;
        }
        return builder.setNotes(record.getNotes()).setSeverity(record.getSeverity()).build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(SymptomRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_SYMPTOM_TYPE, record.getSymptomType());
        values.putString(KEY_NOTES, record.getNotes());
        values.putInt(KEY_SEVERITY, record.getSeverity());
        values.putInt(KEY_COUNT, record.getCount());
        values.putInt(KEY_TEMPORAL_TYPE, record.getTemporalType());
        return values;
    }

    @Override
    public SymptomRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        int symptomType = bundle.getInt(KEY_SYMPTOM_TYPE);
        String notes = bundle.getString(KEY_NOTES);
        int severity = bundle.getInt(KEY_SEVERITY);
        int count = bundle.getInt(KEY_COUNT);
        int temporalType = bundle.getInt(KEY_TEMPORAL_TYPE);

        SymptomRecord.Builder builder;
        switch (temporalType) {
            case SymptomRecord.RECORD_TEMPORAL_TYPE_INSTANT:
                builder = new SymptomRecord.Builder(symptomType, startTime, metadata);
                break;
            case SymptomRecord.RECORD_TEMPORAL_TYPE_LOCAL_DATE:
                builder =
                        new SymptomRecord.Builder(
                                        symptomType,
                                        startTime.atZone(startZoneOffset).toLocalDate(),
                                        metadata)
                                .setCount(count);
                break;
            case SymptomRecord.RECORD_TEMPORAL_TYPE_INTERVAL:
            default:
                builder = new SymptomRecord.Builder(symptomType, startTime, endTime, metadata);
                builder.setStartZoneOffset(startZoneOffset);
                builder.setEndZoneOffset(endZoneOffset);
                builder.setCount(count);
                break;
        }
        return builder.setNotes(notes).setSeverity(severity).build();
    }

    @Override
    public String recordToString(SymptomRecord record) {
        return "SymptomRecord{"
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
                + ",\n\tsymptomType = "
                + record.getSymptomType()
                + ",\n\tnotes = "
                + record.getNotes()
                + ",\n\tseverity = "
                + record.getSeverity()
                + ",\n\tcount = "
                + record.getCount()
                + ",\n\ttemporalType = "
                + record.getTemporalType()
                + "\n}";
    }
}
