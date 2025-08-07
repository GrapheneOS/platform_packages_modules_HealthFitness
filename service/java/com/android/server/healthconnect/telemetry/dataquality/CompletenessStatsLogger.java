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

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DEVICE_INFO_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RECORDING_METHOD_STATS;

import static com.android.healthfitness.flags.Flags.dataCompleteness;

import android.health.HealthFitnessStatsLog;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.RecordTypeIdentifier;

import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;

import java.util.List;
import java.util.Set;

/**
 * Logs Health Connect data completeness stats. Including recording method and device info.
 *
 * @hide
 */
public final class CompletenessStatsLogger {
    private static final InternalHealthConnectMappings HEALTH_CONNECT_MAPPINGS =
            InternalHealthConnectMappings.getInstance();

    private final HealthFitnessStatsLog mHealthFitnessStatsLog;

    public CompletenessStatsLogger(HealthFitnessStatsLog healthFitnessStatsLog) {
        mHealthFitnessStatsLog = healthFitnessStatsLog;
    }

    void logRecordingMethodStats(List<CompletenessStatsCollector.RecordingMethodStat> stats) {
        if (!dataCompleteness()) {
            return;
        }
        for (CompletenessStatsCollector.RecordingMethodStat stat : stats) {
            logRecordingMethodStat(stat.packageName(), stat.recordTypeId(), stat.recordingMethod());
        }
    }

    void logDeviceInfoStats(Set<CompletenessStatsCollector.DeviceInfoStat> stats) {
        if (!dataCompleteness()) {
            return;
        }
        for (CompletenessStatsCollector.DeviceInfoStat stat : stats) {
            logDeviceInfoStat(
                    stat.packageName(),
                    stat.recordTypeId(),
                    stat.hasManufacturer(),
                    stat.hasModel(),
                    stat.hasType());
        }
    }

    private void logRecordingMethodStat(
            String packageName,
            @RecordTypeIdentifier.RecordType int recordTypeId,
            @Metadata.RecordingMethod int recordingMethod) {
        mHealthFitnessStatsLog.write(
                HEALTH_CONNECT_RECORDING_METHOD_STATS,
                packageName,
                HEALTH_CONNECT_MAPPINGS.getLoggingEnumForRecordTypeId(recordTypeId),
                recordingMethod);
    }

    private void logDeviceInfoStat(
            String packageName,
            @RecordTypeIdentifier.RecordType int recordTypeId,
            boolean hasManufacturer,
            boolean hasModel,
            boolean hasType) {
        mHealthFitnessStatsLog.write(
                HEALTH_CONNECT_DEVICE_INFO_STATS,
                packageName,
                HEALTH_CONNECT_MAPPINGS.getLoggingEnumForRecordTypeId(recordTypeId),
                hasManufacturer,
                hasModel,
                hasType);
    }
}
