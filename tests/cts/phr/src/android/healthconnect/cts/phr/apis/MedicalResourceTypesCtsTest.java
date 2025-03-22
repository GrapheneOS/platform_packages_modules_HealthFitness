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

import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_CONDITIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PREGNANCY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_PROCEDURES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VISITS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VITAL_SIGNS;
import static android.healthconnect.cts.phr.utils.ObservationBuilder.ObservationCategory.LABORATORY;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthConnectManager;
import android.health.connect.ReadMedicalResourcesInitialRequest;
import android.health.connect.ReadMedicalResourcesResponse;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.cts.phr.utils.ConditionBuilder;
import android.healthconnect.cts.phr.utils.EncountersBuilder;
import android.healthconnect.cts.phr.utils.MedicationsBuilder;
import android.healthconnect.cts.phr.utils.ObservationBuilder;
import android.healthconnect.cts.phr.utils.PatientBuilder;
import android.healthconnect.cts.phr.utils.PhrCtsTestUtils;
import android.healthconnect.cts.phr.utils.PractitionerBuilder;
import android.healthconnect.cts.phr.utils.ProcedureBuilder;
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

import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class MedicalResourceTypesCtsTest {
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
        TestUtils.deleteAllStagedRemoteData();
        mManager = TestUtils.getHealthConnectManager();
        mUtil = new PhrCtsTestUtils(TestUtils.getHealthConnectManager());
        mUtil.deleteAllMedicalData();
    }

    @After
    public void after() throws InterruptedException {
        mUtil.deleteAllMedicalData();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testPatientInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource patient =
                mUtil.upsertMedicalData(dataSource1.getId(), new PatientBuilder().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_PERSONAL_DETAILS)
                        .build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(patient);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testLabResultsInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource labResult =
                mUtil.upsertMedicalData(
                        dataSource1.getId(),
                        new ObservationBuilder()
                                .setBloodGlucose()
                                .setCategory(LABORATORY)
                                .toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest allLabResultsRequest =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_LABORATORY_RESULTS)
                        .build();

        mManager.readMedicalResources(
                allLabResultsRequest, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(labResult);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testPregnancyInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource pregnancyStatus =
                mUtil.upsertMedicalData(
                        dataSource1.getId(),
                        new ObservationBuilder()
                                .setPregnancyStatus(ObservationBuilder.PregnancyStatus.PREGNANT)
                                .toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest allPregnancyRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_PREGNANCY)
                        .build();

        mManager.readMedicalResources(
                allPregnancyRequest, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(pregnancyStatus);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testSocialHistoryInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource smoking =
                mUtil.upsertMedicalData(
                        dataSource1.getId(),
                        new ObservationBuilder()
                                .setTobaccoUse(ObservationBuilder.CurrentSmokingStatus.SMOKER)
                                .toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest allSocialHistoryRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_SOCIAL_HISTORY)
                        .build();

        mManager.readMedicalResources(
                allSocialHistoryRequest, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(smoking);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testVitalSignsInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource heartRate =
                mUtil.upsertMedicalData(
                        dataSource1.getId(), new ObservationBuilder().setHeartRate(100).toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest allVitalSignsRequest =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VITAL_SIGNS)
                        .build();

        mManager.readMedicalResources(
                allVitalSignsRequest, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(heartRate);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testConditionInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource condition =
                mUtil.upsertMedicalData(dataSource1.getId(), new ConditionBuilder().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest allConditions =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_CONDITIONS)
                        .build();

        mManager.readMedicalResources(allConditions, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(condition);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testPractitionerInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource practitioner =
                mUtil.upsertMedicalData(dataSource1.getId(), new PractitionerBuilder().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS)
                        .build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(practitioner);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testPractitionerRoleInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource practitioner =
                mUtil.upsertMedicalData(dataSource1.getId(), PractitionerBuilder.role().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(
                                MEDICAL_RESOURCE_TYPE_PRACTITIONER_DETAILS)
                        .build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(practitioner);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testProcedureInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource procedure =
                mUtil.upsertMedicalData(dataSource1.getId(), new ProcedureBuilder().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_PROCEDURES)
                        .build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(procedure);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testMedicationInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource medication =
                mUtil.upsertMedicalData(
                        dataSource1.getId(), MedicationsBuilder.medication().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_MEDICATIONS)
                        .build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(medication);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testMedicationStatementInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource statement =
                mUtil.upsertMedicalData(
                        dataSource1.getId(), MedicationsBuilder.statementR4().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_MEDICATIONS)
                        .build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(statement);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testMedicationRequestInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource medicationRequest =
                mUtil.upsertMedicalData(dataSource1.getId(), MedicationsBuilder.request().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest request =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_MEDICATIONS)
                        .build();

        mManager.readMedicalResources(request, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(medicationRequest);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testEncounterInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource encounter =
                mUtil.upsertMedicalData(
                        dataSource1.getId(), EncountersBuilder.encounter().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest allVisits =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VISITS)
                        .build();

        mManager.readMedicalResources(allVisits, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(encounter);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testLocationInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource location =
                mUtil.upsertMedicalData(dataSource1.getId(), EncountersBuilder.location().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest allVisits =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VISITS)
                        .build();

        mManager.readMedicalResources(allVisits, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(location);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testOrganizationInsertAndRead() throws Exception {
        MedicalDataSource dataSource1 = mUtil.createDataSource(getCreateMedicalDataSourceRequest());
        MedicalResource organization =
                mUtil.upsertMedicalData(
                        dataSource1.getId(), EncountersBuilder.organization().toJson());
        HealthConnectReceiver<ReadMedicalResourcesResponse> receiver =
                new HealthConnectReceiver<>();
        ReadMedicalResourcesInitialRequest allVisits =
                new ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VISITS)
                        .build();

        mManager.readMedicalResources(allVisits, Executors.newSingleThreadExecutor(), receiver);

        assertThat(receiver.getResponse().getMedicalResources()).containsExactly(organization);
    }
}
