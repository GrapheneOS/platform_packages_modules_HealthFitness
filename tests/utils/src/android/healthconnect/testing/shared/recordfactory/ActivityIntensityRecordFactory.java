/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE;
import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS;

import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.Metadata;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;

public final class ActivityIntensityRecordFactory extends RecordFactory<ActivityIntensityRecord> {

    private static final String KEY_TYPE = PREFIX + "TYPE";

    @Override
    public ActivityIntensityRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ActivityIntensityRecord.Builder(
                        metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_VIGOROUS)
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public ActivityIntensityRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ActivityIntensityRecord.Builder(
                        metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public ActivityIntensityRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ActivityIntensityRecord.Builder(
                        metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                .build();
    }

    @Override
    protected ActivityIntensityRecord recordWithMetadata(
            ActivityIntensityRecord record, Metadata metadata) {
        return new ActivityIntensityRecord.Builder(
                        metadata,
                        record.getStartTime(),
                        record.getEndTime(),
                        record.getActivityIntensityType())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(ActivityIntensityRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_TYPE, record.getActivityIntensityType());
        return values;
    }

    @Override
    public ActivityIntensityRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        int type = bundle.getInt(KEY_TYPE);
        return new ActivityIntensityRecord.Builder(metadata, startTime, endTime, type)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }

    /** Creates a new {@link ActivityIntensityRecord} with empty metadata. */
    public ActivityIntensityRecord newRecord(Instant startTime, Instant endTime, int type) {
        return newRecord(startTime, endTime, type, null, null);
    }

    /** Creates a new {@link ActivityIntensityRecord} with empty metadata. */
    public ActivityIntensityRecord newRecord(
            Instant startTime,
            Instant endTime,
            int type,
            @Nullable ZoneOffset startZoneOffset,
            @Nullable ZoneOffset endZoneOffset) {
        var builder =
                new ActivityIntensityRecord.Builder(newEmptyMetadata(), startTime, endTime, type);

        if (startZoneOffset != null) {
            builder.setStartZoneOffset(startZoneOffset);
        }

        if (endZoneOffset != null) {
            builder.setEndZoneOffset(endZoneOffset);
        }

        return builder.build();
    }
}
