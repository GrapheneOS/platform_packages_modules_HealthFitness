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

import static android.healthconnect.testing.cts.PermissionUtils.getGrantedHealthPermissions;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadataWithId;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.Record;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy;
import android.healthconnect.testing.cts.testapphelpers.TestAppRule;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.healthconnect.testing.shared.recordfactory.RecordFactory;
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
import java.util.function.Supplier;

abstract class BaseMultiAppApiTest<T extends Record> {
    static final ZonedDateTime YESTERDAY_11AM =
            LocalDate.now(ZoneId.systemDefault())
                    .minusDays(1)
                    .atTime(11, 0)
                    .atZone(ZoneId.systemDefault());
    static final String TEST_APP_ONE_PACKAGE_NAME = getTestAppOnePackageName();

    @Rule(order = 0)
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Rule(order = 1)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 2)
    public final TestAppRule mTestAppTwoRule =
            new TestAppRule.Builder("android.healthconnect.cts.testapp.readWritePerms.A").build();

    final TestAppProxy mTestAppTwo = mTestAppTwoRule.getProxy();

    /**
     * The record class may be unavailable on older builds. Using a supplier makes sure the class
     * resolution gets postponed until {@link #setUp()} which never gets reached if the
     * corresponding API flag is disabled.
     */
    private final Supplier<Class<T>> mRecordClassSupplier;

    private Class<T> mRecordClass;
    final AppPermissions mTestPackagePermissions;
    final AppPermissions mTestAppTwoPermissions;
    private final RecordFactory<T> mRecordFactory;

    BaseMultiAppApiTest(
            Supplier<Class<T>> recordClassSupplier,
            String readPermission,
            String writePermission,
            RecordFactory<T> recordFactory) {
        this(
                recordClassSupplier,
                new AppPermissions(List.of(readPermission), List.of(writePermission)),
                new AppPermissions(List.of(readPermission), List.of(writePermission)),
                recordFactory);
    }

    BaseMultiAppApiTest(
            Supplier<Class<T>> recordClassSupplier,
            AppPermissions testPackagePermissions,
            AppPermissions testAppTwoPermissions,
            RecordFactory<T> recordFactory) {
        mRecordClassSupplier = recordClassSupplier;
        mTestPackagePermissions = testPackagePermissions;
        mTestAppTwoPermissions = testAppTwoPermissions;
        mRecordFactory = recordFactory;
    }

    @Before
    public void setUp() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();

        mRecordClass = mRecordClassSupplier.get();
        assertThat(getGrantedHealthPermissions(getTestAppOnePackageName()))
                .containsAtLeastElementsIn(mTestPackagePermissions.readPermissions);
        assertThat(getGrantedHealthPermissions(getTestAppOnePackageName()))
                .containsAtLeastElementsIn(mTestPackagePermissions.writePermissions);
        assertThat(getGrantedHealthPermissions(mTestAppTwo.getPackageName()))
                .containsAtLeastElementsIn(mTestAppTwoPermissions.readPermissions);
        assertThat(getGrantedHealthPermissions(mTestAppTwo.getPackageName()))
                .containsAtLeastElementsIn(mTestAppTwoPermissions.writePermissions);
    }

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test
    public void insertRecords_noWritePermission_throws() {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));

        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.writePermissions);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> mTestAppTwo.insertRecords(recordsToInsert));

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
                mTestAppTwo.readRecords(
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

        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.readPermissions);
        List<? extends Record> returnedRecords =
                mTestAppTwo.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addId(recordIds.get(0))
                                .build());

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
        List<String> recordIds = mTestAppTwo.insertRecords(recordsToInsert);

        List<? extends Record> returnedRecords =
                mTestAppTwo.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addId(recordIds.get(0))
                                .build());

        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndPackageName(
                                recordsToInsert.get(0),
                                recordIds.get(0),
                                mTestAppTwo.getPackageName()));
    }

    @Test
    public void readRecords_byId_ownRecord_noReadWritePermissions_throws() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = mTestAppTwo.insertRecords(recordsToInsert);

        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.readPermissions);
        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.writePermissions);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestAppTwo.readRecords(
                                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                                .addId(recordIds.get(0))
                                                .build()));

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
        List<String> recordIds = mTestAppTwo.insertRecords(recordsToInsert);

        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.writePermissions);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestAppTwo.deleteRecords(
                                        RecordIdFilter.fromId(mRecordClass, recordIds.get(0))));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndPackageName(
                                recordsToInsert.get(0),
                                recordIds.get(0),
                                mTestAppTwo.getPackageName()));
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
                                mTestAppTwo.deleteRecords(
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
        List<String> recordIds = mTestAppTwo.insertRecords(recordsToInsert);
        Record updatedRecord =
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadataWithId(recordIds.get(0)),
                        YESTERDAY_11AM.plusMinutes(20).toInstant(),
                        YESTERDAY_11AM.plusMinutes(35).toInstant());

        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.writePermissions);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> mTestAppTwo.updateRecords(List.of(updatedRecord)));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndPackageName(
                                recordsToInsert.get(0),
                                recordIds.get(0),
                                mTestAppTwo.getPackageName()));
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
                        () -> mTestAppTwo.updateRecords(List.of(updatedRecord)));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void getChangesToken_noReadPermission_throws() {
        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.readPermissions);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestAppTwo.getChangeLogToken(
                                        new ChangeLogTokenRequest.Builder()
                                                .addRecordType(mRecordClass)
                                                .build()));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void getChanges_noReadPermission_throws() throws Exception {
        String token =
                mTestAppTwo.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder().addRecordType(mRecordClass).build());
        mTestAppTwoRule.revokeHealthPermissions(mTestAppTwoPermissions.readPermissions);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestAppTwo.getChangeLogs(
                                        new ChangeLogsRequest.Builder(token).build()));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    public record AppPermissions(List<String> readPermissions, List<String> writePermissions) {}

    private static String getTestAppOnePackageName() {
        return ApplicationProvider.getApplicationContext().getPackageName();
    }

    private Record withIdAndTestPackageName(Record record, String id) {
        return mRecordFactory.recordWithIdAndPackageName(record, id, TEST_APP_ONE_PACKAGE_NAME);
    }

    private Record withIdAndPackageName(Record record, String id, String packageName) {
        return mRecordFactory.recordWithIdAndPackageName(record, id, packageName);
    }

    static List<String> insertRecordsAndReturnIds(List<? extends Record> records) throws Exception {
        return TestUtils.insertRecords(records).stream().map(r -> r.getMetadata().getId()).toList();
    }

    private List<? extends Record> readAllRecords() throws InterruptedException {
        return TestUtils.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(mRecordClass).build());
    }
}
