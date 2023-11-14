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

package android.healthconnect.cts;

import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readRecords;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static java.util.Comparator.comparing;

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.InstantRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Length;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SharedMemoryTest {

    @Before
    public void before() {
        deleteAllStagedRemoteData();
    }

    @After
    public void after() {
        deleteAllStagedRemoteData();
    }

    @Test
    public void insertRecordsAndReadRecords_viaSharedMemory_recordsEqual() throws Exception {
        DataOrigin dataOrigin =
                new DataOrigin.Builder()
                        .setPackageName(getApplicationContext().getPackageName())
                        .build();

        Metadata metadata = new Metadata.Builder().setDataOrigin(dataOrigin).build();
        int recordCount = 5000;
        List<HeightRecord> records = new ArrayList<>(recordCount);
        Instant now = Instant.now();

        for (int i = 0; i < recordCount; i++) {
            records.add(
                    new HeightRecord.Builder(
                                    metadata,
                                    now.minusMillis(i),
                                    Length.fromMeters(3.0 * i / recordCount))
                            .build());
        }

        insertRecords(records);

        List<HeightRecord> readRecords =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(HeightRecord.class)
                                .setPageSize(records.size())
                                .build());

        assertWithMessage("Record list sizes do not match")
                .that(readRecords.size())
                .isEqualTo(recordCount);

        readRecords.sort(comparing(InstantRecord::getTime).reversed());

        for (int i = 0; i < recordCount; i++) {
            assertThat(readRecords.get(i).getHeight()).isEqualTo(records.get(i).getHeight());
        }
    }
}
