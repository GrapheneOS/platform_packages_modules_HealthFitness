/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.health.connect.datatypes.ExerciseSessionRecord.EXERCISE_DURATION_TOTAL;
import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;
import static android.healthconnect.cts.lib.MultiAppTestUtils.CHANGE_LOGS_RESPONSE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.CHANGE_LOG_TOKEN;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_RECORDS_SIZE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.RECORD_IDS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.SUCCESS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.deleteRecordsAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.getChangeLogTokenAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.getDataOriginPriorityOrder;
import static android.healthconnect.cts.lib.MultiAppTestUtils.insertExerciseSessionAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.insertRecordAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.insertRecordWithAnotherAppPackageName;
import static android.healthconnect.cts.lib.MultiAppTestUtils.insertRecordWithGivenClientId;
import static android.healthconnect.cts.lib.MultiAppTestUtils.insertStepsRecordAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.readChangeLogsUsingDataOriginFiltersAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.readRecordsAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.readRecordsUsingDataOriginFiltersAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.updateRecordsAs;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.deleteTestData;
import static android.healthconnect.cts.utils.TestUtils.fetchDataOriginsPriorityOrder;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponse;
import static android.healthconnect.cts.utils.TestUtils.getApplicationInfo;
import static android.healthconnect.cts.utils.TestUtils.getGrantedHealthPermissions;
import static android.healthconnect.cts.utils.TestUtils.getInstantTime;
import static android.healthconnect.cts.utils.TestUtils.grantPermission;
import static android.healthconnect.cts.utils.TestUtils.readRecords;
import static android.healthconnect.cts.utils.TestUtils.revokeAndThenGrantHealthPermissions;
import static android.healthconnect.cts.utils.TestUtils.revokeHealthPermissions;
import static android.healthconnect.cts.utils.TestUtils.revokePermission;
import static android.healthconnect.cts.utils.TestUtils.updateDataOriginPriorityOrder;
import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import static com.android.compatibility.common.util.FeatureUtil.AUTOMOTIVE_FEATURE;
import static com.android.compatibility.common.util.FeatureUtil.hasSystemFeature;

import static com.google.common.truth.Truth.assertThat;

