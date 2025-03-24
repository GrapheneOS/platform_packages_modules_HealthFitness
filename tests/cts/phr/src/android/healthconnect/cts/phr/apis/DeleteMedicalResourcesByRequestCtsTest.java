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
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.MAX_FOREGROUND_WRITE_CALL_15M;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_BACKGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_FOREGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_ALLERGY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
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

import android.app.UiAutomation;
import android.health.connect.DeleteMedicalResourcesRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
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

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class DeleteMedicalResourcesByRequestCtsTest {
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
    public void testDeleteMedicalResourcesByRequest_migrationInProgress_apiBlocked()
            throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        startMigrationWithShellPermissionIdentity();
        mManager.deleteMedicalResources(request, executor, receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        finishMigrationWithShellPermissionIdentity();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_writeLimitExceeded_throws() throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        DeleteMedicalResourcesRequest deleteRequest =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        // Make the maximum number of calls allowed by quota
        int maximumCalls = MAX_FOREGROUND_WRITE_CALL_15M / mUtil.mLimitsAdjustmentForTesting - 1;
        float remainingQuota = mUtil.tryAcquireCallQuotaNTimesForWrite(dataSource, maximumCalls);

        // Exceed the quota by using up any remaining quota that accumulated during the previous
        // calls and make one additional call.
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        int additionalCalls = (int) Math.ceil(remainingQuota) + 1;
        for (int i = 0; i < additionalCalls; i++) {
            mManager.deleteMedicalResources(
                    deleteRequest, Executors.newSingleThreadExecutor(), callback);
        }

        HealthConnectException exception = callback.assertAndGetException();
        assertThat(exception.getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED);
        assertThat(exception.getMessage()).contains("API call quota exceeded");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_badDataSourceIdUsingReflection_doesntDeleteAll()
            throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        // Create the datasource
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        // Insert some data
        MedicalResource resource1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest deleteRequest =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        // Change the delete request to use an illegal id for this test.
        setFieldValueUsingReflection(deleteRequest, "mDataSourceIds", Set.of("illegal id"));
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);

        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            mManager.deleteMedicalResources(
                                    deleteRequest,
                                    Executors.newSingleThreadExecutor(),
                                    new HealthConnectReceiver<>()));
            // Test resource is still present.
            mManager.readMedicalResources(
                    List.of(resource1.getId()), Executors.newSingleThreadExecutor(), readReceiver);
            assertThat(readReceiver.getResponse()).containsExactly(resource1);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_badResourceType_throwsAndDoesntDeleteAll()
            throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest deleteRequest =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        // Change the delete request to use an illegal type for this test.
        setFieldValueUsingReflection(deleteRequest, "mMedicalResourceTypes", Set.of(100));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.deleteMedicalResources(
                                deleteRequest,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
        // Test resource is still present.
        mManager.readMedicalResources(
                List.of(resource.getId()), Executors.newSingleThreadExecutor(), readReceiver);
        assertThat(readReceiver.getResponse()).containsExactly(resource);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_emptyRequestNoFilters_throwsAndDoesntDeleteAll()
            throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest deleteRequest =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();
        // Change the delete request to have empty filters for this test.
        setFieldValueUsingReflection(deleteRequest, "mMedicalResourceTypes", Set.of());
        setFieldValueUsingReflection(deleteRequest, "mDataSourceIds", Set.of());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.deleteMedicalResources(
                                deleteRequest,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
        // Test resource is still present.
        mManager.readMedicalResources(
                List.of(resource.getId()), Executors.newSingleThreadExecutor(), readReceiver);
        assertThat(readReceiver.getResponse()).containsExactly(resource);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_nothingPresent_succeeds() throws Exception {
        // Insert a data source to ensure we have an appInfoId.
        mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(UUID.randomUUID().toString())
                        .build();

        mManager.deleteMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_resourceTypesMismatch_noDelete()
            throws InterruptedException {
        // Create the datasource
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        // Insert some data
        MedicalResource imm1 = mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource imm2 = mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);

        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES + 1)
                        .build();
        mManager.deleteMedicalResources(request, Executors.newSingleThreadExecutor(), callback);

        callback.verifyNoExceptionOrThrow();
        // Test resource2 is still present
        HealthConnectReceiver<List<MedicalResource>> readReceiver2 = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(imm1.getId(), imm2.getId()),
                Executors.newSingleThreadExecutor(),
                readReceiver2);
        assertThat(readReceiver2.getResponse()).containsExactly(imm1, imm2);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_deleteByType_succeeds()
            throws InterruptedException {
        // Create the datasource
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        // Insert some data
        MedicalResource vaccine1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine2 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource allergy1 = mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_ALLERGY);

        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        mManager.deleteMedicalResources(request, Executors.newSingleThreadExecutor(), callback);

        callback.verifyNoExceptionOrThrow();
        // Test only the allergy is present
        HealthConnectReceiver<List<MedicalResource>> readReceiver2 = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(vaccine1.getId(), vaccine2.getId(), allergy1.getId()),
                Executors.newSingleThreadExecutor(),
                readReceiver2);
        assertThat(readReceiver2.getResponse()).containsExactly(allergy1);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_deleteByTypes_succeeds()
            throws InterruptedException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource vaccine =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource allergy = mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_ALLERGY);

        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                        .build();
        mManager.deleteMedicalResources(request, Executors.newSingleThreadExecutor(), callback);

        callback.verifyNoExceptionOrThrow();
        // Test resources were deleted
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(vaccine.getId(), allergy.getId()),
                Executors.newSingleThreadExecutor(),
                readReceiver);
        assertThat(readReceiver.getResponse()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_deleteByDataSource_succeeds()
            throws InterruptedException {
        // Create the datasource
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        // Insert some data
        MedicalResource vaccine1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine2 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);

        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .build();
        mManager.deleteMedicalResources(request, Executors.newSingleThreadExecutor(), callback);

        callback.verifyNoExceptionOrThrow();
        // Test only vaccine2 is present
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(vaccine1.getId(), vaccine2.getId()),
                Executors.newSingleThreadExecutor(),
                readReceiver);
        assertThat(readReceiver.getResponse()).containsExactly(vaccine2);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_deleteDataSources_succeeds()
            throws InterruptedException {
        // Create the datasource
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        MedicalDataSource dataSource3 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("3"));
        // Insert some data
        MedicalResource vaccine1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine2 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine3 =
                mUtil.upsertMedicalData(dataSource3.getId(), FHIR_DATA_IMMUNIZATION);

        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addDataSourceId(dataSource2.getId())
                        .build();
        mManager.deleteMedicalResources(request, Executors.newSingleThreadExecutor(), callback);

        callback.verifyNoExceptionOrThrow();
        // Test only vaccine3 is present
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(vaccine1.getId(), vaccine2.getId(), vaccine3.getId()),
                Executors.newSingleThreadExecutor(),
                readReceiver);
        assertThat(readReceiver.getResponse()).containsExactly(vaccine3);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_deleteByDataSourceAndResourceType_succeeds()
            throws InterruptedException {
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        MedicalDataSource dataSource3 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("3"));
        MedicalResource vaccineDS1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource allergyDS1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_ALLERGY);
        MedicalResource vaccineDS2 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource allergyDS2 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_ALLERGY);
        MedicalResource allergyDS3 =
                mUtil.upsertMedicalData(dataSource3.getId(), FHIR_DATA_ALLERGY);

        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest deleteRequestDS1andDS3Vaccines =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .addDataSourceId(dataSource3.getId())
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();
        mManager.deleteMedicalResources(
                deleteRequestDS1andDS3Vaccines, Executors.newSingleThreadExecutor(), callback);

        callback.verifyNoExceptionOrThrow();
        // Test only one was deleted
        HealthConnectReceiver<List<MedicalResource>> readReceiver = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(
                        vaccineDS1.getId(),
                        allergyDS1.getId(),
                        vaccineDS2.getId(),
                        allergyDS2.getId(),
                        allergyDS3.getId()),
                Executors.newSingleThreadExecutor(),
                readReceiver);
        assertThat(readReceiver.getResponse())
                .containsExactly(allergyDS1, vaccineDS2, allergyDS2, allergyDS3);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_managementPermissionCreate2Delete1_succeeds()
            throws InterruptedException {
        // Create the datasource
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        // Insert some data
        MedicalResource resource1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource resource2 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);

        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(dataSource1.getId())
                        .build();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalResources(
                            request, Executors.newSingleThreadExecutor(), callback);
                    callback.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        // Test resource2 is still present
        HealthConnectReceiver<List<MedicalResource>> readReceiver2 = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(resource1.getId(), resource2.getId()),
                Executors.newSingleThreadExecutor(),
                readReceiver2);
        assertThat(readReceiver2.getResponse()).containsExactly(resource2);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_managementPermTypeMismatch_noDelete()
            throws InterruptedException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource vaccine =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES + 1)
                        .build();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalResources(
                            request, Executors.newSingleThreadExecutor(), callback);
                    callback.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        // Test resource is still present
        HealthConnectReceiver<List<MedicalResource>> readReceiver2 = new HealthConnectReceiver<>();
        mManager.readMedicalResources(
                List.of(vaccine.getId()), Executors.newSingleThreadExecutor(), readReceiver2);
        assertThat(readReceiver2.getResponse()).containsExactly(vaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_managementPerm_canDeleteDataOwnedByAllApps()
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
        DeleteMedicalResourcesRequest deleteResourcesForBothAppsRequest =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(backgroundAppDataSource.getId())
                        .addDataSourceId(foregroundAppDataSource.getId())
                        .build();

        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalResources(
                            deleteResourcesForBothAppsRequest,
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
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_inForegroundNoPermission_expectError() {
        // App has not been granted any permissions.
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.deleteMedicalResources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_inForegroundOnlyReadPerm_expectError() {
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), READ_MEDICAL_DATA_VACCINES);
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.deleteMedicalResources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByIds_inBackgroundNoPermission_expectError() {
        // App has not been granted any permissions.
        DeleteMedicalResourcesRequest request =
                new DeleteMedicalResourcesRequest.Builder().addDataSourceId(DATA_SOURCE_ID).build();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.deleteMedicalResources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_byDataSourceOwnedByDiffApp_noDelete()
            throws Exception {
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        DeleteMedicalResourcesRequest deleteBackgroundAppResourcesRequest =
                new DeleteMedicalResourcesRequest.Builder()
                        .addDataSourceId(backgroundAppDataSource.getId())
                        .build();

        PHR_FOREGROUND_APP.deleteMedicalResources(deleteBackgroundAppResourcesRequest);

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

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalResourcesByRequest_byResourceTypeByDiffApp_noDelete()
            throws Exception {
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        DeleteMedicalResourcesRequest deleteVaccinesRequest =
                new DeleteMedicalResourcesRequest.Builder()
                        .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                        .build();

        PHR_FOREGROUND_APP.deleteMedicalResources(deleteVaccinesRequest);

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
