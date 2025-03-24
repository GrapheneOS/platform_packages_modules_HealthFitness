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

package android.healthconnect.cts.datatypes.api;

import static android.healthconnect.cts.lib.RecordFactory.newEmptyMetadata;
import static android.healthconnect.cts.lib.RecordFactory.newEmptyMetadataWithId;
import static android.healthconnect.cts.utils.PermissionHelper.getGrantedHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.runWithRevokedPermission;
import static android.healthconnect.cts.utils.PermissionHelper.runWithRevokedPermissions;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.Record;
import android.healthconnect.cts.lib.RecordFactory;
import android.healthconnect.cts.lib.TestAppProxy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

abstract class BaseMultiAppApiTest<T extends Record> {
    private static final ZonedDateTime YESTERDAY_11AM =
            LocalDate.now(ZoneId.systemDefault())
                    .minusDays(1)
                    .atTime(11, 0)
                    .atZone(ZoneId.systemDefault());
    private static final String TEST_PACKAGE_NAME = getTestPackageName();
    private static final TestAppProxy APP_A_WITH_READ_WRITE_PERMS =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.A");

    private final Class<T> mRecordClass;
    private final String mReadPermission;
    private final String mWritePermission;
    private final RecordFactory<T> mRecordFactory;

    BaseMultiAppApiTest(
            Class<T> recordClass,
            String readPermission,
            String writePermission,
            RecordFactory<T> recordFactory) {
        mRecordClass = recordClass;
        mReadPermission = readPermission;
        mWritePermission = writePermission;
        mRecordFactory = recordFactory;
    }

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws InterruptedException {
        assertThat(getGrantedHealthPermissions(getTestPackageName()))
                .containsAtLeast(mReadPermission, mWritePermission);
        assertThat(getGrantedHealthPermissions(APP_A_WITH_READ_WRITE_PERMS.getPackageName()))
                .containsAtLeast(mReadPermission, mWritePermission);
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void insertRecords_noWritePermission_throws() {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                runWithRevokedPermissions(
                                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                                        mWritePermission,
                                        () ->
                                                APP_A_WITH_READ_WRITE_PERMS.insertRecords(
                                                        recordsToInsert)));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void readRecords_byId_otherAppRecord_returnsRecord() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<? extends Record> returnedRecords =
                APP_A_WITH_READ_WRITE_PERMS.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addId(recordIds.get(0))
                                .build());

        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void readRecords_byId_otherAppRecord_noReadPermission_throws() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<? extends Record> returnedRecords =
                runWithRevokedPermissions(
                        () ->
                                APP_A_WITH_READ_WRITE_PERMS.readRecords(
                                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                                .addId(recordIds.get(0))
                                                .build()),
                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                        mReadPermission);

        // TODO(b/309778116): this should be an error rather than an empty response.
        assertThat(returnedRecords).isEmpty();
    }

    @Test
    public void readRecords_byId_ownRecord_noReadPermission_returnsRecord() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = APP_A_WITH_READ_WRITE_PERMS.insertRecords(recordsToInsert);

        List<? extends Record> returnedRecords =
                APP_A_WITH_READ_WRITE_PERMS.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addId(recordIds.get(0))
                                .build());

        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndPackageName(
                                recordsToInsert.get(0),
                                recordIds.get(0),
                                APP_A_WITH_READ_WRITE_PERMS.getPackageName()));
    }

    @Test
    public void readRecords_byId_ownRecord_noReadWritePermissions_throws() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = APP_A_WITH_READ_WRITE_PERMS.insertRecords(recordsToInsert);

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                runWithRevokedPermissions(
                                        () ->
                                                APP_A_WITH_READ_WRITE_PERMS.readRecords(
                                                        new ReadRecordsRequestUsingIds.Builder<>(
                                                                        mRecordClass)
                                                                .addId(recordIds.get(0))
                                                                .build()),
                                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                                        mReadPermission,
                                        mWritePermission));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void deleteRecords_byId_noWritePermission_throws() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = APP_A_WITH_READ_WRITE_PERMS.insertRecords(recordsToInsert);

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                runWithRevokedPermissions(
                                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                                        mWritePermission,
                                        () ->
                                                APP_A_WITH_READ_WRITE_PERMS.deleteRecords(
                                                        RecordIdFilter.fromId(
                                                                mRecordClass, recordIds.get(0)))));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndPackageName(
                                recordsToInsert.get(0),
                                recordIds.get(0),
                                APP_A_WITH_READ_WRITE_PERMS.getPackageName()));
    }

    @Test
    public void deleteRecords_byId_otherAppRecord_throws() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                APP_A_WITH_READ_WRITE_PERMS.deleteRecords(
                                        RecordIdFilter.fromId(mRecordClass, recordIds.get(0))));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void updateRecords_noWritePermission_ownRecord_throws() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = APP_A_WITH_READ_WRITE_PERMS.insertRecords(recordsToInsert);
        Record updatedRecord =
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadataWithId(recordIds.get(0)),
                        YESTERDAY_11AM.plusMinutes(20).toInstant(),
                        YESTERDAY_11AM.plusMinutes(35).toInstant());

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                runWithRevokedPermissions(
                                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                                        mWritePermission,
                                        () ->
                                                APP_A_WITH_READ_WRITE_PERMS.updateRecords(
                                                        List.of(updatedRecord))));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndPackageName(
                                recordsToInsert.get(0),
                                recordIds.get(0),
                                APP_A_WITH_READ_WRITE_PERMS.getPackageName()));
    }

    @Test
    public void updateRecords_otherAppRecord_throws() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        Record updatedRecord =
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadataWithId(recordIds.get(0)),
                        YESTERDAY_11AM.plusMinutes(20).toInstant(),
                        YESTERDAY_11AM.plusMinutes(35).toInstant());

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> APP_A_WITH_READ_WRITE_PERMS.updateRecords(List.of(updatedRecord)));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void getChangesToken_noReadPermission_throws() {
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                runWithRevokedPermission(
                                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                                        mReadPermission,
                                        () ->
                                                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                                                        new ChangeLogTokenRequest.Builder()
                                                                .addRecordType(mRecordClass)
                                                                .build())));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void getChanges_noReadPermission_throws() throws Exception {
        String token =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder().addRecordType(mRecordClass).build());
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                runWithRevokedPermission(
                                        APP_A_WITH_READ_WRITE_PERMS.getPackageName(),
                                        mReadPermission,
                                        () ->
                                                APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(
                                                        new ChangeLogsRequest.Builder(token)
                                                                .build())));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    private static String getTestPackageName() {
        return ApplicationProvider.getApplicationContext().getPackageName();
    }

    private Record withIdAndTestPackageName(Record record, String id) {
        return mRecordFactory.recordWithIdAndPackageName(record, id, TEST_PACKAGE_NAME);
    }

    private Record withIdAndPackageName(Record record, String id, String packageName) {
        return mRecordFactory.recordWithIdAndPackageName(record, id, packageName);
    }

    private static List<String> insertRecordsAndReturnIds(List<Record> records) throws Exception {
        return TestUtils.insertRecords(records).stream().map(r -> r.getMetadata().getId()).toList();
    }

    private List<? extends Record> readAllRecords() throws InterruptedException {
        return TestUtils.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(mRecordClass).build());
    }
}
