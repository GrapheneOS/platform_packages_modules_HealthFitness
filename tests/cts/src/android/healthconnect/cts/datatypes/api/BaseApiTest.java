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

import static android.healthconnect.cts.lib.RecordFactory.newAnotherFullMetadataWithClientId;
import static android.healthconnect.cts.lib.RecordFactory.newAnotherFullMetadataWithClientIdAndVersion;
import static android.healthconnect.cts.lib.RecordFactory.newAnotherFullMetadataWithId;
import static android.healthconnect.cts.lib.RecordFactory.newEmptyMetadata;
import static android.healthconnect.cts.lib.RecordFactory.newEmptyMetadataWithClientId;
import static android.healthconnect.cts.lib.RecordFactory.newEmptyMetadataWithIdClientIdAndVersion;
import static android.healthconnect.cts.lib.RecordFactory.newFullMetadataWithClientId;
import static android.healthconnect.cts.lib.RecordFactory.newFullMetadataWithClientIdAndVersion;
import static android.healthconnect.cts.lib.RecordFactory.newFullMetadataWithId;
import static android.healthconnect.cts.lib.RecordFactory.newFullMetadataWithoutIds;
import static android.healthconnect.cts.utils.PermissionHelper.getGrantedHealthPermissions;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Record;
import android.healthconnect.cts.lib.RecordFactory;
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
import java.util.UUID;

abstract class BaseApiTest<T extends Record> {
    private static final ZonedDateTime YESTERDAY_11AM =
            LocalDate.now(ZoneId.systemDefault())
                    .minusDays(1)
                    .atTime(11, 0)
                    .atZone(ZoneId.systemDefault());
    private static final String TEST_PACKAGE_NAME = getTestPackageName();

    private final Class<T> mRecordClass;
    private final String mReadPermission;
    private final String mWritePermission;
    private final RecordFactory<T> mRecordFactory;

