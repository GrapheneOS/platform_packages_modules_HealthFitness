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

package com.android.server.healthconnect.exportimport;

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentValues;
import android.content.Context;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.device.DeviceDataProviderManager;
import com.android.server.healthconnect.device.DeviceDataSourceHelper;
import com.android.server.healthconnect.device.FakeSerialDeviceDataProviderManager;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@EnableFlags({Flags.FLAG_DEVICE_DATA_PROVIDERS_API, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
public class DdpDatabaseMergerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mTempFolder = new TemporaryFolder();

    private Context mContext;
    private HealthConnectInjector mTargetInjector;
    private DatabaseMerger mDatabaseMerger;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();

        mTargetInjector = createInjector("target");

        mTargetInjector.getDeviceDataProviderManager().initializeOrRefreshCurrentDeviceIds();
        String currentSpn =
                mTargetInjector.getDeviceDataProviderManager().getStableCurrentDeviceId();
        mTargetInjector
                .getAppInfoHelper()
                .addAppInfoIfNoAppInfoEntryExists(currentSpn, "Current Device");

        mDatabaseMerger =
                new DatabaseMerger(
                        mTargetInjector.getAppInfoHelper(),
                        mTargetInjector.getDeviceInfoHelper(),
                        mTargetInjector.getDeviceDataProviderMetadataHelper(),
                        mTargetInjector.getSyntheticPackageNameCreator(),
                        mTargetInjector.getHealthDataCategoryPriorityHelper(),
                        mTargetInjector.getTransactionManager(),
                        mTargetInjector.getFitnessRecordUpsertHelper(),
                        mTargetInjector.getFitnessRecordReadHelper());
    }

    private HealthConnectInjector createInjector(String name) throws Exception {
        File dir = mTempFolder.newFolder(name);
        HealthConnectInjector baseInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(dir)
                        .setDeviceDataSourceHelper(
                                new DeviceDataSourceHelper(() -> "test_device_serial"))
                        .build();

        DeviceDataProviderManager fakeSourceDdpManager =
                new FakeSerialDeviceDataProviderManager(
                        mContext,
                        baseInjector.getDeviceInfoHelper(),
                        baseInjector.getAppInfoHelper(),
                        new DeviceDataSourceHelper(() -> "test_device_serial"),
                        baseInjector.getDeviceDataSourcesHelper(),
                        baseInjector.getDeviceDataProviderMetadataHelper(),
                        baseInjector.getFitnessRecordUpsertHelper(),
                        baseInjector.getFitnessRecordReadHelper(),
                        baseInjector.getFitnessRecordDeleteHelper(),
                        baseInjector.getSyntheticPackageNameCreator(),
                        baseInjector.getPreferenceHelper(),
                        baseInjector.getHealthDataCategoryPriorityHelper(),
                        InternalHealthConnectMappings.getInstance(),
                        true);

        return HealthConnectInjectorImpl.newBuilderForTest(mContext)
                .setEnvironmentDataDirectory(dir)
                .setTransactionManager(baseInjector.getTransactionManager())
                .setAppInfoHelper(baseInjector.getAppInfoHelper())
                .setDeviceInfoHelper(baseInjector.getDeviceInfoHelper())
                .setDeviceDataSourcesHelper(baseInjector.getDeviceDataSourcesHelper())
                .setDeviceDataProviderMetadataHelper(
                        baseInjector.getDeviceDataProviderMetadataHelper())
                .setFitnessRecordUpsertHelper(baseInjector.getFitnessRecordUpsertHelper())
                .setFitnessReadRequestHandler(baseInjector.getFitnessRecordReadHelper())
                .setFitnessRecordDeleteHelper(baseInjector.getFitnessRecordDeleteHelper())
                .setSyntheticPackageNameCreator(baseInjector.getSyntheticPackageNameCreator())
                .setDeviceDataProviderManager(fakeSourceDdpManager)
                .setDeviceDataSourceHelper(new DeviceDataSourceHelper(() -> "test_device_serial"))
                .build();
    }

    @Test
    public void merge_spnRegenerated_success() throws Exception {
        // 1. Setup source database with an SPN and a record
        HealthConnectInjector sourceInjector = createInjector("source");
        sourceInjector.getDeviceDataProviderManager().initializeOrRefreshCurrentDeviceIds();

        String sourceDeviceId = "source_device_id";
        Device sourceDevice =
                new Device.Builder()
                        .setManufacturer("SourceManufacturer")
                        .setModel("SourceModel")
                        .setType(DEVICE_TYPE_PHONE)
                        .setDisplayName("Source Phone")
                        .build();
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(
                        sourceDevice,
                        sourceDeviceId,
                        Set.of(new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build()));
        String sourceDdpPackage = "com.source.ddp";
        sourceInjector
                .getDeviceDataProviderManager()
                .handleAdvertisement(Set.of(advertisement), sourceDdpPackage);

        StepsRecordInternal sourceRecord = (StepsRecordInternal) buildStepsRecord(1000, 2000, 100);
        sourceRecord.setUuid(UUID.randomUUID());
        List<String> uuids =
                sourceInjector
                        .getDeviceDataProviderManager()
                        .insertDeviceRecords(
                                sourceDdpPackage, sourceDeviceId, List.of(sourceRecord));
        UUID sourceGeneratedUuid = UUID.fromString(uuids.get(0));

        // 2. Perform Merge
        HealthConnectDatabase sourceDb =
                new HealthConnectDatabase(
                        HealthConnectContext.create(
                                mContext,
                                UserHandle.of(UserHandle.myUserId()),
                                null,
                                sourceInjector.getEnvironmentDataDirectory()));
        mDatabaseMerger.merge(sourceDb);

        // 3. Verify in target
        List<RecordInternal<?>> targetRecords =
                mTargetInjector
                        .getFitnessRecordReadHelper()
                        .readRecordsUnrestricted(
                                mTargetInjector.getTransactionManager(),
                                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                        .build()
                                        .toReadRecordsRequestParcel(),
                                null,
                                null)
                        .first;

        assertThat(targetRecords).hasSize(1);
        RecordInternal<?> mergedRecord = targetRecords.get(0);
        assertThat(mergedRecord.getUuid()).isEqualTo(sourceGeneratedUuid);

        long mergedAppInfoId = mergedRecord.getAppInfoId();
        String mergedPackageName =
                mTargetInjector.getAppInfoHelper().getPackageName(mergedAppInfoId);

        String expectedSpn =
                mTargetInjector
                        .getSyntheticPackageNameCreator()
                        .createCanonical(DEVICE_TYPE_PHONE, sourceDeviceId);
        assertThat(mergedPackageName).isEqualTo(expectedSpn);
        // Ensure it's not the same as source (which used a different salt)
        assertThat(mergedPackageName)
                .isNotEqualTo(
                        sourceInjector
                                .getSyntheticPackageNameCreator()
                                .createCanonical(DEVICE_TYPE_PHONE, sourceDeviceId));

        long mergedDeviceInfoId = mergedRecord.getDeviceInfoId();
        assertThat(mergedDeviceInfoId).isNotEqualTo(DEFAULT_LONG);

        DeviceInfoHelper.DeviceInfo mergedDeviceInfo =
                mTargetInjector.getDeviceInfoHelper().getDeviceInfo(mergedDeviceInfoId);
        assertThat(mergedDeviceInfo).isNotNull();
        assertThat(mergedDeviceInfo.getDeviceId()).isEqualTo(sourceDeviceId);
    }

    @Test
    @Ignore("Support for this will be added as part of the work in b/435165781")
    public void merge_androidLegacy_mappedToCurrentDevice() throws Exception {
        // 1. Setup source database with "android" package and a record
        HealthConnectInjector sourceInjector = createInjector("source_legacy");
        sourceInjector.getDeviceDataProviderManager().initializeOrRefreshCurrentDeviceIds();

        ContentValues values = new ContentValues();
        values.put(AppInfoHelper.PACKAGE_COLUMN_NAME, "android");
        values.put(AppInfoHelper.APPLICATION_COLUMN_NAME, "Android System");
        long sourceAppInfoId =
                sourceInjector
                        .getTransactionManager()
                        .insertOrThrowOnConflict(
                                new UpsertTableRequest(
                                        AppInfoHelper.TABLE_NAME,
                                        values,
                                        AppInfoHelper.UNIQUE_COLUMN_INFO));

        StepsRecordInternal sourceRecord = (StepsRecordInternal) buildStepsRecord(3000, 4000, 200);
        sourceRecord.setUuid(UUID.randomUUID());
        sourceRecord.setAppInfoId(sourceAppInfoId);
        sourceRecord.setPackageName("android");
        sourceInjector
                .getFitnessRecordUpsertHelper()
                .insertRecordsUnrestricted(List.of(sourceRecord), false);

        // 2. Perform Merge
        HealthConnectDatabase sourceDb =
                new HealthConnectDatabase(
                        HealthConnectContext.create(
                                mContext,
                                UserHandle.of(UserHandle.myUserId()),
                                null,
                                sourceInjector.getEnvironmentDataDirectory()));
        mDatabaseMerger.merge(sourceDb);

        // 3. Verify in target
        List<RecordInternal<?>> targetRecords =
                mTargetInjector
                        .getFitnessRecordReadHelper()
                        .readRecordsUnrestricted(
                                mTargetInjector.getTransactionManager(),
                                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                        .build()
                                        .toReadRecordsRequestParcel(),
                                null,
                                null)
                        .first;

        assertThat(targetRecords).hasSize(1);
        RecordInternal<?> mergedRecord = targetRecords.get(0);

        long mergedAppInfoId = mergedRecord.getAppInfoId();
        String mergedPackageName =
                mTargetInjector.getAppInfoHelper().getPackageName(mergedAppInfoId);

        String currentDeviceSpn =
                mTargetInjector.getDeviceDataProviderManager().getStableCurrentDeviceId();
        assertThat(mergedPackageName).isEqualTo(currentDeviceSpn);
    }

    @Test
    public void merge_ddpMetadata_translated() throws Exception {
        // 1. Setup source database with DDP metadata and a record
        HealthConnectInjector sourceInjector = createInjector("source_ddp");
        sourceInjector.getDeviceDataProviderManager().initializeOrRefreshCurrentDeviceIds();

        String ddpPackage = "com.example.ddp";
        String deviceId = "ddp_device_id";
        Device device =
                new Device.Builder()
                        .setManufacturer("DDP")
                        .setModel("DDP Model")
                        .setType(DEVICE_TYPE_PHONE)
                        .build();
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(
                        device,
                        deviceId,
                        Set.of(new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build()));
        sourceInjector
                .getDeviceDataProviderManager()
                .handleAdvertisement(Set.of(advertisement), ddpPackage);

        StepsRecordInternal sourceRecord = (StepsRecordInternal) buildStepsRecord(5000, 6000, 300);
        sourceRecord.setUuid(UUID.randomUUID());
        List<String> uuids =
                sourceInjector
                        .getDeviceDataProviderManager()
                        .insertDeviceRecords(ddpPackage, deviceId, List.of(sourceRecord));
        UUID sourceGeneratedUuid = UUID.fromString(uuids.get(0));

        // 2. Perform Merge
        HealthConnectDatabase sourceDb =
                new HealthConnectDatabase(
                        HealthConnectContext.create(
                                mContext,
                                UserHandle.of(UserHandle.myUserId()),
                                null,
                                sourceInjector.getEnvironmentDataDirectory()));
        mDatabaseMerger.merge(sourceDb);

        // 3. Verify in target
        List<RecordInternal<?>> targetRecords =
                mTargetInjector
                        .getFitnessRecordReadHelper()
                        .readRecordsUnrestricted(
                                mTargetInjector.getTransactionManager(),
                                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                        .build()
                                        .toReadRecordsRequestParcel(),
                                null,
                                null)
                        .first;

        assertThat(targetRecords).hasSize(1);
        RecordInternal<?> mergedRecord = targetRecords.get(0);
        assertThat(mergedRecord.getUuid()).isEqualTo(sourceGeneratedUuid);

        long mergedDdpId = mergedRecord.getDeviceDataProviderId();
        long expectedDdpId =
                mTargetInjector
                        .getDeviceDataProviderMetadataHelper()
                        .getDeviceDataProviderMetadataId(ddpPackage);
        assertThat(mergedDdpId).isEqualTo(expectedDdpId);
        assertThat(expectedDdpId).isNotEqualTo(-1);
    }

    @Test
    public void merge_ddpAdvertisements_ignored() throws Exception {
        // 1. Setup source database with a DDP advertisement
        HealthConnectInjector sourceInjector = createInjector("source_adv");
        sourceInjector.getDeviceDataProviderManager().initializeOrRefreshCurrentDeviceIds();

        String ddpPackage = "com.example.ddp";
        String deviceId = "ddp_dev_id";
        Device device =
                new Device.Builder()
                        .setManufacturer("DDP")
                        .setModel("DDP Model")
                        .setType(DEVICE_TYPE_PHONE)
                        .build();
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(
                        device,
                        deviceId,
                        Set.of(new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build()));

        sourceInjector
                .getDeviceDataProviderManager()
                .handleAdvertisement(Set.of(advertisement), ddpPackage);
        assertThat(sourceInjector.getDeviceDataSourcesHelper().getDdpMap()).isNotEmpty();

        // 2. Perform Merge
        HealthConnectDatabase sourceDb =
                new HealthConnectDatabase(
                        HealthConnectContext.create(
                                mContext,
                                UserHandle.of(UserHandle.myUserId()),
                                null,
                                sourceInjector.getEnvironmentDataDirectory()));
        mDatabaseMerger.merge(sourceDb);

        // 3. Verify in target
        // TODO once native advertisements merged, this will need to be updated.
        assertThat(mTargetInjector.getDeviceDataSourcesHelper().getDdpMap()).isEmpty();
    }

    @Test
    public void merge_sameDeviceIdAndType_recordsMergedToSamePackageName() throws Exception {
        String sharedDeviceId = "shared_device_id";
        Device sharedDevice =
                new Device.Builder()
                        .setManufacturer("SharedManufacturer")
                        .setModel("SharedModel")
                        .setType(DEVICE_TYPE_PHONE)
                        .setDisplayName("Shared Phone")
                        .build();
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(
                        sharedDevice,
                        sharedDeviceId,
                        Set.of(new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build()));
        String ddpPackage = "com.example.ddp";

        // 1. Advertise and insert in target
        mTargetInjector
                .getDeviceDataProviderManager()
                .handleAdvertisement(Set.of(advertisement), ddpPackage);
        StepsRecordInternal targetRecord = (StepsRecordInternal) buildStepsRecord(100, 200, 10);
        targetRecord.setUuid(UUID.randomUUID());
        mTargetInjector
                .getDeviceDataProviderManager()
                .insertDeviceRecords(ddpPackage, sharedDeviceId, List.of(targetRecord));

        // 2. Advertise and insert in source
        HealthConnectInjector sourceInjector = createInjector("source_same_device");
        sourceInjector.getDeviceDataProviderManager().initializeOrRefreshCurrentDeviceIds();
        sourceInjector
                .getDeviceDataProviderManager()
                .handleAdvertisement(Set.of(advertisement), ddpPackage);
        StepsRecordInternal sourceRecord = (StepsRecordInternal) buildStepsRecord(1000, 2000, 100);
        sourceRecord.setUuid(UUID.randomUUID());
        sourceInjector
                .getDeviceDataProviderManager()
                .insertDeviceRecords(ddpPackage, sharedDeviceId, List.of(sourceRecord));

        // 3. Perform Merge
        HealthConnectDatabase sourceDb =
                new HealthConnectDatabase(
                        HealthConnectContext.create(
                                mContext,
                                UserHandle.of(UserHandle.myUserId()),
                                null,
                                sourceInjector.getEnvironmentDataDirectory()));
        mDatabaseMerger.merge(sourceDb);

        // 4. Verify in target
        List<RecordInternal<?>> targetRecords =
                mTargetInjector
                        .getFitnessRecordReadHelper()
                        .readRecordsUnrestricted(
                                mTargetInjector.getTransactionManager(),
                                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                        .build()
                                        .toReadRecordsRequestParcel(),
                                null,
                                null)
                        .first;

        assertThat(targetRecords).hasSize(2);

        String targetSpn =
                mTargetInjector
                        .getSyntheticPackageNameCreator()
                        .createCanonical(DEVICE_TYPE_PHONE, sharedDeviceId);

        for (RecordInternal<?> record : targetRecords) {
            long appInfoId = record.getAppInfoId();
            String packageName = mTargetInjector.getAppInfoHelper().getPackageName(appInfoId);
            assertThat(packageName).isEqualTo(targetSpn);
        }
    }

    @Test
    public void merge_distinctDdpPackages_consistentDdpIds() throws Exception {
        String targetDdpPackage = "com.target.ddp";
        String targetDeviceId = "target_device_id";
        Device targetDevice =
                new Device.Builder()
                        .setManufacturer("TargetManufacturer")
                        .setModel("TargetModel")
                        .setType(DEVICE_TYPE_PHONE)
                        .build();
        DeviceDataAdvertisement targetAdvertisement =
                new DeviceDataAdvertisement(
                        targetDevice,
                        targetDeviceId,
                        Set.of(new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build()));

        // 1. Advertise and insert in target
        mTargetInjector
                .getDeviceDataProviderManager()
                .handleAdvertisement(Set.of(targetAdvertisement), targetDdpPackage);
        StepsRecordInternal targetRecord = (StepsRecordInternal) buildStepsRecord(100, 200, 10);
        targetRecord.setUuid(UUID.randomUUID());
        List<String> targetUuids =
                mTargetInjector
                        .getDeviceDataProviderManager()
                        .insertDeviceRecords(
                                targetDdpPackage, targetDeviceId, List.of(targetRecord));
        UUID targetGeneratedUuid = UUID.fromString(targetUuids.get(0));

        // 2. Setup source database with a different DDP and device
        String sourceDdpPackage = "com.source.ddp";
        String sourceDeviceId = "source_device_id";
        Device sourceDevice =
                new Device.Builder()
                        .setManufacturer("SourceManufacturer")
                        .setModel("SourceModel")
                        .setType(DEVICE_TYPE_PHONE)
                        .build();
        DeviceDataAdvertisement sourceAdvertisement =
                new DeviceDataAdvertisement(
                        sourceDevice,
                        sourceDeviceId,
                        Set.of(new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build()));

        HealthConnectInjector sourceInjector = createInjector("source_distinct_ddp");
        sourceInjector.getDeviceDataProviderManager().initializeOrRefreshCurrentDeviceIds();
        sourceInjector
                .getDeviceDataProviderManager()
                .handleAdvertisement(Set.of(sourceAdvertisement), sourceDdpPackage);
        StepsRecordInternal sourceRecord = (StepsRecordInternal) buildStepsRecord(1000, 2000, 100);
        sourceRecord.setUuid(UUID.randomUUID());
        List<String> sourceUuids =
                sourceInjector
                        .getDeviceDataProviderManager()
                        .insertDeviceRecords(
                                sourceDdpPackage, sourceDeviceId, List.of(sourceRecord));
        UUID sourceGeneratedUuid = UUID.fromString(sourceUuids.get(0));

        // 3. Perform Merge
        HealthConnectDatabase sourceDb =
                new HealthConnectDatabase(
                        HealthConnectContext.create(
                                mContext,
                                UserHandle.of(UserHandle.myUserId()),
                                null,
                                sourceInjector.getEnvironmentDataDirectory()));
        mDatabaseMerger.merge(sourceDb);

        // 4. Verify in target
        List<RecordInternal<?>> targetRecords =
                mTargetInjector
                        .getFitnessRecordReadHelper()
                        .readRecordsUnrestricted(
                                mTargetInjector.getTransactionManager(),
                                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                        .build()
                                        .toReadRecordsRequestParcel(),
                                null,
                                null)
                        .first;

        assertThat(targetRecords).hasSize(2);

        var ddpIdToMetadata =
                mTargetInjector
                        .getDeviceDataProviderMetadataHelper()
                        .getIdDeviceDataProviderMetadataMap();

        boolean foundTarget = false;
        boolean foundSource = false;

        for (RecordInternal<?> record : targetRecords) {
            long ddpId = record.getDeviceDataProviderId();
            String ddpPackageName = ddpIdToMetadata.get(ddpId).sourcePackageName();
            if (ddpPackageName.equals(targetDdpPackage)) {
                foundTarget = true;
                assertThat(record.getUuid()).isEqualTo(targetGeneratedUuid);
            } else if (ddpPackageName.equals(sourceDdpPackage)) {
                foundSource = true;
                assertThat(record.getUuid()).isEqualTo(sourceGeneratedUuid);
            }
        }

        assertThat(foundTarget).isTrue();
        assertThat(foundSource).isTrue();
    }
}
