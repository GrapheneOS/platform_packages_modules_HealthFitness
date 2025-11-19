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
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadataWithClientId;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthPermissions;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.MenstrualCyclePhaseRecord;
import android.health.connect.datatypes.Record;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.recordfactory.MenstrualCyclePhaseRecordFactory;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.temporal.ChronoUnit;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    Flags.FLAG_CYCLE_PHASES_FLAG,
    Flags.FLAG_CYCLE_PHASES_DB,
    Flags.FLAG_SMOKING_DB,
    Flags.FLAG_SYMPTOMS_DB,
    Flags.FLAG_ALCOHOL_CONSUMPTION_DB
})
public class MenstrualCyclePhaseRecordApiTest extends BaseApiTest<MenstrualCyclePhaseRecord> {
    public MenstrualCyclePhaseRecordApiTest() {
        super(
                () -> MenstrualCyclePhaseRecord.class,
                HealthPermissions.READ_MENSTRUAL_CYCLE_PHASE,
                HealthPermissions.WRITE_MENSTRUAL_CYCLE_PHASE,
                new MenstrualCyclePhaseRecordFactory());
    }

    @Override
    @Test
    public void readRecords_usingFilters_byTimeInstantRangeFilter() throws Exception {
        List<android.health.connect.datatypes.Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(5).toInstant(),
                                YESTERDAY_11AM.minusDays(5).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(4).toInstant(),
                                YESTERDAY_11AM.minusDays(4).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(3).toInstant(),
                                YESTERDAY_11AM.minusDays(3).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(2).toInstant(),
                                YESTERDAY_11AM.minusDays(2).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(1).toInstant(),
                                YESTERDAY_11AM.minusDays(1).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.toInstant(),
                                YESTERDAY_11AM.toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        assertThat(recordIds).hasSize(6);

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(mRecordClass)
                                .setTimeRangeFilter(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        YESTERDAY_11AM
                                                                .minusDays(4)
                                                                .truncatedTo(ChronoUnit.DAYS)
                                                                .toInstant())
                                                .setEndTime(
                                                        YESTERDAY_11AM
                                                                .minusDays(1)
                                                                .truncatedTo(ChronoUnit.DAYS)
                                                                .minusNanos(1)
                                                                .toInstant())
                                                .build())
                                .build());
        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)),
                        withIdAndTestPackageName(recordsToInsert.get(2), recordIds.get(2)),
                        withIdAndTestPackageName(recordsToInsert.get(3), recordIds.get(3)));
    }

    @Override
    @Test
    public void deleteRecords_usingFilters_byTimeInstantRangeFilter() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(5).toInstant(),
                                YESTERDAY_11AM.minusDays(5).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(4).toInstant(),
                                YESTERDAY_11AM.minusDays(4).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(3).toInstant(),
                                YESTERDAY_11AM.minusDays(3).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(2).toInstant(),
                                YESTERDAY_11AM.minusDays(2).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.minusDays(1).toInstant(),
                                YESTERDAY_11AM.minusDays(1).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.toInstant(),
                                YESTERDAY_11AM.toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(mRecordClass)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(
                                                YESTERDAY_11AM
                                                        .minusDays(4)
                                                        .truncatedTo(ChronoUnit.DAYS)
                                                        .toInstant())
                                        .setEndTime(
                                                YESTERDAY_11AM
                                                        .minusDays(1)
                                                        .truncatedTo(ChronoUnit.DAYS)
                                                        .minusNanos(1)
                                                        .toInstant())
                                        .build())
                        .build());

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)),
                        withIdAndTestPackageName(recordsToInsert.get(4), recordIds.get(4)),
                        withIdAndTestPackageName(recordsToInsert.get(5), recordIds.get(5)));
    }

    @Test
    public void insertRecords_sameDate_existingRecordOverridden() throws InterruptedException {
        Record recordToInsert =
                mRecordFactory.newFullRecord(
                        newEmptyMetadata(),
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusHours(1).toInstant());
        TestUtils.insertRecord(recordToInsert);

        Record samdDateRecord =
                mRecordFactory.newFullRecord(
                        newEmptyMetadata(),
                        YESTERDAY_11AM.plusHours(1).toInstant(),
                        YESTERDAY_11AM.plusHours(2).toInstant());
        Record insertedRecord = TestUtils.insertRecord(samdDateRecord);

        assertThat(readAllRecords()).containsExactly(insertedRecord);
    }

    @Test
    public void insertRecords_sameDateWithClientId_existingRecordOverridden()
            throws InterruptedException {
        Record recordToInsert =
                mRecordFactory.newFullRecord(
                        newEmptyMetadataWithClientId("foo-client-id"),
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusHours(1).toInstant());
        TestUtils.insertRecord(recordToInsert);

        Record samdDateRecord =
                mRecordFactory.newFullRecord(
                        newEmptyMetadataWithClientId("bar-client-id"),
                        YESTERDAY_11AM.plusHours(1).toInstant(),
                        YESTERDAY_11AM.plusHours(2).toInstant());
        Record insertedRecord = TestUtils.insertRecord(samdDateRecord);

        assertThat(readAllRecords()).containsExactly(insertedRecord);
    }
}
