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
package android.healthconnect.internal.datatypes;

import static android.health.connect.Constants.DEFAULT_DOUBLE;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_UNKNOWN;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_UNKNOWN;
import static android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_VAPE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NICOTINE_INTAKE;

import static com.android.healthfitness.flags.Flags.FLAG_HEALTH_CONNECT_MAPPINGS;
import static com.android.healthfitness.flags.Flags.FLAG_SMOKING;
import static com.android.healthfitness.flags.Flags.FLAG_SMOKING_DB;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.NicotineIntakeRecord;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.internal.datatypes.NicotineIntakeRecordInternal;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Parcel;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({FLAG_SMOKING, FLAG_SMOKING_DB, FLAG_HEALTH_CONNECT_MAPPINGS})
public class NicotineIntakeRecordInternalTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() {
        HealthConnectMappings.resetInstanceForTesting();
    }

    @Test
    public void toExternalRecord_allFieldsSet() {
        UUID uuid = UUID.randomUUID();

        NicotineIntakeRecordInternal internalRecord =
                (NicotineIntakeRecordInternal)
                        new NicotineIntakeRecordInternal()
                                .setNicotineIntakeGrams(0.01)
                                .setQuantity(20)
                                .setNicotineIntakeType(NICOTINE_INTAKE_TYPE_VAPE)
                                .setStartTime(1357924680)
                                .setEndTime(2468013579L)
                                .setStartZoneOffset(-2 * 3600)
                                .setEndZoneOffset(3 * 3600)
                                .setAppInfoId(123)
                                .setAppName("app.name")
                                .setClientRecordId("client-record-id")
                                .setClientRecordVersion(567)
                                .setDeviceInfoId(8)
                                .setDeviceType(DEVICE_TYPE_WATCH)
                                .setLastModifiedTime(9012345)
                                .setManufacturer("manufacturer")
                                .setModel("model")
                                .setPackageName("package.name")
                                .setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED)
                                .setRowId(468)
                                .setUuid(uuid);

        NicotineIntakeRecord externalRecord = internalRecord.toExternalRecord();

        Metadata metadata = externalRecord.getMetadata();
        assertThat(externalRecord.getRecordType()).isEqualTo(RECORD_TYPE_NICOTINE_INTAKE);
        assertThat(externalRecord.getNicotineIntake()).isEqualTo(Mass.fromGrams(0.01));
        assertThat(externalRecord.getNicotineIntakeType()).isEqualTo(NICOTINE_INTAKE_TYPE_VAPE);
        assertThat(externalRecord.getQuantity()).isEqualTo(20);
        assertThat(externalRecord.getStartTime()).isEqualTo(Instant.ofEpochMilli(1357924680));
        assertThat(externalRecord.getEndTime()).isEqualTo(Instant.ofEpochMilli(2468013579L));
        assertThat(externalRecord.getStartZoneOffset()).isEqualTo(ZoneOffset.ofHours(-2));
        assertThat(externalRecord.getEndZoneOffset()).isEqualTo(ZoneOffset.ofHours(3));
        assertThat(metadata.getClientRecordId()).isEqualTo("client-record-id");
        assertThat(metadata.getClientRecordVersion()).isEqualTo(567);
        assertThat(metadata.getDataOrigin().getPackageName()).isEqualTo("package.name");
        assertThat(metadata.getDevice().getType()).isEqualTo(DEVICE_TYPE_WATCH);
        assertThat(metadata.getDevice().getModel()).isEqualTo("model");
        assertThat(metadata.getDevice().getManufacturer()).isEqualTo("manufacturer");
        assertThat(metadata.getId()).isEqualTo(uuid.toString());
        assertThat(metadata.getLastModifiedTime()).isEqualTo(Instant.ofEpochMilli(9012345));
        assertThat(metadata.getRecordingMethod())
                .isEqualTo(RECORDING_METHOD_AUTOMATICALLY_RECORDED);
    }

    @Test
    public void toExternalRecord_optionalFieldsNotSet() {
        UUID uuid = UUID.randomUUID();

        NicotineIntakeRecordInternal internalRecord =
                (NicotineIntakeRecordInternal)
                        new NicotineIntakeRecordInternal()
                                .setQuantity(20)
                                .setNicotineIntakeType(NICOTINE_INTAKE_TYPE_VAPE)
                                .setPackageName("package.name")
                                .setUuid(uuid);

        NicotineIntakeRecord externalRecord = internalRecord.toExternalRecord();

        Metadata metadata = externalRecord.getMetadata();

        assertThat(externalRecord.getRecordType()).isEqualTo(RECORD_TYPE_NICOTINE_INTAKE);
        assertThat(externalRecord.getNicotineIntake()).isEqualTo(null);
        assertThat(externalRecord.getQuantity()).isEqualTo(20);
        assertThat(externalRecord.getNicotineIntakeType()).isEqualTo(NICOTINE_INTAKE_TYPE_VAPE);
        assertThat(externalRecord.getStartTime()).isEqualTo(Instant.EPOCH);
        assertThat(externalRecord.getEndTime()).isEqualTo(Instant.EPOCH);
        assertThat(externalRecord.getStartZoneOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(externalRecord.getEndZoneOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(metadata.getClientRecordId()).isNull();
        assertThat(metadata.getClientRecordVersion()).isEqualTo(-1);
        assertThat(metadata.getDataOrigin().getPackageName()).isEqualTo("package.name");
        assertThat(metadata.getDevice().getType()).isEqualTo(DEVICE_TYPE_UNKNOWN);
        assertThat(metadata.getDevice().getModel()).isNull();
        assertThat(metadata.getDevice().getManufacturer()).isNull();
        assertThat(metadata.getId()).isEqualTo(uuid.toString());
        assertThat(metadata.getLastModifiedTime()).isEqualTo(Instant.EPOCH.minusMillis(1));
        assertThat(metadata.getRecordingMethod()).isEqualTo(RECORDING_METHOD_UNKNOWN);
    }

    @Test
    public void writeToParcel_populateUsing_allFieldsSet() {
        UUID uuid = UUID.randomUUID();

        NicotineIntakeRecordInternal internalRecord =
                (NicotineIntakeRecordInternal)
                        new NicotineIntakeRecordInternal()
                                .setNicotineIntakeGrams(0.01)
                                .setQuantity(20)
                                .setNicotineIntakeType(NICOTINE_INTAKE_TYPE_VAPE)
                                .setStartTime(1357924680)
                                .setEndTime(2468013579L)
                                .setStartZoneOffset(-2 * 3600)
                                .setEndZoneOffset(3 * 3600)
                                .setAppInfoId(123)
                                .setAppName("app.name")
                                .setClientRecordId("client-record-id")
                                .setClientRecordVersion(567)
                                .setDeviceInfoId(8)
                                .setDeviceType(DEVICE_TYPE_WATCH)
                                .setLastModifiedTime(9012345)
                                .setManufacturer("manufacturer")
                                .setModel("model")
                                .setPackageName("package.name")
                                .setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED)
                                .setRowId(468)
                                .setUuid(uuid);

        Parcel parcel = Parcel.obtain();
        internalRecord.writeToParcel(parcel);
        parcel.setDataPosition(0);
        NicotineIntakeRecordInternal decodedRecord = new NicotineIntakeRecordInternal();
        decodedRecord.populateUsing(parcel);
        parcel.recycle();

        assertThat(decodedRecord.getRecordType()).isEqualTo(RECORD_TYPE_NICOTINE_INTAKE);
        assertThat(decodedRecord.getNicotineIntakeGrams()).isEqualTo(0.01);
        assertThat(decodedRecord.getQuantity()).isEqualTo(20);
        assertThat(decodedRecord.getNicotineIntakeType()).isEqualTo(NICOTINE_INTAKE_TYPE_VAPE);
        assertThat(decodedRecord.getStartTimeInMillis()).isEqualTo(1357924680);
        assertThat(decodedRecord.getEndTimeInMillis()).isEqualTo(2468013579L);
        assertThat(decodedRecord.getStartZoneOffsetInSeconds()).isEqualTo(-2 * 3600);
        assertThat(decodedRecord.getEndZoneOffsetInSeconds()).isEqualTo(3 * 3600);
        assertThat(decodedRecord.getAppInfoId()).isEqualTo(-1);
        assertThat(decodedRecord.getAppName()).isEqualTo("app.name");
        assertThat(decodedRecord.getClientRecordId()).isEqualTo("client-record-id");
        assertThat(decodedRecord.getClientRecordVersion()).isEqualTo(567);
        assertThat(decodedRecord.getDeviceInfoId()).isEqualTo(-1);
        assertThat(decodedRecord.getDeviceType()).isEqualTo(DEVICE_TYPE_WATCH);
        assertThat(decodedRecord.getLastModifiedTime()).isEqualTo(9012345);
        assertThat(decodedRecord.getManufacturer()).isEqualTo("manufacturer");
        assertThat(decodedRecord.getModel()).isEqualTo("model");
        assertThat(decodedRecord.getPackageName()).isEqualTo("package.name");
        assertThat(decodedRecord.getRecordingMethod())
                .isEqualTo(RECORDING_METHOD_AUTOMATICALLY_RECORDED);
        assertThat(decodedRecord.getRowId()).isEqualTo(-1);
        assertThat(decodedRecord.getUuid()).isEqualTo(uuid);
    }

    @Test
    public void writeToParcel_populateUsing_optionalFieldsNotSet() {
        UUID uuid = UUID.randomUUID();

        NicotineIntakeRecordInternal internalRecord =
                (NicotineIntakeRecordInternal)
                        new NicotineIntakeRecordInternal()
                                .setQuantity(20)
                                .setNicotineIntakeType(NICOTINE_INTAKE_TYPE_VAPE)
                                .setPackageName("package.name")
                                .setUuid(uuid);

        Parcel parcel = Parcel.obtain();
        internalRecord.writeToParcel(parcel);
        parcel.setDataPosition(0);
        NicotineIntakeRecordInternal decodedRecord = new NicotineIntakeRecordInternal();
        decodedRecord.populateUsing(parcel);
        parcel.recycle();

        assertThat(decodedRecord.getRecordType()).isEqualTo(RECORD_TYPE_NICOTINE_INTAKE);
        assertThat(decodedRecord.getNicotineIntakeGrams()).isEqualTo(DEFAULT_DOUBLE);
        assertThat(decodedRecord.getNicotineIntakeType()).isEqualTo(NICOTINE_INTAKE_TYPE_VAPE);
        assertThat(decodedRecord.getQuantity()).isEqualTo(20);
        assertThat(decodedRecord.getStartTimeInMillis()).isEqualTo(0);
        assertThat(decodedRecord.getEndTimeInMillis()).isEqualTo(0);
        assertThat(decodedRecord.getStartZoneOffsetInSeconds()).isEqualTo(0);
        assertThat(decodedRecord.getEndZoneOffsetInSeconds()).isEqualTo(0);
        assertThat(decodedRecord.getAppInfoId()).isEqualTo(-1);
        assertThat(decodedRecord.getAppName()).isNull();
        assertThat(decodedRecord.getClientRecordId()).isNull();
        assertThat(decodedRecord.getClientRecordVersion()).isEqualTo(-1);
        assertThat(decodedRecord.getDeviceInfoId()).isEqualTo(-1);
        assertThat(decodedRecord.getDeviceType()).isEqualTo(DEVICE_TYPE_UNKNOWN);
        assertThat(decodedRecord.getLastModifiedTime()).isEqualTo(-1);
        assertThat(decodedRecord.getManufacturer()).isNull();
        assertThat(decodedRecord.getModel()).isNull();
        assertThat(decodedRecord.getPackageName()).isEqualTo("package.name");
        assertThat(decodedRecord.getRecordingMethod()).isEqualTo(RECORDING_METHOD_UNKNOWN);
        assertThat(decodedRecord.getRowId()).isEqualTo(-1);
        assertThat(decodedRecord.getUuid()).isEqualTo(uuid);
    }

    @Test
    public void nicotineIntakeRecord_toInternalRecord_allFieldsSet() {
        UUID uuid = UUID.randomUUID();
        Metadata metadata =
                new Metadata.Builder()
                        .setId(uuid.toString())
                        .setClientRecordId("client-record-id")
                        .setClientRecordVersion(567)
                        .setDevice(
                                new Device.Builder()
                                        .setType(DEVICE_TYPE_WATCH)
                                        .setModel("model")
                                        .setManufacturer("manufacturer")
                                        .build())
                        .setDataOrigin(
                                new DataOrigin.Builder().setPackageName("package.name").build())
                        .setRecordingMethod(RECORDING_METHOD_MANUAL_ENTRY)
                        .setLastModifiedTime(Instant.ofEpochMilli(9012345))
                        .build();
        NicotineIntakeRecord externalRecord =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                Instant.ofEpochMilli(1357924680),
                                Instant.ofEpochMilli(2468013579L),
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(ZoneOffset.ofHours(-2))
                        .setEndZoneOffset(ZoneOffset.ofHours(3))
                        .setNicotineIntake(Mass.fromGrams(0.01))
                        .build();

        NicotineIntakeRecordInternal internalRecord = externalRecord.toRecordInternal();

        assertThat(internalRecord.getNicotineIntakeGrams()).isEqualTo(0.01);
        assertThat(internalRecord.getQuantity()).isEqualTo(20);
        assertThat(internalRecord.getNicotineIntakeType()).isEqualTo(NICOTINE_INTAKE_TYPE_VAPE);
        assertThat(internalRecord.getStartTimeInMillis()).isEqualTo(1357924680);
        assertThat(internalRecord.getEndTimeInMillis()).isEqualTo(2468013579L);
        assertThat(internalRecord.getStartZoneOffsetInSeconds()).isEqualTo(-2 * 3600);
        assertThat(internalRecord.getEndZoneOffsetInSeconds()).isEqualTo(3 * 3600);
        assertThat(internalRecord.getAppInfoId()).isEqualTo(-1);
        assertThat(internalRecord.getAppName()).isNull();
        assertThat(internalRecord.getClientRecordId()).isEqualTo("client-record-id");
        assertThat(internalRecord.getClientRecordVersion()).isEqualTo(567);
        assertThat(internalRecord.getDeviceInfoId()).isEqualTo(-1);
        assertThat(internalRecord.getDeviceType()).isEqualTo(DEVICE_TYPE_WATCH);
        assertThat(internalRecord.getLastModifiedTime()).isEqualTo(9012345);
        assertThat(internalRecord.getManufacturer()).isEqualTo("manufacturer");
        assertThat(internalRecord.getModel()).isEqualTo("model");
        assertThat(internalRecord.getPackageName()).isEqualTo("package.name");
        assertThat(internalRecord.getRecordingMethod()).isEqualTo(RECORDING_METHOD_MANUAL_ENTRY);
        assertThat(internalRecord.getRowId()).isEqualTo(-1);
        assertThat(internalRecord.getUuid()).isEqualTo(uuid);
    }

    @Test
    public void nicotineIntakeRecord_toInternalRecord_optionalFieldsNotSet() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
        Metadata metadata = new Metadata.Builder().build();
        NicotineIntakeRecord externalRecord =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                Instant.ofEpochMilli(1357924680),
                                Instant.ofEpochMilli(2468013579L),
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .build();

        NicotineIntakeRecordInternal internalRecord = externalRecord.toRecordInternal();

        assertThat(internalRecord.getNicotineIntakeGrams()).isEqualTo(DEFAULT_DOUBLE);
        assertThat(internalRecord.getQuantity()).isEqualTo(20);
        assertThat(internalRecord.getNicotineIntakeType()).isEqualTo(NICOTINE_INTAKE_TYPE_VAPE);
        assertThat(internalRecord.getStartTimeInMillis()).isEqualTo(1357924680);
        assertThat(internalRecord.getEndTimeInMillis()).isEqualTo(2468013579L);
        assertThat(internalRecord.getStartZoneOffsetInSeconds()).isEqualTo(0);
        assertThat(internalRecord.getEndZoneOffsetInSeconds()).isEqualTo(0);
        assertThat(internalRecord.getAppInfoId()).isEqualTo(-1);
        assertThat(internalRecord.getAppName()).isNull();
        assertThat(internalRecord.getClientRecordId()).isNull();
        assertThat(internalRecord.getClientRecordVersion()).isEqualTo(0);
        assertThat(internalRecord.getDeviceInfoId()).isEqualTo(-1);
        assertThat(internalRecord.getDeviceType()).isEqualTo(DEVICE_TYPE_UNKNOWN);
        assertThat(internalRecord.getLastModifiedTime()).isEqualTo(0);
        assertThat(internalRecord.getManufacturer()).isNull();
        assertThat(internalRecord.getModel()).isNull();
        assertThat(internalRecord.getPackageName()).isNull();
        assertThat(internalRecord.getRecordingMethod()).isEqualTo(RECORDING_METHOD_UNKNOWN);
        assertThat(internalRecord.getRowId()).isEqualTo(-1);
        assertThat(internalRecord.getUuid()).isNull();
    }
}
