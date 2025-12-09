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
package android.healthconnect.cts.device;

import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;
import static android.healthconnect.testing.cts.TestUtils.advertiseDevice;
import static android.healthconnect.testing.cts.TestUtils.deleteDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.getAggregateResponseWithManagePermission;
import static android.healthconnect.testing.cts.TestUtils.getChangeLogToken;
import static android.healthconnect.testing.cts.TestUtils.getChangeLogs;
import static android.healthconnect.testing.cts.TestUtils.getContributorApplicationsInfo;
import static android.healthconnect.testing.cts.TestUtils.getCurrentDeviceId;
import static android.healthconnect.testing.cts.TestUtils.getDeviceDataSourceInfos;
import static android.healthconnect.testing.cts.TestUtils.getPriorityWithManageHealthDataPermission;
import static android.healthconnect.testing.cts.TestUtils.insertDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.isMaskedSyntheticPackageName;
import static android.healthconnect.testing.cts.TestUtils.queryAllRecordTypesInfo;
import static android.healthconnect.testing.cts.TestUtils.readAllRecords;
import static android.healthconnect.testing.cts.TestUtils.readDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.readRecords;
import static android.healthconnect.testing.cts.TestUtils.updateDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.updatePriorityWithManageHealthDataPermission;
import static android.healthconnect.testing.cts.TestUtils.verifyDeleteRecords;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;

