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

package com.android.server.healthconnect.backuprestore;

import static android.health.connect.datatypes.BloodPressureRecord.BloodPressureMeasurementLocation.BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST;
import static android.health.connect.datatypes.BloodPressureRecord.BodyPosition.BODY_POSITION_LYING_DOWN;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED;

import static com.android.server.healthconnect.backuprestore.ProtoTestData.generateRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import android.annotation.SuppressLint;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BloodPressure;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.InstantRecord;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.IntervalRecord;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Record;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Steps;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public final class RecordProtoConverterTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private RecordProtoConverter mConverter;
    private HealthConnectMappings mHealthConnectMappings;

    @Before
    @SuppressLint("VisibleForTests") // this is indeed a test file
    public void setup() {
        HealthConnectMappings.resetInstanceForTesting();
        mConverter = new RecordProtoConverter();
        mHealthConnectMappings = HealthConnectMappings.getInstance();
    }

    @Test
    @RequiresFlagsEnabled({
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB
    })
    @EnableFlags({Flags.FLAG_CYCLE_PHASES_FLAG, Flags.FLAG_CYCLE_PHASES_DB})
    public void canConvertEveryRecordType() throws Exception {
        assumeTrue(AconfigFlagHelper.isCyclePhasesEnabled());
        for (int recordTypeId : mHealthConnectMappings.getAllRecordTypeIdentifiers()) {
            var recordProto = generateRecord(recordTypeId);
            var recordInternal = mConverter.toRecordInternal(recordProto);
            assertThat(mConverter.toRecordProto(recordInternal)).isEqualTo(recordProto);
        }
    }

    @Test
    public void convertSetValues_intervalRecord() throws Exception {
        var intervalRecord =
                IntervalRecord.newBuilder()
                        .setStartTime(12345)
                        .setStartZoneOffset(3600)
                        .setEndTime(54321)
                        .setEndZoneOffset(3600)
                        .setSteps(Steps.newBuilder().setCount(123))
                        .build();
        var recordProto =
                Record.newBuilder()
                        .setUuid(UUID.randomUUID().toString())
                        .setPackageName("packageName")
                        .setLastModifiedTime(123456)
                        .setClientRecordId("clientId")
                        .setClientRecordVersion(3)
                        .setManufacturer("manufacturer")
                        .setModel("model")
                        .setDeviceType(DEVICE_TYPE_PHONE)
                        .setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED)
                        .setIntervalRecord(intervalRecord)
                        .build();

        var recordInternal = mConverter.toRecordInternal(recordProto);

        assertThat(mConverter.toRecordProto(recordInternal)).isEqualTo(recordProto);
    }

    @Test
    public void convertSetValues_instantRecord() throws Exception {
        var instantRecord =
                InstantRecord.newBuilder()
                        .setTime(12345)
                        .setZoneOffset(3600)
                        .setBloodPressure(
                                BloodPressure.newBuilder()
                                        .setMeasurementLocation(
                                                BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST)
                                        .setSystolic(120)
                                        .setDiastolic(80)
                                        .setBodyPosition(BODY_POSITION_LYING_DOWN))
                        .build();
        var recordProto =
                Record.newBuilder()
                        .setUuid(UUID.randomUUID().toString())
                        .setPackageName("packageName")
                        .setLastModifiedTime(123456)
                        .setClientRecordId("clientId")
                        .setClientRecordVersion(3)
                        .setManufacturer("manufacturer")
                        .setModel("model")
                        .setDeviceType(DEVICE_TYPE_PHONE)
                        .setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED)
                        .setInstantRecord(instantRecord)
                        .build();

        var recordInternal = mConverter.toRecordInternal(recordProto);

        assertThat(mConverter.toRecordProto(recordInternal)).isEqualTo(recordProto);
    }

    @Test
    public void convertDefaultValues_intervalRecord() throws Exception {
        var intervalRecord =
                IntervalRecord.newBuilder()
                        .setStartTime(12345)
                        .setStartZoneOffset(3600)
                        .setEndTime(54321)
                        .setEndZoneOffset(3600)
                        .setSteps(Steps.newBuilder().setCount(123))
                        .build();
        var recordProto = Record.newBuilder().setIntervalRecord(intervalRecord).build();

        var recordInternal = mConverter.toRecordInternal(recordProto);

        assertThat(mConverter.toRecordProto(recordInternal)).isEqualTo(recordProto);
    }

    @Test
    public void convertDefaultValues_instantRecord() throws Exception {
        var instantRecord =
                InstantRecord.newBuilder()
                        .setTime(12345)
                        .setZoneOffset(3600)
                        .setBloodPressure(
                                BloodPressure.newBuilder()
                                        .setMeasurementLocation(
                                                BLOOD_PRESSURE_MEASUREMENT_LOCATION_RIGHT_WRIST)
                                        .setSystolic(120)
                                        .setDiastolic(80)
                                        .setBodyPosition(BODY_POSITION_LYING_DOWN))
                        .build();
        var recordProto = Record.newBuilder().setInstantRecord(instantRecord).build();

        var recordInternal = mConverter.toRecordInternal(recordProto);

        assertThat(mConverter.toRecordProto(recordInternal)).isEqualTo(recordProto);
    }

    @Test
    public void noSubRecordType_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mConverter.toRecordInternal(Record.getDefaultInstance()));
    }

    @Test
    public void instantRecordWithNoRecordType_throwsIllegalArgumentException() {
        var invalidInstantRecord = InstantRecord.getDefaultInstance();
        var recordProto = Record.newBuilder().setInstantRecord(invalidInstantRecord).build();
        assertThrows(
                IllegalArgumentException.class, () -> mConverter.toRecordInternal(recordProto));
    }

    @Test
    public void intervalRecordWithNoRecordType_throwsIllegalArgumentException() {
        var invalidIntervalRecord = IntervalRecord.getDefaultInstance();
        var recordProto = Record.newBuilder().setIntervalRecord(invalidIntervalRecord).build();
        assertThrows(
                IllegalArgumentException.class, () -> mConverter.toRecordInternal(recordProto));
    }
}
