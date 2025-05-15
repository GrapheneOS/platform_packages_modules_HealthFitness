/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.healthconnect.phr.storage;

import static android.health.connect.Constants.DELETE;
import static android.health.connect.datatypes.FhirResource.FHIR_RESOURCE_TYPE_IMMUNIZATION;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_VERSION_R4;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.createAllergyMedicalResource;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.createDifferentVaccineMedicalResource;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.createVaccineMedicalResource;

import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.health.connect.datatypes.FhirResource;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.healthconnect.testing.shared.phr.ImmunizationBuilder;
import android.healthconnect.testing.unittest.PhrTestUtils;
import android.healthconnect.testing.unittest.PhrTestUtils.MedicalChangeLogEntry;
import android.healthconnect.testing.unittest.TransactionTestUtils;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@EnableFlags({FLAG_PHR_CHANGE_LOGS, FLAG_PHR_CHANGE_LOGS_DB})
public final class MedicalChangeLogsHelperTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private MedicalChangeLogsHelper mMedicalChangeLogsHelper;
    private TransactionManager mTransactionManager;
    private AppInfoHelper mAppInfoHelper;
    private PhrTestUtils mPhrTestUtils;
    private TransactionTestUtils mTransactionTestUtils;

    private static final String PACKAGE_NAME_1 = "com.test.app1";
    private static final String PACKAGE_NAME_2 = "com.test.app2";

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .build();
        mMedicalChangeLogsHelper =
                new MedicalChangeLogsHelper(
                        injector.getTransactionManager(),
                        injector.getAppInfoHelper(),
                        injector.getMedicalDataSourceHelper());
        mTransactionManager = injector.getTransactionManager();
        mAppInfoHelper = injector.getAppInfoHelper();
        mPhrTestUtils = new PhrTestUtils(injector);
        mTransactionTestUtils = new TransactionTestUtils(injector);
    }

    @Test
    public void generateDeletionChangeLogsForMedicalResources_withAppId() {
        mTransactionTestUtils.insertApp(PACKAGE_NAME_1);
        long appId1 = mAppInfoHelper.getAppInfoId(PACKAGE_NAME_1);
        MedicalDataSource dataSource1 =
                mPhrTestUtils.insertR4MedicalDataSource("ds1", PACKAGE_NAME_1);
        MedicalResource vaccine1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource vaccine2 = createDifferentVaccineMedicalResource(dataSource1.getId());
        // Create a third distinct vaccine
        FhirResource fhirResource3 =
                new FhirResource.Builder(
                                FHIR_RESOURCE_TYPE_IMMUNIZATION,
                                "Immunization3",
                                new ImmunizationBuilder().setId("Immunization3").toJson())
                        .build();
        MedicalResource vaccine3 =
                new MedicalResource.Builder(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                dataSource1.getId(),
                                FHIR_VERSION_R4,
                                fhirResource3)
                        .build();
        List<MedicalResource> resourcesToDelete = List.of(vaccine1, vaccine2, vaccine3);
        mPhrTestUtils.upsertResources(resourcesToDelete, PACKAGE_NAME_1);

        mTransactionManager.runAsTransaction(
                db -> {
                    mMedicalChangeLogsHelper.generateDeletionChangeLogsForMedicalResources(
                            db, resourcesToDelete, appId1);
                });

        List<MedicalChangeLogEntry> changeLogs = mPhrTestUtils.getAllDeleteMedicalChangeLogs();
        assertThat(changeLogs)
                .containsExactly(
                        new MedicalChangeLogEntry(
                                DELETE,
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                appId1,
                                List.of(vaccine1.getId(), vaccine2.getId(), vaccine3.getId())));
    }

    @Test
    public void generateDeletionChangeLogsForMedicalResources_withoutAppId() {
        mTransactionTestUtils.insertApp(PACKAGE_NAME_1);
        mTransactionTestUtils.insertApp(PACKAGE_NAME_2);
        long appId1 = mAppInfoHelper.getAppInfoId(PACKAGE_NAME_1);
        long appId2 = mAppInfoHelper.getAppInfoId(PACKAGE_NAME_2);
        MedicalDataSource dataSource1 =
                mPhrTestUtils.insertR4MedicalDataSource("ds1", PACKAGE_NAME_1);
        MedicalDataSource dataSource2 =
                mPhrTestUtils.insertR4MedicalDataSource("ds2", PACKAGE_NAME_2);
        // Create resources for App 1
        MedicalResource vaccine1Ds1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource vaccine2Ds1 = createDifferentVaccineMedicalResource(dataSource1.getId());
        MedicalResource allergy1Ds1 = createAllergyMedicalResource(dataSource1.getId());
        List<MedicalResource> resourcesForApp1 = List.of(vaccine1Ds1, vaccine2Ds1, allergy1Ds1);
        // Create resources for App 2
        MedicalResource vaccine1Ds2 = createVaccineMedicalResource(dataSource2.getId());
        List<MedicalResource> resourcesForApp2 = List.of(vaccine1Ds2);
        // Upsert resources in bulk for each app
        mPhrTestUtils.upsertResources(resourcesForApp1, PACKAGE_NAME_1);
        mPhrTestUtils.upsertResources(resourcesForApp2, PACKAGE_NAME_2);
        // Combine all resources for deletion
        List<MedicalResource> resourcesToDelete = new ArrayList<>();
        resourcesToDelete.addAll(resourcesForApp1);
        resourcesToDelete.addAll(resourcesForApp2);

        mTransactionManager.runAsTransaction(
                db -> {
                    mMedicalChangeLogsHelper.generateDeletionChangeLogsForMedicalResources(
                            db, resourcesToDelete, null);
                });

        MedicalChangeLogEntry expectedLogForVaccinesDs1 =
                new MedicalChangeLogEntry(
                        DELETE,
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        appId1,
                        List.of(vaccine1Ds1.getId(), vaccine2Ds1.getId()));
        MedicalChangeLogEntry expectedLogForAllergyDs1 =
                new MedicalChangeLogEntry(
                        DELETE,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                        appId1,
                        List.of(allergy1Ds1.getId()));
        MedicalChangeLogEntry expectedLogForVaccineDs2 =
                new MedicalChangeLogEntry(
                        DELETE,
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        appId2,
                        List.of(vaccine1Ds2.getId()));
        List<MedicalChangeLogEntry> changeLogs = mPhrTestUtils.getAllDeleteMedicalChangeLogs();
        assertThat(changeLogs)
                .containsExactly(
                        expectedLogForVaccinesDs1,
                        expectedLogForAllergyDs1,
                        expectedLogForVaccineDs2);
    }

    @Test
    public void generateDeletionChangeLogsForMedicalResources_withReadRequest_withAppId() {
        mTransactionTestUtils.insertApp(PACKAGE_NAME_1);
        mTransactionTestUtils.insertApp(PACKAGE_NAME_2);
        long appId1 = mAppInfoHelper.getAppInfoId(PACKAGE_NAME_1);
        MedicalDataSource dataSource1 =
                mPhrTestUtils.insertR4MedicalDataSource("ds1", PACKAGE_NAME_1);
        MedicalDataSource dataSource2 =
                mPhrTestUtils.insertR4MedicalDataSource("ds2", PACKAGE_NAME_2);
        // Create resources for App 1 (these will be deleted by the request)
        MedicalResource vaccine1Ds1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource vaccine2Ds1 = createDifferentVaccineMedicalResource(dataSource1.getId());
        MedicalResource allergy1Ds1 = createAllergyMedicalResource(dataSource1.getId());
        List<MedicalResource> resourcesForApp1 = List.of(vaccine1Ds1, vaccine2Ds1, allergy1Ds1);
        // Create resources for App 2 (these should NOT be deleted by the request)
        MedicalResource vaccine1Ds2 = createVaccineMedicalResource(dataSource2.getId());
        MedicalResource allergy1Ds2 = createAllergyMedicalResource(dataSource2.getId());
        List<MedicalResource> resourcesForApp2 = List.of(vaccine1Ds2, allergy1Ds2);
        // Upsert resources in bulk for each app
        mPhrTestUtils.upsertResources(resourcesForApp1, PACKAGE_NAME_1);
        mPhrTestUtils.upsertResources(resourcesForApp2, PACKAGE_NAME_2);
        // Create a ReadTableRequest to select resources belonging to PACKAGE_NAME_1
        ReadTableRequest readRequestForApp1 =
                MedicalResourceHelper.getFilteredReadRequestForResources(
                        /* dataSourceIds= */ List.of(),
                        /* medicalResourceTypes= */ Set.of(
                                MEDICAL_RESOURCE_TYPE_VACCINES,
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES),
                        /* appId= */ appId1);

        mTransactionManager.runAsTransaction(
                db -> {
                    mMedicalChangeLogsHelper.generateDeletionChangeLogsForMedicalResources(
                            db, readRequestForApp1, appId1);
                });

        // Expect change logs only for resources from PACKAGE_NAME_1
        MedicalChangeLogEntry expectedLogForVaccinesApp1 =
                new MedicalChangeLogEntry(
                        DELETE,
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        appId1,
                        List.of(vaccine1Ds1.getId(), vaccine2Ds1.getId()));
        MedicalChangeLogEntry expectedLogForAllergyApp1 =
                new MedicalChangeLogEntry(
                        DELETE,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                        appId1,
                        List.of(allergy1Ds1.getId()));
        List<MedicalChangeLogEntry> changeLogs = mPhrTestUtils.getAllDeleteMedicalChangeLogs();
        assertThat(changeLogs)
                .containsExactly(expectedLogForVaccinesApp1, expectedLogForAllergyApp1);
    }

    @Test
    public void generateDeletionChangeLogsForMedicalResources_withReadRequest_withoutAppId() {
        mTransactionTestUtils.insertApp(PACKAGE_NAME_1);
        mTransactionTestUtils.insertApp(PACKAGE_NAME_2);
        long appId1 = mAppInfoHelper.getAppInfoId(PACKAGE_NAME_1);
        long appId2 = mAppInfoHelper.getAppInfoId(PACKAGE_NAME_2);
        MedicalDataSource dataSource1 =
                mPhrTestUtils.insertR4MedicalDataSource("ds1", PACKAGE_NAME_1);
        MedicalDataSource dataSource2 =
                mPhrTestUtils.insertR4MedicalDataSource("ds2", PACKAGE_NAME_2);
        // Create resources for App 1
        MedicalResource vaccine1Ds1 = createVaccineMedicalResource(dataSource1.getId());
        MedicalResource vaccine2Ds1 = createDifferentVaccineMedicalResource(dataSource1.getId());
        MedicalResource allergy1Ds1 =
                createAllergyMedicalResource(dataSource1.getId()); // Not selected by read request
        List<MedicalResource> resourcesForApp1 = List.of(vaccine1Ds1, vaccine2Ds1, allergy1Ds1);
        // Create resources for App 2
        MedicalResource vaccine1Ds2 = createVaccineMedicalResource(dataSource2.getId());
        MedicalResource vaccine2Ds2 = createDifferentVaccineMedicalResource(dataSource2.getId());
        MedicalResource allergy1Ds2 =
                createAllergyMedicalResource(dataSource2.getId()); // Not selected by read request
        List<MedicalResource> resourcesForApp2 = List.of(vaccine1Ds2, vaccine2Ds2, allergy1Ds2);
        // Upsert resources in bulk for each app
        mPhrTestUtils.upsertResources(resourcesForApp1, PACKAGE_NAME_1);
        mPhrTestUtils.upsertResources(resourcesForApp2, PACKAGE_NAME_2);
        // Create a ReadTableRequest to select only Vaccine resources from any app
        ReadTableRequest readRequestForVaccines =
                MedicalResourceHelper.getFilteredReadRequestForResources(
                        /* dataSourceIds= */ List.of(),
                        /* medicalResourceTypes= */ Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                        /* appId= */ null);

        mTransactionManager.runAsTransaction(
                db -> {
                    mMedicalChangeLogsHelper.generateDeletionChangeLogsForMedicalResources(
                            db, readRequestForVaccines, null);
                });

        // Expect change logs for Vaccine resources from both apps
        MedicalChangeLogEntry expectedLogForVaccinesApp1 =
                new MedicalChangeLogEntry(
                        DELETE,
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        appId1,
                        List.of(vaccine1Ds1.getId(), vaccine2Ds1.getId()));
        MedicalChangeLogEntry expectedLogForVaccinesApp2 =
                new MedicalChangeLogEntry(
                        DELETE,
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        appId2,
                        List.of(vaccine1Ds2.getId(), vaccine2Ds2.getId()));
        List<MedicalChangeLogEntry> changeLogs = mPhrTestUtils.getAllDeleteMedicalChangeLogs();
        assertThat(changeLogs)
                .containsExactly(expectedLogForVaccinesApp1, expectedLogForVaccinesApp2);
    }
}
