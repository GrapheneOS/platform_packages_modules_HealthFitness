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

package com.android.server.healthconnect.fitness;

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_DELETE;
import static android.healthconnect.testing.shared.DataFactory.buildDevice;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.RecordIdFiltersParcel;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.health.connect.internal.datatypes.RecordInternal;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.device.FakeSerialDeviceDataProviderManager;
import com.android.server.healthconnect.device.FakeSerialDeviceDataSourceHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class FitnessRecordDeleteHelperTest {
    private static final String TEST_PACKAGE_NAME = "package.name";
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private UserHandle mUserHandle;
    private AccessLogsHelper mAccessLogsHelper;
    private FitnessRecordDeleteHelper mFitnessRecordDeleteHelper;
    private InternalHealthConnectMappings mInternalHealthConnectMappings;
    private FitnessTestUtils mFitnessTestUtils;
    private FakeSerialDeviceDataProviderManager mDeviceDataProviderManager;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mUserHandle = context.getUser();
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mFitnessRecordDeleteHelper = spy(injector.getFitnessRecordDeleteHelper());
        mAccessLogsHelper = injector.getAccessLogsHelper();
        mInternalHealthConnectMappings = injector.getInternalHealthConnectMappings();
        mFitnessTestUtils = new FitnessTestUtils(injector);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            mDeviceDataProviderManager =
                    new FakeSerialDeviceDataProviderManager(
                            context,
                            injector.getDeviceInfoHelper(),
                            injector.getAppInfoHelper(),
                            new FakeSerialDeviceDataSourceHelper(),
                            injector.getDeviceDataSourcesHelper(),
                            injector.getDeviceDataProviderMetadataHelper(),
                            injector.getFitnessRecordUpsertHelper(),
                            injector.getFitnessRecordReadHelper(),
                            injector.getFitnessRecordDeleteHelper(),
                            injector.getSyntheticPackageNameCreator(),
                            true);
            mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        }
    }

    @Test
    public void deleteRecords_byIdFilter_generateChangeLogs() {
        List<String> uuids =
                mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, buildStepsRecord(123, 456, 100));
        List<RecordIdFilter> ids = List.of(RecordIdFilter.fromId(StepsRecord.class, uuids.get(0)));

        DeleteUsingFiltersRequestParcel request =
                new DeleteUsingFiltersRequestParcel(
                        new RecordIdFiltersParcel(ids), TEST_PACKAGE_NAME);
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                request,
                /* grantedPerRecordWritePermissions= */ Collections.emptySet(),
                /* enforceSelfDelete */ true,
                /* shouldRecordAccessLog= */ false);
        List<UUID> uuidList = mFitnessTestUtils.getAllDeletedUuids();
        assertThat(uuidList).hasSize(1);
        assertThat(uuidList.get(0).toString()).isEqualTo(uuids.get(0));
    }

    @Test
    public void deleteRecords_byTimeFilter_generateChangeLogs() {
        List<String> uuids =
                mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, buildStepsRecord(123, 456, 100));

        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* grantedPerRecordWritePermissions= */ Collections.emptySet(),
                /* enforceSelfDelete */ true,
                /* shouldRecordAccessLog= */ false);
        List<UUID> uuidList = mFitnessTestUtils.getAllDeletedUuids();
        assertThat(uuidList).hasSize(1);
        assertThat(uuidList.get(0).toString()).isEqualTo(uuids.get(0));
    }

    @Test
    public void deleteRecords_byTimeFilter_bulkDelete_generateChangeLogs() {
        ImmutableList.Builder<RecordInternal<?>> records = new ImmutableList.Builder<>();
        for (int i = 0; i <= DEFAULT_PAGE_SIZE; i++) {
            records.add(buildStepsRecord(i * 1000L, (i + 1) * 1000L, 9527));
        }
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, records.build());

        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* grantedPerRecordWritePermissions= */ Collections.emptySet(),
                /* enforceSelfDelete */ true,
                /* shouldRecordAccessLog= */ false);

        List<UUID> uuidList = mFitnessTestUtils.getAllDeletedUuids();
        assertThat(uuidList).hasSize(DEFAULT_PAGE_SIZE + 1);
    }

    @Test
    public void deleteRecords_shouldRecordAccessLog_logged() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* grantedPerRecordWritePermissions= */ Collections.emptySet(),
                /* enforceSelfDelete */ true,
                /* shouldRecordAccessLog= */ true);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(StepsRecord.class, HeartRateRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_DELETE);
    }

    @Test
    public void deleteRecords_shouldNotRecordAccessLog_noLog() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* grantedPerRecordWritePermissions= */ Collections.emptySet(),
                /* enforceSelfDelete */ true,
                /* shouldRecordAccessLog= */ false);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVICE_DATA_PROVIDERS_API, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
    public void deleteRecordsNonIdFilters_callingInternalDelete_doesNotAddDdpToDeleteRequests() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();

        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* grantedPerRecordWritePermissions= */ Collections.emptySet(),
                /* holdsDataManagementPermission */ false,
                /* shouldRecordAccessLog= */ false);

        ArgumentCaptor<List<RecordDeleteTableRequest>> requestCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mFitnessRecordDeleteHelper)
                .delete(any(), requestCaptor.capture(), any(), anyBoolean(), anyBoolean());
        requestCaptor
                .getValue()
                .forEach(
                        request ->
                                assertThat(request.getReadCommand())
                                        .doesNotContain(RecordHelper.DDP_ID_COLUMN_NAME));
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVICE_DATA_PROVIDERS_API, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
    public void deleteRecordsIdFilters_callingInternalDelete_doesNotAddDdpToDeleteRequests() {
        DeleteUsingFiltersRequestParcel deleteRequestParcel =
                new DeleteUsingFiltersRequestParcel(
                        new RecordIdFiltersParcel(
                                List.of(
                                        RecordIdFilter.fromId(
                                                StepsRecord.class, UUID.randomUUID().toString()))),
                        TEST_PACKAGE_NAME);

        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                deleteRequestParcel,
                /* grantedPerRecordWritePermissions= */ Collections.emptySet(),
                /* holdsDataManagementPermission */ false,
                /* shouldRecordAccessLog= */ false);

        ArgumentCaptor<List<RecordDeleteTableRequest>> requestCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mFitnessRecordDeleteHelper)
                .delete(any(), requestCaptor.capture(), any(), anyBoolean(), anyBoolean());
        requestCaptor
                .getValue()
                .forEach(
                        request ->
                                assertThat(request.getReadCommand())
                                        .doesNotContain(RecordHelper.DDP_ID_COLUMN_NAME));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deleteDeviceRecordsNonIdFilter_callingInternalDelete_addsDdpIdsToDeleteRequests() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();

        mFitnessRecordDeleteHelper.deleteDeviceRecords(
                TEST_PACKAGE_NAME,
                1234,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* grantedPerRecordWritePermissions= */ Collections.emptySet());

        ArgumentCaptor<List<RecordDeleteTableRequest>> requestCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mFitnessRecordDeleteHelper)
                .delete(any(), requestCaptor.capture(), any(), anyBoolean(), anyBoolean());
        requestCaptor
                .getValue()
                .forEach(
                        request ->
                                assertThat(request.getReadCommand())
                                        .contains(
                                                "AND "
                                                        + RecordHelper.DDP_ID_COLUMN_NAME
                                                        + " = '1234'"));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deleteDeviceRecordsIdFilter_callingInternalDelete_addsDdpIdsToDeleteRequests() {
        DeleteUsingFiltersRequestParcel deleteRequestParcel =
                new DeleteUsingFiltersRequestParcel(
                        new RecordIdFiltersParcel(
                                List.of(
                                        RecordIdFilter.fromId(
                                                StepsRecord.class, UUID.randomUUID().toString()))),
                        TEST_PACKAGE_NAME);
        deleteRequestParcel.setPackageNameFilters(List.of());

        mFitnessRecordDeleteHelper.deleteDeviceRecords(
                TEST_PACKAGE_NAME,
                1234,
                deleteRequestParcel,
                /* grantedPerRecordWritePermissions= */ Collections.emptySet());

        ArgumentCaptor<List<RecordDeleteTableRequest>> requestCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(mFitnessRecordDeleteHelper)
                .delete(any(), requestCaptor.capture(), any(), anyBoolean(), anyBoolean());
        requestCaptor
                .getValue()
                .forEach(
                        request ->
                                assertThat(request.getReadCommand())
                                        .contains(
                                                "AND "
                                                        + RecordHelper.DDP_ID_COLUMN_NAME
                                                        + " = '1234'"));
    }

    @Test
    public void deleteDeviceRecordsNonIdFilter_shouldNotRecordAccessLog_noLog() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        mFitnessRecordDeleteHelper.deleteDeviceRecords(
                TEST_PACKAGE_NAME,
                1,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* grantedPerRecordWritePermissions= */ Collections.emptySet());

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void deleteDeviceRecordsIdFilter_shouldNotRecordAccessLog_noLog() {
        DeleteUsingFiltersRequestParcel deleteRequestParcel =
                new DeleteUsingFiltersRequestParcel(
                        new RecordIdFiltersParcel(
                                List.of(
                                        RecordIdFilter.fromId(
                                                StepsRecord.class, UUID.randomUUID().toString()))),
                        TEST_PACKAGE_NAME);
        deleteRequestParcel.setPackageNameFilters(List.of());

        mFitnessRecordDeleteHelper.deleteDeviceRecords(
                TEST_PACKAGE_NAME,
                1,
                deleteRequestParcel,
                /* grantedPerRecordWritePermissions= */ Collections.emptySet());

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVICE_DATA_PROVIDERS_API, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
    public void deleteDeviceRecords_shouldEnforceSelfRead_setsPackageFiltersToDdpPackageName() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();

        mFitnessRecordDeleteHelper.deleteDeviceRecords(
                TEST_PACKAGE_NAME,
                1,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* grantedPerRecordWritePermissions= */ Collections.emptySet());

        ArgumentCaptor<DeleteUsingFiltersRequestParcel> parcelCaptor =
                ArgumentCaptor.forClass(DeleteUsingFiltersRequestParcel.class);
        verify(mFitnessRecordDeleteHelper)
                .deleteByNonIdFilter(any(), parcelCaptor.capture(), any(), anyBoolean(), anyLong());
        assertThat(parcelCaptor.getValue().getPackageNameFilters())
                .containsExactly(TEST_PACKAGE_NAME);
    }

    @Test
    @EnableFlags({Flags.FLAG_DEVICE_DATA_PROVIDERS_API, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
    public void deleteDeviceRecordsNonIdFilter_withDeviceRecord_deletesRecord() {
        String deviceId = "device";
        Device device = buildDevice();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisement =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisement);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), TEST_PACKAGE_NAME);
        mDeviceDataProviderManager.insertDeviceRecords(
                TEST_PACKAGE_NAME, deviceId, List.of(buildStepsRecord(123, 456, 100)));

        DeleteUsingFiltersRequest deleteRequest = new DeleteUsingFiltersRequest.Builder().build();

        long deviceAppInfoId =
                mDeviceDataProviderManager.getOrThrowAppInfoId(TEST_PACKAGE_NAME, deviceId);
        String deviceSpn =
                mDeviceDataProviderManager.getOrThrowSyntheticPackageName(deviceAppInfoId);
        int deletedRecords =
                mFitnessRecordDeleteHelper.deleteDeviceRecords(
                        deviceSpn,
                        1L,
                        new DeleteUsingFiltersRequestParcel(deleteRequest),
                        /* grantedPerRecordWritePermissions= */ Collections.emptySet());

        assertThat(deletedRecords).isEqualTo(1);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void deleteDeviceRecordsIdFilter_withDeviceRecord_deletesRecord() {
        String deviceId = "device";
        Device device = buildDevice();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisement =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisement);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), TEST_PACKAGE_NAME);
        String id =
                mDeviceDataProviderManager
                        .insertDeviceRecords(
                                TEST_PACKAGE_NAME,
                                deviceId,
                                List.of(buildStepsRecord(123, 456, 100)))
                        .get(0);

        DeleteUsingFiltersRequestParcel deleteRequestParcel =
                new DeleteUsingFiltersRequestParcel(
                        new RecordIdFiltersParcel(
                                List.of(RecordIdFilter.fromId(StepsRecord.class, id))),
                        TEST_PACKAGE_NAME);
        deleteRequestParcel.setPackageNameFilters(List.of());

        long deviceAppInfoId =
                mDeviceDataProviderManager.getOrThrowAppInfoId(TEST_PACKAGE_NAME, deviceId);
        String deviceSpn =
                mDeviceDataProviderManager.getOrThrowSyntheticPackageName(deviceAppInfoId);
        int deletedRecords =
                mFitnessRecordDeleteHelper.deleteDeviceRecords(
                        deviceSpn,
                        1L,
                        deleteRequestParcel,
                        /* grantedPerRecordWritePermissions= */ Collections.emptySet());

        assertThat(deletedRecords).isEqualTo(1);
    }

    @Test
    public void deleteDeviceRecords_withDataOrigin_throws() {
        DeleteUsingFiltersRequestParcel deleteRequestParcel =
                new DeleteUsingFiltersRequestParcel(
                        new DeleteUsingFiltersRequest.Builder()
                                .addDataOrigin(
                                        new DataOrigin.Builder().setPackageName("package").build())
                                .build());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mFitnessRecordDeleteHelper.deleteDeviceRecords(
                                TEST_PACKAGE_NAME,
                                1,
                                deleteRequestParcel,
                                /* grantedPerRecordWritePermissions= */ Collections.emptySet()));
    }

    @Test
    public void deleteRecordsUnrestricted() {
        RecordInternal<StepsRecord> stepsRecord = buildStepsRecord(123456, 654321, 123);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, stepsRecord);

        List<RecordInternal<?>> records =
                mFitnessTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                List.of(stepsRecord.getUuid())));
        assertThat(records).hasSize(1);

        RecordHelper<?> recordHelper =
                mInternalHealthConnectMappings.getRecordHelper(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS);
        RecordDeleteTableRequest deleteTableRequest =
                new RecordDeleteTableRequest(
                        new DeleteTableRequest(recordHelper.getMainTableName())
                                .setPackageFilter(RecordHelper.APP_INFO_ID_COLUMN_NAME, List.of())
                                .setIdColumnName(RecordHelper.UUID_COLUMN_NAME),
                        RecordTypeIdentifier.RECORD_TYPE_STEPS);
        mFitnessRecordDeleteHelper.deleteRecordsUnrestricted(List.of(deleteTableRequest));

        records =
                mFitnessTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                List.of(stepsRecord.getUuid())));
        assertThat(records).hasSize(0);
    }

    @Test
    public void deleteRecordsUnrestricted_noAccessLogs() {
        RecordHelper<?> recordHelper =
                mInternalHealthConnectMappings.getRecordHelper(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS);
        RecordDeleteTableRequest deleteTableRequest =
                new RecordDeleteTableRequest(
                        new DeleteTableRequest(recordHelper.getMainTableName())
                                .setPackageFilter(RecordHelper.APP_INFO_ID_COLUMN_NAME, List.of())
                                .setIdColumnName(RecordHelper.UUID_COLUMN_NAME),
                        RecordTypeIdentifier.RECORD_TYPE_STEPS);
        mFitnessRecordDeleteHelper.deleteRecordsUnrestricted(List.of(deleteTableRequest));

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }
}
