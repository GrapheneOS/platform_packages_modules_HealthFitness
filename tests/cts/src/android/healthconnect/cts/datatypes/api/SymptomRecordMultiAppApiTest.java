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

package android.healthconnect.cts.datatypes.api;

import static com.android.healthfitness.flags.Flags.FLAG_SMOKING_DB;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthPermissions;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SymptomRecord;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.recordfactory.SymptomRecordFactory;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({FLAG_SYMPTOMS, FLAG_SYMPTOMS_DB, FLAG_SMOKING_DB})
public class SymptomRecordMultiAppApiTest extends BaseMultiAppApiTest<SymptomRecord> {

    private final SymptomRecordFactory mSymptomRecordFactory;

    public SymptomRecordMultiAppApiTest() {
        super(
                () -> SymptomRecord.class,
                /* testPackagePermissions= */ new AppPermissions(
                        List.of(
                                HealthPermissions.READ_SYMPTOM_COUGH,
                                HealthPermissions.READ_SYMPTOM_DIZZINESS,
                                HealthPermissions.READ_SYMPTOM_FEVER,
                                HealthPermissions.READ_SYMPTOM_HEADACHE,
                                HealthPermissions.READ_SYMPTOM_SNORE),
                        List.of(
                                HealthPermissions.WRITE_SYMPTOM_COUGH,
                                HealthPermissions.WRITE_SYMPTOM_DIZZINESS,
                                HealthPermissions.WRITE_SYMPTOM_HEADACHE,
                                HealthPermissions.WRITE_SYMPTOM_SNORE)),
                /* testAppTwoPermissions= */ new AppPermissions(
                        List.of(
                                HealthPermissions.READ_SYMPTOM_COUGH,
                                HealthPermissions.READ_SYMPTOM_SNORE),
                        List.of(
                                HealthPermissions.WRITE_SYMPTOM_COUGH,
                                HealthPermissions.WRITE_SYMPTOM_SNORE)),
                new SymptomRecordFactory());
        mSymptomRecordFactory = new SymptomRecordFactory();
    }

