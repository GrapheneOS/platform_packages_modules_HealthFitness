/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.common.metadata;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_UNKNOWN;
import static android.healthconnect.testing.unittest.TaskUtils.TEST_USER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class DeviceInfoHelperTest {

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;

    private DeviceInfoHelper mDeviceInfoHelper;
    private TransactionManager mTransactionManager;
    private SyntheticPackageNameCreator mSyntheticPackageNameCreator;

    @Before
    public void setUp() {
        when(mContext.getUser()).thenReturn(TEST_USER);
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getApplicationContext()).thenReturn(mContext);

        HealthConnectContext hcContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        /* databaseDirName= */ null,
                        mEnvironmentDataDir.getRoot());
        mTransactionManager = spy(TransactionManager.create(hcContext));

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setTransactionManager(mTransactionManager)
                        .setAppOpLogsHelper(mock(AppOpLogsHelper.class))
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mDeviceInfoHelper = healthConnectInjector.getDeviceInfoHelper();
        mSyntheticPackageNameCreator = healthConnectInjector.getSyntheticPackageNameCreator();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void populateDeviceInfoId_deviceInfoNotInCache_insertsNewEntry() {
        RecordInternal<?> recordInternal = getStepsRecordInternal();

        mDeviceInfoHelper.populateDeviceInfoId(recordInternal);

        assertThat(recordInternal.getDeviceInfoId()).isEqualTo(1L);
        verify(mTransactionManager, times(1))
                .insertOrThrowOnConflict(any(UpsertTableRequest.class));
        verify(mTransactionManager, times(1)).read(any(ReadTableRequest.class));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void populateDeviceInfoId_deviceInfoInCache_doesNotInsert() {
        RecordInternal<?> recordInternal = getStepsRecordInternal();

        mDeviceInfoHelper.populateDeviceInfoId(recordInternal);
        mDeviceInfoHelper.populateDeviceInfoId(recordInternal);

        assertThat(recordInternal.getDeviceInfoId()).isEqualTo(1L);
        verify(mTransactionManager, times(1))
                .insertOrThrowOnConflict(any(UpsertTableRequest.class));
        verify(mTransactionManager, times(1)).read(any(ReadTableRequest.class));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void clearCache_cacheIsCleared_doesNotRewriteToDb() {
        RecordInternal<?> recordInternal = getStepsRecordInternal();
        mDeviceInfoHelper.populateDeviceInfoId(recordInternal);

        mDeviceInfoHelper.clearCache();

        mDeviceInfoHelper.populateDeviceInfoId(recordInternal);
        verify(mTransactionManager, times(2)).read(any(ReadTableRequest.class));
        verify(mTransactionManager, times(1))
                .insertOrThrowOnConflict(any(UpsertTableRequest.class));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void populateRecordWithValue_populatesRecordCorrectly() {
        RecordInternal<?> storedRecordInternal = getStepsRecordInternal();
        mDeviceInfoHelper.populateDeviceInfoId(storedRecordInternal);
        RecordInternal<?> retrievedRecordInternal = new StepsRecordInternal();

        mDeviceInfoHelper.populateRecordWithValue(1L, retrievedRecordInternal);

        assertThat(retrievedRecordInternal.getManufacturer()).isEqualTo("Google");
        assertThat(retrievedRecordInternal.getModel()).isEqualTo("Pixel");
        assertThat(retrievedRecordInternal.getDeviceType()).isEqualTo(DEVICE_TYPE_PHONE);
        assertThat(retrievedRecordInternal.getDisplayName()).isEqualTo("Pixel Phone");
    }

    @Test
    @DisableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void populateRecordWithValue_flagDisabled_doesNotPopulateEnhancedDeviceInfo() {
        RecordInternal<?> storedRecordInternal = getStepsRecordInternal();
        mDeviceInfoHelper.populateDeviceInfoId(storedRecordInternal);
        RecordInternal<?> retrievedRecordInternal = new StepsRecordInternal();

        mDeviceInfoHelper.populateRecordWithValue(1L, retrievedRecordInternal);

        assertThat(retrievedRecordInternal.getManufacturer()).isEqualTo("Google");
        assertThat(retrievedRecordInternal.getModel()).isEqualTo("Pixel");
        assertThat(retrievedRecordInternal.getDeviceType()).isEqualTo(DEVICE_TYPE_PHONE);
        assertThat(retrievedRecordInternal.getDisplayName()).isNull();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void deviceInfosAreEqual() {
        DeviceInfoHelper.DeviceInfo deviceInfo1 =
                new DeviceInfoHelper.DeviceInfo(
                        "Google", "Pixel", DEVICE_TYPE_PHONE, "pixel_id", "Pixel Phone");
        DeviceInfoHelper.DeviceInfo deviceInfo2 =
                new DeviceInfoHelper.DeviceInfo(
                        "Google", "Pixel", DEVICE_TYPE_PHONE, "pixel_id", "Pixel Phone");

        assertThat(deviceInfo1).isEqualTo(deviceInfo2);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void deviceInfosAreDifferent() {
        DeviceInfoHelper.DeviceInfo deviceInfo =
                new DeviceInfoHelper.DeviceInfo(
                        "Google", "Pixel", DEVICE_TYPE_PHONE, "pixel_id", "Pixel Phone");
        DeviceInfoHelper.DeviceInfo differentManufacturer =
                new DeviceInfoHelper.DeviceInfo(
                        "Test", "Pixel", DEVICE_TYPE_PHONE, "pixel_id", "Pixel Phone");
        DeviceInfoHelper.DeviceInfo differentModel =
                new DeviceInfoHelper.DeviceInfo(
                        "Google", "Test", DEVICE_TYPE_PHONE, "pixel_id", "Pixel Phone");
        DeviceInfoHelper.DeviceInfo differentDeviceType =
                new DeviceInfoHelper.DeviceInfo(
                        "Google", "Pixel", DEVICE_TYPE_UNKNOWN, "pixel_id", "Pixel Phone");
        DeviceInfoHelper.DeviceInfo differentId =
                new DeviceInfoHelper.DeviceInfo(
                        "Google", "Pixel", DEVICE_TYPE_PHONE, "Test", "Pixel Phone");
        DeviceInfoHelper.DeviceInfo differentDisplayName =
                new DeviceInfoHelper.DeviceInfo(
                        "Google", "Pixel", DEVICE_TYPE_PHONE, "pixel_id", "Test");

        assertThat(deviceInfo).isNotEqualTo(differentManufacturer);
        assertThat(deviceInfo).isNotEqualTo(differentModel);
        assertThat(deviceInfo).isNotEqualTo(differentDeviceType);
        assertThat(deviceInfo).isNotEqualTo(differentId);
        assertThat(deviceInfo).isNotEqualTo(differentDisplayName);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void getDeviceInfo_deviceIdNotInCache_returnsNull() {
        DeviceInfoHelper.DeviceInfo deviceInfo = mDeviceInfoHelper.getDeviceInfo(1);
        assertThat(deviceInfo).isNull();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void getDeviceInfoId_deviceInfoNotInCache_returnsNull() {
        DeviceInfoHelper.DeviceInfo nonExistentDeviceInfo =
                new DeviceInfoHelper.DeviceInfo(
                        "NonExistent", "Device", DEVICE_TYPE_UNKNOWN, null, "Non Existent Device");

        Long id = mDeviceInfoHelper.getDeviceInfoId(nonExistentDeviceInfo);

        assertThat(id).isNull();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void getDeviceInfoId_deviceInfoInCache_returnsCorrectId() {
        RecordInternal<?> recordInternal = getStepsRecordInternal();
        mDeviceInfoHelper.populateDeviceInfoId(recordInternal);
        DeviceInfoHelper.DeviceInfo existingDeviceInfo =
                new DeviceInfoHelper.DeviceInfo(
                        "Google", "Pixel", DEVICE_TYPE_PHONE, null, "Pixel Phone");

        Long id = mDeviceInfoHelper.getDeviceInfoId(existingDeviceInfo);

        assertThat(id).isEqualTo(1L);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API
    })
    public void getDeviceInfo_deviceIdInCache_returnsCorrectDeviceInfo() {
        DeviceInfoHelper.DeviceInfo deviceInfoWithId =
                new DeviceInfoHelper.DeviceInfo(
                        "Google", "Pixel", DEVICE_TYPE_PHONE, "pixel_id", "Pixel Phone");
        long deviceInfoId = mDeviceInfoHelper.insertIfNotPresent(deviceInfoWithId);

        DeviceInfoHelper.DeviceInfo retrievedDeviceInfo =
                mDeviceInfoHelper.getDeviceInfo(deviceInfoId);

        assertThat(retrievedDeviceInfo).isNotNull();
        assertThat(retrievedDeviceInfo.getManufacturer()).isEqualTo("Google");
        assertThat(retrievedDeviceInfo.getModel()).isEqualTo("Pixel");
        assertThat(retrievedDeviceInfo.getDeviceType()).isEqualTo(DEVICE_TYPE_PHONE);
        assertThat(retrievedDeviceInfo.getDeviceId()).isEqualTo("pixel_id");
        assertThat(retrievedDeviceInfo.getDisplayName()).isEqualTo("Pixel Phone");
    }

    private RecordInternal<?> getStepsRecordInternal() {
        StepsRecordInternal recordInternal = new StepsRecordInternal();
        recordInternal.setManufacturer("Google");
        recordInternal.setModel("Pixel");
        recordInternal.setDeviceType(DEVICE_TYPE_PHONE);
        recordInternal.setDisplayName("Pixel Phone");
        return recordInternal;
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB
    })
    public void populateDeviceInfoId_syntheticPackageNameAndDeviceInfoIdPresent_returnsEarly() {
        RecordInternal<?> recordInternal = getStepsRecordInternal();
        recordInternal.setPackageName(
                mSyntheticPackageNameCreator.createCanonical(DEVICE_TYPE_PHONE, "test_device_id"));
        recordInternal.setDeviceInfoId(100L);
        clearInvocations(mTransactionManager);

        mDeviceInfoHelper.populateDeviceInfoId(recordInternal);

        verify(mTransactionManager, times(0))
                .insertOrThrowOnConflict(any(UpsertTableRequest.class));
        verify(mTransactionManager, times(0)).read(any(ReadTableRequest.class));
        assertThat(recordInternal.getDeviceInfoId()).isEqualTo(100L);
    }
}
