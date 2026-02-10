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
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_READ;
import static android.healthconnect.testing.cts.PermissionUtils.grantHealthPermission;
import static android.healthconnect.testing.cts.PermissionUtils.grantHealthPermissions;
import static android.healthconnect.testing.cts.PermissionUtils.revokeAllHealthPermissions;
import static android.healthconnect.testing.cts.PermissionUtils.revokeHealthPermission;
import static android.healthconnect.testing.cts.PhrCtsTestUtils.MAX_FOREGROUND_READ_CALL_15M;
import static android.healthconnect.testing.cts.PhrCtsTestUtils.PHR_BACKGROUND_APP;
import static android.healthconnect.testing.cts.PhrCtsTestUtils.PHR_BACKGROUND_APP_PKG;
import static android.healthconnect.testing.cts.PhrCtsTestUtils.PHR_FOREGROUND_APP;
import static android.healthconnect.testing.cts.PhrCtsTestUtils.PHR_FOREGROUND_APP_PKG;
import static android.healthconnect.testing.cts.TestUtils.finishMigrationWithShellPermissionIdentity;
import static android.healthconnect.testing.cts.TestUtils.startMigrationWithShellPermissionIdentity;
import static android.healthconnect.testing.shared.DataFactory.MAXIMUM_PAGE_SIZE;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DIFFERENT_FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_ALLERGY;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.MEDICAL_DATA_SOURCE_EQUIVALENCE;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.addCompletedStatus;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.testing.cts.HealthConnectReceiver;
import android.healthconnect.testing.cts.PhrCtsTestUtils;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.healthconnect.testing.shared.phr.PhrDataFactory;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.SystemUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class GetMedicalDataSourcesByIdsCtsTest {
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
    public void before() throws Exception {
        // To make sure we don't leave any state behind after running each test.
        revokeAllHealthPermissions(PHR_BACKGROUND_APP_PKG, "to test specific permissions");
        revokeAllHealthPermissions(PHR_FOREGROUND_APP_PKG, "to test specific permissions");
        TestUtils.deleteAllDataFromHealthConnect();
        mManager = TestUtils.getHealthConnectManager();
        mUtil = new PhrCtsTestUtils(TestUtils.getHealthConnectManager());
        if (TestUtils.setLowerRateLimitsForTesting(true)) {
            // 10 comes from the setLowerRateLimitsForTesting method in RateLimiter.
            mUtil.mLimitsAdjustmentForTesting = 40;
        }
    }

    @After
    public void after() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
        TestUtils.setLowerRateLimitsForTesting(false);
    }

    @Test
    public void testGetMedicalDataSourcesById_migrationInProgress_apiBlocked() throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();

        startMigrationWithShellPermissionIdentity();
        mManager.getMedicalDataSources(
                List.of(dataSource.getId()), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        finishMigrationWithShellPermissionIdentity();
    }

    @Test
    public void testGetMedicalDataSourcesById_readLimitExceeded_throws()
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
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();
        int additionalCalls = (int) Math.ceil(remainingQuota) + 1;
        for (int i = 0; i < additionalCalls; i++) {
            mManager.getMedicalDataSources(
                    List.of(dataSource.getId()), Executors.newSingleThreadExecutor(), receiver);
        }

        HealthConnectException exception = receiver.assertAndGetException();
        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_RATE_LIMIT_EXCEEDED);
        assertThat(exception.getMessage()).contains("API call quota exceeded");
    }

    @Test
    public void testGetMedicalDataSourcesById_emptyIds_returnsEmptyList()
            throws InterruptedException {
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();

        mManager.getMedicalDataSources(List.of(), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).isEmpty();
    }

    @Test
    public void testGetMedicalDataSourcesById_exceedsMaxPageSize_throws()
            throws InterruptedException {
        List<String> dataSourceIds = new ArrayList<>(MAXIMUM_PAGE_SIZE + 1);
        for (int i = 0; i < MAXIMUM_PAGE_SIZE + 1; i++) {
            dataSourceIds.add(UUID.randomUUID().toString());
        }

        Exception exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mManager.getMedicalDataSources(
                                        dataSourceIds,
                                        Executors.newSingleThreadExecutor(),
                                        new HealthConnectReceiver<>()));
        assertThat(exception.getMessage()).contains("The number of requested IDs must be <= 5000");
    }

    @Test
    public void testGetMedicalDataSourcesById_invalidId_throws() throws Exception {
        HealthConnectReceiver<List<MedicalDataSource>> callback = new HealthConnectReceiver<>();

        mManager.getMedicalDataSources(
                List.of("illegal id"), Executors.newSingleThreadExecutor(), callback);

        assertThat(callback.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void testGetMedicalDataSourcesById_someValidAndInvalidIds_throws() throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();

        // A mix of valid and invalid ids are given.
        mManager.getMedicalDataSources(
                List.of(dataSource.getId(), "foo"), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void testGetMedicalDataSourcesById_withManagePerm_notPresent_returnsEmptyList()
            throws Exception {
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();
        List<String> ids = List.of(DATA_SOURCE_ID);

        SystemUtil.runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            ids, Executors.newSingleThreadExecutor(), receiver);
                    assertThat(receiver.getResponse()).isEmpty();
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    public void testGetMedicalDataSourcesById_withoutManagePerm_notPresent_returnsEmptyList()
            throws Exception {
        mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();

        // Testing the case where there exists dataSources in HC, but the user is requesting
        // a valid dataSource ID that does not exist in HC.
        mManager.getMedicalDataSources(
                List.of(DATA_SOURCE_ID), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).isEmpty();
    }

    @Test
    public void testGetMedicalDataSourcesById_withManageHealthDataPerm_getsAll() throws Exception {
        // Data written by a different app.
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);
        MedicalDataSource dataSource1 =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        // Data written by the reading app itself.
        MedicalDataSource dataSource2 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();

        // If app has MANAGE_HEALTH_DATA permission, it should be able to read all dataSources.
        SystemUtil.runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            List.of(dataSource1.getId(), dataSource2.getId()),
                            Executors.newSingleThreadExecutor(),
                            receiver);
                    assertThat(receiver.getResponse()).containsExactly(dataSource1, dataSource2);
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    public void testGetMedicalDataSourcesById_onePresentWithoutData_returnsItAndNullUpdateTime()
            throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();

        mManager.getMedicalDataSources(
                List.of(dataSource.getId()), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).containsExactly(dataSource);
        assertThat(dataSource.getLastDataUpdateTime()).isNull();
    }

    @Test
    public void testGetMedicalDataSourcesById_onePresentWithData_returnsCorrectLastDataUpdateTime()
            throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        Instant insertTime = Instant.now();
        mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();

        mManager.getMedicalDataSources(
                List.of(dataSource.getId()), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).hasSize(1);
        Instant lastDataUpdateTime = receiver.getResponse().get(0).getLastDataUpdateTime();
        assertThat(lastDataUpdateTime).isAtLeast(insertTime);
        assertThat(lastDataUpdateTime).isAtMost(Instant.now());
    }

    @Test
    public void testGetMedicalDataSourcesById_dataUpdated_returnsCorrectLastDataUpdateTime()
            throws Exception {
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        Instant updateTime = Instant.now();
        mUtil.upsertMedicalData(dataSource.getId(), addCompletedStatus(FHIR_DATA_IMMUNIZATION));
        HealthConnectReceiver<List<MedicalDataSource>> receiver = new HealthConnectReceiver<>();

        mManager.getMedicalDataSources(
                List.of(dataSource.getId()), Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse()).hasSize(1);
        Instant lastDataUpdateTime = receiver.getResponse().get(0).getLastDataUpdateTime();
        assertThat(lastDataUpdateTime).isAtLeast(updateTime);
        assertThat(lastDataUpdateTime).isAtMost(Instant.now());
    }

    @Test
    public void testGetMedicalResourcesByIds_deletedResource_notCountedInLastDataUpdateTime()
            throws InterruptedException {
        Instant beforeUpsertTime = Instant.now();
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("ds1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("ds2"));
        MedicalResource dataSource1resource1 =
                mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalResource dataSource2resource1 =
                mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource2.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        HealthConnectReceiver<Void> callback = new HealthConnectReceiver<>();
        Instant beforeDeleteTime = Instant.now();
        mManager.deleteMedicalResources(
                List.of(dataSource1resource1.getId(), dataSource2resource1.getId()),
                Executors.newSingleThreadExecutor(),
                callback);
        callback.verifyNoExceptionOrThrow();
        HealthConnectReceiver<List<MedicalDataSource>> readReceiver1 =
                new HealthConnectReceiver<>();
        HealthConnectReceiver<List<MedicalDataSource>> readReceiver2 =
                new HealthConnectReceiver<>();

        mManager.getMedicalDataSources(
                List.of(dataSource1.getId()), Executors.newSingleThreadExecutor(), readReceiver1);
        mManager.getMedicalDataSources(
                List.of(dataSource2.getId()), Executors.newSingleThreadExecutor(), readReceiver2);

        // The last data update time of dataSource1 is expected to be null because we have deleted
        // all data.
        assertThat(readReceiver1.getResponse().get(0).getLastDataUpdateTime()).isNull();
        // The last data update time of dataSource2 is expected to be before the delete time, as the
        // delete is not taken into account.
        assertThat(readReceiver2.getResponse().get(0).getLastDataUpdateTime())
                .isAtLeast(beforeUpsertTime);
        assertThat(readReceiver2.getResponse().get(0).getLastDataUpdateTime())
                .isAtMost(beforeDeleteTime);
    }

    @Test
    public void testGetMedicalDataSourcesById_inForegroundWithNoPerms_throws() {
        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_FOREGROUND_APP.getMedicalDataSources(List.of(DATA_SOURCE_ID)));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testGetMedicalDataSourcesById_inForegroundWithReadPermNoWritePerm()
            throws Exception {
        // To write data from two different apps.
        grantHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);
        grantHealthPermissions(
                PHR_FOREGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in foreground, no write permission but has
        // vaccine read permission. App can read all dataSources belonging to vaccines.
        revokeHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);
        List<MedicalDataSource> result =
                PHR_FOREGROUND_APP.getMedicalDataSources(
                        List.of(
                                dataSource1Foreground.getId(),
                                dataSource1Background.getId(),
                                dataSource2Background.getId()));

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Foreground, dataSource1Background);
    }

    @Test
    public void testGetMedicalDataSourcesById_inForegroundHasWritePermNoReadPerms()
            throws Exception {
        // To write data from two different apps.
        grantHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);
        grantHealthPermission(PHR_FOREGROUND_APP_PKG, WRITE_MEDICAL_DATA);
        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in foreground, has write permission but
        // no read permission for any resource types.
        // App can only read dataSources they wrote themselves.
        List<MedicalDataSource> result =
                PHR_FOREGROUND_APP.getMedicalDataSources(
                        List.of(
                                dataSource1Foreground.getId(),
                                dataSource1Background.getId(),
                                dataSource2Background.getId()));

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Foreground);
    }

    @Test
    public void testGetMedicalDataSourcesById_inForegroundHasWriteAndReadPerms() throws Exception {
        // To write data from two different apps.
        grantHealthPermission(PHR_BACKGROUND_APP_PKG, WRITE_MEDICAL_DATA);
        grantHealthPermissions(
                PHR_FOREGROUND_APP_PKG, List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
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
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types.
        List<MedicalDataSource> result =
                PHR_FOREGROUND_APP.getMedicalDataSources(
                        List.of(
                                dataSource1Foreground.getId(),
                                dataSource2Foreground.getId(),
                                dataSource1Background.getId(),
                                dataSource2Background.getId()));

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(
                        dataSource1Foreground, dataSource2Foreground, dataSource1Background);
    }

    @Test
    public void testGetMedicalDataSourcesById_inBgWithoutBgPermHasWritePermNoReadPerms()
            throws Exception {
        // The app under test.
        grantHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        // Another app to write some more data.
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());

        // Calling app is running in the background without background read permission.
        // The app has write permission and read vaccine permission.
        // The app can read data sources created by itself only.
        List<MedicalDataSource> result =
                PHR_BACKGROUND_APP.getMedicalDataSources(
                        List.of(dataSource1Foreground.getId(), dataSource2Background.getId()));

        assertThat(result).containsExactly(dataSource2Background);
    }

    @Test
    public void testGetMedicalDataSourcesById_inBgWithNoPerms_throws() throws Exception {
        // App has not been granted any permissions.
        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> PHR_BACKGROUND_APP.getMedicalDataSources(List.of(DATA_SOURCE_ID)));
        assertThat(exception.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testGetMedicalDataSourcesById_inBgWithoutBgPermNoWritePermOnlyReadPerm()
            throws Exception {
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in background without background read perm, no write permission but has
        // vaccine read permission. App can read dataSources belonging to vaccines that
        // the app wrote itself.
        revokeHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        List<MedicalDataSource> result =
                PHR_BACKGROUND_APP.getMedicalDataSources(
                        List.of(
                                dataSource1Foreground.getId(),
                                dataSource1Background.getId(),
                                dataSource2Background.getId()));

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background);
    }

    @Test
    public void testGetMedicalDataSourcesById_inBgWithBgReadPermNoWritePermHasReadPerm()
            throws Exception {
        // To write data from two different apps.
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(
                        WRITE_MEDICAL_DATA,
                        READ_HEALTH_DATA_IN_BACKGROUND,
                        READ_MEDICAL_DATA_VACCINES));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSource1Foreground.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        PHR_BACKGROUND_APP.upsertMedicalResource(
                dataSource1Background.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource2Background.getId(), FHIR_DATA_ALLERGY);

        // App is in background with background read permission, no write permission but has
        // vaccine read permission. App can read all dataSources belonging to vaccines.
        revokeHealthPermission(PHR_BACKGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        List<MedicalDataSource> result =
                PHR_BACKGROUND_APP.getMedicalDataSources(
                        List.of(
                                dataSource1Foreground.getId(),
                                dataSource1Background.getId(),
                                dataSource2Background.getId()));

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Foreground, dataSource1Background);
    }

    @Test
    public void testGetMedicalDataSourcesById_inBgWithBgReadPermHasWritePermNoReadPerms()
            throws Exception {
        // To write data from two different apps.
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_HEALTH_DATA_IN_BACKGROUND));
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource dataSource1Foreground =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        MedicalDataSource dataSource1Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/1"));
        MedicalDataSource dataSource2Background =
                PHR_BACKGROUND_APP.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("ds/2"));

        // App is in background with background read permission, has write permission but
        // no read permission for any resource types.
        // App can only read dataSources they wrote themselves.
        List<MedicalDataSource> result =
                PHR_BACKGROUND_APP.getMedicalDataSources(
                        List.of(
                                dataSource1Foreground.getId(),
                                dataSource1Background.getId(),
                                dataSource2Background.getId()));

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(dataSource1Background, dataSource2Background);
    }

    @Test
    public void testGetMedicalDataSourcesById_inBgWithBgPermHasWriteAndReadPerm() throws Exception {
        // To write data from two different apps.
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

        // App is in background, has background read permission,
        // has write permission and vaccine read permissions.
        // App can read dataSources they wrote themselves and dataSources belonging to
        // vaccine resource types.
        List<MedicalDataSource> result =
                PHR_BACKGROUND_APP.getMedicalDataSources(
                        List.of(
                                dataSource1Foreground.getId(),
                                dataSource2Foreground.getId(),
                                dataSource1Background.getId(),
                                dataSource2Background.getId()));

        assertThat(result)
                .comparingElementsUsing(MEDICAL_DATA_SOURCE_EQUIVALENCE)
                .containsExactly(
                        dataSource1Background, dataSource2Background, dataSource1Foreground);
    }

    @Test
    public void testGetMedicalDataSourcesById_inBgWithoutBgPermHasReadWritePerm_noAccessLog()
            throws Exception {
        // App has write and read permission for vaccines, but not background read permission, so
        // can only read its own data sources
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        MedicalDataSource dataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        List<AccessLog> upsertAccessLogs = TestUtils.queryAccessLogs();

        List<MedicalDataSource> readDataSources =
                PHR_BACKGROUND_APP.getMedicalDataSources(List.of(dataSource.getId()));

        // App can read the data source through self read, so no access logs expected.
        assertThat(readDataSources).hasSize(1);
        assertThat(TestUtils.queryAccessLogs()).hasSize(upsertAccessLogs.size());
    }

    @Test
    public void testGetMedicalDataSourcesById_inBgWithBgPermReadPermForLinkedSources_accessLogs()
            throws Exception {
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(READ_HEALTH_DATA_IN_BACKGROUND, READ_MEDICAL_DATA_VACCINES));
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        mUtil.upsertMedicalData(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        List<AccessLog> upsertAccessLogs = TestUtils.queryAccessLogs();
        Instant timeBeforeRead = Instant.now();

        List<MedicalDataSource> result =
                PHR_BACKGROUND_APP.getMedicalDataSources(List.of(dataSource.getId()));
        List<AccessLog> accessLogs = TestUtils.queryAccessLogs();
        accessLogs.sort(Comparator.comparing(AccessLog::getAccessTime));

        assertThat(result).hasSize(1);
        assertThat(accessLogs).hasSize(upsertAccessLogs.size() + 1);
        AccessLog readLog = accessLogs.get(upsertAccessLogs.size());
        assertThat(readLog.getPackageName()).isEqualTo(PHR_BACKGROUND_APP.getPackageName());
        assertThat(readLog.getAccessTime()).isAtLeast(timeBeforeRead);
        assertThat(readLog.getMedicalResourceTypes()).isEmpty();
        assertThat(readLog.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
        assertThat(readLog.getRecordTypes()).isEmpty();
        assertThat(readLog.isMedicalDataSourceAccessed()).isTrue();
    }

    @Test
    public void testGetMedicalDataSourcesById_inBgWithBgPerm_noMatchingDataNoAccessLogs()
            throws Exception {
        // App has background read and read permission for vaccines, but the data source contains
        // no resources
        grantHealthPermissions(
                PHR_BACKGROUND_APP.getPackageName(),
                List.of(READ_HEALTH_DATA_IN_BACKGROUND, READ_MEDICAL_DATA_VACCINES));
        MedicalDataSource dataSource = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        List<AccessLog> upsertAccessLogs = TestUtils.queryAccessLogs();

        List<MedicalDataSource> result =
                PHR_BACKGROUND_APP.getMedicalDataSources(List.of(dataSource.getId()));

        assertThat(result).isEmpty();
        assertThat(TestUtils.queryAccessLogs()).hasSize(upsertAccessLogs.size());
    }

    @Test
    public void testGetMedicalDataSourcesById_dataManagementPerm_noAccessLogs() throws Exception {
        grantHealthPermissions(PHR_BACKGROUND_APP.getPackageName(), List.of(WRITE_MEDICAL_DATA));
        MedicalDataSource dataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        List<AccessLog> upsertAccessLogs = TestUtils.queryAccessLogs();
        HealthConnectReceiver<List<MedicalDataSource>> callback = new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.getMedicalDataSources(
                            List.of(dataSource.getId()),
                            Executors.newSingleThreadExecutor(),
                            callback);
                    callback.verifyNoExceptionOrThrow();
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        assertThat(TestUtils.queryAccessLogs()).hasSize(upsertAccessLogs.size());
    }

    @Test
    public void testGetMedicalDataSourcesById_writePermOnly_noAccessLogsForSelfData()
            throws Exception {
        grantHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        MedicalDataSource emptyDataSource =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSourceWithVaccine =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest("2"));
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSourceWithVaccine.getId(), FHIR_DATA_IMMUNIZATION);
        List<AccessLog> upsertAccessLogs = TestUtils.queryAccessLogs();

        List<MedicalDataSource> result =
                PHR_FOREGROUND_APP.getMedicalDataSources(
                        List.of(emptyDataSource.getId(), dataSourceWithVaccine.getId()));

        assertThat(result).hasSize(2);
        assertThat(TestUtils.queryAccessLogs()).hasSize(upsertAccessLogs.size());
    }

    @Test
    public void testGetMedicalDataSourcesById_readPermOnly_accessLogForSelfData() throws Exception {
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(READ_MEDICAL_DATA_VACCINES, WRITE_MEDICAL_DATA));
        MedicalDataSource dataSourceWithVaccine =
                PHR_FOREGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_FOREGROUND_APP.upsertMedicalResource(
                dataSourceWithVaccine.getId(), FHIR_DATA_IMMUNIZATION);
        revokeHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        List<AccessLog> upsertAccessLogs = TestUtils.queryAccessLogs();
        Instant timeBeforeRead = Instant.now();

        List<MedicalDataSource> result =
                PHR_FOREGROUND_APP.getMedicalDataSources(List.of(dataSourceWithVaccine.getId()));

        assertThat(result).hasSize(1);
        List<AccessLog> accessLogs = TestUtils.queryAccessLogs();
        int upsertAccessLogSize = upsertAccessLogs.size();
        assertThat(accessLogs).hasSize(upsertAccessLogSize + 1);
        AccessLog log = accessLogs.get(upsertAccessLogSize);
        assertThat(log.getPackageName()).isEqualTo(PHR_FOREGROUND_APP.getPackageName());
        assertThat(log.getAccessTime()).isAtLeast(timeBeforeRead);
        assertThat(log.getMedicalResourceTypes()).isEmpty();
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
        assertThat(log.getRecordTypes()).isEmpty();
        assertThat(log.isMedicalDataSourceAccessed()).isTrue();
    }

    @Test
    public void testGetMedicalDataSourcesById_readPermOnly_accessLogsForNonSelfData()
            throws Exception {
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermissions(PHR_BACKGROUND_APP.getPackageName(), List.of(WRITE_MEDICAL_DATA));
        MedicalDataSource dataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        revokeHealthPermission(PHR_FOREGROUND_APP.getPackageName(), WRITE_MEDICAL_DATA);
        List<AccessLog> upsertAccessLogs = TestUtils.queryAccessLogs();
        Instant timeBeforeRead = Instant.now();

        List<MedicalDataSource> result =
                PHR_FOREGROUND_APP.getMedicalDataSources(List.of(dataSource.getId()));
        List<AccessLog> accessLogs = TestUtils.queryAccessLogs();
        accessLogs.sort(Comparator.comparing(AccessLog::getAccessTime));

        assertThat(result).hasSize(1);
        int upsertAccessLogSize = upsertAccessLogs.size();
        assertThat(accessLogs).hasSize(upsertAccessLogSize + 1);
        AccessLog log = accessLogs.get(upsertAccessLogSize);
        assertThat(log.getPackageName()).isEqualTo(PHR_FOREGROUND_APP.getPackageName());
        assertThat(log.getAccessTime()).isAtLeast(timeBeforeRead);
        assertThat(log.getMedicalResourceTypes()).isEmpty();
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
        assertThat(log.getRecordTypes()).isEmpty();
        assertThat(log.isMedicalDataSourceAccessed()).isTrue();
    }

    @Test
    public void testGetMedicalDataSourcesById_readWritePerm_accessLogsForNonSelfData()
            throws Exception {
        grantHealthPermissions(
                PHR_FOREGROUND_APP.getPackageName(),
                List.of(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES));
        grantHealthPermissions(PHR_BACKGROUND_APP.getPackageName(), List.of(WRITE_MEDICAL_DATA));
        MedicalDataSource dataSource =
                PHR_BACKGROUND_APP.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        PHR_BACKGROUND_APP.upsertMedicalResource(dataSource.getId(), FHIR_DATA_IMMUNIZATION);
        List<AccessLog> upsertAccessLogs = TestUtils.queryAccessLogs();
        Instant timeBeforeRead = Instant.now();

        List<MedicalDataSource> result =
                PHR_FOREGROUND_APP.getMedicalDataSources(List.of(dataSource.getId()));
        List<AccessLog> accessLogs = TestUtils.queryAccessLogs();
        accessLogs.sort(Comparator.comparing(AccessLog::getAccessTime));

        assertThat(result).hasSize(1);
        assertThat(accessLogs).hasSize(upsertAccessLogs.size() + 1);
        AccessLog readLog = accessLogs.get(upsertAccessLogs.size());
        assertThat(readLog.getPackageName()).isEqualTo(PHR_FOREGROUND_APP.getPackageName());
        assertThat(readLog.getAccessTime()).isAtLeast(timeBeforeRead);
        assertThat(readLog.getMedicalResourceTypes()).isEmpty();
        assertThat(readLog.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
        assertThat(readLog.getRecordTypes()).isEmpty();
        assertThat(readLog.isMedicalDataSourceAccessed()).isTrue();
    }
}
