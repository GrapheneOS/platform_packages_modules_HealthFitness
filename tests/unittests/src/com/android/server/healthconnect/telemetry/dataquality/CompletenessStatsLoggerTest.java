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
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.health.HealthFitnessStatsLog;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.DataTypeDescriptor;
import android.health.connect.internal.datatypes.utils.DataTypeDescriptors;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.healthfitness.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public class CompletenessStatsLoggerTest {
    private static final String TEST_PACKAGE = "test.package";
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    private CompletenessStatsLogger mCompletenessStatsLogger;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mCompletenessStatsLogger = new CompletenessStatsLogger(mHealthFitnessStatsLog);
    }

    @After
    public void tearDown() {
        Mockito.clearInvocations(mHealthFitnessStatsLog);
    }

    @Test
    @EnableFlags(Flags.FLAG_DATA_COMPLETENESS)
    public void logRecordingMethodStat_flagEnabled_logged() {
        for (DataTypeDescriptor descriptor : DataTypeDescriptors.getAllDataTypeDescriptors()) {
            @RecordTypeIdentifier.RecordType int recordType = descriptor.getRecordTypeIdentifier();
            for (int recordingMethod : Metadata.VALID_TYPES) {
                mCompletenessStatsLogger.logRecordingMethodStat(
                        TEST_PACKAGE, recordingMethod, recordType);

                verify(mHealthFitnessStatsLog)
                        .write(
                                HEALTH_CONNECT_RECORDING_METHOD_STATS,
                                TEST_PACKAGE,
                                recordingMethod,
                                recordType);
            }
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_DATA_COMPLETENESS)
    public void logRecordingMethodStat_flagDisabled_noOp() {
        for (int recordingMethod : Metadata.VALID_TYPES) {
            mCompletenessStatsLogger.logRecordingMethodStat(
                    TEST_PACKAGE, recordingMethod, RECORD_TYPE_STEPS);

            verify(mHealthFitnessStatsLog, never())
                    .write(
                            eq(HEALTH_CONNECT_RECORDING_METHOD_STATS),
                            anyString(),
                            anyInt(),
                            anyInt());
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_DATA_COMPLETENESS)
    public void logDeviceInfoStat_flagEnabled_logged() {
        mCompletenessStatsLogger.logDeviceInfoStat(
                TEST_PACKAGE, RECORD_TYPE_STEPS, true, true, false);
        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DEVICE_INFO_STATS,
                        TEST_PACKAGE,
                        RECORD_TYPE_STEPS,
                        true,
                        true,
                        false);

        mCompletenessStatsLogger.logDeviceInfoStat(
                TEST_PACKAGE, RECORD_TYPE_DISTANCE, true, false, true);
        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DEVICE_INFO_STATS,
                        TEST_PACKAGE,
                        RECORD_TYPE_DISTANCE,
                        true,
                        false,
                        true);

        mCompletenessStatsLogger.logDeviceInfoStat(
                TEST_PACKAGE, RECORD_TYPE_HEART_RATE, false, true, true);
        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_DEVICE_INFO_STATS,
                        TEST_PACKAGE,
                        RECORD_TYPE_HEART_RATE,
                        false,
                        true,
                        true);
    }

    @Test
    @DisableFlags(Flags.FLAG_DATA_COMPLETENESS)
    public void logDeviceInfoStat_flagDisabled_noOp() {
        mCompletenessStatsLogger.logDeviceInfoStat(
                TEST_PACKAGE, RECORD_TYPE_STEPS, true, true, true);

        verify(mHealthFitnessStatsLog, never())
                .write(
                        eq(HEALTH_CONNECT_DEVICE_INFO_STATS),
                        anyString(),
                        anyInt(),
                        anyBoolean(),
                        anyBoolean(),
                        anyBoolean());
    }
}
