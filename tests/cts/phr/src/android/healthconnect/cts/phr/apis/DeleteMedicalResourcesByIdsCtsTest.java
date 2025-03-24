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

package android.healthconnect.cts.phr.apis;

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.MAX_FOREGROUND_WRITE_CALL_15M;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_BACKGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_FOREGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_RESOURCE_ID_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalResourceId;
import static android.healthconnect.cts.utils.DataFactory.MAXIMUM_PAGE_SIZE;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermission;
import static android.healthconnect.cts.utils.PermissionHelper.revokeAllHealthPermissions;
import static android.healthconnect.cts.utils.TestUtils.finishMigrationWithShellPermissionIdentity;
import static android.healthconnect.cts.utils.TestUtils.setFieldValueUsingReflection;
import static android.healthconnect.cts.utils.TestUtils.startMigrationWithShellPermissionIdentity;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.MedicalResourceId;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.HealthConnectReceiver;
import android.healthconnect.cts.utils.TestUtils;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class DeleteMedicalResourcesByIdsCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private HealthConnectManager mManager;
    private PhrCtsTestUtils mUtil;

    @Before
    public void setUp() throws Exception {
        revokeAllHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(), "to test specific permissions");
        revokeAllHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(), "to test specific permissions");
        TestUtils.deleteAllStagedRemoteData();
        mManager = TestUtils.getHealthConnectManager();
        mUtil = new PhrCtsTestUtils(mManager);
        mUtil.deleteAllMedicalData();
        if (TestUtils.setLowerRateLimitsForTesting(true)) {
            mUtil.mLimitsAdjustmentForTesting = 10;
        }
    }

    @After
    public void after() throws InterruptedException {
        mUtil.deleteAllMedicalData();
        TestUtils.setLowerRateLimitsForTesting(false);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_migrationInProgress_apiBlocked()
            throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        startMigrationWithShellPermissionIdentity();
        mManager.deleteMedicalResources(List.of(getMedicalResourceId()), executor, receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        finishMigrationWithShellPermissionIdentity();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_writeLimitExceeded_throws() throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        // Make the maximum number of calls allowed by quota
        int maximumCalls = MAX_FOREGROUND_WRITE_CALL_15M / mUtil.mLimitsAdjustmentForTesting - 1;
        float remainingQuota = mUtil.tryAcquireCallQuotaNTimesForWrite(dataSource, maximumCalls);

        // Exceed the quota by using up any remaining quota that accumulated during the previous
        // calls and make one additional call.
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        int additionalCalls = (int) Math.ceil(remainingQuota) + 1;
        for (int i = 0; i < additionalCalls; i++) {
            mManager.deleteMedicalResources(
                    List.of(getMedicalResourceId()), Executors.newSingleThreadExecutor(), callback);
        }

        HealthConnectException exception = callback.assertAndGetException();
        assertThat(exception.getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED);
        assertThat(exception.getMessage()).contains("API call quota exceeded");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_invalidResourceTypeInIdByReflection_throws()
            throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        MedicalResourceId id =
                new MedicalResourceId(
                        DATA_SOURCE_ID,
                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_RESOURCE_ID_IMMUNIZATION);

        setFieldValueUsingReflection(id, "mFhirResourceType", 100);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.deleteMedicalResources(
                                List.of(id),
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
        // Test resource is still present.
        mManager.readMedicalResources(
                List.of(resource.getId()), Executors.newSingleThreadExecutor(), readReceiver);
        assertThat(readReceiver.getResponse()).containsExactly(resource);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByIds_invalidDataSourceIdInIdByReflection_throwsNoDelete()
            throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        MedicalResourceId id =
                new MedicalResourceId(
                        DATA_SOURCE_ID,
                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_RESOURCE_ID_IMMUNIZATION);

        setFieldValueUsingReflection(id, "mDataSourceId", "invalid id");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.deleteMedicalResources(
                                List.of(id),
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
        // Test resource is still present.
        mManager.readMedicalResources(
                List.of(resource.getId()), Executors.newSingleThreadExecutor(), readReceiver);
        assertThat(readReceiver.getResponse()).containsExactly(resource);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_idsNonExistent_succeeds()
            throws InterruptedException {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        List<MedicalResourceId> ids = new ArrayList<>(MAXIMUM_PAGE_SIZE + 1);
        for (int i = 0; i < MAXIMUM_PAGE_SIZE + 1; i++) {
            ids.add(
                    new MedicalResourceId(
                            dataSource.getId(),
                            FHIR_RESOURCE_TYPE_IMMUNIZATION,
                            FHIR_RESOURCE_ID_IMMUNIZATION + "." + i));
        }
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        mManager.deleteMedicalResources(ids, Executors.newSingleThreadExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_anIdMissing_succeeds() throws InterruptedException {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        MedicalResourceId id =
                new MedicalResourceId(
                        dataSource.getId(),
                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_RESOURCE_ID_IMMUNIZATION);

        mManager.deleteMedicalResources(List.of(id), Executors.newSingleThreadExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    @RequiresFlagsEnabled(FLAG_PERSONAL_HEALTH_RECORD)
    public void testDeleteMedicalResourcesByIds_emptyIds_succeedsAndNoDelete()
            throws InterruptedException {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        mManager.deleteMedicalResources(List.of(), Executors.newSingleThreadExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
        // Assert that resource still exists.
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(resource.getId()), Executors.newSingleThreadExecutor(), readReceiver);
        assertThat(readReceiver.getResponse()).hasSize(1);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_Create3Delete2_succeeds()
            throws InterruptedException {
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("ds1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("ds2"));
        // Insert some data
        MedicalResource resource1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource resource2 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource resource3 =
                mUtil.upsertMedicalData(dataSource2.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        mManager.deleteMedicalResources(
                List.of(resource1.getId(), resource2.getId()),
                Executors.newSingleThreadExecutor(),
                callback);

        callback.verifyNoExceptionOrThrow();
        // Test resource3 is still present
        HealthConnectReceiver<ReadMedicalResourcesResponse> readReceiver =
                new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build(),
                Executors.newSingleThreadExecutor(),
                readReceiver);
        assertThat(readReceiver.getResponse().getMedicalResources()).containsExactly(resource3);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_managementPermissionAMissingId_succeeds()
            throws InterruptedException {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        MedicalResourceId id =
                new MedicalResourceId(
                        dataSource.getId(),
                        FHIR_RESOURCE_TYPE_IMMUNIZATION,
                        FHIR_RESOURCE_ID_IMMUNIZATION);

        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalResources(
                            List.of(id), Executors.newSingleThreadExecutor(), receiver);
                    receiver.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_managementPermissionNoData_succeeds()
            throws InterruptedException {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        List<MedicalResourceId> ids = new ArrayList<>(MAXIMUM_PAGE_SIZE + 1);
        for (int i = 0; i < MAXIMUM_PAGE_SIZE + 1; i++) {
            ids.add(
                    new MedicalResourceId(
                            dataSource.getId(),
                            FHIR_RESOURCE_TYPE_IMMUNIZATION,
                            FHIR_RESOURCE_ID_IMMUNIZATION + "." + i));
        }
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalResources(
                            ids, Executors.newSingleThreadExecutor(), receiver);
                    receiver.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_PERSONAL_HEALTH_RECORD)
    public void testDeleteMedicalResourcesByIds_managementPermissionEmptyIds_succeeds()
            throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalResources(
                            List.of(), Executors.newSingleThreadExecutor(), receiver);
                    receiver.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        // Assert that resource still exists.
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(resource.getId()), Executors.newSingleThreadExecutor(), readReceiver);
        assertThat(readReceiver.getResponse()).hasSize(1);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_managementPermissionCreate2Delete1_succeeds()
            throws InterruptedException {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        // Insert some data
        MedicalResource resource1 =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource resource2 =
                mUtil.upsertMedicalData(dataSource.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalResources(
                            List.of(resource1.getId()),
                            Executors.newSingleThreadExecutor(),
                            callback);
                    callback.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        // Test resource2 is still present
        HealthConnectReceiver<ReadMedicalResourcesResponse> readReceiver =
                new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build(),
                Executors.newSingleThreadExecutor(),
                readReceiver);
        assertThat(readReceiver.getResponse().getMedicalResources()).containsExactly(resource2);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_managementPerm_canDeleteDataOwnedByAllApps()
            throws Exception {
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalResources(
                            List.of(foregroundAppVaccine.getId(), backgroundAppVaccine.getId()),
                            Executors.newSingleThreadExecutor(),
                            callback);
                    callback.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        // Test that the resources are not present anymore
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.readMedicalResources(
                            List.of(backgroundAppVaccine.getId(), foregroundAppVaccine.getId()),
                            Executors.newSingleThreadExecutor(),
                            readReceiver);
                    assertThat(readReceiver.getResponse()).isEmpty();
                });
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_inForegroundOnlyReadPermissions_expectError() {
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), READ_MEDICAL_DATA_VACCINES);
        MedicalResourceId id =
                new MedicalResourceId(DATA_SOURCE_ID, FHIR_RESOURCE_TYPE_IMMUNIZATION, "1");

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.deleteMedicalResources(List.of(id)));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_inForegroundNoPermission_expectError() {
        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                PHR_FOREGROUND_APP.deleteMedicalResources(
                                        List.of(getMedicalResourceId())));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_inBackgroundNoPermission_expectError() {
        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                PHR_BACKGROUND_APP.deleteMedicalResources(
                                        List.of(getMedicalResourceId())));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_resourceOwnedByDiffApp_noDelete() throws Exception {
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        PHR_FOREGROUND_APP.deleteMedicalResources(List.of(backgroundAppVaccine.getId()));

        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        // Test that the vaccine is still present
        runWithShellPermissionIdentity(
                () -> {
                    mManager.readMedicalResources(
                            List.of(backgroundAppVaccine.getId()),
                            Executors.newSingleThreadExecutor(),
                            readReceiver);
                    assertThat(readReceiver.getResponse()).containsExactly(backgroundAppVaccine);
                });
    }
}