    @Test
    public void insertRecords_haveAllPermissions_throwsNoException() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_DIZZINESS),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_HEADACHE),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        assertThat(recordIds.size()).isEqualTo(4);
    }

    @Test
    public void insertRecords_missingPermissions_throwsException() {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_DIZZINESS),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_FEVER),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_HEADACHE),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> insertRecordsAndReturnIds(recordsToInsert));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void updateRecords_haveAllPermissions_throwsNoException() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_DIZZINESS),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_HEADACHE),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        List<Record> records = TestUtils.insertRecords(recordsToInsert);
        assertThat(records.size()).isEqualTo(4);

        // Throws no exception
        TestUtils.updateRecords(records);
    }

    @Test
    public void updateRecords_missingPermissions_throwsException() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        mTestAppTwo.insertRecords(recordsToInsert);
        List<SymptomRecord> records =
                mTestAppTwo.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SymptomRecord.class).build());

        mTestAppTwoRule.revokeHealthPermission(HealthPermissions.WRITE_SYMPTOM_SNORE);

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class, () -> mTestAppTwo.updateRecords(records));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void updateRecords_changeSymptomType_throwsException() throws Exception {
        List<Record> recordsToInsert =
                List.of(SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH));
        List<Record> records = TestUtils.insertRecords(recordsToInsert);
        assertThat(records.size()).isEqualTo(1);

        SymptomRecord updatedRecord =
                mSymptomRecordFactory.recordWithMetadata(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE),
                        records.get(0).getMetadata());

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> TestUtils.updateRecords(List.of(updatedRecord)));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void updateRecords_doNotChangeSymptomType_throwsNoException() throws Exception {
        List<Record> recordsToInsert =
                List.of(SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH));

        List<Record> records = TestUtils.insertRecords(recordsToInsert);
        assertThat(records.size()).isEqualTo(1);

        SymptomRecord updatedRecord =
                mSymptomRecordFactory.newFullRecord(
                        records.get(0).getMetadata(),
                        YESTERDAY_11AM.plusMinutes(10).toInstant(),
                        YESTERDAY_11AM.plusMinutes(15).toInstant());

        // Throws no exception
        TestUtils.updateRecords(List.of(updatedRecord));
    }

    @Test
    public void deleteRecords_onlyDeleteSymptomsWithWritePermission() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        List<String> recordIds = mTestAppTwo.insertRecords(recordsToInsert);

        mTestAppTwoRule.revokeHealthPermission(HealthPermissions.WRITE_SYMPTOM_SNORE);

        // Try to delete both records but can only delete COUGH due to missing write SNORE
        // permission
        mTestAppTwo.deleteRecords(
                RecordIdFilter.fromId(SymptomRecord.class, recordIds.get(0)),
                RecordIdFilter.fromId(SymptomRecord.class, recordIds.get(1)));

        List<SymptomRecord> records =
                mTestAppTwo.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SymptomRecord.class).build());
        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getSymptomType()).isEqualTo(SymptomRecord.SYMPTOM_TYPE_SNORE);
    }

    @Test
    public void deleteRecords_noSymptomWritePermission_throwsException() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        List<String> recordIds = mTestAppTwo.insertRecords(recordsToInsert);

        mTestAppTwoRule.revokeHealthPermissions(
                List.of(
                        HealthPermissions.WRITE_SYMPTOM_SNORE,
                        HealthPermissions.WRITE_SYMPTOM_COUGH));

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestAppTwo.deleteRecords(
                                        RecordIdFilter.fromId(
                                                SymptomRecord.class, recordIds.get(0)),
                                        RecordIdFilter.fromId(
                                                SymptomRecord.class, recordIds.get(1))));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void readRecords_onlyReadSymptomsWithReadPermission() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_DIZZINESS),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_HEADACHE),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        insertRecordsAndReturnIds(recordsToInsert);

        List<SymptomRecord> recordsRead =
                mTestAppTwo.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SymptomRecord.class).build());

        assertThat(recordsRead.size()).isEqualTo(2);
        assertThat(
                        recordsRead.stream()
                                .map(SymptomRecord::getSymptomType)
                                .collect(Collectors.toSet()))
                .containsExactly(
                        SymptomRecord.SYMPTOM_TYPE_SNORE, SymptomRecord.SYMPTOM_TYPE_COUGH);
    }

    @Test
    public void readRecords_noSymptomReadPermission_throwsException() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_DIZZINESS),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_HEADACHE),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        insertRecordsAndReturnIds(recordsToInsert);

        mTestAppTwoRule.revokeHealthPermissions(
                List.of(
                        HealthPermissions.READ_SYMPTOM_SNORE,
                        HealthPermissions.READ_SYMPTOM_COUGH,
                        HealthPermissions.WRITE_SYMPTOM_SNORE,
                        HealthPermissions.WRITE_SYMPTOM_COUGH));

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestAppTwo.readRecords(
                                        new ReadRecordsRequestUsingFilters.Builder<>(
                                                        SymptomRecord.class)
                                                .build()));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void readRecordsById_onlyReadSymptomsWithReadPermission() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_DIZZINESS),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_HEADACHE),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<SymptomRecord> recordsRead =
                mTestAppTwo.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(SymptomRecord.class)
                                .addId(recordIds.get(0))
                                .addId(recordIds.get(1))
                                .addId(recordIds.get(2))
                                .addId(recordIds.get(3))
                                .build());

        assertThat(recordsRead.size()).isEqualTo(2);
        assertThat(
                        recordsRead.stream()
                                .map(SymptomRecord::getSymptomType)
                                .collect(Collectors.toSet()))
                .containsExactly(
                        SymptomRecord.SYMPTOM_TYPE_SNORE, SymptomRecord.SYMPTOM_TYPE_COUGH);
    }

    @Test
    public void readRecordsById_noSymptomReadPermission_throwsException() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_DIZZINESS),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_HEADACHE),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        mTestAppTwoRule.revokeHealthPermissions(
                List.of(
                        HealthPermissions.READ_SYMPTOM_SNORE,
                        HealthPermissions.READ_SYMPTOM_COUGH,
                        HealthPermissions.WRITE_SYMPTOM_SNORE,
                        HealthPermissions.WRITE_SYMPTOM_COUGH));

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestAppTwo.readRecords(
                                        new ReadRecordsRequestUsingIds.Builder<>(
                                                        SymptomRecord.class)
                                                .addId(recordIds.get(0))
                                                .addId(recordIds.get(1))
                                                .addId(recordIds.get(2))
                                                .addId(recordIds.get(3))
                                                .build()));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testChangeLogs_insertSymptomsRecords_returnsUpsertLogsAppHoldsPermissionFor()
            throws Exception {
        // App One has permission to read/write cough/snore and App B has permission to read cough
        // only
        String changeLogToken =
                mTestAppTwo.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(SymptomRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_DIZZINESS),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_HEADACHE));
        insertRecordsAndReturnIds(recordsToInsert);
        List<String> coughAndSnoreRecordsIdInserted =
                insertRecordsAndReturnIds(
                        List.of(
                                SymptomRecordFactory.newInstantRecord(
                                        SymptomRecord.SYMPTOM_TYPE_COUGH),
                                SymptomRecordFactory.newInstantRecord(
                                        SymptomRecord.SYMPTOM_TYPE_SNORE)));

        ChangeLogsResponse response = mTestAppTwo.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedRecords()).hasSize(2);
        assertThat(
                        response.getUpsertedRecords().stream()
                                .map(record -> record.getMetadata().getId())
                                .collect(Collectors.toList()))
                .containsAtLeastElementsIn(coughAndSnoreRecordsIdInserted);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    public void testChangeLogs_deleteSymptomsRecords_returnsAllDeletedSymptomRecordIds()
            throws Exception {
        String changeLogToken =
                mTestAppTwo.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(SymptomRecord.class)
                                .build());

        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_DIZZINESS),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_HEADACHE));
        List<Record> records = TestUtils.insertRecords(recordsToInsert);
        List<Record> coughAndSnoreRecordsInserted =
                TestUtils.insertRecords(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));

        TestUtils.deleteRecords(records);
        TestUtils.deleteRecords(coughAndSnoreRecordsInserted);

        ChangeLogsResponse response = mTestAppTwo.getChangeLogs(changeLogsRequest);

        assertThat(response.getDeletedLogs().size()).isAtLeast(2);
        assertThat(
                        response.getDeletedLogs().stream()
                                .map(ChangeLogsResponse.DeletedLog::getDeletedRecordId)
                                .collect(Collectors.toList()))
                .containsAtLeastElementsIn(
                        coughAndSnoreRecordsInserted.stream()
                                .map(record -> record.getMetadata().getId())
                                .collect(Collectors.toSet()));
    }

    @Test
    public void
            testChangeLogs_getChangeLogToken_throwsExceptionForAppWithNoReadSymptomPermission() {
        mTestAppTwoRule.revokeHealthPermissions(
                List.of(
                        HealthPermissions.READ_SYMPTOM_SNORE,
                        HealthPermissions.READ_SYMPTOM_COUGH));
        assertThrows(
                HealthConnectException.class,
                () ->
                        mTestAppTwo.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(SymptomRecord.class)
                                        .build()));
    }

    @Test
    public void testChangeLogs_getChangeLogs_throwsExceptionForAppWithNoReadSymptomPermission()
            throws Exception {

        String changeLogToken =
                mTestAppTwo.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(SymptomRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        // Revoke permission after token generation
        mTestAppTwoRule.revokeHealthPermissions(
                List.of(
                        HealthPermissions.READ_SYMPTOM_SNORE,
                        HealthPermissions.READ_SYMPTOM_COUGH));

        assertThrows(
                HealthConnectException.class, () -> mTestAppTwo.getChangeLogs(changeLogsRequest));
    }

    @Test
    public void readRecords_ownRecord_noReadPermission_returnsRecord() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        mTestAppTwo.insertRecords(recordsToInsert);

        mTestAppTwoRule.revokeHealthPermissions(
                List.of(
                        HealthPermissions.READ_SYMPTOM_COUGH,
                        HealthPermissions.READ_SYMPTOM_SNORE,
                        HealthPermissions.WRITE_SYMPTOM_SNORE));

        List<SymptomRecord> records =
                mTestAppTwo.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SymptomRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(mTestAppTwo.getPackageName())
                                                .build())
                                .build());

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getSymptomType()).isEqualTo(SymptomRecord.SYMPTOM_TYPE_COUGH);
    }

    @Test
    @Override
    public void readRecords_byId_ownRecord_noReadPermission_returnsRecord() throws Exception {
        String coughRecordId =
                mTestAppTwo
                        .insertRecords(
                                List.of(
                                        SymptomRecordFactory.newInstantRecord(
                                                SymptomRecord.SYMPTOM_TYPE_COUGH)))
                        .get(0);
        String snoreRecordId =
                mTestAppTwo
                        .insertRecords(
                                List.of(
                                        SymptomRecordFactory.newInstantRecord(
                                                SymptomRecord.SYMPTOM_TYPE_SNORE)))
                        .get(0);

        mTestAppTwoRule.revokeHealthPermissions(
                List.of(
                        HealthPermissions.READ_SYMPTOM_COUGH,
                        HealthPermissions.READ_SYMPTOM_SNORE,
                        HealthPermissions.WRITE_SYMPTOM_SNORE));

        List<SymptomRecord> records =
                mTestAppTwo.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(SymptomRecord.class)
                                .addId(snoreRecordId)
                                .addId(coughRecordId)
                                .build());

        assertThat(records.size()).isEqualTo(1);
        assertThat(records.get(0).getSymptomType()).isEqualTo(SymptomRecord.SYMPTOM_TYPE_COUGH);
    }

    @Test
    @Override
    public void readRecords_byId_ownRecord_noReadWritePermissions_throws() throws Exception {
        String coughRecordId =
                mTestAppTwo
                        .insertRecords(
                                List.of(
                                        SymptomRecordFactory.newInstantRecord(
                                                SymptomRecord.SYMPTOM_TYPE_COUGH)))
                        .get(0);
        String snoreRecordId =
                mTestAppTwo
                        .insertRecords(
                                List.of(
                                        SymptomRecordFactory.newInstantRecord(
                                                SymptomRecord.SYMPTOM_TYPE_SNORE)))
                        .get(0);

        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.readPermissions());
        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.writePermissions());

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestAppTwo.readRecords(
                                        new ReadRecordsRequestUsingIds.Builder<>(
                                                        SymptomRecord.class)
                                                .addId(coughRecordId)
                                                .addId(snoreRecordId)
                                                .build()));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void readRecords_ownRecord_noReadWritePermissions_throws() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH),
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));
        mTestAppTwo.insertRecords(recordsToInsert);

        mTestAppTwoRule.revokeHealthPermissions(
                List.of(
                        HealthPermissions.READ_SYMPTOM_COUGH,
                        HealthPermissions.READ_SYMPTOM_SNORE,
                        HealthPermissions.WRITE_SYMPTOM_SNORE));

        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.readPermissions());
        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.writePermissions());

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestAppTwo.readRecords(
                                        new ReadRecordsRequestUsingFilters.Builder<>(
                                                        SymptomRecord.class)
                                                .addDataOrigins(
                                                        new DataOrigin.Builder()
                                                                .setPackageName(
                                                                        mTestAppTwo
                                                                                .getPackageName())
                                                                .build())
                                                .build()));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }
}
