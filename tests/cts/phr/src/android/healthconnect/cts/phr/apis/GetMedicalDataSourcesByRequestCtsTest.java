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
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.MAX_FOREGROUND_READ_CALL_15M;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_BACKGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_BACKGROUND_APP_PKG;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_FOREGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_FOREGROUND_APP_PKG;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_ALLERGY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.MEDICAL_DATA_SOURCE_EQUIVALENCE;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermission;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.revokeAllHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.revokeHealthPermission;
import static android.healthconnect.cts.utils.TestUtils.finishMigrationWithShellPermissionIdentity;
import static android.healthconnect.cts.utils.TestUtils.setFieldValueUsingReflection;
import static android.healthconnect.cts.utils.TestUtils.startMigrationWithShellPermissionIdentity;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.app.UiAutomation;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.phr.utils.PhrDataFactory;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DataFactory;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.HealthConnectReceiver;
import android.healthconnect.cts.utils.TestUtils;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class GetMedicalDataSourcesByRequestCtsTest {
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
    public void before() throws InterruptedException {
        // To make sure we don't leave any state behind after running each test.
        revokeAllHealthPermissions(PHR_BACKGROUND_APP_PKG, "to test specific permissions");
        revokeAllHealthPermissions(PHR_FOREGROUND_APP_PKG, "to test specific permissions");
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
    public void testGetMedicalDataSourcesByRequest_invalidPackageNameByReflection_throws()
            throws NoSuchFieldException, IllegalAccessException {
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .build();
        setFieldValueUsingReflection(request, "mPackageNames", Set.of("InvalidPackageName"));

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.getMedicalDataSources(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetMedicalDataSourcesByRequest_migrationInProgress_apiBlocked()
            throws Exception {
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();

        startMigrationWithShellPermissionIdentity();
        mManager.getMedicalDataSources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);

        finishMigrationWithShellPermissionIdentity();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetMedicalDataSourcesByRequest_readLimitExceeded_throws()
            throws InterruptedException {
        MedicalDataSource dataSource =
                mUtil.createDataSource(PhrDataFactory.getCreateMedicalDataSourceRequest());
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        // Make the maximum number of calls allowed by quota
        int maximumCalls = MAX_FOREGROUND_READ_CALL_15M / mUtil.mLimitsAdjustmentForTesting;
        float remainingQuota =
                mUtil.tryAcquireCallQuotaNTimesForRead(dataSource, List.of(resource), maximumCalls);

        // Exceed the quota by using up any remaining quota that accumulated during the previous
        // calls and make one additional call.
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();
        int additionalCalls = (int) Math.ceil(remainingQuota) + 1;
        for (int i = 0; i < additionalCalls; i++) {
            mManager.getMedicalDataSources(request, Executors.newSingleThreadExecutor(), receiver);
        }

        HealthConnectException exception = receiver.assertAndGetException();
        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED);
        assertThat(exception.getMessage()).contains("API call quota exceeded");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetMedicalDataSourcesByRequest_withManageHealthPerm_nothingPresent_returnEmpty()
            throws Exception {
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            mManager.getMedicalDataSources(request, Executors.newSingleThreadExecutor(), receiver);

            assertThat(receiver.getResponse()).isEmpty();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetMedicalDataSourcesByRequest_withManageHealthPerm_canReadAll()
            throws Exception {
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource dataSource1 =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        MedicalDataSource dataSource2 =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA_PERMISSION);
        try {
            mManager.getMedicalDataSources(
                    new GetMedicalDataSourcesRequest.Builder().build(),
                    Executors.newSingleThreadExecutor(),
                    receiver);

            assertThat(receiver.getResponse())
                    .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                    .containsExactly(dataSource1, dataSource2);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetMedicalDataSourcesByRequest_onePresentNoData_returnsItAndNullUpdateTime()
            throws Exception {
        HealthConnectReceiver<MedicalDataSource> createReceiver = new HealthConnectReceiver<>();
        mManager.createMedicalDataSource(
                getCreateMedicalDataSourceRequest("ds/1"),
                Executors.newSingleThreadExecutor(),
                createReceiver);
        MedicalDataSource dataSource = createReceiver.getResponse();
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();

        mManager.getMedicalDataSources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).containsExactly(dataSource);
        assertThat(dataSource.getLastDataUpdateTime()).isNull();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void
            testGetMedicalDataSourcesByRequest_onePresentWithData_returnsCorrectLastDataUpdateTime()
                    throws Exception {
        HealthConnectReceiver<MedicalDataSource> createReceiver = new HealthConnectReceiver<>();
        mManager.createMedicalDataSource(
                getCreateMedicalDataSourceRequest(),
                Executors.newSingleThreadExecutor(),
                createReceiver);
        MedicalDataSource dataSource = createReceiver.getResponse();
        Instant insertTime = DataFactory.NOW;
        mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();

        mManager.getMedicalDataSources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).hasSize(1);
        Instant lastDataUpdateTime = receiver.getResponse().get(0).getLastDataUpdateTime();
        assertThat(lastDataUpdateTime).isAtLeast(insertTime);
        assertThat(lastDataUpdateTime).isAtMost(Instant.now());
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetMedicalResourcesByRequest_deletedResource_notCountedInLastDataUpdateTime()
            throws InterruptedException {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource resource =
                mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        mManager.deleteMedicalResources(
                List.of(resource.getId()), Executors.newSingleThreadExecutor(), callback);
        callback.verifyNoExceptionOrThrow();
        HealthConnectReceiver<List<MedicalDataSource>> readReceiver = new HealthConnectReceiver<>();

        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        mManager.getMedicalDataSources(request, Executors.newSingleThreadExecutor(), readReceiver);

        // The last data update time of dataSource is expected to be null because we have deleted
        // all data.
        assertThat(readReceiver.getResponse().get(0).getLastDataUpdateTime()).isNull();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetMedicalDataSourcesByRequest_InBackgroundNoPermission_throws() {
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();

        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.getMedicalDataSources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetMedicalDataSourcesByRequest_InForegroundNoPermission_throws() {
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();

        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.getMedicalDataSources(request));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetByPackage_packageFilterEmpty_inBgWithBgPermHasWriteAndReadPerm()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in background with background read perm, has write permission and
        // has vaccine read permissions.
        // The packageName set in the request is empty so no filtering based on packageNames.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types.
        List<MedicalDataSource> result =
                PHR_BACKGROUND_APP.getMedicalDataSources(
                        new GetMedicalDataSourcesRequest.Builder().build());

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(
                        dataSource1Background, dataSource2Background, dataSource1Foreground);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetByPackage_packageFilterEmpty_inForegroundHasWriteAndReadPerm()
            throws Exception {
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in foreground, has write permission and vaccine read permissions.
        // The packageName set in the request is empty so no filtering based on packageNames.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types.
        List<MedicalDataSource> result =
                PHR_FOREGROUND_APP.getMedicalDataSources(
                        new GetMedicalDataSourcesRequest.Builder().build());

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(
                        dataSource1Foreground, dataSource2Foreground, dataSource1Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetByPackage_withPackageFilterSelfIncluded_inFgHasWriteAndReadPerm()
            throws Exception {
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .build();

        // App is in foreground, has write permission and has vaccine read permissions.
        // The app's package name is included in the list of packages.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types written by any of the given packages.
        List<MedicalDataSource> result = PHR_FOREGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(
                        dataSource1Foreground, dataSource2Foreground, dataSource1Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetByPackage_withPackageFilterSelfIncluded_inBgWithPermHasWriteAndReadPerm()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .build();

        // App is in background with background read, has write permission and has vaccine
        // read permissions.
        // The app's package name is included in the list of packages.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types written by any of the given packages.
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(
                        dataSource1Background, dataSource2Background, dataSource1Foreground);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetByPackage_withPackageFilterSelfNotIncluded_inBgWithPermHasWriteAndReadPerm()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .build();

        // App is in background with background read perm, has write permission and
        // has vaccine read permissions.
        // The app's package name is not included in the list of packages.
        // App can read dataSources belonging to vaccine resource types written by any of
        // the given packages.
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Foreground);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetByPackage_withPackageFilterSelfNotIncluded_inForegroundWriteAndReadPerm()
            throws Exception {
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in foreground, has write permission and has vaccine read permissions.
        // The app's package name is not included in the list of packages.
        // App can read dataSources belonging to vaccine resource types written by any of
        // the given packages.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .build();
        List<MedicalDataSource> result = PHR_FOREGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetByPackage_emptyPackageFilter_inBgWithoutBgPermHasWritePermNoReadPerms()
            throws Exception {
        grantHealthPermissions(PHR_FOREGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA));
        grantHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in background without background read perm, has write permission and
        // no read permissions.
        // The packageNames filter is empty so no filter is applied.
        // App can read dataSources they wrote themself.
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background, dataSource2Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_withPackageFilterSelfIncluded_inBgWithoutBgPermHasWritePermButNoRead()
            throws Exception {
        grantHealthPermissions(PHR_FOREGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA));
        grantHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in background without background read perm, has write permission and
        // no read permissions.
        // The app's package name is included in the list of packages.
        // App can read dataSources they wrote themself.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background, dataSource2Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_withPackageFilterSelfNotIncluded_inBgWithoutBgPermHasWritePermNoRead()
            throws Exception {
        grantHealthPermissions(PHR_FOREGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA));
        grantHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        // App is in background without background read perm, has write permission and
        // no read permissions.
        // The app's package name is not included in the list of packages.
        // App can read no dataSources.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .build();
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.getMedicalDataSources(request));
        assertThat(exception.getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_emptyPackageFilter_inBgWithoutBgPermHasWritePermAndReadPerms()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in background without background read perm, has write permission and
        // has read vaccine permission.
        // The packageNames is empty so no filtering is applied.
        // App can read dataSources they wrote themselves.
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background, dataSource2Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_withPackageFilterSelfIncluded_inBgWithoutBgPermHasWriteAndReadPerm()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in background without background read perm, has write permission and
        // has read vaccine permission.
        // The app's package name is included in the list of packages.
        // App can read dataSources they wrote themselves.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background, dataSource2Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_noPackageFilter_inBgWithoutBgPermHasReadPermNoWritePerm()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);
        // Revoke the write permission that was granted before.
        revokeHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        // App is in background without background read perm, has no write permission and
        // has read vaccine permission.
        // The packageNames is empty so no filtering based on packageNames.
        // App can read dataSources belonging to vaccines the app wrote itself.
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_withPackageFilterSelfIncluded_inBgWithoutBgPermHasReadPermNoWrite()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);
        // Revoke the write permission that was granted before.
        revokeHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        // App is in background without background read perm, has no write permission and
        // has read vaccine permission.
        // The app's package name is included in the list of packages.
        // App can read dataSources belonging to vaccines the app wrote itself.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_emptyPackageFilter_inBgWithoutBgPermHasMultipleReadPermsNoWritePerm()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP_PKG,
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_MEDICAL_DATA_VACCINES,
                        READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES));
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);
        // Revoke the write permission that was granted before.
        revokeHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        // App is in background without background read perm, no write permission but has
        // vaccine and allergy read permission.
        // PackageNames is empty so no filtering based on packageNames is applied.
        // App can read dataSources belonging to
        // vaccines and allergy resource types that the app wrote itself.
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background, dataSource2Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_filterOnPackageWithSelf_inBgWithoutBgPermHasMultipleReadPermsNoWrite()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP_PKG,
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_MEDICAL_DATA_VACCINES,
                        READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES));
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);
        // Revoke the write permission that was granted before.
        revokeHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        // App is in background without background read perm, no write permission but has
        // vaccine and allergy read permission. App can read dataSources belonging to
        // vaccines and allergy resource types that the app wrote itself.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background, dataSource2Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_noPackageFilter_inForegroundHasWritePermNoReadPerm()
            throws Exception {
        grantHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in foreground or background with background read perm, has write permission but
        // no read permission for any resource types.
        // The packageNames is empty so no filtering is applied based on packageNames.
        // App can read only read dataSources they wrote themselves.
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        List<MedicalDataSource> result = PHR_FOREGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Foreground, dataSource2Foreground);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_noPackageFilter_inBgWithBgPermHasWritePermNoReadPerm()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP_PKG,
                List.of(WRITE_MEDICAL_DATA, READ_HEALTH_DATA_IN_BACKGROUND));
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in background with background read perm, has write permission but
        // no read permission for any resource types.
        // The packageNames is empty so no filtering is applied based on packageNames.
        // App can read only read dataSources they wrote themselves.
        GetMedicalDataSourcesRequest request = new GetMedicalDataSourcesRequest.Builder().build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background, dataSource2Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_packageFilterWithSelf_inFgHasWritePermNoReadPerm() throws Exception {
        grantHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in foreground or background with background read perm, has write permission but
        // no read permission for any resource types.
        // The packageNames is empty so no filtering is applied based on packageNames.
        // App can read only read dataSources they wrote themselves.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .build();
        List<MedicalDataSource> result = PHR_FOREGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Foreground, dataSource2Foreground);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_packageFilterWithSelf_inBgWithBgPermHasWritePermNoReadPerm()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP_PKG,
                List.of(WRITE_MEDICAL_DATA, READ_HEALTH_DATA_IN_BACKGROUND));
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);

        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(dataSource2Foreground.getId(), FHIR_DATA_ALLERGY);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in background with background read perm, has write permission but
        // no read permission for any resource types.
        // The packageNames is empty so no filtering is applied based on packageNames.
        // App can read only read dataSources they wrote themselves.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .build();
        List<MedicalDataSource> result = PHR_BACKGROUND_APP.getMedicalDataSources(request);

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background, dataSource2Background);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void
            getByPackages_withPackageFilterSelfNotIncluded_inForegroundHasWritePermNoReadPerms() {
        grantHealthPermissions(PHR_FOREGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA));

        // App is in foreground, has write permission but no read permission for any resource types.
        // App package name is not included in the set of given packageNames.
        // App can not read any dataSources.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_BACKGROUND_APP_PKG)
                        .build();
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.getMedicalDataSources(request));
        assertThat(exception.getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void getByPackages_withPackageFilterSelfNotIncluded_inBgWithBgReadHasWritePermNoRead() {
        grantHealthPermissions(PHR_BACKGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA));

        // App is in background with background read permission, has write permission but no read
        // permission for any resource types.
        // App package name is not included in the set of given packageNames.
        // App can not read any dataSources.
        GetMedicalDataSourcesRequest request =
                new GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(PHR_FOREGROUND_APP_PKG)
                        .build();
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.getMedicalDataSources(request));
        assertThat(exception.getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }
}
