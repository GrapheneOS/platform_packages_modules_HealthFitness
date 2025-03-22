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
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.MAX_FOREGROUND_WRITE_CALL_15M;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_BACKGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_FOREGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.MEDICAL_DATA_SOURCE_EQUIVALENCE;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermission;
import static android.healthconnect.cts.utils.PermissionHelper.revokeAllHealthPermissions;
import static android.healthconnect.cts.utils.TestUtils.finishMigrationWithShellPermissionIdentity;
import static android.healthconnect.cts.utils.TestUtils.startMigrationWithShellPermissionIdentity;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
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

import java.util.List;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class DeleteMedicalDataSourceWithDataCtsTest {
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
    public void testDeleteMedicalDataSource_migrationInProgress_apiBlocked()
            throws InterruptedException {
        startMigrationWithShellPermissionIdentity();
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        mManager.deleteMedicalDataSourceWithData(
                DATA_SOURCE_ID, Executors.newSingleThreadExecutor(), callback);

        assertThat(callback.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);

        finishMigrationWithShellPermissionIdentity();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_writeLimitExceeded_throws() throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        // Make the maximum number of delete medical resources calls just to use up the WRITE quota,
        // because we cannot delete the created data source multiple times. Minus 1 because of the
        // above call.
        int maximumCalls = MAX_FOREGROUND_WRITE_CALL_15M / mUtil.mLimitsAdjustmentForTesting - 1;
        float remainingQuota = mUtil.tryAcquireCallQuotaNTimesForWrite(dataSource, maximumCalls);

        // Exceed the quota by using up any remaining quota that accumulated during the previous
        // calls and make one additional call.
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        int additionalCalls = (int) Math.ceil(remainingQuota) + 1;
        for (int i = 0; i < additionalCalls; i++) {
            mManager.deleteMedicalDataSourceWithData(
                    DATA_SOURCE_ID, Executors.newSingleThreadExecutor(), callback);
        }

        HealthConnectException exception = callback.assertAndGetException();
        assertThat(exception.getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED);
        assertThat(exception.getMessage()).contains("API call quota exceeded");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_existsWithoutData_succeedsAndDeletes()
            throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        mManager.deleteMedicalDataSourceWithData(
                dataSource.getId(), Executors.newSingleThreadExecutor(), callback);

        callback.verifyNoExceptionOrThrow();
        // Verifies that data source is deleted.
        HealthConnectReceiver<List<MedicalDataSource>> readReceiver = new HealthConnectReceiver<>();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            new GetMedicalDataSourcesRequest.Builder().build(),
                            Executors.newSingleThreadExecutor(),
                            readReceiver);
                    assertThat(readReceiver.getResponse()).isEmpty();
                });
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_existsWithData_succeedsAndDeletes() throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        mManager.deleteMedicalDataSourceWithData(
                dataSource.getId(), Executors.newSingleThreadExecutor(), callback);

        callback.verifyNoExceptionOrThrow();
        HealthConnectReceiver<List<MedicalDataSource>> dataSourceReadReceiver =
                new HealthConnectReceiver<>();
        HealthConnectReceiver<ReadMedicalResourcesResponse> resourceReadReceiver =
                new HealthConnectReceiver<>();
        // Verifies that both data source and resource are deleted.
        runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            new GetMedicalDataSourcesRequest.Builder().build(),
                            Executors.newSingleThreadExecutor(),
                            dataSourceReadReceiver);
                    assertThat(dataSourceReadReceiver.getResponse()).isEmpty();
                    mManager.readMedicalResources(
                            new ReadMedicalResourcesInitialRequest.Builder(
                                            MEDICAL_RESOURCE_TYPE_VACCINES)
                                    .build(),
                            Executors.newSingleThreadExecutor(),
                            resourceReadReceiver);
                    assertThat(resourceReadReceiver.getResponse().getMedicalResources()).isEmpty();
                });
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_doesntExist_throws() throws Exception {
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        mManager.deleteMedicalDataSourceWithData(
                DATA_SOURCE_ID, Executors.newSingleThreadExecutor(), callback);

        assertThat(callback.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_withManagePerm_existsWithoutData_succeedsAndDeletes()
            throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalDataSourceWithData(
                            dataSource.getId(), Executors.newSingleThreadExecutor(), callback);
                    callback.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        // Verifies that data source is deleted.
        HealthConnectReceiver<List<MedicalDataSource>> readReceiver = new HealthConnectReceiver<>();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            new GetMedicalDataSourcesRequest.Builder().build(),
                            Executors.newSingleThreadExecutor(),
                            readReceiver);
                    assertThat(readReceiver.getResponse()).isEmpty();
                });
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_withManagePerm_existsWithData_succeedsAndDeletes()
            throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalDataSourceWithData(
                            dataSource.getId(), Executors.newSingleThreadExecutor(), callback);
                    callback.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        HealthConnectReceiver<List<MedicalDataSource>> dataSourceReadReceiver =
                new HealthConnectReceiver<>();
        HealthConnectReceiver<ReadMedicalResourcesResponse> resourceReadReceiver =
                new HealthConnectReceiver<>();
        // Verifies that both data source and resource are deleted.
        runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            new GetMedicalDataSourcesRequest.Builder().build(),
                            Executors.newSingleThreadExecutor(),
                            dataSourceReadReceiver);
                    assertThat(dataSourceReadReceiver.getResponse()).isEmpty();
                    mManager.readMedicalResources(
                            new ReadMedicalResourcesInitialRequest.Builder(
                                            MEDICAL_RESOURCE_TYPE_VACCINES)
                                    .build(),
                            Executors.newSingleThreadExecutor(),
                            resourceReadReceiver);
                    assertThat(resourceReadReceiver.getResponse().getMedicalResources()).isEmpty();
                });
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_withManagePerm_doesntExist_throws() throws Exception {
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalDataSourceWithData(
                            DATA_SOURCE_ID, Executors.newSingleThreadExecutor(), callback);
                    assertThat(callback.assertAndGetException().getErrorCode())
                            .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_emptyId_throws() throws Exception {
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        mManager.deleteMedicalDataSourceWithData(
                " ", Executors.newSingleThreadExecutor(), callback);

        assertThat(callback.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_invalidId_throws() throws Exception {
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        mManager.deleteMedicalDataSourceWithData(
                "illegal id", Executors.newSingleThreadExecutor(), callback);

        assertThat(callback.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_differentPackage_throws() throws Exception {
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource dataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource resource =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        mManager.deleteMedicalDataSourceWithData(
                dataSource.getId(), Executors.newSingleThreadExecutor(), callback);

        assertThat(callback.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        HealthConnectReceiver<List<MedicalDataSource>> dataSourceReadReceiver =
                new HealthConnectReceiver<>();
        HealthConnectReceiver<List<MedicalResource>> resourceReadReceiver =
                new HealthConnectReceiver<>();
        // Verifies that both data source and resource are NOT deleted.
        runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            List.of(dataSource.getId()),
                            Executors.newSingleThreadExecutor(),
                            dataSourceReadReceiver);
                    assertThat(dataSourceReadReceiver.getResponse())
                            .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                            .containsExactly(dataSource);
                    mManager.readMedicalResources(
                            List.of(resource.getId()),
                            Executors.newSingleThreadExecutor(),
                            resourceReadReceiver);
                    assertThat(resourceReadReceiver.getResponse()).containsExactly(resource);
                });
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_inForegroundNoPermission_throws() {
        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.deleteMedicalDataSourceWithData(DATA_SOURCE_ID));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_inForegroundOnlyReadPerm_throws() {
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), READ_MEDICAL_DATA_VACCINES);

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.deleteMedicalDataSourceWithData(DATA_SOURCE_ID));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_inBackgroundNoPermission_throws() {
        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.deleteMedicalDataSourceWithData(DATA_SOURCE_ID));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testDeleteMedicalDataSource_withManagePerm_differentPackage_succeedsAndDeletes()
            throws Exception {
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource dataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource resource =
                PHR_BACKGROUND_APP.upsertMedicalResource(
                        dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.deleteMedicalDataSourceWithData(
                            dataSource.getId(), Executors.newSingleThreadExecutor(), callback);
                    callback.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        HealthConnectReceiver<List<MedicalDataSource>> dataSourceReadReceiver =
                new HealthConnectReceiver<>();
        HealthConnectReceiver<List<MedicalResource>> resourceReadReceiver =
                new HealthConnectReceiver<>();
        // Verifies that both data source and resource are deleted.
        runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            List.of(dataSource.getId()),
                            Executors.newSingleThreadExecutor(),
                            dataSourceReadReceiver);
                    assertThat(dataSourceReadReceiver.getResponse()).isEmpty();
                    mManager.readMedicalResources(
                            List.of(resource.getId()),
                            Executors.newSingleThreadExecutor(),
                            resourceReadReceiver);
                    assertThat(resourceReadReceiver.getResponse()).isEmpty();
                });
    }
}
