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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.internal.datatypes.utils.HealthConnectMappings;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.proto.backuprestore.BloodPressure;
import com.android.server.healthconnect.proto.backuprestore.InstantRecord;
import com.android.server.healthconnect.proto.backuprestore.IntervalRecord;
import com.android.server.healthconnect.proto.backuprestore.Record;
import com.android.server.healthconnect.proto.backuprestore.Steps;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public final class RecordProtoConverterTest {

    private final RecordProtoConverter mConverter = new RecordProtoConverter();
    private final HealthConnectMappings mHealthConnectMappings =
            HealthConnectMappings.getInstance();

    @Test
    public void canConvertEveryRecordType() throws Exception {
        for (int recordTypeId : mHealthConnectMappings.getAllRecordTypeIdentifiers()) {
            var recordProto =
                    com.android.server.healthconnect.backuprestore.ProtoTestData.generateRecord(
                            recordTypeId);
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
