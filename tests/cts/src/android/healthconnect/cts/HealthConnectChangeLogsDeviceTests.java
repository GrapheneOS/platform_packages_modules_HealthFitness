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

package android.healthconnect.cts;

import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest;

import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy;
import android.healthconnect.testing.cts.testapphelpers.TestAppRule;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
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

@RunWith(AndroidJUnit4.class)
public class HealthConnectChangeLogsDeviceTests {

    @Rule
    public final TestAppRule mAppAWithReadWritePermsRule =
            new TestAppRule.Builder("android.healthconnect.cts.testapp.readWritePerms.A").build();

    @Rule
    public final TestAppRule mAppBWithReadWritePermsRule =
            new TestAppRule.Builder("android.healthconnect.cts.testapp.readWritePerms.B").build();

    private final TestAppProxy mAppAWithReadWritePerms = mAppAWithReadWritePermsRule.getProxy();
    private final TestAppProxy mAppBWithReadWritePerms = mAppBWithReadWritePermsRule.getProxy();

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
                mAppAWithReadWritePerms.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = mAppAWithReadWritePerms.insertRecord(getStepsRecord());
        String recordIdInsertedByAppB = mAppBWithReadWritePerms.insertRecord(getStepsRecord());
        StepsRecord recordInsertedByAppA =
                mAppAWithReadWritePerms
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                        .addId(recordIdInsertedByAppA)
                                        .build())
                        .get(0);
        StepsRecord recordInsertedByAppB =
                mAppBWithReadWritePerms
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                        .addId(recordIdInsertedByAppB)
                                        .build())
                        .get(0);
        ChangeLogsResponse response = mAppAWithReadWritePerms.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedRecords())
                .containsExactly(recordInsertedByAppA, recordInsertedByAppB);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    public void testChangeLogs_insert_multipleApps_filterDataOrigin_returnsUpsertLogs()
            throws Exception {
        String changeLogToken =
                mAppAWithReadWritePerms.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        mAppAWithReadWritePerms.getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = mAppAWithReadWritePerms.insertRecord(getStepsRecord());
        mAppBWithReadWritePerms.insertRecord(getStepsRecord());
        StepsRecord recordInsertedByAppA =
                mAppAWithReadWritePerms
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                        .addId(recordIdInsertedByAppA)
                                        .build())
                        .get(0);
        ChangeLogsResponse response = mAppAWithReadWritePerms.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedRecords()).containsExactly(recordInsertedByAppA);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    public void testChangeLogs_insertAndDelete_multipleApps_noFilter_returnsDeletedLogsForAllApps()
            throws Exception {
        String changeLogToken =
                mAppAWithReadWritePerms.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = mAppAWithReadWritePerms.insertRecord(getStepsRecord());
        String recordIdInsertedByAppB = mAppBWithReadWritePerms.insertRecord(getStepsRecord());
        mAppAWithReadWritePerms.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppA));
        mAppBWithReadWritePerms.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppB));
        ChangeLogsResponse response = mAppAWithReadWritePerms.getChangeLogs(changeLogsRequest);

        assertThat(response.getDeletedLogs())
                .comparingElementsUsing(DELETED_LOG_TO_STRING_ID_CORRESPONDENCE)
                .containsExactly(recordIdInsertedByAppA, recordIdInsertedByAppB);
        assertThat(response.getUpsertedRecords()).isEmpty();
    }

    @Test
    public void testChangeLogs_insertAndDelete_multipleApps_filterDataOrigin_returnsDeletedLogs()
            throws Exception {
        String changeLogToken =
                mAppAWithReadWritePerms.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        mAppBWithReadWritePerms.getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = mAppAWithReadWritePerms.insertRecord(getStepsRecord());
        String recordIdInsertedByAppB = mAppBWithReadWritePerms.insertRecord(getStepsRecord());
        mAppAWithReadWritePerms.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppA));
        mAppBWithReadWritePerms.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppB));
        ChangeLogsResponse response = mAppAWithReadWritePerms.getChangeLogs(changeLogsRequest);

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
                mAppAWithReadWritePerms.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        MedicalDataSource dataSourceByAppA =
                mAppAWithReadWritePerms.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appA"));
        MedicalResource medicalResourceInsertedByAppA =
                mAppAWithReadWritePerms.upsertMedicalResource(
                        dataSourceByAppA.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSourceByAppB =
                mAppBWithReadWritePerms.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appB"));
        MedicalResource medicalResourceInsertedByAppB =
                mAppBWithReadWritePerms.upsertMedicalResource(
                        dataSourceByAppB.getId(), FHIR_DATA_IMMUNIZATION);
        ChangeLogsResponse response = mAppAWithReadWritePerms.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedMedicalResources())
                .containsExactly(medicalResourceInsertedByAppA, medicalResourceInsertedByAppB);
        assertThat(response.getDeletedMedicalResources()).isEmpty();
    }

    @Test
    @RequiresFlagsEnabled({FLAG_PHR_CHANGE_LOGS})
    public void testChangeLogs_phr_insert_multipleApps_filterDataOrigin_returnsUpsertLogs()
            throws Exception {
        String changeLogToken =
                mAppAWithReadWritePerms.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        mAppAWithReadWritePerms.getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        MedicalDataSource dataSourceByAppA =
                mAppAWithReadWritePerms.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appA"));
        MedicalResource medicalResourceInsertedByAppA =
                mAppAWithReadWritePerms.upsertMedicalResource(
                        dataSourceByAppA.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSourceByAppB =
                mAppBWithReadWritePerms.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appB"));
        MedicalResource medicalResourceInsertedByAppB =
                mAppBWithReadWritePerms.upsertMedicalResource(
                        dataSourceByAppB.getId(), FHIR_DATA_IMMUNIZATION);
        ChangeLogsResponse response = mAppAWithReadWritePerms.getChangeLogs(changeLogsRequest);

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
                mAppAWithReadWritePerms.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        MedicalDataSource dataSourceByAppA =
                mAppAWithReadWritePerms.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appA"));
        MedicalResource medicalResourceInsertedByAppA =
                mAppAWithReadWritePerms.upsertMedicalResource(
                        dataSourceByAppA.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSourceByAppB =
                mAppBWithReadWritePerms.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appB"));
        MedicalResource medicalResourceInsertedByAppB =
                mAppBWithReadWritePerms.upsertMedicalResource(
                        dataSourceByAppB.getId(), FHIR_DATA_IMMUNIZATION);
        mAppAWithReadWritePerms.deleteMedicalResources(
                List.of(medicalResourceInsertedByAppA.getId()));
        mAppBWithReadWritePerms.deleteMedicalResources(
                List.of(medicalResourceInsertedByAppB.getId()));
        ChangeLogsResponse response = mAppAWithReadWritePerms.getChangeLogs(changeLogsRequest);

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
                mAppAWithReadWritePerms.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        mAppBWithReadWritePerms.getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        MedicalDataSource dataSourceByAppA =
                mAppAWithReadWritePerms.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appA"));
        MedicalResource medicalResourceInsertedByAppA =
                mAppAWithReadWritePerms.upsertMedicalResource(
                        dataSourceByAppA.getId(), FHIR_DATA_IMMUNIZATION);
        MedicalDataSource dataSourceByAppB =
                mAppBWithReadWritePerms.createMedicalDataSource(
                        getCreateMedicalDataSourceRequest("appB"));
        MedicalResource medicalResourceInsertedByAppB =
                mAppBWithReadWritePerms.upsertMedicalResource(
                        dataSourceByAppB.getId(), FHIR_DATA_IMMUNIZATION);
        mAppAWithReadWritePerms.deleteMedicalResources(
                List.of(medicalResourceInsertedByAppA.getId()));
        mAppBWithReadWritePerms.deleteMedicalResources(
                List.of(medicalResourceInsertedByAppB.getId()));
        ChangeLogsResponse response = mAppAWithReadWritePerms.getChangeLogs(changeLogsRequest);

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
}
