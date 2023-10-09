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

package com.android.server.healthconnect.storage;

import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;

import static com.android.server.healthconnect.storage.datatypehelpers.TransactionTestUtils.createBloodPressureRecord;
import static com.android.server.healthconnect.storage.datatypehelpers.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.test.runner.AndroidJUnit4;

import com.android.server.healthconnect.HealthConnectUserContext;
import com.android.server.healthconnect.storage.datatypehelpers.DatabaseHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthConnectDatabaseTestRule;
import com.android.server.healthconnect.storage.datatypehelpers.TransactionTestUtils;
import com.android.server.healthconnect.storage.request.ReadTransactionRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class TransactionManagerTest {
    private static final Map<String, Boolean> NO_EXTRA_PERMS = Map.of();
    private static final String TEST_PACKAGE_NAME = "package.name";
    @Rule public final HealthConnectDatabaseTestRule testRule = new HealthConnectDatabaseTestRule();

    private TransactionTestUtils mTransactionTestUtils;
    private TransactionManager mTransactionManager;

    @Before
    public void setup() {
        HealthConnectUserContext context = testRule.getUserContext();
        mTransactionManager = TransactionManager.getInstance(context);
        mTransactionTestUtils = new TransactionTestUtils(context, mTransactionManager);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
    }

    @After
    public void tearDown() {
        DatabaseHelper.clearAllData(mTransactionManager);
        TransactionManager.clearInstance();
    }

    @Test
    public void readRecordsById_multipleRecordTypes_returnsAllRecords() {
        long startTimeMillis = 123;
        long endTimeMillis = 456;
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        createStepsRecord(startTimeMillis, endTimeMillis, 100),
                        createBloodPressureRecord(endTimeMillis, 120.0, 80.0));

        List<UUID> stepsUuids = ImmutableList.of(UUID.fromString(uuids.get(0)));
        List<UUID> bloodPressureUuids = ImmutableList.of(UUID.fromString(uuids.get(1)));
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        TEST_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                stepsUuids,
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                bloodPressureUuids),
                        /* startDateAccess= */ 0,
                        NO_EXTRA_PERMS);

        List<RecordInternal<?>> records = mTransactionManager.readRecordsByIds(request);
        assertThat(records).hasSize(2);
        assertThat(records.get(0).getUuid().toString()).isEqualTo(uuids.get(0));
        assertThat(records.get(1).getUuid().toString()).isEqualTo(uuids.get(1));
    }

    @Test
    public void readRecordsById_exceedMaxPageSize_recordsNotReturned() {
        List<RecordInternal<?>> inputRecords = new ArrayList<>(MAXIMUM_PAGE_SIZE + 1);
        for (int i = 0; i < MAXIMUM_PAGE_SIZE + 1; i++) {
            inputRecords.add(createStepsRecord(i, i + 1, 100));
        }
        List<String> uuidStrings =
                mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, inputRecords);
        List<UUID> uuids = new ArrayList<>(uuidStrings.size());
        for (String uuidString : uuidStrings) {
            uuids.add(UUID.fromString(uuidString));
        }

        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        TEST_PACKAGE_NAME,
                        ImmutableMap.of(RecordTypeIdentifier.RECORD_TYPE_STEPS, uuids),
                        /* startDateAccess= */ 0,
                        NO_EXTRA_PERMS);

        List<RecordInternal<?>> records = mTransactionManager.readRecordsByIds(request);
        assertThat(records).hasSize(MAXIMUM_PAGE_SIZE);
    }

    @Test
    public void readRecordsAndNextRecordStartTime_returnsRecordsAndTimestamp() {
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        createStepsRecord(400, 500, 100),
                        createStepsRecord(500, 600, 100));

        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.ofEpochMilli(1000))
                                        .build())
                        .setPageSize(1)
                        .build();

        ReadTransactionRequest readTransactionRequest =
                new ReadTransactionRequest(
                        TEST_PACKAGE_NAME,
                        request.toReadRecordsRequestParcel(),
                        /* startDateAccess= */ 0,
                        /* enforceSelfRead= */ false,
                        /* extraReadPermsMapping= */ new ArrayMap<>());
        Pair<List<RecordInternal<?>>, Long> blah =
                mTransactionManager.readRecordsAndNextRecordStartTime(readTransactionRequest);
        List<RecordInternal<?>> records = blah.first;
        assertThat(records).hasSize(1);
        assertThat(blah.first.get(0).getUuid()).isEqualTo(UUID.fromString(uuids.get(0)));
        assertThat(blah.second).isEqualTo(500);
    }

    @Test
    public void readRecordsAndNextRecordStartTime_multipleRecordTypes_throws() {
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        TEST_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                List.of(UUID.randomUUID()),
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                List.of(UUID.randomUUID())),
                        /* startDateAccess= */ 0,
                        NO_EXTRA_PERMS);

        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mTransactionManager.readRecordsAndNextRecordStartTime(request));
        assertThat(thrown.getMessage()).contains("expected one element");
    }
}
