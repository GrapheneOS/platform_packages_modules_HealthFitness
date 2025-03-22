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
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.MAX_FOREGROUND_WRITE_CALL_15M;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_BACKGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrCtsTestUtils.PHR_FOREGROUND_APP;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_DISPLAY_NAME_EXCEEDED_CHARS;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_FHIR_BASE_URI_EXCEEDED_CHARS;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_DATA_SOURCE_BASE_URI;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_VERSION_R4B;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_VERSION_UNSUPPORTED;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.MAX_ALLOWED_MEDICAL_DATA_SOURCES;
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

import android.health.connect.CreateMedicalDataSourceRequest;
import android.health.connect.GetMedicalDataSourcesRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.MedicalDataSource;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.HealthConnectReceiver;
import android.healthconnect.cts.utils.TestUtils;
import android.net.Uri;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class CreateMedicalDataSourceCtsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private HealthConnectManager mManager;
    private PhrCtsTestUtils mUtil;

    private static final String APP_PACKAGE_NAME = "android.healthconnect.cts.phr";

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
    public void testCreateMedicalDataSource_migrationInProgress_apiBlocked()
            throws InterruptedException {
        startMigrationWithShellPermissionIdentity();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<MedicalDataSource> receiver = new HealthConnectReceiver<>();

        mManager.createMedicalDataSource(getCreateMedicalDataSourceRequest(), executor, receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);

        finishMigrationWithShellPermissionIdentity();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_hasDataManagementPermission_throws()
            throws InterruptedException {
        HealthConnectReceiver<MedicalDataSource> receiver = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.createMedicalDataSource(
                            getCreateMedicalDataSourceRequest(),
                            Executors.newSingleThreadExecutor(),
                            receiver);
                    assertThat(receiver.assertAndGetException().getErrorCode())
                            .isEqualTo(HealthConnectException.ERROR_SECURITY);
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_writeLimitExceeded_throws() throws Exception {
        MedicalDataSource dataSource =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("test-id"));
        // Make the maximum number of delete medical resources calls just to use up the WRITE quota,
        // because data sources one app can create has a lower limit than this rate limit. Minus 1
        // because of the above call.
        int maximumCalls = MAX_FOREGROUND_WRITE_CALL_15M / mUtil.mLimitsAdjustmentForTesting - 1;
        float remainingQuota = mUtil.tryAcquireCallQuotaNTimesForWrite(dataSource, maximumCalls);

        // Exceed the quota by using up any remaining quota that accumulated during the previous
        // calls and make one additional call.
        HealthConnectReceiver<MedicalDataSource> callback = new HealthConnectReceiver<>();
        int additionalCalls = (int) Math.ceil(remainingQuota) + 1;
        for (int i = 0; i < additionalCalls; i++) {
            mManager.createMedicalDataSource(
                    getCreateMedicalDataSourceRequest(String.valueOf(i)),
                    Executors.newSingleThreadExecutor(),
                    callback);
        }

        HealthConnectException exception = callback.assertAndGetException();
        assertThat(callback.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED);
        assertThat(exception.getMessage()).contains("API call quota exceeded");
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_invalidEmptyDisplayName_throws()
            throws NoSuchFieldException, IllegalAccessException {
        CreateMedicalDataSourceRequest request = getCreateMedicalDataSourceRequest();

        setFieldValueUsingReflection(request, "mDisplayName", "");

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.createMedicalDataSource(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_displayNameExceedsLimit_throws()
            throws NoSuchFieldException, IllegalAccessException {
        CreateMedicalDataSourceRequest request = getCreateMedicalDataSourceRequest();

        setFieldValueUsingReflection(
                request, "mDisplayName", DATA_SOURCE_DISPLAY_NAME_EXCEEDED_CHARS);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.createMedicalDataSource(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_invalidEmptyFhirBaseUri_throws()
            throws NoSuchFieldException, IllegalAccessException {
        CreateMedicalDataSourceRequest request = getCreateMedicalDataSourceRequest();

        setFieldValueUsingReflection(request, "mFhirBaseUri", Uri.EMPTY);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.createMedicalDataSource(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_fhirBaseUriExceedsLimit_throws()
            throws NoSuchFieldException, IllegalAccessException {
        CreateMedicalDataSourceRequest request = getCreateMedicalDataSourceRequest();

        setFieldValueUsingReflection(
                request, "mFhirBaseUri", DATA_SOURCE_FHIR_BASE_URI_EXCEEDED_CHARS);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.createMedicalDataSource(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_invalidFhirVersion_throws()
            throws NoSuchFieldException, IllegalAccessException {
        CreateMedicalDataSourceRequest request = getCreateMedicalDataSourceRequest();

        setFieldValueUsingReflection(request, "mFhirVersion", FHIR_VERSION_UNSUPPORTED);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mManager.createMedicalDataSource(
                                request,
                                Executors.newSingleThreadExecutor(),
                                new HealthConnectReceiver<>()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_succeeds() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<MedicalDataSource> receiver = new HealthConnectReceiver<>();
        CreateMedicalDataSourceRequest request = getCreateMedicalDataSourceRequest();

        mManager.createMedicalDataSource(request, executor, receiver);

        MedicalDataSource responseDataSource = receiver.getResponse();
        assertThat(responseDataSource).isInstanceOf(MedicalDataSource.class);
        assertThat(responseDataSource.getId()).isNotEmpty();
        assertThat(responseDataSource.getFhirBaseUri()).isEqualTo(request.getFhirBaseUri());
        assertThat(responseDataSource.getDisplayName()).isEqualTo(request.getDisplayName());
        assertThat(responseDataSource.getFhirVersion()).isEqualTo(request.getFhirVersion());
        assertThat(responseDataSource.getPackageName()).isEqualTo(APP_PACKAGE_NAME);
        // Assert that it exists in the db
        HealthConnectReceiver<List<MedicalDataSource>> readReceiver = new HealthConnectReceiver<>();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            List.of(responseDataSource.getId()),
                            Executors.newSingleThreadExecutor(),
                            readReceiver);
                    assertThat(readReceiver.getResponse()).hasSize(1);
                });
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_maxNumberOfSources_succeeds()
            throws InterruptedException {
        for (int i = 0; i < MAX_ALLOWED_MEDICAL_DATA_SOURCES - 1; i++) {
            String suffix = String.valueOf(i);
            mUtil.createDataSource(getCreateMedicalDataSourceRequest(suffix));
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<MedicalDataSource> receiver = new HealthConnectReceiver<>();
        mManager.createMedicalDataSource(getCreateMedicalDataSourceRequest(), executor, receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_moreThanAllowedMax_throws()
            throws InterruptedException {
        for (int i = 0; i < MAX_ALLOWED_MEDICAL_DATA_SOURCES; i++) {
            String suffix = String.valueOf(i);
            mUtil.createDataSource(getCreateMedicalDataSourceRequest(suffix));
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<MedicalDataSource> receiver = new HealthConnectReceiver<>();
        mManager.createMedicalDataSource(getCreateMedicalDataSourceRequest(), executor, receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_duplicateDisplayName_throws()
            throws InterruptedException {
        mUtil.createDataSource(getCreateMedicalDataSourceRequest("ds1"));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<MedicalDataSource> receiver = new HealthConnectReceiver<>();
        mManager.createMedicalDataSource(
                getCreateMedicalDataSourceRequest("ds1"), executor, receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_duplicateDisplayNameFromSeparatePackages_succeeds()
            throws Exception {
        CreateMedicalDataSourceRequest request1 =
                new CreateMedicalDataSourceRequest.Builder(
                                DATA_SOURCE_FHIR_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                FHIR_VERSION_R4)
                        .build();
        CreateMedicalDataSourceRequest request2 =
                new CreateMedicalDataSourceRequest.Builder(
                                DIFFERENT_DATA_SOURCE_BASE_URI,
                                DATA_SOURCE_DISPLAY_NAME,
                                FHIR_VERSION_R4B)
                        .build();
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        PHR_BACKGROUND_APP.createMedicalDataSource(request1);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        HealthConnectReceiver<MedicalDataSource> receiver = new HealthConnectReceiver<>();
        mManager.createMedicalDataSource(request2, executor, receiver);

        receiver.verifyNoExceptionOrThrow();
        // Assert both data sources exist
        HealthConnectReceiver<List<MedicalDataSource>> readReceiver = new HealthConnectReceiver<>();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            new GetMedicalDataSourcesRequest.Builder().build(),
                            Executors.newSingleThreadExecutor(),
                            readReceiver);
                    assertThat(readReceiver.getResponse()).hasSize(2);
                });
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_inForegroundNoWritePerms_throws() {
        // No write permission has been granted.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                PHR_FOREGROUND_APP.createMedicalDataSource(
                                        getCreateMedicalDataSourceRequest()));

        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testCreateMedicalDataSource_inBackgroundNoWritePerms_throws() {
        // No write permission has been granted.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                PHR_BACKGROUND_APP.createMedicalDataSource(
                                        getCreateMedicalDataSourceRequest()));

        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }
}
