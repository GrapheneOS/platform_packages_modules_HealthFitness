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

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthPermissions;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.Record;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.recordfactory.ExerciseSessionRecordFactory;
import android.platform.test.annotations.AppModeFull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.IntStream;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class ExerciseSessionRecordApiTest extends BaseApiTest<ExerciseSessionRecord> {
    public ExerciseSessionRecordApiTest() {
        super(
                () -> ExerciseSessionRecord.class,
                HealthPermissions.READ_EXERCISE,
                HealthPermissions.WRITE_EXERCISE,
                new ExerciseSessionRecordFactory());
    }

    @Test
    public void insertRecords_maxAllowedRecords_returnsInsertedRecords() throws Exception {
        // SQLite has a limit of 1000 host parameters. While we currently handle this in
        // WhereClauses by inlining values, this test ensures that we don't inadvertently change
        // the implementation in the future to one that hits this limit, and that we can handle
        // the maximum allowed page size of 5000 records.
        List<Record> recordsToInsert =
                IntStream.range(0, 5000)
                        .mapToObj(
                                i ->
                                        mRecordFactory.newEmptyRecord(
                                                newEmptyMetadata(),
                                                YESTERDAY_11AM
                                                        .minusDays(2)
                                                        .plusSeconds(i)
                                                        .toInstant(),
                                                YESTERDAY_11AM
                                                        .minusDays(2)
                                                        .plusSeconds(i)
                                                        .plusNanos(500 * 1000000)
                                                        .toInstant()))
                        .map(Record.class::cast)
                        .toList();

        TestUtils.insertRecords(recordsToInsert);

        assertThat(
                        TestUtils.readRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(
                                                ExerciseSessionRecord.class)
                                        .setPageSize(5000)
                                        .build()))
                .hasSize(5000);
    }
}
