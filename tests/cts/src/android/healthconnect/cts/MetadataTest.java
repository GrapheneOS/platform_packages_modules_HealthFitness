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
package android.healthconnect.cts;

import static android.healthconnect.testing.cts.TestUtils.deleteAllDataFromHealthConnect;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.cts.TestUtils.readRecords;
import static android.healthconnect.testing.cts.TestUtils.updateRecords;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not available in instant mode.")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
    Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
    Flags.FLAG_DEVELOPMENT_DATABASE_RW
})
public class MetadataTest {
    private static final String TEST_DISPLAY_NAME = "MyTestDevice";
    private static final String UPDATED_TEST_DISPLAY_NAME = "MyUpdatedTestDevice";

    @Rule public final TestRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws InterruptedException {
        deleteAllDataFromHealthConnect();
    }

    @After
    public void tearDown() throws InterruptedException {
        deleteAllDataFromHealthConnect();
    }

    @Test
    public void testInsertAndReadRecord_deviceDisplayNameIsPersisted() throws Exception {
        Device device = new Device.Builder().setDisplayName(TEST_DISPLAY_NAME).build();
        Metadata metadata = getMetadataBuilder(device).build();
        HeartRateRecord record = getHeartRateRecord(metadata);

        List<Record> records = insertRecords(record);
        assertThat(records).hasSize(1);

        List<HeartRateRecord> readRecords = readHeartRateRecords();
        assertThat(readRecords).hasSize(1);
        assertThat(readRecords.get(0).getMetadata().getDevice().getDisplayName())
                .isEqualTo(TEST_DISPLAY_NAME);
    }

    @Test
    public void testUpdateRecord_deviceDisplayNameIsUpdated() throws Exception {
        Device device = new Device.Builder().setDisplayName(TEST_DISPLAY_NAME).build();
        Metadata metadata = getMetadataBuilder(device).build();
        HeartRateRecord recordToInsert = getHeartRateRecord(metadata);

        List<Record> insertedRecords = insertRecords(recordToInsert);
        assertThat(insertedRecords).hasSize(1);
        String recordId = insertedRecords.get(0).getMetadata().getId();

        Device updatedDevice =
                new Device.Builder().setDisplayName(UPDATED_TEST_DISPLAY_NAME).build();
        Metadata updatedMetadata = getMetadataBuilder(updatedDevice).setId(recordId).build();
        HeartRateRecord recordToUpdate = getHeartRateRecord(updatedMetadata);
        updateRecords(Collections.singletonList(recordToUpdate));

        List<HeartRateRecord> records = readHeartRateRecords();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getMetadata().getDevice().getDisplayName())
                .isEqualTo(UPDATED_TEST_DISPLAY_NAME);
        assertThat(records.get(0).getMetadata().getId()).isEqualTo(recordId);
    }

    private Metadata.Builder getMetadataBuilder(Device device) {
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        return new Metadata.Builder().setDevice(device).setDataOrigin(dataOrigin);
    }

    private HeartRateRecord getHeartRateRecord(Metadata metadata) {
        Instant now = Instant.now();
        return new HeartRateRecord.Builder(
                        metadata,
                        now,
                        now.plusSeconds(1),
                        Collections.singletonList(new HeartRateRecord.HeartRateSample(100, now)))
                .build();
    }

    private List<HeartRateRecord> readHeartRateRecords() throws InterruptedException {
        ReadRecordsRequestUsingFilters<HeartRateRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(HeartRateRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(
                                                Instant.now().plus(java.time.Duration.ofDays(1)))
                                        .build())
                        .build();
        return readRecords(request);
    }
}
