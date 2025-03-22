/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.healthconnect.cts;

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.healthconnect.cts.lib.TestAppProxy.APP_WRITE_PERMS_ONLY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.utils.DataFactory.getTestRecords;
import static android.healthconnect.cts.utils.HealthConnectReceiver.callAndGetResponseWithShellPermissionIdentity;
import static android.healthconnect.cts.utils.TestOutcomeReceiver.outcomeExecutor;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import static com.android.compatibility.common.util.SystemUtil.getEventually;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.health.connect.ApplicationInfoResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.AppInfo;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.MedicalDataSource;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.HealthConnectReceiver;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.stream.Collectors;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class GetApplicationInfoTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private Context mContext;
    private HealthConnectManager mManager;
    private PhrCtsTestUtils mPhrTestUtils;

    @Before
    public void setUp() throws InterruptedException {
        deleteAllStagedRemoteData();
        mContext = ApplicationProvider.getApplicationContext();
        deleteAllRecords(mContext.getApplicationInfo().packageName);
        mManager = requireNonNull(mContext.getSystemService(HealthConnectManager.class));
        mPhrTestUtils = new PhrCtsTestUtils(mManager);
        mPhrTestUtils.deleteAllMedicalData();
    }

    @After
    public void after() throws InterruptedException {
        deleteAllStagedRemoteData();
        mPhrTestUtils.deleteAllMedicalData();
    }

    private void deleteAllRecords(String packageName) throws InterruptedException {
        verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName(packageName).build())
                        .build());
    }

    @Test
    public void testEmptyApplicationInfo() throws InterruptedException {
        ApplicationInfoResponse response =
                callAndGetResponseWithShellPermissionIdentity(
                        mManager::getContributorApplicationsInfo, MANAGE_HEALTH_DATA_PERMISSION);

        assertThat(response.getApplicationInfoList()).hasSize(0);
    }

    @Test
    public void testEmptyApplicationInfo_no_perm() throws InterruptedException {
        HealthConnectReceiver<ApplicationInfoResponse> receiver = new HealthConnectReceiver<>();
        mManager.getContributorApplicationsInfo(outcomeExecutor(), receiver);
        HealthConnectException healthConnectException = receiver.assertAndGetException();
        assertThat(healthConnectException.getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void testGetApplicationInfo() throws Exception {
        insertRecords(getTestRecords());
        // App info table will be updated in the background, so might take some additional time.
        ApplicationInfoResponse eventualResponse =
                getEventually(
                        () -> {
                            ApplicationInfoResponse response =
                                    callAndGetResponseWithShellPermissionIdentity(
                                            mManager::getContributorApplicationsInfo,
                                            MANAGE_HEALTH_DATA_PERMISSION);
                            assertThat(response.getApplicationInfoList()).hasSize(1);
                            return response;
                        });

        AppInfo appInfo = eventualResponse.getApplicationInfoList().get(0);
        ApplicationInfo applicationInfo = mContext.getApplicationInfo();
        assertThat(appInfo.getPackageName()).isEqualTo(applicationInfo.packageName);
        assertThat(appInfo.getName())
                .isEqualTo(
                        mContext.getPackageManager()
                                .getApplicationLabel(applicationInfo)
                                .toString());
        assertThat(appInfo.getIcon()).isNotNull();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetApplicationInfo_appCreatesMedicalDataSourceOnly_isInContributingApps()
            throws Exception {
        // Create health fitness data.
        insertRecords(getTestRecords());
        // Create medical data with a different package.
        APP_WRITE_PERMS_ONLY.createMedicalDataSource(getCreateMedicalDataSourceRequest());

        // App info table will be updated in the background, so might take some additional time.
        ApplicationInfoResponse eventualResponse =
                getEventually(
                        () -> {
                            ApplicationInfoResponse response =
                                    callAndGetResponseWithShellPermissionIdentity(
                                            mManager::getContributorApplicationsInfo,
                                            MANAGE_HEALTH_DATA_PERMISSION);
                            assertThat(response.getApplicationInfoList()).hasSize(2);
                            return response;
                        });

        assertThat(
                        eventualResponse.getApplicationInfoList().stream()
                                .map(AppInfo::getPackageName)
                                .collect(Collectors.toSet()))
                .containsExactly(
                        mContext.getApplicationInfo().packageName,
                        APP_WRITE_PERMS_ONLY.getPackageName());
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetApplicationInfo_appCreatesMedicalDataSourceWithData_isInContributingApps()
            throws Exception {
        // Create health fitness data.
        insertRecords(getTestRecords());
        // Create a dataSource and a medicalResource with a different package.
        MedicalDataSource dataSource =
                APP_WRITE_PERMS_ONLY.createMedicalDataSource(getCreateMedicalDataSourceRequest());
        APP_WRITE_PERMS_ONLY.upsertMedicalResource(dataSource.getId(), FHIR_DATA_IMMUNIZATION);

        // App info table will be updated in the background, so might take some additional time.
        ApplicationInfoResponse eventualResponse =
                getEventually(
                        () -> {
                            ApplicationInfoResponse response =
                                    callAndGetResponseWithShellPermissionIdentity(
                                            mManager::getContributorApplicationsInfo,
                                            MANAGE_HEALTH_DATA_PERMISSION);
                            assertThat(response.getApplicationInfoList()).hasSize(2);
                            return response;
                        });

        assertThat(
                        eventualResponse.getApplicationInfoList().stream()
                                .map(AppInfo::getPackageName)
                                .collect(Collectors.toSet()))
                .containsExactly(
                        mContext.getApplicationInfo().packageName,
                        APP_WRITE_PERMS_ONLY.getPackageName());
    }
}
