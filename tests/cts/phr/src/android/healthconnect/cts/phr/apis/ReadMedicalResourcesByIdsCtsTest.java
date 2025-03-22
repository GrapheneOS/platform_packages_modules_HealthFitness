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
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_CONDITIONS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PREGNANCY;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PROCEDURES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VISITS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_CONDITIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VISITS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.MAX_FOREGROUND_READ_CALL_15M;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_BACKGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_FOREGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_ALLERGY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_RESOURCE_ID_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getMedicalResourceId;
import static android.healthconnect.cts.utils.DataFactory.MAXIMUM_PAGE_SIZE;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermission;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.revokeAllHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.revokeHealthPermission;
import static android.healthconnect.cts.utils.TestUtils.finishMigrationWithShellPermissionIdentity;
import static android.healthconnect.cts.utils.TestUtils.setFieldValueUsingReflection;
import static android.healthconnect.cts.utils.TestUtils.startMigrationWithShellPermissionIdentity;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.MedicalResourceId;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.phr.utils.PhrDataFactory;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.HealthConnectReceiver;
import android.healthconnect.cts.utils.TestUtils;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class ReadMedicalResourcesByIdsCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private PhrCtsTestUtils mUtil;

    private HealthConnectManager mManager;

    @Before
    public void setUp() throws InterruptedException {
        // To make sure we don't leave any state behind after running each test.
        revokeAllHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(), "to test specific permissions");
        revokeAllHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(), "to test specific permissions");
        TestUtils.deleteAllStagedRemoteData();
        mUtil = new PhrCtsTestUtils(TestUtils.getHealthConnectManager());
        mUtil.deleteAllMedicalData();
        mManager = TestUtils.getHealthConnectManager();
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
    public void testReadMedicalResourcesByIds_migrationInProgress_apiBlocked()
            throws InterruptedException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalResource vaccine =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<List<MedicalResource>> receiver = new HealthConnectReceiver<>();

        startMigrationWithShellPermissionIdentity();
        mManager.readMedicalResources(List.of(vaccine.getId()), executor, receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        finishMigrationWithShellPermissionIdentity();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByIds_readLimitExceeded_throws()
            throws InterruptedException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(PhrDataFactory.getCreateMedicalDataSourceRequest());
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        // Make the maximum number of calls allowed by quota
        int maximumCalls = MAX_FOREGROUND_READ_CALL_15M / mUtil.mLimitsAdjustmentForTesting;
        float remainingQuota =
                mUtil.tryAcquireCallQuotaNTimesForRead(dataSource, List.of(resource), maximumCalls);

        // Exceed the quota by using up any remaining quota that accumulated during the previous
        // calls and make one additional call.
        HealthConnectReceiver<List<MedicalResource>> receiver = new HealthConnectReceiver<>();
        int additionalCalls = (int) Math.ceil(remainingQuota) + 1;
        for (int i = 0; i < additionalCalls; i++) {
            mManager.readMedicalResources(
                    List.of(resource.getId()), Executors.newSingleThreadExecutor(), receiver);
        }

        HealthConnectException exception = receiver.assertAndGetException();
        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED);
        assertThat(exception.getMessage()).contains("API call quota exceeded");
    }

    @Test
    @RequiresFlagsEnabled(FLAG_PERSONAL_HEALTH_RECORD)
    public void testReadMedicalResourcesByIds_emptyIds_returnsEmptyList()
            throws InterruptedException {
        HealthConnectReceiver<List<MedicalResource>> receiver = new HealthConnectReceiver<>();

        mManager.readMedicalResources(List.of(), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled(FLAG_PERSONAL_HEALTH_RECORD)
    public void testReadMedicalResourcesByIds_exceedsMaxPageSize_throws() {
        HealthConnectReceiver<List<MedicalResource>> receiver = new HealthConnectReceiver<>();
        List<MedicalResourceId> ids = new ArrayList<>(MAXIMUM_PAGE_SIZE + 1);
        for (int i = 0; i < MAXIMUM_PAGE_SIZE + 1; i++) {
            ids.add(
                    new MedicalResourceId(
                            UUID.randomUUID().toString(),
                            FHIR_RESOURCE_TYPE_IMMUNIZATION,
                            FHIR_RESOURCE_ID_IMMUNIZATION));
        }

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.readMedicalResources(
                                ids, Executors.newSingleThreadExecutor(), receiver));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByIds_invalidResourceTypeByReflection_throws()
            throws NoSuchFieldException, IllegalAccessException {
        MedicalResourceId id = getMedicalResourceId();

        setFieldValueUsingReflection(id, "mFhirResourceType", 100);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.readMedicalResources(
                                List.of(id),
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByIds_invalidDataSourceIdByReflection_throws()
            throws NoSuchFieldException, IllegalAccessException {
        MedicalResourceId id = getMedicalResourceId();

        setFieldValueUsingReflection(id, "mDataSourceId", "invalid id");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.readMedicalResources(
                                List.of(id),
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByIds_noData_returnsEmptyList()
            throws InterruptedException {
        HealthConnectReceiver<List<MedicalResource>> receiver = new HealthConnectReceiver<>();
        List<MedicalResourceId> ids = List.of(getMedicalResourceId());

        mManager.readMedicalResources(ids, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByIds_happyPath_succeeds() throws InterruptedException {
        // Create two data sources.
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        // Insert 3 vaccines and 1 allergy.
        MedicalResource vaccine1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine2 =
                mUtil.upsertMedicalData(dataSource2.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        // Vaccine 3 will not be checked for, but inserted to check that everything isn't read.
        mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource allergy = mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_ALLERGY);
        HealthConnectReceiver<List<MedicalResource>> receiver = new HealthConnectReceiver<>();

        mManager.readMedicalResources(
                List.of(
                        vaccine1.getId(),
                        vaccine2.getId(),
                        // leave out 3
                        allergy.getId()),
                Executors.newSingleThreadExecutor(),
                receiver);

        assertThat(receiver.getResponse()).containsExactly(vaccine1, vaccine2, allergy);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadMedicalResourcesByIds_happyPathWithManageHealthDataPermission_succeeds()
            throws InterruptedException {
        // Create two data sources.
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        // Insert 3 vaccines and 1 allergy.
        MedicalResource vaccine1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource vaccine2 =
                mUtil.upsertMedicalData(dataSource2.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        // Vaccine 3 will not be checked for, but inserted to check that everything isn't read.
        mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource allergy = mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_ALLERGY);
        HealthConnectReceiver<List<MedicalResource>> receiver = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () ->
                        mManager.readMedicalResources(
                                List.of(
                                        vaccine1.getId(),
                                        vaccine2.getId(),
                                        // leave out 3
                                        allergy.getId()),
                                Executors.newSingleThreadExecutor(),
                                receiver),
                MANAGE_HEALTH_DATA_PERMISSION);

        assertThat(receiver.getResponse()).containsExactly(vaccine1, vaccine2, allergy);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inForegroundWithNoPerms_throws() {
        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                PHR_FOREGROUND_APP.readMedicalResources(
                                        List.of(getMedicalResourceId())));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains("Caller doesn't have permission to read or write medical data");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inForegroundWithReadPermNoWritePerm_onlyReturnsResourcesWithReadPerms()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine and one allergy
        // each and the calling app only has READ_MEDICAL_DATA_VACCINES permissions
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource foregroundAppAllergy =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource backgroundAppAllergy =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_ALLERGY);

        revokeHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        // When the app tries to read all resources by id from the foreground
        List<MedicalResource> responseResources =
                PHR_FOREGROUND_APP.readMedicalResources(
                        List.of(
                                foregroundAppVaccine.getId(), foregroundAppAllergy.getId(),
                                backgroundAppVaccine.getId(), backgroundAppAllergy.getId()));

        // Then it receives all vaccine resources
        assertThat(responseResources).containsExactly(foregroundAppVaccine, backgroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inForegroundHasWritePermNoReadPerms_onlyReturnsDataFromOwnDataSources()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // calling app only has WRITE_MEDICAL_DATA permissions
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        // When the app reads all vaccine resources from the foreground
        List<MedicalResource> resourcesResponse =
                PHR_FOREGROUND_APP.readMedicalResources(
                        List.of(foregroundAppVaccine.getId(), backgroundAppVaccine.getId()));

        // Then it only receives the vaccine resources written by itself
        assertThat(resourcesResponse).containsExactly(foregroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inForegroundHasWriteAndReadPerms_returnsSelfDataAndOtherDataWithReadPerms()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine and one allergy
        // each and the calling app only has WRITE_MEDICAL_DATA and READ_MEDICAL_DATA_VACCINES
        // permissions
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource foregroundAppAllergy =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource backgroundAppAllergy =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_ALLERGY);

        // When the app tries to read all resources from the foreground
        List<MedicalResource> resourcesResponse =
                PHR_FOREGROUND_APP.readMedicalResources(
                        List.of(
                                foregroundAppVaccine.getId(), foregroundAppAllergy.getId(),
                                backgroundAppVaccine.getId(), backgroundAppAllergy.getId()));

        // Then it receives all vaccine resources, but only the allergy resources written by
        // itself
        assertThat(resourcesResponse)
                .containsExactly(foregroundAppVaccine, backgroundAppVaccine, foregroundAppAllergy);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithBgReadNoOtherPerms_throws() {
        // App has background read permissions, but no other permissions.
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), READ_HEALTH_DATA_IN_BACKGROUND);

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                PHR_BACKGROUND_APP.readMedicalResources(
                                        List.of(getMedicalResourceId())));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains("Caller doesn't have permission to read or write medical data");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithBgReadHasReadPermNoWritePerm_onlyReturnsResourcesWithReadPerms()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine and one allergy
        // each and the calling app has READ_HEALTH_DATA_IN_BACKGROUND and
        // READ_MEDICAL_DATA_VACCINES permissions
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource foregroundAppAllergy =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource backgroundAppAllergy =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_ALLERGY);

        revokeHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        // When the app tries to read all resources from the background
        List<MedicalResource> resourcesResponse =
                PHR_BACKGROUND_APP.readMedicalResources(
                        List.of(
                                foregroundAppVaccine.getId(), foregroundAppAllergy.getId(),
                                backgroundAppVaccine.getId(), backgroundAppAllergy.getId()));

        // Then it receives only vaccine resources
        assertThat(resourcesResponse).containsExactly(foregroundAppVaccine, backgroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithBgReadHasWritePermNoReadPerms_onlyReturnsDataFromOwnDataSources()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // calling app only has READ_HEALTH_DATA_IN_BACKGROUND and WRITE_MEDICAL_DATA permissions
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_HEALTH_DATA_IN_BACKGROUND));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource foregroundAppAllergy =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource backgroundAppAllergy =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_ALLERGY);

        // When the app tries to read all resources from the background
        List<MedicalResource> resourcesResponse =
                PHR_BACKGROUND_APP.readMedicalResources(
                        List.of(
                                foregroundAppVaccine.getId(), foregroundAppAllergy.getId(),
                                backgroundAppVaccine.getId(), backgroundAppAllergy.getId()));

        // Then it only receives the resources written by itself
        assertThat(resourcesResponse).containsExactly(backgroundAppVaccine, backgroundAppAllergy);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void
            testRead_inBgWithBgReadHasWriteAndReadPerms_canReadSelfDataAndOtherDataWithReadPerms()
                    throws Exception {
        // Given that we have two data sources from two apps with one vaccine and one allergy
        // each and the calling app has READ_HEALTH_DATA_IN_BACKGROUND, WRITE_MEDICAL_DATA and
        // READ_MEDICAL_DATA_VACCINES permissions
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource foregroundAppAllergy =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource backgroundAppAllergy =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_ALLERGY);

        // When the app tries to read all resources from the background
        List<MedicalResource> responseResources =
                PHR_BACKGROUND_APP.readMedicalResources(
                        List.of(
                                foregroundAppVaccine.getId(), foregroundAppAllergy.getId(),
                                backgroundAppVaccine.getId(), backgroundAppAllergy.getId()));

        // Then it receives all vaccine resources, but only the allergy resources written by
        // itself
        assertThat(responseResources)
                .containsExactly(foregroundAppVaccine, backgroundAppVaccine, backgroundAppAllergy);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBackgoundWithNoPerms_throws() {
        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                PHR_BACKGROUND_APP.readMedicalResources(
                                        List.of(getMedicalResourceId())));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(exception)
                .hasMessageThat()
                .contains("Caller doesn't have permission to read or write medical data");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithoutBgReadOnlyWritePerm_onlyReturnsDataFromOwnDataSources()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine each and the
        // calling app only has WRITE_MEDICAL_DATA permissions
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);

        // When the app tries to read all resources from the background
        List<MedicalResource> responseResources =
                PHR_BACKGROUND_APP.readMedicalResources(
                        List.of(foregroundAppVaccine.getId(), backgroundAppVaccine.getId()));

        // Then it only receives its own resources
        assertThat(responseResources).containsExactly(backgroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testRead_inBgWithoutBgReadOnlyReadPerm_onlyReturnsDataWithReadPerms()
            throws Exception {
        // Given that we have two data sources from two apps with one vaccine and one allergy
        // each and the
        // and the calling app only has READ_MEDICAL_DATA_VACCINES permissions
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource foregroundAppDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource foregroundAppVaccine =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource foregroundAppAllergy =
                PHR_FOREGROUND_APP.upsertMedicalResource(
                        foregroundAppDataSource.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource backgroundAppDataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource backgroundAppVaccine =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource backgroundAppAllergy =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        backgroundAppDataSource.getId(), FHIR_DATA_ALLERGY);

        revokeHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        // When the app tries to read all resources from the background
        List<MedicalResource> responseResources =
                PHR_BACKGROUND_APP.readMedicalResources(
                        List.of(
                                foregroundAppVaccine.getId(), foregroundAppAllergy.getId(),
                                backgroundAppVaccine.getId(), backgroundAppAllergy.getId()));

        // Then it only receives its own vaccine resources
        assertThat(responseResources).containsExactly(backgroundAppVaccine);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testReadPermissionMapping_eachPermissionOnlyGivesAccessToSpecificData()
            throws Exception {
        List<MedicalResourceId> allInsertedIds =
                mUtil.insertSourceAndOneResourcePerPermissionCategory(PHR_BACKGROUND_APP);
        Map<String, Integer> permissionToExpectedMedicalResourceTypeMap =
                Map.ofEntries(
                        Map.entry(READ_MEDICAL_DATA_VACCINES, MEDICAL_RESOURCE_TYPE_VACCINES),
                        Map.entry(
                                READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        Map.entry(READ_MEDICAL_DATA_CONDITIONS, MEDICAL_RESOURCE_TYPE_CONDITIONS),
                        Map.entry(READ_MEDICAL_DATA_MEDICATIONS, MEDICAL_RESOURCE_TYPE_MEDICATIONS),
                        Map.entry(
                                READ_MEDICAL_DATA_PERSONAL_DETAILS,
                                MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS),
                        Map.entry(
                                READ_MEDICAL_DATA_PRACTITIONER_DETAILS,
                                MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS),
                        Map.entry(READ_MEDICAL_DATA_VISITS, MEDICAL_RESOURCE_TYPE_VISITS),
                        Map.entry(READ_MEDICAL_DATA_PROCEDURES, MEDICAL_RESOURCE_TYPE_PROCEDURES),
                        Map.entry(READ_MEDICAL_DATA_PREGNANCY, MEDICAL_RESOURCE_TYPE_PREGNANCY),
                        Map.entry(
                                READ_MEDICAL_DATA_SOCIAL_HISTORY,
                                MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY),
                        Map.entry(READ_MEDICAL_DATA_VITAL_SIGNS, MEDICAL_RESOURCE_TYPE_VITAL_SIGNS),
                        Map.entry(
                                READ_MEDICAL_DATA_LABORATORY_RESULTS,
                                MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS));

        // When permissions are granted for one category at a time, then only that category will be
        // returned
        for (String permission : permissionToExpectedMedicalResourceTypeMap.keySet()) {
            int expectedResourceType = permissionToExpectedMedicalResourceTypeMap.get(permission);
            grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), permission);

            List<MedicalResource> returnedResources =
                    PHR_FOREGROUND_APP.readMedicalResources(allInsertedIds);

            assertWithMessage("Reading data with permission: " + permission)
                    .that(returnedResources)
                    .hasSize(1);
            assertThat(returnedResources.get(0).getType()).isEqualTo(expectedResourceType);

            revokeHealthPermission(PHR_FOREGROUND_APP.getPackageName(), permission);
        }
    }
}
