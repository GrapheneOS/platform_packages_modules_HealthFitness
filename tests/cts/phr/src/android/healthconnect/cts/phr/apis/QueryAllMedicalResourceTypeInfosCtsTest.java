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
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DIFFERENT_FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_ALLERGY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;
import static android.healthconnect.cts.utils.TestUtils.finishMigrationWithShellPermissionIdentity;
import static android.healthconnect.cts.utils.TestUtils.startMigrationWithShellPermissionIdentity;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.MedicalResourceTypeInfo;
import android.health.connect.datatypes.MedicalDataSource;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class QueryAllMedicalResourceTypeInfosCtsTest {
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
        TestUtils.deleteAllStagedRemoteData();
        mManager = TestUtils.getHealthConnectManager();
        mUtil = new PhrCtsTestUtils(mManager);
        mUtil.deleteAllMedicalData();
    }

    @After
    public void after() throws InterruptedException {
        mUtil.deleteAllMedicalData();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testQueryAllMedicalResourceTypeInfos_migrationInProgress_apiBlocked()
            throws Exception {
        startMigrationWithShellPermissionIdentity();
        HealthConnectReceiver<List<MedicalResourceTypeInfo>> receiver =
                new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.queryAllMedicalResourceTypeInfos(
                            Executors.newSingleThreadExecutor(), receiver);
                    assertThat(receiver.assertAndGetException().getErrorCode())
                            .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
                },
                MANAGE_HEALTH_DATA_PERMISSION);

        finishMigrationWithShellPermissionIdentity();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testQueryAllMedicalResourceTypeInfos_withManagePerm_hasData_succeeds()
            throws Exception {
        // Create some data sources with data: ds1 contains [vaccine, differentVaccine,
        // allergy], ds2 contains [vaccine], and ds3 contains [allergy].
        MedicalDataSource dataSource1 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("1"));
        MedicalDataSource dataSource2 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("2"));
        MedicalDataSource dataSource3 =
                mUtil.createDataSource(getCreateMedicalDataSourceRequest("3"));
        Instant upsertTime = Instant.now();
        mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource1.getId(), DIFFERENT_FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource1.getId(), FHIR_DATA_ALLERGY);
        mUtil.upsertMedicalData(dataSource2.getId(), FHIR_DATA_IMMUNIZATION);
        mUtil.upsertMedicalData(dataSource3.getId(), FHIR_DATA_ALLERGY);
        HealthConnectReceiver<List<MedicalResourceTypeInfo>> receiver =
                new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.queryAllMedicalResourceTypeInfos(
                            Executors.newSingleThreadExecutor(), receiver);
                    receiver.verifyNoExceptionOrThrow();

                    List<MedicalResourceTypeInfo> response = receiver.getResponse();
                    for (MedicalResourceTypeInfo info : response) {
                        info.getContributingDataSources()
                                .forEach(
                                        source -> {
                                            assertThat(source.getLastDataUpdateTime())
                                                    .isAtLeast(upsertTime);
                                            assertThat(source.getLastDataUpdateTime())
                                                    .isAtMost(Instant.now());
                                        });
                    }
                    List<MedicalResourceTypeInfo> responseWithoutLastUpdateTime = new ArrayList<>();
                    for (MedicalResourceTypeInfo info : response) {
                        MedicalResourceTypeInfo infoWithoutLastDataUpdateTime =
                                new MedicalResourceTypeInfo(
                                        info.getMedicalResourceType(),
                                        info.getContributingDataSources().stream()
                                                .map(
                                                        source ->
                                                                new MedicalDataSource.Builder(
                                                                                source)
                                                                        .setLastDataUpdateTime(null)
                                                                        .build())
                                                .collect(Collectors.toSet()));
                        responseWithoutLastUpdateTime.add(infoWithoutLastDataUpdateTime);
                    }
                    assertThat(responseWithoutLastUpdateTime)
                            .containsExactly(
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                            Set.of(dataSource1, dataSource3)),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_CONDITIONS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_VACCINES,
                                            Set.of(dataSource1, dataSource2)),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_MEDICATIONS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PREGNANCY, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PROCEDURES, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_VISITS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_VITAL_SIGNS, Set.of()));
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testQueryAllMedicalResourceTypeInfos_withManagePerm_noDataSources_succeeds() {
        HealthConnectReceiver<List<MedicalResourceTypeInfo>> receiver =
                new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.queryAllMedicalResourceTypeInfos(
                            Executors.newSingleThreadExecutor(), receiver);
                    receiver.verifyNoExceptionOrThrow();

                    assertThat(receiver.getResponse())
                            .containsExactly(
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_CONDITIONS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_VACCINES, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_MEDICATIONS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PREGNANCY, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PROCEDURES, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_VISITS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_VITAL_SIGNS, Set.of()));
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testQueryAllMedicalResourceTypeInfos_withManagePerm_noMedicalResources_succeeds()
            throws Exception {
        mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        HealthConnectReceiver<List<MedicalResourceTypeInfo>> receiver =
                new HealthConnectReceiver<>();

        runWithShellPermissionIdentity(
                () -> {
                    mManager.queryAllMedicalResourceTypeInfos(
                            Executors.newSingleThreadExecutor(), receiver);

                    assertThat(receiver.getResponse())
                            .containsExactly(
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_CONDITIONS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_VACCINES, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_MEDICATIONS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PREGNANCY, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_PROCEDURES, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_VISITS, Set.of()),
                                    new MedicalResourceTypeInfo(
                                            MEDICAL_RESOURCE_TYPE_VITAL_SIGNS, Set.of()));
                },
                MANAGE_HEALTH_DATA_PERMISSION);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testQueryAllMedicalResourceTypeInfos_withoutManagePerm_throws()
            throws InterruptedException {
        HealthConnectReceiver<List<MedicalResourceTypeInfo>> receiver =
                new HealthConnectReceiver<>();

        mManager.queryAllMedicalResourceTypeInfos(Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }
}
