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

package com.android.server.healthconnect.device;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;

import static com.android.server.healthconnect.device.DeviceRecordHelper.DEVICE_DATA_PROVIDER_PACKAGE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.healthconnect.testing.unittest.mocks.AndroidPackageMocker;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;

import com.google.common.collect.Iterables;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class DeviceRecordHelperTest {

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;
    @Mock private Context mContext;

    private static final String TEST_PACKAGE_NAME = "package.name";
    private UserHandle mUserHandle;
    private DeviceRecordHelper mDeviceRecordHelper;
    private AppInfoHelper mAppInfoHelper;
    private AccessLogsHelper mAccessLogsHelper;
    private ChangeLogsHelper mChangeLogsHelper;
    private ChangeLogsRequestHelper mChangeLogsRequestHelper;
    private FitnessTestUtils mFitnessTestUtils;

    private static final DeviceDataSource TEST_DEVICE_DATA_SOURCE =
            new DeviceDataSource(
                    /* manufacturer= */ "Acme Corp.",
                    /* model= */ "FabPhone",
                    /* type= */ DEVICE_TYPE_PHONE,
                    /* deviceId= */ "a25341c4-c39a-4605-acc6-d4a6bc903413",
                    /* displayName= */ "My mobile phone");

    private static final Instant NOW = Instant.ofEpochMilli(1742835562527L);

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        mContext = spy(InstrumentationRegistry.getInstrumentation().getContext());
        AndroidPackageMocker.addToContext(mContext);
        mUserHandle = mContext.getUser();
        DeviceDataSourceHelper deviceDataSourceHelper = new FakeSerialDeviceDataSourceHelper();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setDeviceDataSourceHelper(deviceDataSourceHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mDeviceRecordHelper = healthConnectInjector.getDeviceRecordHelper();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();
        mChangeLogsHelper = healthConnectInjector.getChangeLogsHelper();
        mChangeLogsRequestHelper = healthConnectInjector.getChangeLogsRequestHelper();
        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);

        FitnessTestUtils fitnessTestUtils = new FitnessTestUtils(healthConnectInjector);
        fitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void insertRecords_packageNameSetToAndroid() {
        mDeviceRecordHelper.insertRecords(
                TEST_DEVICE_DATA_SOURCE,
                List.of(
                        createDeviceStepsRecord(
                                NOW.minusMillis(5_000).toEpochMilli(), NOW.toEpochMilli(), 7)));

        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        StepsRecordInternal record = (StepsRecordInternal) records.get(0);
        assertThat(record.getPackageName()).isEqualTo("android");
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void insertRecords_recordValuesCorrect() {
        mDeviceRecordHelper.insertRecords(
                TEST_DEVICE_DATA_SOURCE,
                List.of(
                        createDeviceStepsRecord(
                                NOW.minusMillis(5_000).toEpochMilli(), NOW.toEpochMilli(), 7)));

        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        StepsRecordInternal record = (StepsRecordInternal) records.get(0);
        assertThat(record.getStartTimeInMillis()).isEqualTo(NOW.minusMillis(5_000).toEpochMilli());
        assertThat(record.getEndTimeInMillis()).isEqualTo(NOW.toEpochMilli());
        assertThat(record.getCount()).isEqualTo(7);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void insertRecords_deviceMetadataCorrect() {
        mDeviceRecordHelper.insertRecords(
                TEST_DEVICE_DATA_SOURCE,
                List.of(
                        createDeviceStepsRecord(
                                NOW.minusMillis(5_000).toEpochMilli(), NOW.toEpochMilli(), 7)));

        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(1);
        StepsRecordInternal record = (StepsRecordInternal) records.get(0);
        assertThat(record.getDeviceType()).isEqualTo(DEVICE_TYPE_PHONE);
        assertThat(record.getManufacturer()).isEqualTo("Acme Corp.");
        assertThat(record.getModel()).isEqualTo("FabPhone");
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void insertRecords_noAccessLogsGenerated() {
        mDeviceRecordHelper.insertRecords(
                TEST_DEVICE_DATA_SOURCE,
                List.of(
                        createDeviceStepsRecord(
                                NOW.minusMillis(5_000).toEpochMilli(), NOW.toEpochMilli(), 7)));

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);

        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void insertRecords_changelogsGenerated() {
        // Get the row ID before insertion to fetch logs generated after this point
        long initialChangeLogRowId = mChangeLogsHelper.getLatestRowId();

        mDeviceRecordHelper.insertRecords(
                TEST_DEVICE_DATA_SOURCE,
                List.of(
                        createDeviceStepsRecord(
                                NOW.minusMillis(5_000).toEpochMilli(), NOW.toEpochMilli(), 7)));

        List<RecordInternal<?>> insertedRecords =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);
        assertThat(insertedRecords).hasSize(1);
        UUID insertedUuid = insertedRecords.get(0).getUuid();

        ChangeLogsRequestHelper.TokenRequest tokenRequestState =
                new ChangeLogsRequestHelper.TokenRequest(
                        /* packageNamesToFilter= */ Collections.emptyList(),
                        /* recordTypes= */ Collections.emptyList(),
                        /* medicalResourceTypes= */ Collections.emptyList(),
                        /* requestingPackageName= */ TEST_PACKAGE_NAME,
                        /* rowIdChangeLogs= */ initialChangeLogRowId);

        ChangeLogsRequest changeLogsRequest =
                new ChangeLogsRequest.Builder(
                                mChangeLogsRequestHelper.getToken(
                                        mChangeLogsHelper.getLatestRowId(),
                                        DEVICE_DATA_PROVIDER_PACKAGE,
                                        new ChangeLogTokenRequest.Builder()
                                                .addRecordType(StepsRecord.class)
                                                .build()))
                        .build();

        ChangeLogsHelper.ChangeLogsResponse changeLogsResponse =
                mChangeLogsHelper.getChangeLogs(
                        mAppInfoHelper,
                        tokenRequestState,
                        changeLogsRequest,
                        mChangeLogsRequestHelper);

        assertThat(
                        Iterables.getOnlyElement(
                                changeLogsResponse.getRecordTypeToUpsertedUuids().values()))
                .containsExactly(insertedUuid);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void insertRecords_multipleRecords_insertedCorrectly() {
        Instant time1 = NOW.minusMillis(15_000);
        Instant time2 = NOW.minusMillis(10_000);
        Instant time3 = NOW.minusMillis(5_000);

        List<RecordInternal<StepsRecord>> recordsToInsert =
                List.of(
                        createDeviceStepsRecord(time1.toEpochMilli(), time2.toEpochMilli(), 5),
                        createDeviceStepsRecord(time2.toEpochMilli(), time3.toEpochMilli(), 10),
                        createDeviceStepsRecord(time3.toEpochMilli(), NOW.toEpochMilli(), 15));

        mDeviceRecordHelper.insertRecords(TEST_DEVICE_DATA_SOURCE, recordsToInsert);

        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).hasSize(3);
        assertThat(records.stream().map(r -> ((StepsRecordInternal) r).getCount()).toList())
                .containsExactly(5, 10, 15);
        assertThat(records.stream().map(RecordInternal::getPackageName).distinct().toList())
                .containsExactly(DEVICE_DATA_PROVIDER_PACKAGE);
        assertThat(records.stream().map(RecordInternal::getManufacturer).distinct().toList())
                .containsExactly(TEST_DEVICE_DATA_SOURCE.getManufacturer());
        assertThat(records.stream().map(RecordInternal::getModel).distinct().toList())
                .containsExactly(TEST_DEVICE_DATA_SOURCE.getModel());
    }

    @Test
    @DisableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void flagDisabled_noRecordsInserted() {
        mDeviceRecordHelper.insertRecords(
                TEST_DEVICE_DATA_SOURCE,
                List.of(
                        createDeviceStepsRecord(
                                NOW.minusMillis(5_000).toEpochMilli(), NOW.toEpochMilli(), 7)));

        List<RecordInternal<?>> records =
                mFitnessTestUtils.readAllRecordsOfType(TEST_PACKAGE_NAME, StepsRecord.class);

        assertThat(records).isEmpty();
    }

    private RecordInternal<StepsRecord> createDeviceStepsRecord(
            long startTimeMillis, long endTimeMillis, int stepsCount) {
        return new StepsRecordInternal()
                .setCount(stepsCount)
                .setStartTime(startTimeMillis)
                .setEndTime(endTimeMillis);
    }
}