    BaseApiTest(
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
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void insertRecords_returnsInsertedRecords() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()));

        List<Record> insertedRecords = TestUtils.insertRecords(recordsToInsert);

        assertThat(insertedRecords)
                .containsExactly(
                        withIdAndTestPackageName(
                                recordsToInsert.get(0),
                                insertedRecords.get(0).getMetadata().getId()),
                        withIdAndTestPackageName(
                                recordsToInsert.get(1),
                                insertedRecords.get(1).getMetadata().getId()));
    }

    @Test
    public void insertRecords_newClientVersionIsLower_doesNotUpdate() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        String ignoredId = UUID.randomUUID().toString();
        Record updatedRecord =
                mRecordFactory.anotherFullRecord(
                        newEmptyMetadataWithIdClientIdAndVersion(ignoredId, "foo-client-id", 122),
                        YESTERDAY_11AM.plusMinutes(40).toInstant(),
                        YESTERDAY_11AM.plusMinutes(55).toInstant());

        TestUtils.insertRecords(List.of(updatedRecord));

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void insertRecords_newClientVersionIsEqual_updatesRecord() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        String ignoredId = UUID.randomUUID().toString();
        Record updatedRecord =
                mRecordFactory.anotherFullRecord(
                        newEmptyMetadataWithIdClientIdAndVersion(ignoredId, "foo-client-id", 123),
                        YESTERDAY_11AM.plusMinutes(40).toInstant(),
                        YESTERDAY_11AM.plusMinutes(55).toInstant());

        TestUtils.insertRecords(List.of(updatedRecord));

        assertThat(readAllRecords())
                .containsExactly(withIdAndTestPackageName(updatedRecord, recordIds.get(0)));
    }

    @Test
    public void insertRecords_newClientVersionIsGreater_updatesRecord() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        String ignoredId = UUID.randomUUID().toString();
        Record updatedRecord =
                mRecordFactory.anotherFullRecord(
                        newEmptyMetadataWithIdClientIdAndVersion(ignoredId, "foo-client-id", 124),
                        YESTERDAY_11AM.plusMinutes(40).toInstant(),
                        YESTERDAY_11AM.plusMinutes(55).toInstant());

        TestUtils.insertRecords(List.of(updatedRecord));

        assertThat(readAllRecords())
                .containsExactly(withIdAndTestPackageName(updatedRecord, recordIds.get(0)));
    }

    @Test
    public void readRecords_usingIds_returnsRecord() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithoutIds(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()),
                        mRecordFactory.anotherFullRecord(
                                newFullMetadataWithId(UUID.randomUUID().toString()),
                                YESTERDAY_11AM.plusMinutes(50).toInstant(),
                                YESTERDAY_11AM.plusMinutes(58).toInstant()));

        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addId(recordIds.get(0))
                                .build());

        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void readRecords_usingIds_multipleRecords_returnsRecords() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithoutIds(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()),
                        mRecordFactory.anotherFullRecord(
                                newAnotherFullMetadataWithId(UUID.randomUUID().toString()),
                                YESTERDAY_11AM.plusMinutes(50).toInstant(),
                                YESTERDAY_11AM.plusMinutes(58).toInstant()));

        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addId(recordIds.get(1))
                                .addId(recordIds.get(2))
                                .build());

        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)),
                        withIdAndTestPackageName(recordsToInsert.get(2), recordIds.get(2)));
    }

    @Test
    public void readRecords_usingIds_absentId_returnsEmptyResponse() throws Exception {
        TestUtils.insertRecords(
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadata(),
                        YESTERDAY_11AM.plusMinutes(10).toInstant(),
                        YESTERDAY_11AM.plusMinutes(25).toInstant()),
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadata(),
                        YESTERDAY_11AM.plusMinutes(30).toInstant(),
                        YESTERDAY_11AM.plusMinutes(45).toInstant()),
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadata(),
                        YESTERDAY_11AM.plusMinutes(50).toInstant(),
                        YESTERDAY_11AM.plusMinutes(59).toInstant()));

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addId(UUID.randomUUID().toString())
                                .addId(UUID.randomUUID().toString())
                                .addId(UUID.randomUUID().toString())
                                .build());

        assertThat(returnedRecords).isEmpty();
    }

    @Test
    public void readRecords_usingClientRecordId_returnsRecord() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newFullMetadataWithClientIdAndVersion("buzz-client-id", 456),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()),
                        mRecordFactory.anotherFullRecord(
                                newFullMetadataWithClientIdAndVersion("bar-client-id", 789),
                                YESTERDAY_11AM.plusMinutes(50).toInstant(),
                                YESTERDAY_11AM.plusMinutes(58).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addClientRecordId("foo-client-id")
                                .build());

        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void readRecords_usingClientRecordId_multipleClientId_returnsRecords() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithClientId("foo-client-id"),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("buzz-client-id"),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()),
                        mRecordFactory.anotherFullRecord(
                                newAnotherFullMetadataWithClientId("bar-client-id"),
                                YESTERDAY_11AM.plusMinutes(50).toInstant(),
                                YESTERDAY_11AM.plusMinutes(58).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addClientRecordId("buzz-client-id")
                                .addClientRecordId("bar-client-id")
                                .build());

        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)),
                        withIdAndTestPackageName(recordsToInsert.get(2), recordIds.get(2)));
    }

    @Test
    public void readRecords_usingClientIds_absentClientId_returnsEmptyResponse() throws Exception {
        TestUtils.insertRecords(
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadataWithClientId("foo-client-id"),
                        YESTERDAY_11AM.plusMinutes(10).toInstant(),
                        YESTERDAY_11AM.plusMinutes(25).toInstant()),
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadataWithClientId("bar-client-id"),
                        YESTERDAY_11AM.plusMinutes(30).toInstant(),
                        YESTERDAY_11AM.plusMinutes(45).toInstant()),
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadataWithClientId("buzz-client-id"),
                        YESTERDAY_11AM.plusMinutes(50).toInstant(),
                        YESTERDAY_11AM.plusMinutes(59).toInstant()));

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                .addClientRecordId("oo-client-id")
                                .addClientRecordId("foo-client-i")
                                .addClientRecordId("foo-client-id2")
                                .build());

        assertThat(returnedRecords).isEmpty();
    }

    @Test
    public void readRecords_usingFilters_default() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(50).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(mRecordClass).build());

        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)),
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)),
                        withIdAndTestPackageName(recordsToInsert.get(2), recordIds.get(2)),
                        withIdAndTestPackageName(recordsToInsert.get(3), recordIds.get(3)));
    }

    @Test
    public void readRecords_usingFilters_byPackageName() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(50).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(mRecordClass)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(TEST_PACKAGE_NAME)
                                                .build())
                                .build());

        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)),
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)),
                        withIdAndTestPackageName(recordsToInsert.get(2), recordIds.get(2)),
                        withIdAndTestPackageName(recordsToInsert.get(3), recordIds.get(3)));
    }

    @Test
    public void readRecords_usingFilters_byAbsentPackageName() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(50).toInstant()));
        TestUtils.insertRecords(recordsToInsert);

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(mRecordClass)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName("absent.package.name")
                                                .build())
                                .build());

        assertThat(returnedRecords).isEmpty();
    }

    @Test
    public void readRecords_usingFilters_byTimeInstantRangeFilter() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(25).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(55).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(51).toInstant(),
                                YESTERDAY_11AM.plusMinutes(60).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(60).toInstant(),
                                YESTERDAY_11AM.plusMinutes(70).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        List<? extends Record> returnedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(mRecordClass)
                                .setTimeRangeFilter(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        YESTERDAY_11AM.plusMinutes(20).toInstant())
                                                .setEndTime(
                                                        YESTERDAY_11AM.plusMinutes(50).toInstant())
                                                .build())
                                .build());
        assertThat(returnedRecords)
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)),
                        withIdAndTestPackageName(recordsToInsert.get(2), recordIds.get(2)),
                        withIdAndTestPackageName(recordsToInsert.get(3), recordIds.get(3)));
    }

    @Test
    public void deleteRecords_usingFilters_default() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(50).toInstant()));
        TestUtils.insertRecords(recordsToInsert);

        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder().addRecordType(mRecordClass).build());

        assertThat(readAllRecords()).isEmpty();
    }

    @Test
    public void deleteRecords_usingFilters_byTimeInstantRangeFilter() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(25).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(55).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(50).toInstant(),
                                YESTERDAY_11AM.plusMinutes(60).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(60).toInstant(),
                                YESTERDAY_11AM.plusMinutes(70).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(mRecordClass)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(YESTERDAY_11AM.plusMinutes(20).toInstant())
                                        .setEndTime(YESTERDAY_11AM.plusMinutes(50).toInstant())
                                        .build())
                        .build());

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)),
                        withIdAndTestPackageName(recordsToInsert.get(4), recordIds.get(4)),
                        withIdAndTestPackageName(recordsToInsert.get(5), recordIds.get(5)));
    }

    @Test
    public void deleteRecords_usingFilters_byPackageName() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(50).toInstant()));
        TestUtils.insertRecords(recordsToInsert);

        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(mRecordClass)
                        .addDataOrigin(
                                new DataOrigin.Builder().setPackageName(TEST_PACKAGE_NAME).build())
                        .build());

        assertThat(readAllRecords()).isEmpty();
    }

    @Test
    public void deleteRecords_usingFilters_byAbsentPackageName() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(mRecordClass)
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName("absent.package.name")
                                        .build())
                        .build());

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)),
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)),
                        withIdAndTestPackageName(recordsToInsert.get(2), recordIds.get(2)));
    }

    @Test
    public void deleteRecords_usingIds() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        TestUtils.verifyDeleteRecords(
                List.of(
                        RecordIdFilter.fromId(mRecordClass, recordIds.get(1)),
                        RecordIdFilter.fromId(mRecordClass, recordIds.get(2))));

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void deleteRecords_usingId_absentId() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        TestUtils.verifyDeleteRecords(
                List.of(RecordIdFilter.fromId(mRecordClass, UUID.randomUUID().toString())));

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)),
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)));
    }

    @Test
    public void deleteRecords_usingClientRecordIds() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("foo-client-id"),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("bar-client-id"),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("buzz-client-id"),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        TestUtils.verifyDeleteRecords(
                List.of(
                        RecordIdFilter.fromClientRecordId(mRecordClass, "bar-client-id"),
                        RecordIdFilter.fromClientRecordId(mRecordClass, "buzz-client-id")));

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void deleteRecords_usingClientRecordIds_absentClientRecordIds() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("foo-client-id"),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("bar-client-id"),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("buzz-client-id"),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);

        TestUtils.verifyDeleteRecords(
                List.of(
                        RecordIdFilter.fromClientRecordId(mRecordClass, "foo-client-i"),
                        RecordIdFilter.fromClientRecordId(mRecordClass, "oo-client-id"),
                        RecordIdFilter.fromClientRecordId(mRecordClass, "foo-client-id2"),
                        RecordIdFilter.fromClientRecordId(mRecordClass, "Foo-client-id")));

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)),
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)),
                        withIdAndTestPackageName(recordsToInsert.get(2), recordIds.get(2)));
    }

    @Test
    public void updateRecords_usingId() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithoutIds(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        Record updatedRecord =
                mRecordFactory.anotherFullRecord(
                        newFullMetadataWithId(recordIds.get(0)),
                        YESTERDAY_11AM.plusMinutes(40).toInstant(),
                        YESTERDAY_11AM.plusMinutes(55).toInstant());

        TestUtils.updateRecords(List.of(updatedRecord));

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(updatedRecord, recordIds.get(0)),
                        withIdAndTestPackageName(recordsToInsert.get(1), recordIds.get(1)));
    }

    @Test
    public void updateRecords_usingClientRecordId() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.anotherFullRecord(
                                newAnotherFullMetadataWithClientIdAndVersion("bar-client-id", 789),
                                YESTERDAY_11AM.plusMinutes(50).toInstant(),
                                YESTERDAY_11AM.plusMinutes(58).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        String ignoredId = UUID.randomUUID().toString();
        int ignoredVersion = 1;
        Record updatedRecord =
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadataWithIdClientIdAndVersion(
                                ignoredId, "bar-client-id", ignoredVersion),
                        YESTERDAY_11AM.plusMinutes(40).toInstant(),
                        YESTERDAY_11AM.plusMinutes(55).toInstant());

        TestUtils.updateRecords(List.of(updatedRecord));

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)),
                        withIdAndTestPackageName(updatedRecord, recordIds.get(1)));
    }

    @Test
    public void updateRecords_usingId_absentId() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        Record updatedRecord =
                mRecordFactory.newFullRecord(
                        newFullMetadataWithId(UUID.randomUUID().toString()),
                        YESTERDAY_11AM.plusMinutes(40).toInstant(),
                        YESTERDAY_11AM.plusMinutes(55).toInstant());

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> TestUtils.updateRecords(List.of(updatedRecord)));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void updateRecords_usingClientRecordId_absentClientId() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("foo-client-id"),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        Record updatedRecord =
                mRecordFactory.newEmptyRecord(
                        newEmptyMetadataWithClientId("Foo-client-id"),
                        YESTERDAY_11AM.plusMinutes(40).toInstant(),
                        YESTERDAY_11AM.plusMinutes(55).toInstant());

        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> TestUtils.updateRecords(List.of(updatedRecord)));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);

        assertThat(readAllRecords())
                .containsExactly(
                        withIdAndTestPackageName(recordsToInsert.get(0), recordIds.get(0)));
    }

    @Test
    public void insertRecords_generatesChangelogs() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithClientIdAndVersion("foo-client-id", 123),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()));
        String token =
                TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(mRecordClass)
                                        .build())
                        .getToken();
        List<Record> insertedRecords = TestUtils.insertRecords(recordsToInsert);

        ChangeLogsResponse response =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        assertThat(response.getUpsertedRecords())
                .containsExactly(
                        withIdAndTestPackageName(
                                insertedRecords.get(0),
                                insertedRecords.get(0).getMetadata().getId()),
                        withIdAndTestPackageName(
                                insertedRecords.get(1),
                                insertedRecords.get(1).getMetadata().getId()));
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    public void deleteRecords_usingFilters_generatesChangelogs() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        String token =
                TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(mRecordClass)
                                        .build())
                        .getToken();
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder().addRecordType(mRecordClass).build());

        ChangeLogsResponse response =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        assertThat(response.getUpsertedRecords()).isEmpty();
        assertThat(
                        response.getDeletedLogs().stream()
                                .map(ChangeLogsResponse.DeletedLog::getDeletedRecordId)
                                .toList())
                .containsExactlyElementsIn(recordIds);
    }

    @Test
    public void deleteRecords_usingIds_generatesChangelogs() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("bar-client-id"),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(40).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        String token =
                TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(mRecordClass)
                                        .build())
                        .getToken();
        TestUtils.verifyDeleteRecords(
                List.of(
                        RecordIdFilter.fromId(mRecordClass, recordIds.get(1)),
                        RecordIdFilter.fromClientRecordId(mRecordClass, "bar-client-id")));

        ChangeLogsResponse response =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        assertThat(response.getUpsertedRecords()).isEmpty();
        assertThat(
                        response.getDeletedLogs().stream()
                                .map(ChangeLogsResponse.DeletedLog::getDeletedRecordId)
                                .toList())
                .containsExactly(recordIds.get(1), recordIds.get(2));
    }

    @Test
    public void updateRecords_generatesChangelogs() throws Exception {
        List<Record> recordsToInsert =
                List.of(
                        mRecordFactory.newFullRecord(
                                newFullMetadataWithClientId("foo-client-id"),
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(15).toInstant()),
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadata(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                YESTERDAY_11AM.plusMinutes(45).toInstant()));
        List<String> recordIds = insertRecordsAndReturnIds(recordsToInsert);
        List<Record> updatedRecords =
                List.of(
                        mRecordFactory.newEmptyRecord(
                                newEmptyMetadataWithClientId("foo-client-id"),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(27).toInstant()),
                        mRecordFactory.anotherFullRecord(
                                newFullMetadataWithId(recordIds.get(1)),
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(55).toInstant()));
        String token =
                TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(mRecordClass)
                                        .build())
                        .getToken();
        TestUtils.updateRecords(updatedRecords);

        ChangeLogsResponse response =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        assertThat(response.getUpsertedRecords())
                .containsExactly(
                        withIdAndTestPackageName(updatedRecords.get(0), recordIds.get(0)),
                        withIdAndTestPackageName(updatedRecords.get(1), recordIds.get(1)));
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    private static String getTestPackageName() {
        return ApplicationProvider.getApplicationContext().getPackageName();
    }

    private Record withIdAndTestPackageName(Record record, String id) {
        return mRecordFactory.recordWithIdAndPackageName(record, id, TEST_PACKAGE_NAME);
    }

    private static List<String> insertRecordsAndReturnIds(List<Record> records) throws Exception {
        return TestUtils.insertRecords(records).stream().map(r -> r.getMetadata().getId()).toList();
    }

    private List<? extends Record> readAllRecords() throws InterruptedException {
        return TestUtils.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(mRecordClass).build());
    }
}
