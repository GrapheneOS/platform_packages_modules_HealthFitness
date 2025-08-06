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
package com.android.server.healthconnect.telemetry.dataquality;

import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.RecordTypeIdentifier;

import java.util.List;

/**
 * A class to collect Health Connect data completeness stats. Including recording method and device
 * info.
 *
 * @hide
 */
public final class CompletenessStatsCollector {
    public CompletenessStatsCollector() {}

    List<RecordingMethodStat> readRecordingMethodStats() {
        return List.of();
    }

    /**
     * Data class to hold latency i.e. time between session end and time when the session was
     * inserted for every record.
     *
     * @param packageName The package name of the app that inserted the records.
     * @param recordingMethod The {@link android.healthfitness.api.RecordingMethod} used.
     * @param recordTypeId The {@link android.healthfitness.api.DataType} of the records.
     */
    record RecordingMethodStat(
            String packageName,
            @RecordTypeIdentifier.RecordType int recordTypeId,
            @Metadata.RecordingMethod int recordingMethod) {}
}
