/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.healthconnect.logging;

import android.health.HealthFitnessStatsLog;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;

import java.util.Map;
import java.util.Set;

/**
 * Class to log Health Connect Ecosystem metrics every 24hrs.
 *
 * @hide
 */
class EcosystemStatsLogger {

    private final HealthFitnessStatsLog mStatsLog;

    EcosystemStatsLogger(HealthFitnessStatsLog statsLog) {
        mStatsLog = statsLog;
    }

    /** Write Health Connect Ecosystem stats to statsd. */
    void log(EcosystemStatsCollector ecosystemStatsCollector) {
        if (!AconfigFlagHelper.isEcosystemMetricsEnabled()) {
            return;
        }
        ecosystemStatsCollector.processReadAccessLogs();

        logDirectionalAppPairings(ecosystemStatsCollector);
        logDirectionalAppPairingsPerDataType(ecosystemStatsCollector);

        mStatsLog.write(
                HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS,
                convertRecordTypeIdSetToLoggableArray(
                        ecosystemStatsCollector.getDataTypesReadOrWritten()),
                convertRecordTypeIdSetToLoggableArray(ecosystemStatsCollector.getDataTypesRead()),
                convertRecordTypeIdSetToLoggableArray(
                        ecosystemStatsCollector.getDataTypesWritten()),
                convertRecordTypeIdSetToLoggableArray(ecosystemStatsCollector.getDataTypeShared()),
                ecosystemStatsCollector.getNumberOfAppPairings());
    }

    private void logDirectionalAppPairings(EcosystemStatsCollector ecosystemStatsCollector) {
        Map<String, Set<String>> directionalAppPairings =
                ecosystemStatsCollector.getDirectionalAppPairings();

        for (Map.Entry<String, Set<String>> directionalAppPairing :
                directionalAppPairings.entrySet()) {
            String writerPackageName = directionalAppPairing.getKey();
            for (String readerPackageName : directionalAppPairing.getValue()) {
                mStatsLog.write(
                        HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS,
                        writerPackageName,
                        readerPackageName,
                        HealthFitnessStatsLog
                                .HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__DATA_TYPE_UNKNOWN,
                        HealthFitnessStatsLog
                                .HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING);
            }
        }
    }

    private void logDirectionalAppPairingsPerDataType(
            EcosystemStatsCollector ecosystemStatsCollector) {
        Map<String, Map<Integer, Set<String>>> directionalAppPairingsForDataTypes =
                ecosystemStatsCollector.getDirectionalAppPairingsPerDataType();
        InternalHealthConnectMappings internalHealthConnectMappings =
                InternalHealthConnectMappings.getInstance();

        for (Map.Entry<String, Map<Integer, Set<String>>> directionalAppPairingsPerDataType :
                directionalAppPairingsForDataTypes.entrySet()) {
            String writerPackageName = directionalAppPairingsPerDataType.getKey();
            for (Map.Entry<Integer, Set<String>> dataTypeToReaderPackageName :
                    directionalAppPairingsPerDataType.getValue().entrySet()) {
                int dataType =
                        internalHealthConnectMappings.getLoggingEnumForRecordTypeId(
                                dataTypeToReaderPackageName.getKey());
                for (String readerPackageName : dataTypeToReaderPackageName.getValue()) {
                    mStatsLog.write(
                            HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS,
                            writerPackageName,
                            readerPackageName,
                            dataType,
                            HealthFitnessStatsLog
                                    .HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING_PER_DATA_TYPE);
                }
            }
        }
    }

    private static int[] convertRecordTypeIdSetToLoggableArray(Set<Integer> recordTypeIds) {
        InternalHealthConnectMappings internalHealthConnectMappings =
                InternalHealthConnectMappings.getInstance();
        int[] loggableRecordTypeIds = new int[recordTypeIds.size()];

        int i = 0;
        for (Integer recordTyeId : recordTypeIds) {
            loggableRecordTypeIds[i++] =
                    internalHealthConnectMappings.getLoggingEnumForRecordTypeId(recordTyeId);
        }
        return loggableRecordTypeIds;
    }
}
