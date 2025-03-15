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

package com.android.server.healthconnect.logging;

import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createBloodPressureRecord;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createSpeedRecordInternal;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.FhirVersion;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.phr.utils.ImmunizationBuilder;
import android.net.Uri;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.datatypehelpers.DatabaseStatsCollector;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.request.UpsertMedicalResourceInternalRequest;
import com.android.server.healthconnect.testing.fakes.FakeTimeSource;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
public class DatabaseStatsCollectorTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String PACKAGE_NAME = "com.my.package";
    private static final Instant INSTANT_NOW = Instant.now();

    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;

    private long mPackageAppInfoId;

    private TransactionTestUtils mTransactionTestUtils;
    private HealthConnectInjector mHealthConnectInjector;

    private DatabaseStatsCollector mDatabaseStatsCollector;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        FakeTimeSource mFakeTimeSource = new FakeTimeSource(INSTANT_NOW);
        mHealthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setTimeSource(mFakeTimeSource)
                        .setEnvironmentDataDirectory(mTemporaryFolder.getRoot())
                        .build();
        mTransactionTestUtils = new TransactionTestUtils(mHealthConnectInjector);
        mTransactionTestUtils.insertApp(PACKAGE_NAME);
        mPackageAppInfoId = mHealthConnectInjector.getAppInfoHelper().getAppInfoId(PACKAGE_NAME);

        mDatabaseStatsCollector = mHealthConnectInjector.getDatabaseStatsCollector();
    }

    @Test
    public void testGetFileBytes_noData_small() {
        assertThat(
                        mDatabaseStatsCollector.getFileBytes(
                                List.of(MedicalDataSourceHelper.getMainTableName())))
                .isLessThan(10_000L);
    }

    @Test
    public void testGetFileBytes_nonExistentTable_zero() {
        assertThat(mDatabaseStatsCollector.getFileBytes(List.of("foo"))).isEqualTo(0);
    }

    @Test
    public void testGetFileBytes_noTables_zero() {
        assertThat(mDatabaseStatsCollector.getFileBytes(List.of())).isEqualTo(0);
    }

    @Test
    public void testGetFileBytes_manyResources_plausibleSize() {
        // Insert a data source.
        MedicalDataSourceHelper dataSourceHelper =
                mHealthConnectInjector.getMedicalDataSourceHelper();
        FhirVersion fhirVersion = FhirVersion.parseFhirVersion("4.0.1");
        MedicalDataSource dataSource =
                dataSourceHelper.createMedicalDataSource(
                        new CreateMedicalDataSourceRequest.Builder(
                                        Uri.parse("http://fakebaseuri.com/"),
                                        "Hospital",
                                        fhirVersion)
                                .build(),
                        PACKAGE_NAME);
        // Insert 100 resources into that data source
        ArrayList<UpsertMedicalResourceInternalRequest> resourceRequests = new ArrayList<>();
        ImmunizationBuilder builder = new ImmunizationBuilder();
        for (int i = 0; i < 100; i++) {
            String resourceId = "imm" + i;
            builder.setId(resourceId);
            resourceRequests.add(
                    new UpsertMedicalResourceInternalRequest()
                            .setMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                            .setFhirResourceType(FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION)
                            .setFhirResourceId(resourceId)
                            .setDataSourceId(dataSource.getId())
                            .setFhirVersion(fhirVersion)
                            .setData(builder.toJson()));
        }
        MedicalResourceHelper resourceHelper = mHealthConnectInjector.getMedicalResourceHelper();
        resourceHelper.upsertMedicalResources(PACKAGE_NAME, resourceRequests);

        long tableSizeBytes =
                mDatabaseStatsCollector.getFileBytes(
                        List.of(MedicalResourceHelper.getMainTableName()));
        // We don't want to be too prescriptive about size, but the size should be of
        // the order of 1-200k for these resources.
        assertThat(tableSizeBytes).isGreaterThan(100_000);
        assertThat(tableSizeBytes).isLessThan(1_000_000);
    }

    @Test
    public void testGetFileBytes_multipleTables_sumOfTables() {
        long dataSourceTableBytes =
                mDatabaseStatsCollector.getFileBytes(
                        List.of(MedicalDataSourceHelper.getMainTableName()));
        long resourceTableBytes =
                mDatabaseStatsCollector.getFileBytes(
                        List.of(MedicalResourceHelper.getMainTableName()));

        assertThat(
                        mDatabaseStatsCollector.getFileBytes(
                                List.of(
                                        MedicalResourceHelper.getMainTableName(),
                                        MedicalDataSourceHelper.getMainTableName())))
                .isEqualTo(dataSourceTableBytes + resourceTableBytes);
    }

    @Test
    public void testChangeLogsTableDatabaseLogsStats() {
        mTransactionTestUtils.insertRecords(
                PACKAGE_NAME,
                createStepsRecord(
                        "client.id1",
                        /* startTimeMillis= */ 4000,
                        /* endTimeMillis= */ 4500,
                        /* stepsCount= */ 1000));
        mTransactionTestUtils.insertRecords(
                PACKAGE_NAME,
                createBloodPressureRecord(
                        /* appInfoId= */ mPackageAppInfoId,
                        /* timeMillis= */ 4000,
                        /* systolic= */ 120,
                        /* diastolic= */ 80));

        assertThat(mDatabaseStatsCollector.getNumberOfChangeLogs()).isEqualTo(2L);
    }

    @Test
    public void testIntervalRecordsTableDatabaseLogsStats() {
        mTransactionTestUtils.insertRecords(
                PACKAGE_NAME,
                createStepsRecord(
                        "client.id1",
                        /* startTimeMillis= */ 4000,
                        /* endTimeMillis= */ 4500,
                        /* stepsCount= */ 1000),
                createStepsRecord(
                        "client.id2",
                        /* startTimeMillis= */ 6000,
                        /* endTimeMillis= */ 7000,
                        /* stepsCount= */ 500));

        assertThat(mDatabaseStatsCollector.getNumberOfIntervalRecordRows()).isEqualTo(2L);
    }

    @Test
    public void testInstantRecordsTableDatabaseLogsStats() {
        mTransactionTestUtils.insertRecords(
                PACKAGE_NAME,
                createBloodPressureRecord(
                        /* appInfoId= */ mPackageAppInfoId,
                        /* timeMillis= */ 4000,
                        /* systolic= */ 120,
                        /* diastolic= */ 80));

        assertThat(mDatabaseStatsCollector.getNumberOfInstantRecordRows()).isEqualTo(1L);
    }

    @Test
    public void testSeriesRecordsTableDatabaseLogsStats() {
        mTransactionTestUtils.insertRecords(
                PACKAGE_NAME, createSpeedRecordInternal(/* startTine= */ INSTANT_NOW));
        mTransactionTestUtils.insertRecords(
                PACKAGE_NAME,
                createSpeedRecordInternal(/* startTine= */ Instant.now().minusSeconds(100)));

        assertThat(mDatabaseStatsCollector.getNumberOfSeriesRecordRows()).isEqualTo(2L);
    }

    @Test
    public void testGetDatabaseSizeDatabaseLogsStats() {
        mTransactionTestUtils.insertRecords(
                PACKAGE_NAME,
                createStepsRecord(
                        "client.id1",
                        /* startTimeMillis= */ 4000,
                        /* endTimeMillis= */ 4500,
                        /* stepsCount= */ 1000));

        assertThat(mDatabaseStatsCollector.getDatabaseSize()).isGreaterThan(0);
    }
}