import static com.android.compatibility.common.util.SystemUtil.getEventually;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.time.Instant.EPOCH;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.DeviceDataSourceInfo;
import android.health.connect.FetchDataOriginsPriorityOrderResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthDataCategory;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.RecordIdFilter;
import android.health.connect.RecordTypeInfoResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.TimeRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.AppInfo;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({Flags.FLAG_DEVICE_DATA_PROVIDERS_API, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
public class MaskingTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    /**
     * The unique identifier for this device, primarily used during the initial advertisement phase.
     *
     * <p>This ID is essential for identifying the device within DDP specific API calls. It serves
     * as the persistent handle for the device.
     */
    private String mDeviceId = "testId";

    /**
     * The synthetic, masked representation of the advertised device's package name that is exposed
     * to external clients.
     *
     * <p>This variable acts as the "golden value" for verification. When testing API responses, you
     * must assert that the returned package name matches this value.
     *
     * <p><strong>Security Warning:</strong> If the package name received by a client differs from
     * this value (e.g., if it matches the internal, canonical representation) it indicates that
     * masking has failed.
     */
    private String mMaskedDeviceName;

    private String mInsertedRecordId;

    @Before
    public void before() throws Exception {
        TestUtils.deleteAllDataFromHealthConnect();
        insertDeviceDataAndInitializeIdentifiers();
    }

    @After
    public void after() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test
    public void getCurrentPriority_updatePriority_masks() throws InterruptedException {
        updatePriorityWithManageHealthDataPermission(
                HealthDataCategory.ACTIVITY, List.of(mMaskedDeviceName));

        FetchDataOriginsPriorityOrderResponse response =
                getPriorityWithManageHealthDataPermission(HealthDataCategory.ACTIVITY);

        DataOrigin maskedOrigin =
                new DataOrigin.Builder().setPackageName(mMaskedDeviceName).build();

        // DataOrigins contains exactly one source and it's masked
        assertThat(response.getDataOriginsPriorityOrder()).isNotNull();
        assertThat(response.getDataOriginsPriorityOrder()).containsExactly(maskedOrigin);
    }

    @Test
    public void getContributorApplicationsInfo_masks() throws InterruptedException {
        List<AppInfo> response = getContributorApplicationsInfo();

        // response contains exactly one info and it's masked
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getPackageName()).isEqualTo(mMaskedDeviceName);
    }

    @Test
    public void queryAllRecordTypesInfo_masks() throws Exception {
        Map<Class<? extends Record>, RecordTypeInfoResponse> eventualResponse =
                getEventually(
                        () -> {
                            Map<Class<? extends Record>, RecordTypeInfoResponse> response =
                                    queryAllRecordTypesInfo();
                            assertThat(response.get(StepsRecord.class).getContributingPackages())
                                    .hasSize(1);
                            return response;
                        });

        // response contains exactly one info and it's masked
        DataOrigin maskedOrigin =
                new DataOrigin.Builder().setPackageName(mMaskedDeviceName).build();
        assertThat(eventualResponse.get(StepsRecord.class).getContributingPackages())
                .containsExactly(maskedOrigin);
    }

    @Test
    public void getDeviceDataSourceInfos_masks() throws InterruptedException {
        List<DeviceDataSourceInfo> response = getDeviceDataSourceInfos();

        // response contains exactly one info and it's masked
        assertThat(response).hasSize(1);
        assertThat(
                        isMaskedSyntheticPackageName(
                                response.get(0).getDeviceDataOrigin().getPackageName()))
                .isTrue();
    }

    @Test
    public void recordMatchmakingDenial_masks() {
        // See {@link HealthConnectServiceImplTest#recordMatchmakingDenial_withMaskedNames_unmasks}
    }

    @Test
    public void aggregateRecords_masks() throws InterruptedException {
        TestUtils.setupAggregation(mMaskedDeviceName, HealthDataCategory.ACTIVITY);
        TimeRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(EPOCH)
                        .setEndTime(Instant.now())
                        .build();
        DataOrigin maskedOrigin =
                new DataOrigin.Builder().setPackageName(mMaskedDeviceName).build();
        AggregateRecordsRequest<Long> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Long>(timeRangeFilter)
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .addDataOriginsFilter(maskedOrigin)
                        .build();

        AggregateRecordsResponse<Long> response =
                getAggregateResponseWithManagePermission(aggregateRecordsRequest);

        // Unmasks and applies filter for the device data
        assertThat(response.get(STEPS_COUNT_TOTAL)).isNotNull();
        assertThat(response.get(STEPS_COUNT_TOTAL)).isEqualTo(1000L);

        // DataOrigins contains exactly one source and it's masked
        assertThat(response.getDataOrigins(STEPS_COUNT_TOTAL)).isNotNull();
        assertThat(response.getDataOrigins(STEPS_COUNT_TOTAL)).containsExactly(maskedOrigin);
    }

    @Test
    public void readRecords_masks() throws InterruptedException {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .addDataOrigins(
                                new DataOrigin.Builder().setPackageName(mMaskedDeviceName).build())
                        .build();
        List<StepsRecord> readRecords = readRecords(request);

        // Unmasks and applies filter for the device data
        assertThat(readRecords).hasSize(1);
        assertThat(readRecords.get(0).getCount()).isEqualTo(1000);

        // The single record of the response is masked
        DataOrigin maskedOrigin =
                new DataOrigin.Builder().setPackageName(mMaskedDeviceName).build();
        assertThat(readRecords.get(0).getMetadata().getDataOrigin()).isEqualTo(maskedOrigin);
    }

    @Test
    public void readRecords_withUnknownMaskedSPN_returnsNone() throws InterruptedException {
        String unknownMasked = "com.android.healthconnect.scale.j00e7797d540a3800867a1e33d194a303";

        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .addDataOrigins(
                                new DataOrigin.Builder().setPackageName(unknownMasked).build())
                        .build();
        List<StepsRecord> readRecords = readRecords(request);

        assertThat(readRecords).isEmpty();
    }

    @Test
    public void readDeviceRecords_masks() throws InterruptedException {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId(mDeviceId)
                        .build();
        List<StepsRecord> readRecords = readDeviceRecords(request);

        // Applies filter for the device data
        assertThat(readRecords).hasSize(1);
        assertThat(readRecords.get(0).getCount()).isEqualTo(1000);

        // The single record of the response is masked
        DataOrigin maskedOrigin =
                new DataOrigin.Builder().setPackageName(mMaskedDeviceName).build();
        assertThat(readRecords.get(0).getMetadata().getDataOrigin()).isEqualTo(maskedOrigin);
    }

    @Test
    public void getChangeLogToken_getChangeLogs_queryAccessLogs_masks()
            throws InterruptedException {
        ChangeLogTokenResponse tokenResponse =
                getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(mMaskedDeviceName)
                                                .build())
                                .addRecordType(StepsRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest =
                new ChangeLogsRequest.Builder(tokenResponse.getToken()).build();
        ChangeLogsResponse changeLogsResponse = getChangeLogs(changeLogsRequest);

        assertThat(changeLogsResponse.getUpsertedRecords()).isEmpty();

        insertDeviceRecords(
                mDeviceId,
                List.of(
                        getStepsRecord(
                                1000,
                                Instant.now().minus(1, HOURS),
                                Instant.now().minus(1, MINUTES))));
        changeLogsResponse = getChangeLogs(changeLogsRequest);

        // Unmasks and applies filter for the device data
        assertThat(changeLogsResponse.getUpsertedRecords()).hasSize(1);

        // The single upserted record is masked
        DataOrigin maskedOrigin =
                new DataOrigin.Builder().setPackageName(mMaskedDeviceName).build();
        assertThat(changeLogsResponse.getUpsertedRecords().get(0).getMetadata().getDataOrigin())
                .isEqualTo(maskedOrigin);
    }

    // Reason: Only the deleteDeviceRecords API is allowed to delete device data
    @Test
    public void deleteRecords_usingPackageNameFilters_doesNotUnmask_doesNotDelete()
            throws InterruptedException {
        DataOrigin maskedOrigin =
                new DataOrigin.Builder().setPackageName(mMaskedDeviceName).build();

        DeleteUsingFiltersRequest request =
                new DeleteUsingFiltersRequest.Builder().addDataOrigin(maskedOrigin).build();

        assertThat(readAllRecords(StepsRecord.class)).hasSize(1);

        verifyDeleteRecords(request);

        assertThat(readAllRecords(StepsRecord.class)).hasSize(1);
    }

    // Reason: Only the deleteDeviceRecords API is allowed to delete device data
    @Test
    public void deleteRecords_usingDeviceIds_throws() throws InterruptedException {
        List<RecordIdFilter> recordIds =
                Collections.singletonList(
                        RecordIdFilter.fromId(StepsRecord.class, mInsertedRecordId));

        assertThat(readAllRecords(StepsRecord.class)).hasSize(1);

        assertThrows(HealthConnectException.class, () -> verifyDeleteRecords(recordIds));
    }

    // Reason: Only the deleteDeviceRecords API is allowed to delete device data
    @Test
    public void deleteRecords_usingTypeFilter_doesNotDelete() throws InterruptedException {
        assertThat(readAllRecords(StepsRecord.class)).hasSize(1);

        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(EPOCH)
                        .setEndTime(Instant.now())
                        .build();
        verifyDeleteRecords(StepsRecord.class, timeRangeFilter);

        assertThat(readAllRecords(StepsRecord.class)).hasSize(1);
    }

    @Test
    public void
            advertiseDeviceDataSources_insertDeviceRecords_readDeviceRecords_currentDevice_masks()
                    throws InterruptedException {
        String currentDeviceId = getCurrentDeviceId();
        advertiseDevice(currentDeviceId, StepsRecord.class);
        String maskedSPN = getDeviceDataSourceInfos().get(0).getDeviceDataOrigin().getPackageName();

        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = start.plus(1, HOURS);
        List<StepsRecord> records = List.of(getStepsRecord(1234, start, end));
        insertDeviceRecords(currentDeviceId, records);

        List<StepsRecord> readRecords =
                TestUtils.readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(currentDeviceId)
                                .build());

        assertThat(readRecords).hasSize(1);
        assertThat(readRecords.get(0).getCount()).isEqualTo(1234);
        assertThat(readRecords.get(0).getMetadata().getDataOrigin().getPackageName())
                .isEqualTo(maskedSPN);
    }

    @Test
    public void updateDeviceRecords_deleteDeviceRecords_currentDevice_masks()
            throws InterruptedException {
        String currentDeviceId = getCurrentDeviceId();
        advertiseDevice(currentDeviceId, StepsRecord.class);
        String maskedSPN = getDeviceDataSourceInfos().get(0).getDeviceDataOrigin().getPackageName();

        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = start.plus(1, HOURS);
        List<StepsRecord> records = List.of(getStepsRecord(1234, start, end));
        String insertedId =
                insertDeviceRecords(currentDeviceId, records).get(0).getMetadata().getId();

        List<StepsRecord> readRecords =
                TestUtils.readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(currentDeviceId)
                                .build());

        assertThat(readRecords).hasSize(1);

        StepsRecord updateRecord =
                getStepsRecord(50, new Metadata.Builder().setId(insertedId).build());
        updateDeviceRecords(currentDeviceId, List.of(updateRecord));

        List<StepsRecord> updatedReadRecords =
                TestUtils.readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(currentDeviceId)
                                .build());

        assertThat(updatedReadRecords).hasSize(1);
        assertThat(updatedReadRecords.get(0).getCount()).isEqualTo(50);
        assertThat(updatedReadRecords.get(0).getMetadata().getDataOrigin().getPackageName())
                .isEqualTo(maskedSPN);

        deleteDeviceRecords(
                currentDeviceId,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder().setStartTime(start).build());

        List<StepsRecord> deletedReadRecords =
                TestUtils.readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(currentDeviceId)
                                .build());

        assertThat(deletedReadRecords).isEmpty();
    }

    @Test
    public void getCurrentDeviceId_masks() throws InterruptedException {
        String deviceId = getCurrentDeviceId();

        assertThat(isMaskedSyntheticPackageName(deviceId)).isTrue();
    }

    @Test
    public void getDeviceDataSources_masks() throws InterruptedException {
        TestUtils.verifyGetDeviceDataSourcesWithPermission(
                android.health.connect.HealthPermissions.READ_STEPS,
                dataSources -> {
                    assertThat(dataSources).hasSize(1);
                    assertThat(dataSources.get(0).getDeviceDataOrigin().getPackageName())
                            .isEqualTo(mMaskedDeviceName);
                });
    }

    private void insertDeviceDataAndInitializeIdentifiers() throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);
        Instant start = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant end = start.plus(1, HOURS);
        List<StepsRecord> records = List.of(getStepsRecord(1000, start, end));
        mInsertedRecordId = insertDeviceRecords(mDeviceId, records).get(0).getMetadata().getId();
        mMaskedDeviceName =
                getDeviceDataSourceInfos().get(0).getDeviceDataOrigin().getPackageName();
    }
}
