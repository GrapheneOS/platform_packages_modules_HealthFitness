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

package android.healthconnect.cts.device;

import static android.health.connect.HealthPermissions.READ_SYMPTOM_COUGH;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.testing.cts.PermissionUtils.revokeHealthPermission;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest;

import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.Flags.FLAG_SMOKING_DB;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.SymptomRecord;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.healthconnect.testing.shared.recordfactory.SymptomRecordFactory;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.truth.Correspondence;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class HealthConnectChangeLogsDeviceTests {

    private static final TestAppProxy APP_A_WITH_READ_WRITE_PERMS =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.A");

    private static final TestAppProxy APP_B_WITH_READ_WRITE_PERMS =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.B");

    private static final TestAppProxy APP_C_WITH_WRITE_PERMS_ONLY =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.writePermsOnly");

    private static final Correspondence<ChangeLogsResponse.DeletedLog, String>
            DELETED_LOG_TO_STRING_ID_CORRESPONDENCE =
                    Correspondence.from(
                            (deletedLog, stringId) ->
                                    deletedLog.getDeletedRecordId().equals(stringId),
                            "has matching string id");

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws Exception {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test
    public void testChangeLogs_insert_multipleApps_noFilter_returnsUpsertLogsForAllApps()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = APP_A_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        String recordIdInsertedByAppB = APP_B_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        StepsRecord recordInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                        .addId(recordIdInsertedByAppA)
                                        .build())
                        .get(0);
        StepsRecord recordInsertedByAppB =
                APP_B_WITH_READ_WRITE_PERMS
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                        .addId(recordIdInsertedByAppB)
                                        .build())
                        .get(0);
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedRecords())
                .containsExactly(recordInsertedByAppA, recordInsertedByAppB);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    public void testChangeLogs_insert_multipleApps_filterDataOrigin_returnsUpsertLogs()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        APP_A_WITH_READ_WRITE_PERMS
                                                                .getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = APP_A_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        APP_B_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        StepsRecord recordInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                        .addId(recordIdInsertedByAppA)
                                        .build())
                        .get(0);
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedRecords()).containsExactly(recordInsertedByAppA);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    public void testChangeLogs_insertAndDelete_multipleApps_noFilter_returnsDeletedLogsForAllApps()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = APP_A_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        String recordIdInsertedByAppB = APP_B_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        APP_A_WITH_READ_WRITE_PERMS.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppA));
        APP_B_WITH_READ_WRITE_PERMS.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppB));
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getDeletedLogs())
                .comparingElementsUsing(DELETED_LOG_TO_STRING_ID_CORRESPONDENCE)
                .containsExactly(recordIdInsertedByAppA, recordIdInsertedByAppB);
        assertThat(response.getUpsertedRecords()).isEmpty();
    }

    @Test
    public void testChangeLogs_insertAndDelete_multipleApps_filterDataOrigin_returnsDeletedLogs()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        APP_B_WITH_READ_WRITE_PERMS
                                                                .getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = APP_A_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        String recordIdInsertedByAppB = APP_B_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        APP_A_WITH_READ_WRITE_PERMS.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppA));
        APP_B_WITH_READ_WRITE_PERMS.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppB));
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getDeletedLogs())
                .comparingElementsUsing(DELETED_LOG_TO_STRING_ID_CORRESPONDENCE)
                .containsExactly(recordIdInsertedByAppB);
        assertThat(response.getUpsertedRecords()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PHR_CHANGE_LOGS})
    public void testChangeLogs_phr_insert_multipleApps_noFilter_returnsUpsertLogsForAllApps()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        MedicalDataSource dataSourceByAppA =
                APP_A_WITH_READ_WRITE_PERMS.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appA"));
        MedicalResource medicalResourceInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(
                        dataSourceByAppA.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSourceByAppB =
                APP_B_WITH_READ_WRITE_PERMS.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appB"));
        MedicalResource medicalResourceInsertedByAppB =
                APP_B_WITH_READ_WRITE_PERMS.upsertMedicalResource(
                        dataSourceByAppB.getId(), FHIR_DATA_IMMUNIZATION);
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedMedicalResources())
                .containsExactly(medicalResourceInsertedByAppA, medicalResourceInsertedByAppB);
        assertThat(response.getDeletedMedicalResources()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PHR_CHANGE_LOGS})
    public void testChangeLogs_phr_insert_multipleApps_filterDataOrigin_returnsUpsertLogs()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        APP_A_WITH_READ_WRITE_PERMS
                                                                .getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        MedicalDataSource dataSourceByAppA =
                APP_A_WITH_READ_WRITE_PERMS.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appA"));
        MedicalResource medicalResourceInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(
                        dataSourceByAppA.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSourceByAppB =
                APP_B_WITH_READ_WRITE_PERMS.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appB"));
        MedicalResource medicalResourceInsertedByAppB =
                APP_B_WITH_READ_WRITE_PERMS.upsertMedicalResource(
                        dataSourceByAppB.getId(), FHIR_DATA_IMMUNIZATION);
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedMedicalResources())
                .containsExactly(medicalResourceInsertedByAppA);
        assertThat(response.getUpsertedMedicalResources())
                .doesNotContain(medicalResourceInsertedByAppB);
        assertThat(response.getDeletedMedicalResources()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PHR_CHANGE_LOGS})
    public void
            testChangeLogs_phr_insertAndDelete_multipleApps_noFilter_returnsDeletedLogsForAllApps()
                    throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        MedicalDataSource dataSourceByAppA =
                APP_A_WITH_READ_WRITE_PERMS.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appA"));
        MedicalResource medicalResourceInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(
                        dataSourceByAppA.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSourceByAppB =
                APP_B_WITH_READ_WRITE_PERMS.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appB"));
        MedicalResource medicalResourceInsertedByAppB =
                APP_B_WITH_READ_WRITE_PERMS.upsertMedicalResource(
                        dataSourceByAppB.getId(), FHIR_DATA_IMMUNIZATION);
        APP_A_WITH_READ_WRITE_PERMS.deleteMedicalResources(
                List.of(medicalResourceInsertedByAppA.getId()));
        APP_B_WITH_READ_WRITE_PERMS.deleteMedicalResources(
                List.of(medicalResourceInsertedByAppB.getId()));
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getDeletedMedicalResources())
                .comparingElementsUsing(
                        Correspondence
                                .<ChangeLogsResponse.DeletedMedicalResource, MedicalResource>from(
                                        (deletedMedicalResource, medicalResource) ->
                                                deletedMedicalResource
                                                        .getDeletedMedicalResourceId()
                                                        .equals(medicalResource.getId()),
                                        "has matching medical resource id"))
                .containsExactly(medicalResourceInsertedByAppA, medicalResourceInsertedByAppB);
        assertThat(response.getUpsertedMedicalResources()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PHR_CHANGE_LOGS})
    public void
            testChangeLogs_phr_insertAndDelete_multipleApps_filterDataOrigin_returnsDeletedLogs()
                    throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        APP_B_WITH_READ_WRITE_PERMS
                                                                .getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        MedicalDataSource dataSourceByAppA =
                APP_A_WITH_READ_WRITE_PERMS.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appA"));
        MedicalResource medicalResourceInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(
                        dataSourceByAppA.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSourceByAppB =
                APP_B_WITH_READ_WRITE_PERMS.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appB"));
        MedicalResource medicalResourceInsertedByAppB =
                APP_B_WITH_READ_WRITE_PERMS.upsertMedicalResource(
                        dataSourceByAppB.getId(), FHIR_DATA_IMMUNIZATION);
        APP_A_WITH_READ_WRITE_PERMS.deleteMedicalResources(
                List.of(medicalResourceInsertedByAppA.getId()));
        APP_B_WITH_READ_WRITE_PERMS.deleteMedicalResources(
                List.of(medicalResourceInsertedByAppB.getId()));
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getDeletedMedicalResources())
                .comparingElementsUsing(
                        Correspondence
                                .<ChangeLogsResponse.DeletedMedicalResource, MedicalResource>from(
                                        (deletedMedicalResource, medicalResource) ->
                                                deletedMedicalResource
                                                        .getDeletedMedicalResourceId()
                                                        .equals(medicalResource.getId()),
                                        "has matching medical resource id"))
                .containsExactly(medicalResourceInsertedByAppB);
        assertThat(response.getUpsertedMedicalResources()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_SYMPTOMS, FLAG_SYMPTOMS_DB, FLAG_SMOKING_DB})
    public void testChangeLogs_insertSymptomsRecords_returnsUpsertLogsAppHoldsPermissionFor()
            throws Exception {
        // App A has permission to read/write cough/snore and App B has permission to read cough
        // only
        String changeLogToken =
                APP_B_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(SymptomRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String coughRecordIdInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS.insertRecord(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH));
        APP_A_WITH_READ_WRITE_PERMS.insertRecord(
                SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));

        SymptomRecord coughSymptomRecordsInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(SymptomRecord.class)
                                        .addId(coughRecordIdInsertedByAppA)
                                        .build())
                        .get(0);

        ChangeLogsResponse response = APP_B_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedRecords()).hasSize(1);
        assertThat(response.getUpsertedRecords())
                .containsExactly(coughSymptomRecordsInsertedByAppA);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_SYMPTOMS, FLAG_SYMPTOMS_DB, FLAG_SMOKING_DB})
    public void testChangeLogs_deleteSymptomsRecords_returnsAllDeletedSymptomRecordIds()
            throws Exception {
        // App A has permission to read/write cough/snore and App B has permission to read cough
        // only
        String changeLogToken =
                APP_B_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(SymptomRecord.class)
                                .build());

        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String coughRecordIdInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS.insertRecord(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_COUGH));
        String snoreRecordIdInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS.insertRecord(
                        SymptomRecordFactory.newInstantRecord(SymptomRecord.SYMPTOM_TYPE_SNORE));

        APP_A_WITH_READ_WRITE_PERMS.deleteRecords(
                RecordIdFilter.fromId(SymptomRecord.class, coughRecordIdInsertedByAppA),
                RecordIdFilter.fromId(SymptomRecord.class, snoreRecordIdInsertedByAppA));

        ChangeLogsResponse response = APP_B_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getDeletedLogs().size()).isAtLeast(1);
        assertThat(
                        response.getDeletedLogs().stream()
                                .map(ChangeLogsResponse.DeletedLog::getDeletedRecordId)
                                .collect(Collectors.toList()))
                .contains(coughRecordIdInsertedByAppA);
    }

    @Test
    @RequiresFlagsEnabled({FLAG_SYMPTOMS, FLAG_SYMPTOMS_DB, FLAG_SMOKING_DB})
    public void
            testChangeLogs_getChangeLogToken_throwsExceptionForAppWithNoReadSymptomPermission() {
        // App A has permission to read/write cough/snore and App C has permission to write cough
        // only and no read symptom permission
        assertThrows(
                HealthConnectException.class,
                () ->
                        APP_C_WITH_WRITE_PERMS_ONLY.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(SymptomRecord.class)
                                        .build()));
    }

    @Test
    @RequiresFlagsEnabled({FLAG_SYMPTOMS, FLAG_SYMPTOMS_DB, FLAG_SMOKING_DB})
    public void testChangeLogs_getChangeLogs_throwsExceptionForAppWithNoReadSymptomPermission()
            throws Exception {
        // App A has permission to read/write cough/snore and App B has permission to read cough
        // only
        String changeLogToken =
                APP_B_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(SymptomRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        // Revoke permission after token generation
        revokeHealthPermission(APP_B_WITH_READ_WRITE_PERMS.getPackageName(), READ_SYMPTOM_COUGH);

        assertThrows(
                HealthConnectException.class,
                () -> APP_B_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest));
    }
}
