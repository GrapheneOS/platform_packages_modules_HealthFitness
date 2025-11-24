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

import static android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadataWithId;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthPermissions;
import android.health.connect.RecordIdFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SymptomRecord;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.recordfactory.SymptomRecordFactory;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    Flags.FLAG_SYMPTOMS,
    Flags.FLAG_SYMPTOMS_DB,
    Flags.FLAG_HEALTH_CONNECT_MAPPINGS,
})
public class SymptomRecordApiTest extends BaseApiTest<SymptomRecord> {
    public SymptomRecordApiTest() {
        super(
                () -> SymptomRecord.class,
                HealthPermissions.READ_SYMPTOM_COUGH,
                HealthPermissions.WRITE_SYMPTOM_COUGH,
                new SymptomRecordFactory());
    }

    @Test
    @Override
    public void insertRecords_generatesChangelogs() throws Exception {
        List<SymptomRecord> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()));
        String token =
                TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(mRecordClass)
                                        .build())
                        .getToken();
        List<Record> insertedRecords = TestUtils.insertRecords(recordsToInsert);

        ChangeLogsResponse response =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        assertThat(response.getUpsertedRecords()).hasSize(2);
        assertThat(response.getUpsertedRecords()).containsExactlyElementsIn(insertedRecords);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    @Override
    public void deleteRecords_usingFilters_generatesChangelogs() throws Exception {
        List<SymptomRecord> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        String token =
                TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(mRecordClass)
                                        .build())
                        .getToken();
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder().addRecordType(mRecordClass).build());

        ChangeLogsResponse response =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        assertThat(response.getUpsertedRecords()).isEmpty();
        assertThat(response.getDeletedLogs()).hasSize(2);
        assertThat(
                        response.getDeletedLogs().stream()
                                .map(ChangeLogsResponse.DeletedLog::getDeletedRecordId)
                                .toList())
                .containsExactlyElementsIn(recordIds);
    }

    @Test
    @Override
    public void deleteRecords_usingIds_generatesChangelogs() throws Exception {
        List<SymptomRecord> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        String token =
                TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(mRecordClass)
                                        .build())
                        .getToken();
        TestUtils.verifyDeleteRecords(
                List.of(
                        RecordIdFilter.fromId(mRecordClass, recordIds.get(1)),
                        RecordIdFilter.fromId(mRecordClass, recordIds.get(2))));

        ChangeLogsResponse response =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        assertThat(response.getUpsertedRecords()).isEmpty();
        assertThat(response.getDeletedLogs()).hasSize(2);
        assertThat(
                        response.getDeletedLogs().stream()
                                .map(ChangeLogsResponse.DeletedLog::getDeletedRecordId)
                                .toList())
                .containsExactly(recordIds.get(1), recordIds.get(2));
    }

    @Test
    @Override
    public void updateRecords_generatesChangelogs() throws Exception {
        List<SymptomRecord> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        List<SymptomRecord> updatedRecords =
                List.of(
                        mRecordFactory.anotherFullRecord(
                                newEmptyMetadataWithId(recordIds.get(0)),
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(55).toInstant()));
        String token =
                TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(mRecordClass)
                                        .build())
                        .getToken();
        TestUtils.updateRecords(updatedRecords);

        ChangeLogsResponse response =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        assertThat(response.getUpsertedRecords()).hasSize(1);
        SymptomRecord updatedRecord = (SymptomRecord) response.getUpsertedRecords().get(0);
        assertThat(updatedRecord.getMetadata().getId()).isEqualTo(recordIds.get(0));
        assertThat(updatedRecord.getStartTime())
                .isEqualTo(YESTERDAY_11AM.plusMinutes(40).toInstant());
        assertThat(updatedRecord.getEndTime())
                .isEqualTo(YESTERDAY_11AM.plusMinutes(55).toInstant());
        assertThat(response.getDeletedLogs()).isEmpty();
    }
}