import android.app.UiAutomation;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthDataCategory;
import android.health.connect.HealthPermissions;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.UpdateDataOriginPriorityOrderRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.cts.utils.TestUtils;
import android.os.Bundle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.cts.install.lib.TestApp;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class HealthConnectDeviceTest {
    static final String TAG = "HealthConnectDeviceTest";
    public static final String MANAGE_HEALTH_DATA = HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
    static final long VERSION_CODE = 1;
    private static final int ASYNC_RETRIES = 3;
    private static final int ASYNC_RETRY_DELAY_MILLIS = 500;

    static final TestApp APP_A_WITH_READ_WRITE_PERMS =
            new TestApp(
                    "TestAppA",
                    "android.healthconnect.cts.testapp.readWritePerms.A",
                    VERSION_CODE,
                    false,
                    "CtsHealthConnectTestAppA.apk");

    private static final TestApp APP_B_WITH_READ_WRITE_PERMS =
            new TestApp(
                    "TestAppB",
                    "android.healthconnect.cts.testapp.readWritePerms.B",
                    VERSION_CODE,
                    false,
                    "CtsHealthConnectTestAppB.apk");

    private static final TestApp APP_WITH_WRITE_PERMS_ONLY =
            new TestApp(
                    "TestAppC",
                    "android.healthconnect.cts.testapp.writePermsOnly",
                    VERSION_CODE,
                    false,
                    "CtsHealthConnectTestAppWithWritePermissionsOnly.apk");

    private static final TestApp APP_WITH_DATA_MANAGE_PERMS_ONLY =
            new TestApp(
                    "TestAppD",
                    "android.healthconnect.cts.testapp.data.manage.permissions",
                    VERSION_CODE,
                    false,
                    "CtsHealthConnectTestAppWithDataManagePermission.apk");

    @Before
    public void setUp() {
        Assume.assumeFalse(hasSystemFeature(AUTOMOTIVE_FEATURE));
    }

    @After
    public void tearDown() throws InterruptedException {
        deleteTestData();
        deleteAllStagedRemoteData();
    }

    @Test
    public void testAppWithNormalReadWritePermCanInsertRecord() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
    }

    @Test
    public void testAnAppCantDeleteAnotherAppEntry() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        try {
            deleteRecordsAs(APP_B_WITH_READ_WRITE_PERMS, listOfRecordIdsAndClass);
            Assert.fail("Should have thrown an Invalid Argument Exception!");
        } catch (HealthConnectException e) {

            assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        }
    }

    @Test
    public void testAnAppCantUpdateAnotherAppEntry() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        try {
            updateRecordsAs(APP_B_WITH_READ_WRITE_PERMS, listOfRecordIdsAndClass);
            Assert.fail("Should have thrown an Invalid Argument Exception!");
        } catch (HealthConnectException e) {
            assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        }
    }

    @Test
    public void testDataOriginGetsOverriddenBySelfPackageName() throws Exception {
        Bundle bundle =
                insertRecordWithAnotherAppPackageName(
                        APP_A_WITH_READ_WRITE_PERMS, APP_B_WITH_READ_WRITE_PERMS);

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds : listOfRecordIdsAndClass) {
            List<Record> records =
                    (List<Record>)
                            readRecords(
                                    new ReadRecordsRequestUsingFilters.Builder<>(
                                                    (Class<? extends Record>)
                                                            Class.forName(
                                                                    recordTypeAndRecordIds
                                                                            .getRecordType()))
                                            .build());

            for (Record record : records) {
                assertThat(record.getMetadata().getDataOrigin().getPackageName())
                        .isEqualTo(APP_A_WITH_READ_WRITE_PERMS.getPackageName());
            }
        }
    }

    @Test
    public void testAppWithWritePermsOnlyCanReadItsOwnEntry() throws Exception {
        Bundle bundle = insertRecordAs(APP_WITH_WRITE_PERMS_ONLY);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        ArrayList<String> recordClassesToRead = new ArrayList<>();
        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds : listOfRecordIdsAndClass) {
            recordClassesToRead.add(recordTypeAndRecordIds.getRecordType());
        }

        bundle = readRecordsAs(APP_WITH_WRITE_PERMS_ONLY, recordClassesToRead);
        assertThat(bundle.getInt(READ_RECORDS_SIZE)).isNotEqualTo(0);
    }

    @Test
    public void testAppWithWritePermsOnlyCantReadAnotherAppEntry() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        ArrayList<String> recordClassesToRead = new ArrayList<>();
        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds : listOfRecordIdsAndClass) {
            recordClassesToRead.add(recordTypeAndRecordIds.getRecordType());
        }

        bundle = readRecordsAs(APP_WITH_WRITE_PERMS_ONLY, recordClassesToRead);
        assertThat(bundle.getInt(READ_RECORDS_SIZE)).isEqualTo(0);
    }

    @Test
    public void testAppWithManageHealthDataPermsOnlyCantInsertRecords() throws Exception {
        try {
            insertRecordAs(APP_WITH_DATA_MANAGE_PERMS_ONLY);
            Assert.fail("Should have thrown Exception while inserting records!");
        } catch (HealthConnectException e) {
            assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        }
    }

    @Test
    public void testAppWithManageHealthDataPermsOnlyCantUpdateRecords() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        try {
            updateRecordsAs(APP_WITH_DATA_MANAGE_PERMS_ONLY, listOfRecordIdsAndClass);
            Assert.fail("Should have thrown Health Connect Exception!");
        } catch (HealthConnectException e) {
            assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        }
    }

    @Test
    public void testTwoAppsCanUseSameClientRecordIdsToInsert() throws Exception {
        final double clientId = Math.random();
        Bundle bundle = insertRecordWithGivenClientId(APP_A_WITH_READ_WRITE_PERMS, clientId);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        bundle = insertRecordWithGivenClientId(APP_B_WITH_READ_WRITE_PERMS, clientId);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
    }

    @Test
    public void testAppCanReadRecordsUsingDataOriginFilters() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        int noOfRecordsInsertedByAppA = 0;
        Set<String> recordClassesToReadSet = new HashSet<>();
        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds : listOfRecordIdsAndClass) {
            noOfRecordsInsertedByAppA += recordTypeAndRecordIds.getRecordIds().size();
            recordClassesToReadSet.add(recordTypeAndRecordIds.getRecordType());
        }

        bundle = insertRecordAs(APP_B_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds : listOfRecordIdsAndClass) {
            recordClassesToReadSet.add(recordTypeAndRecordIds.getRecordType());
        }

        ArrayList<String> recordClassesToRead = new ArrayList<>();
        for (String recordClass : recordClassesToReadSet) {
            recordClassesToRead.add(recordClass);
        }
        bundle =
                readRecordsUsingDataOriginFiltersAs(
                        APP_A_WITH_READ_WRITE_PERMS, recordClassesToRead);
        assertThat(bundle.getInt(READ_RECORDS_SIZE)).isEqualTo(noOfRecordsInsertedByAppA);
    }

    @Test
    public void testAppCanReadChangeLogsUsingDataOriginFilters() throws Exception {
        Bundle bundle =
                getChangeLogTokenAs(
                        APP_B_WITH_READ_WRITE_PERMS,
                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                        null);
        String changeLogTokenForAppB = bundle.getString(CHANGE_LOG_TOKEN);

        bundle =
                getChangeLogTokenAs(
                        APP_A_WITH_READ_WRITE_PERMS,
                        APP_B_WITH_READ_WRITE_PERMS.getPackageName(),
                        null);
        String changeLogTokenForAppA = bundle.getString(CHANGE_LOG_TOKEN);

        bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        List<String> listOfRecordIdsInsertedByAppA = new ArrayList<>();
        int noOfRecordsInsertedByAppA = 0;
        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds : listOfRecordIdsAndClass) {
            noOfRecordsInsertedByAppA += recordTypeAndRecordIds.getRecordIds().size();
            listOfRecordIdsInsertedByAppA.addAll(recordTypeAndRecordIds.getRecordIds());
        }

        updateRecordsAs(APP_A_WITH_READ_WRITE_PERMS, listOfRecordIdsAndClass);

        bundle = insertRecordAs(APP_B_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        int noOfRecordsInsertedByAppB = 0;
        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds : listOfRecordIdsAndClass) {
            noOfRecordsInsertedByAppB += recordTypeAndRecordIds.getRecordIds().size();
        }

        deleteRecordsAs(APP_B_WITH_READ_WRITE_PERMS, listOfRecordIdsAndClass);

        bundle =
                readChangeLogsUsingDataOriginFiltersAs(
                        APP_B_WITH_READ_WRITE_PERMS, changeLogTokenForAppB);

        ChangeLogsResponse response = bundle.getParcelable(CHANGE_LOGS_RESPONSE);

        assertThat(response.getUpsertedRecords()).hasSize(noOfRecordsInsertedByAppA);
        assertThat(
                        response.getUpsertedRecords().stream()
                                .map(Record::getMetadata)
                                .map(Metadata::getId)
                                .toList())
                .containsExactlyElementsIn(listOfRecordIdsInsertedByAppA);

        assertThat(response.getDeletedLogs()).isEmpty();

        bundle =
                readChangeLogsUsingDataOriginFiltersAs(
                        APP_A_WITH_READ_WRITE_PERMS, changeLogTokenForAppA);

        response = bundle.getParcelable(CHANGE_LOGS_RESPONSE);

        assertThat(response.getUpsertedRecords()).isEmpty();
        assertThat(response.getDeletedLogs()).hasSize(noOfRecordsInsertedByAppB);
    }

    @Test
    public void testGrantingCorrectPermsPutsTheAppInPriorityList() throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        List<String> oldPriorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        List<String> healthPerms =
                getGrantedHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        revokeHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        for (String perm : healthPerms) {
            grantPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), perm);
        }

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        List<String> newPriorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(newPriorityList).hasSize(oldPriorityList.size() + 1);
        assertThat(newPriorityList).contains(APP_A_WITH_READ_WRITE_PERMS.getPackageName());
    }

    @Test
    public void testRevokingOnlyOneCorrectPermissionDoesntRemoveAppFromPriorityList()
            throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        List<String> healthPerms =
                getGrantedHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        revokeHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        for (String perm : healthPerms) {
            grantPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), perm);
        }

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        List<String> oldPriorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(oldPriorityList).contains(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        revokePermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), healthPerms.get(0));

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        List<String> newPriorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(newPriorityList).contains(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        grantPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), healthPerms.get(0));
    }

    @Test
    public void testRevokingAllCorrectPermissionsRemovesAppFromPriorityList()
            throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        List<String> healthPerms =
                getGrantedHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        revokeHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        for (String perm : healthPerms) {
            grantPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), perm);
        }

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        List<String> oldPriorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(oldPriorityList).contains(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        for (String perm : healthPerms) {
            revokePermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), perm);
        }

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        List<String> newPriorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(newPriorityList.contains(APP_A_WITH_READ_WRITE_PERMS.getPackageName()))
                .isFalse();

        for (String perm : healthPerms) {
            grantPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), perm);
        }
    }

    @Test
    public void testAppWithManageHealthDataPermissionCanUpdatePriority()
            throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        List<String> healthPerms =
                getGrantedHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());

        revokeHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName());
        revokeHealthPermissions(APP_B_WITH_READ_WRITE_PERMS.getPackageName());

        for (String perm : healthPerms) {
            grantPermission(APP_A_WITH_READ_WRITE_PERMS.getPackageName(), perm);
        }

        for (String perm : healthPerms) {
            grantPermission(APP_B_WITH_READ_WRITE_PERMS.getPackageName(), perm);
        }

        List<DataOrigin> dataOriginPrioOrder =
                List.of(
                        new DataOrigin.Builder()
                                .setPackageName(APP_B_WITH_READ_WRITE_PERMS.getPackageName())
                                .build(),
                        new DataOrigin.Builder()
                                .setPackageName(APP_A_WITH_READ_WRITE_PERMS.getPackageName())
                                .build());

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        updateDataOriginPriorityOrder(
                new UpdateDataOriginPriorityOrderRequest(
                        dataOriginPrioOrder, HealthDataCategory.ACTIVITY));

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        List<String> newPriorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(
                        newPriorityList.equals(
                                dataOriginPrioOrder.stream()
                                        .map(dataOrigin -> dataOrigin.getPackageName())
                                        .collect(Collectors.toList())))
                .isTrue();
    }

    @Test
    public void testAppWithManageHealthDataPermsCanReadAnotherAppEntry() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        ArrayList<String> recordClassesToRead = new ArrayList<>();
        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds : listOfRecordIdsAndClass) {
            recordClassesToRead.add(recordTypeAndRecordIds.getRecordType());
        }

        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        int recordsSize = 0;
        try {
            for (String recordClass : recordClassesToRead) {
                List<? extends Record> recordsRead =
                        readRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(
                                                (Class<? extends Record>)
                                                        Class.forName(recordClass))
                                        .build(),
                                ApplicationProvider.getApplicationContext());

                recordsSize += recordsRead.size();
            }
        } catch (Exception e) {
            Assert.fail(
                    "App with MANAGE_HEALTH_DATA  permission should have read entries of another"
                            + " app!");
        }
        assertThat(recordsSize).isNotEqualTo(0);
    }

    @Test
    public void testAppWithManageHealthDataPermsCanDeleteAnotherAppEntry() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass =
                (List<TestUtils.RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS);

        List<RecordIdFilter> recordIdFilters = new ArrayList<>();
        for (TestUtils.RecordTypeAndRecordIds recordTypeAndRecordIds : listOfRecordIdsAndClass) {
            for (String recordId : recordTypeAndRecordIds.getRecordIds()) {
                recordIdFilters.add(
                        RecordIdFilter.fromId(
                                (Class<? extends Record>)
                                        Class.forName(recordTypeAndRecordIds.getRecordType()),
                                recordId));
            }
        }

        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            verifyDeleteRecords(recordIdFilters, ApplicationProvider.getApplicationContext());
        } catch (Exception e) {
            Assert.fail(
                    "App with MANAGE_HEALTH_DATA  permission should have deleted data from other"
                            + " app!");
        }
    }

    @Test
    public void testToVerifyGetContributorApplicationsInfo() throws Exception {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        bundle = insertRecordAs(APP_B_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<String> pkgNameList =
                List.of(
                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                        APP_B_WITH_READ_WRITE_PERMS.getPackageName());

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);

        // Contributor information is updated asynchronously, so retry with delay until the update
        // finishes (or we run out of retries).
        for (int i = 1; i <= ASYNC_RETRIES; i++) {
            List<String> appInfoList =
                    getApplicationInfo().stream()
                            .map(appInfo -> appInfo.getPackageName())
                            .collect(Collectors.toList());

            try {
                assertThat(appInfoList).containsAtLeastElementsIn(pkgNameList);
            } catch (AssertionError e) {
                if (i < ASYNC_RETRIES) {
                    Thread.sleep(ASYNC_RETRY_DELAY_MILLIS);
                    continue;
                }

                throw new AssertionError(e);
            }
        }
    }

    @Test
    public void testAggregationOutputForTotalStepsCountWithDataFromTwoAppsHavingDifferentPriority()
            throws Exception {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        revokeAndThenGrantHealthPermissions(APP_A_WITH_READ_WRITE_PERMS);
        revokeAndThenGrantHealthPermissions(APP_B_WITH_READ_WRITE_PERMS);

        List<DataOrigin> dataOriginPrioOrder =
                getDataOriginPriorityOrder(
                        APP_A_WITH_READ_WRITE_PERMS, APP_B_WITH_READ_WRITE_PERMS);

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        List<String> priorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(
                        priorityList.equals(
                                dataOriginPrioOrder.stream()
                                        .map(dataOrigin -> dataOrigin.getPackageName())
                                        .collect(Collectors.toList())))
                .isTrue();

        Bundle bundle =
                insertStepsRecordAs(APP_A_WITH_READ_WRITE_PERMS, "01:00 PM", "03:00 PM", 1000);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
        bundle = insertStepsRecordAs(APP_B_WITH_READ_WRITE_PERMS, "02:00 PM", "04:00 PM", 2000);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        AggregateRecordsRequest<Long> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Long>(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(getInstantTime("01:00 PM"))
                                        .setEndTime(getInstantTime("04:00 PM"))
                                        .build())
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        assertThat(aggregateRecordsRequest.getAggregationTypes()).isNotNull();
        assertThat(aggregateRecordsRequest.getTimeRangeFilter()).isNotNull();
        assertThat(aggregateRecordsRequest.getDataOriginsFilters()).isNotNull();

        AggregateRecordsResponse<Long> oldResponse = getAggregateResponse(aggregateRecordsRequest);
        assertThat(oldResponse.get(STEPS_COUNT_TOTAL)).isNotNull();
        assertThat(oldResponse.get(STEPS_COUNT_TOTAL)).isEqualTo(2000);

        dataOriginPrioOrder =
                getDataOriginPriorityOrder(
                        APP_B_WITH_READ_WRITE_PERMS, APP_A_WITH_READ_WRITE_PERMS);

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        updateDataOriginPriorityOrder(
                new UpdateDataOriginPriorityOrderRequest(
                        dataOriginPrioOrder, HealthDataCategory.ACTIVITY));

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        priorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(
                        priorityList.equals(
                                dataOriginPrioOrder.stream()
                                        .map(dataOrigin -> dataOrigin.getPackageName())
                                        .collect(Collectors.toList())))
                .isTrue();

        AggregateRecordsResponse<Long> newResponse = getAggregateResponse(aggregateRecordsRequest);
        assertThat(newResponse.get(STEPS_COUNT_TOTAL)).isNotNull();
        assertThat(newResponse.get(STEPS_COUNT_TOTAL)).isEqualTo(2500);
    }

    @Test
    public void testAggregationOutputForExerciseSessionWithDataFromTwoAppsHavingDifferentPriority()
            throws Exception {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        revokeAndThenGrantHealthPermissions(APP_A_WITH_READ_WRITE_PERMS);
        revokeAndThenGrantHealthPermissions(APP_B_WITH_READ_WRITE_PERMS);

        List<DataOrigin> dataOriginPrioOrder =
                getDataOriginPriorityOrder(
                        APP_A_WITH_READ_WRITE_PERMS, APP_B_WITH_READ_WRITE_PERMS);

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        List<String> priorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(
                        priorityList.equals(
                                dataOriginPrioOrder.stream()
                                        .map(dataOrigin -> dataOrigin.getPackageName())
                                        .collect(Collectors.toList())))
                .isTrue();

        Bundle bundle =
                insertExerciseSessionAs(
                        APP_A_WITH_READ_WRITE_PERMS,
                        "01:00 PM",
                        "03:00 PM",
                        "02:00 PM",
                        "03:00 PM");
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
        bundle =
                insertExerciseSessionAs(
                        APP_B_WITH_READ_WRITE_PERMS, "02:00 PM", "03:00 PM", null, null);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        AggregateRecordsRequest<Long> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Long>(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(getInstantTime("01:00 PM"))
                                        .setEndTime(getInstantTime("03:00 PM"))
                                        .build())
                        .addAggregationType(EXERCISE_DURATION_TOTAL)
                        .build();
        assertThat(aggregateRecordsRequest.getAggregationTypes()).isNotNull();
        assertThat(aggregateRecordsRequest.getTimeRangeFilter()).isNotNull();
        assertThat(aggregateRecordsRequest.getDataOriginsFilters()).isNotNull();

        AggregateRecordsResponse<Long> response = getAggregateResponse(aggregateRecordsRequest);
        assertThat(response.get(EXERCISE_DURATION_TOTAL)).isNotNull();
        assertThat(response.get(EXERCISE_DURATION_TOTAL))
                .isEqualTo(
                        (getInstantTime("03:00 PM").toEpochMilli()
                                        - getInstantTime("01:00 PM").toEpochMilli())
                                - (getInstantTime("03:00 PM").toEpochMilli()
                                        - getInstantTime("02:00 PM").toEpochMilli()));

        dataOriginPrioOrder =
                getDataOriginPriorityOrder(
                        APP_B_WITH_READ_WRITE_PERMS, APP_A_WITH_READ_WRITE_PERMS);

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        updateDataOriginPriorityOrder(
                new UpdateDataOriginPriorityOrderRequest(
                        dataOriginPrioOrder, HealthDataCategory.ACTIVITY));

        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        priorityList =
                fetchDataOriginsPriorityOrder(HealthDataCategory.ACTIVITY)
                        .getDataOriginsPriorityOrder()
                        .stream()
                        .map(dataOrigin -> dataOrigin.getPackageName())
                        .collect(Collectors.toList());

        assertThat(
                        priorityList.equals(
                                dataOriginPrioOrder.stream()
                                        .map(dataOrigin -> dataOrigin.getPackageName())
                                        .collect(Collectors.toList())))
                .isTrue();

        AggregateRecordsResponse<Long> newResponse = getAggregateResponse(aggregateRecordsRequest);
        assertThat(newResponse.get(EXERCISE_DURATION_TOTAL)).isNotNull();
        assertThat(newResponse.get(EXERCISE_DURATION_TOTAL))
                .isEqualTo(
                        getInstantTime("03:00 PM").toEpochMilli()
                                - getInstantTime("01:00 PM").toEpochMilli());
    }

    @Test
    public void testToVerifyNoPermissionChangeLog() throws Exception {
        ArrayList<String> recordClassesToRead = new ArrayList();
        recordClassesToRead.add(HeartRateRecord.class.getName());
        recordClassesToRead.add(StepsRecord.class.getName());

        Bundle bundle =
                getChangeLogTokenAs(
                        APP_B_WITH_READ_WRITE_PERMS,
                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                        recordClassesToRead);
        String changeLogTokenForAppB = bundle.getString(CHANGE_LOG_TOKEN);

        bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();

        List<String> healthPerms =
                getGrantedHealthPermissions(APP_B_WITH_READ_WRITE_PERMS.getPackageName());

        revokeHealthPermissions(APP_B_WITH_READ_WRITE_PERMS.getPackageName());

        try {
            readChangeLogsUsingDataOriginFiltersAs(
                    APP_B_WITH_READ_WRITE_PERMS, changeLogTokenForAppB);
            Assert.fail(
                    "Should have thrown exception in reading changeLogs without read permissions!");
        } catch (HealthConnectException e) {
            assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        }

        try {
            getChangeLogTokenAs(
                    APP_B_WITH_READ_WRITE_PERMS,
                    APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                    recordClassesToRead);
            Assert.fail(
                    "Should have thrown exception in getting change log token without read "
                            + "permission!");
        } catch (HealthConnectException e) {
            assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        }

        for (String perm : healthPerms) {
            grantPermission(APP_B_WITH_READ_WRITE_PERMS.getPackageName(), perm);
        }
    }
}
