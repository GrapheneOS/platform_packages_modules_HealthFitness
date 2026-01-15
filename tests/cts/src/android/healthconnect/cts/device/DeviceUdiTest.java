/*
 * Copyright (C) 2026 The Android Open Source Project
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

package android.healthconnect.cts.device;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.healthconnect.testing.shared.recordfactory.StepsRecordFactory;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({Flags.FLAG_DEVICE_UDI})
public class DeviceUdiTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private static final String TEST_UDI = "test_udi_123";
    private static final String ANOTHER_TEST_UDI = "another_test_udi_456";

    private final StepsRecordFactory mStepsRecordFactory = new StepsRecordFactory();

    @Before
    public void setUp() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test
    public void testDeviceBuilder_setUdi_getUdiReturnsCorrectValue() {
        Device device = new Device.Builder().setUdi(TEST_UDI).build();
        assertThat(device.getUdi()).isEqualTo(TEST_UDI);
    }

    @Test
    public void testDeviceBuilder_setUdiNull_getUdiReturnsNull() {
        Device device = new Device.Builder().setUdi(null).build();
        assertThat(device.getUdi()).isNull();
    }

    @Test
    public void testDeviceBuilder_setUdiEmpty_getUdiReturnsEmpty() {
        Device device = new Device.Builder().setUdi("").build();
        assertThat(device.getUdi()).isEqualTo("");
    }

    @Test
    public void testBasicCrud_withUdi() throws InterruptedException {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setUdi(TEST_UDI)
                        .build();
        Metadata metadata = new Metadata.Builder().setDevice(device).build();
        Instant now = Instant.now();
        StepsRecord record =
                mStepsRecordFactory.newEmptyRecord(metadata, now, now.plusMillis(1000));

        List<Record> insertedRecords = TestUtils.insertRecords(List.of(record));
        assertThat(insertedRecords).hasSize(1);
        String recordId = insertedRecords.get(0).getMetadata().getId();

        List<StepsRecord> readRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                .addId(recordId)
                                .build());
        assertThat(readRecords).hasSize(1);
        assertThat(readRecords.get(0).getMetadata().getDevice().getUdi()).isEqualTo(TEST_UDI);
    }

    @Test
    public void testRecordUpdate_addUdi() throws InterruptedException {
        Device initialDevice =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .build();
        Metadata initialMetadata = new Metadata.Builder().setDevice(initialDevice).build();
        Instant now = Instant.now();
        StepsRecord initialRecord =
                mStepsRecordFactory.newEmptyRecord(initialMetadata, now, now.plusMillis(1000));

        List<Record> insertedRecords = TestUtils.insertRecords(List.of(initialRecord));
        String recordId = insertedRecords.get(0).getMetadata().getId();

        Device updatedDevice =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setUdi(TEST_UDI)
                        .build();
        Metadata updatedMetadata =
                new Metadata.Builder().setId(recordId).setDevice(updatedDevice).build();
        StepsRecord updatedRecord =
                mStepsRecordFactory.newEmptyRecord(updatedMetadata, now, now.plusMillis(1000));

        TestUtils.updateRecords(List.of(updatedRecord));

        List<StepsRecord> readRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                .addId(recordId)
                                .build());
        assertThat(readRecords).hasSize(1);
        assertThat(readRecords.get(0).getMetadata().getDevice().getUdi()).isEqualTo(TEST_UDI);
    }

    @Test
    public void testRecordUpdate_changeUdi() throws InterruptedException {
        Device initialDevice =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setUdi(TEST_UDI)
                        .build();
        Metadata initialMetadata = new Metadata.Builder().setDevice(initialDevice).build();
        Instant now = Instant.now();
        StepsRecord initialRecord =
                mStepsRecordFactory.newEmptyRecord(initialMetadata, now, now.plusMillis(1000));

        List<Record> insertedRecords = TestUtils.insertRecords(List.of(initialRecord));
        String recordId = insertedRecords.get(0).getMetadata().getId();

        Device updatedDevice =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setUdi(ANOTHER_TEST_UDI)
                        .build();
        Metadata updatedMetadata =
                new Metadata.Builder().setId(recordId).setDevice(updatedDevice).build();
        StepsRecord updatedRecord =
                mStepsRecordFactory.newEmptyRecord(updatedMetadata, now, now.plusMillis(1000));

        TestUtils.updateRecords(List.of(updatedRecord));

        List<StepsRecord> readRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                .addId(recordId)
                                .build());
        assertThat(readRecords).hasSize(1);
        assertThat(readRecords.get(0).getMetadata().getDevice().getUdi())
                .isEqualTo(ANOTHER_TEST_UDI);
    }

    @Test
    public void testRecordUpdate_removeUdi() throws InterruptedException {
        Device initialDevice =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setUdi(TEST_UDI)
                        .build();
        Metadata initialMetadata = new Metadata.Builder().setDevice(initialDevice).build();
        Instant now = Instant.now();
        StepsRecord initialRecord =
                mStepsRecordFactory.newEmptyRecord(initialMetadata, now, now.plusMillis(1000));

        List<Record> insertedRecords = TestUtils.insertRecords(List.of(initialRecord));
        String recordId = insertedRecords.get(0).getMetadata().getId();

        Device updatedDevice =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setUdi(null)
                        .build();
        Metadata updatedMetadata =
                new Metadata.Builder().setId(recordId).setDevice(updatedDevice).build();
        StepsRecord updatedRecord =
                mStepsRecordFactory.newEmptyRecord(updatedMetadata, now, now.plusMillis(1000));

        TestUtils.updateRecords(List.of(updatedRecord));

        List<StepsRecord> readRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                .addId(recordId)
                                .build());
        assertThat(readRecords).hasSize(1);
        assertThat(readRecords.get(0).getMetadata().getDevice().getUdi()).isNull();
    }
}
