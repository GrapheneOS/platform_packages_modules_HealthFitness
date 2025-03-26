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
package com.android.server.healthconnect.common.changelog;

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createDifferentVaccineMedicalResource;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.createVaccineMedicalResource;

import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS_DB;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.MedicalResourceId;
import android.health.connect.aidl.RecordsParcel;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.health.connect.testing.RecordFactory;
import android.os.Parcel;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ChangeLogsResponseTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final Instant DELETION_TIME = Instant.ofEpochMilli(123456);
    private static final String TEST_TOKEN = "test_token";
    private static final String TEST_RECORD_ID = UUID.randomUUID().toString();

    @Test
    public void testConstructor_primary() {
        List<Record> upsertedRecords =
                List.of(RecordFactory.makePopulatedRecord(RECORD_TYPE_STEPS));
        List<ChangeLogsResponse.DeletedLog> deletedLogs =
                List.of(new ChangeLogsResponse.DeletedLog(TEST_RECORD_ID, Instant.now()));
        List<MedicalResource> upsertedMedicalResources =
                List.of(createVaccineMedicalResource(DATA_SOURCE_ID));
        List<ChangeLogsResponse.DeletedMedicalResource> deletedMedicalResources =
                List.of(
                        new ChangeLogsResponse.DeletedMedicalResource(
                                createDifferentVaccineMedicalResource(DATA_SOURCE_ID).getId(),
                                Instant.now()));

        ChangeLogsResponse response =
                new ChangeLogsResponse(
                        upsertedRecords,
                        deletedLogs,
                        upsertedMedicalResources,
                        deletedMedicalResources,
                        TEST_TOKEN,
                        true);

        assertThat(response.getUpsertedRecords()).isEqualTo(upsertedRecords);
        assertThat(response.getDeletedLogs()).isEqualTo(deletedLogs);
        assertThat(response.getUpsertedMedicalResources()).isEqualTo(upsertedMedicalResources);
        assertThat(response.getDeletedMedicalResources()).isEqualTo(deletedMedicalResources);
        assertThat(response.getNextChangesToken()).isEqualTo(TEST_TOKEN);
        assertThat(response.hasMorePages()).isTrue();
    }

    @Test
    @SuppressWarnings("deprecation") // Testing deprecated constructor
    public void testConstructor_deprecated() {
        Record record = RecordFactory.makePopulatedRecord(RECORD_TYPE_STEPS);
        RecordsParcel recordsParcel = new RecordsParcel(List.of(record.toRecordInternal()));
        List<ChangeLogsResponse.DeletedLog> deletedLogs =
                List.of(new ChangeLogsResponse.DeletedLog(TEST_RECORD_ID, DELETION_TIME));

        ChangeLogsResponse response =
                new ChangeLogsResponse(recordsParcel, deletedLogs, TEST_TOKEN, false);

        assertThat(response.getUpsertedRecords()).containsExactly(record);
        assertThat(response.getDeletedLogs()).isEqualTo(deletedLogs);
        assertThat(response.getUpsertedMedicalResources())
                .isEmpty(); // Deprecated constructor doesn't handle these
        assertThat(response.getDeletedMedicalResources())
                .isEmpty(); // Deprecated constructor doesn't handle these
        assertThat(response.getNextChangesToken()).isEqualTo(TEST_TOKEN);
        assertThat(response.hasMorePages()).isFalse();
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
    })
    public void testParceling_flagEnabled() {
        Record upsertedRecord = RecordFactory.makePopulatedRecord(RECORD_TYPE_STEPS);
        ChangeLogsResponse.DeletedLog deletedLog =
                new ChangeLogsResponse.DeletedLog(TEST_RECORD_ID, DELETION_TIME);
        MedicalResource upsertedMedicalResource = createVaccineMedicalResource(DATA_SOURCE_ID);
        ChangeLogsResponse.DeletedMedicalResource deletedMedicalResource =
                new ChangeLogsResponse.DeletedMedicalResource(
                        createDifferentVaccineMedicalResource(DATA_SOURCE_ID).getId(),
                        DELETION_TIME);

        ChangeLogsResponse originalResponse =
                new ChangeLogsResponse(
                        List.of(upsertedRecord),
                        List.of(deletedLog),
                        List.of(upsertedMedicalResource),
                        List.of(deletedMedicalResource),
                        TEST_TOKEN,
                        true);

        Parcel parcel = Parcel.obtain();
        originalResponse.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ChangeLogsResponse unparceledResponse = ChangeLogsResponse.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(unparceledResponse).isEqualTo(originalResponse);
    }

    @Test
    @RequiresFlagsDisabled({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
    })
    public void testParceling_flagDisabled() {
        Record upsertedRecord = RecordFactory.makePopulatedRecord(RECORD_TYPE_STEPS);
        ChangeLogsResponse.DeletedLog deletedLog =
                new ChangeLogsResponse.DeletedLog(TEST_RECORD_ID, DELETION_TIME);
        MedicalResource upsertedMedicalResource =
                createVaccineMedicalResource(DATA_SOURCE_ID); // Will be ignored
        ChangeLogsResponse.DeletedMedicalResource deletedMedicalResource =
                new ChangeLogsResponse.DeletedMedicalResource(
                        createDifferentVaccineMedicalResource(DATA_SOURCE_ID).getId(),
                        DELETION_TIME); // Will be ignored

        ChangeLogsResponse originalResponse =
                new ChangeLogsResponse(
                        List.of(upsertedRecord),
                        List.of(deletedLog),
                        List.of(upsertedMedicalResource), // Included for constructor
                        List.of(deletedMedicalResource), // Included for constructor
                        TEST_TOKEN,
                        false);

        Parcel parcel = Parcel.obtain();
        originalResponse.writeToParcel(parcel, 0); // Medical resources won't be written
        parcel.setDataPosition(0);

        ChangeLogsResponse unparceledResponse = ChangeLogsResponse.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(unparceledResponse.getUpsertedRecords())
                .isEqualTo(originalResponse.getUpsertedRecords());
        assertThat(unparceledResponse.getDeletedLogs())
                .isEqualTo(originalResponse.getDeletedLogs());
        assertThat(unparceledResponse.getUpsertedMedicalResources())
                .isEmpty(); // Not parceled/unparceled
        assertThat(unparceledResponse.getDeletedMedicalResources())
                .isEmpty(); // Not parceled/unparceled
        assertThat(unparceledResponse.getNextChangesToken()).isEqualTo(TEST_TOKEN);
        assertThat(unparceledResponse.hasMorePages()).isFalse();
    }

    @Test
    public void testDeletedLog_constructorAndGetters() {
        ChangeLogsResponse.DeletedLog deletedLog =
                new ChangeLogsResponse.DeletedLog(TEST_RECORD_ID, DELETION_TIME);
        assertThat(deletedLog.getDeletedRecordId()).isEqualTo(TEST_RECORD_ID);
        assertThat(deletedLog.getDeletedTime()).isEqualTo(DELETION_TIME);
    }

    @Test
    @SuppressWarnings("deprecation") // Testing deprecated constructor
    public void testDeletedLog_deprecatedConstructor() {
        ChangeLogsResponse.DeletedLog deletedLog =
                new ChangeLogsResponse.DeletedLog(TEST_RECORD_ID, DELETION_TIME.toEpochMilli());
        assertThat(deletedLog.getDeletedRecordId()).isEqualTo(TEST_RECORD_ID);
        assertThat(deletedLog.getDeletedTime()).isEqualTo(DELETION_TIME);
    }

    @Test
    public void testDeletedMedicalResource_constructorAndGetters() {
        MedicalResourceId resourceId = createVaccineMedicalResource(DATA_SOURCE_ID).getId();
        ChangeLogsResponse.DeletedMedicalResource deletedResource =
                new ChangeLogsResponse.DeletedMedicalResource(resourceId, DELETION_TIME);
        assertThat(deletedResource.getDeletedMedicalResourceId()).isEqualTo(resourceId);
        assertThat(deletedResource.getDataSourceId()).isEqualTo(DATA_SOURCE_ID);
        assertThat(deletedResource.getDeletedTime()).isEqualTo(DELETION_TIME);
    }
}
