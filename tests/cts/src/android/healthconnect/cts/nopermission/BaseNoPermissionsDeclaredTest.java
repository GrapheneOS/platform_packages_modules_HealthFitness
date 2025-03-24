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

package android.healthconnect.cts.nopermission;

import static android.healthconnect.cts.utils.PermissionHelper.getDeclaredHealthPermissions;
import static android.healthconnect.cts.utils.TestUtils.getChangeLogToken;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readRecords;
import static android.healthconnect.cts.utils.TestUtils.updateRecords;
import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
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

abstract class BaseNoPermissionsDeclaredTest<T extends Record> {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private static final ZonedDateTime YESTERDAY_11AM =
            LocalDate.now(ZoneId.systemDefault())
                    .minusDays(1)
                    .atTime(11, 0)
                    .atZone(ZoneId.systemDefault());

    private final Class<T> mRecordClass;
    private final RecordFactory<T> mRecordFactory;

    BaseNoPermissionsDeclaredTest(Class<T> recordClass, RecordFactory<T> recordFactory) {
        mRecordClass = recordClass;
        mRecordFactory = recordFactory;
    }

    @Before
    public void setUp() throws InterruptedException {
        assertThat(getDeclaredHealthPermissions(getTestPackageName())).isEmpty();
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void insertRecords_returnsSecurityError() {
        Record record =
                mRecordFactory.newEmptyRecord(
                        RecordFactory.newEmptyMetadata(),
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusMinutes(30).toInstant());

        HealthConnectException e =
                assertThrows(HealthConnectException.class, () -> insertRecords(record));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void updateRecords_returnsSecurityError() {
        Record record =
                mRecordFactory.newEmptyRecord(
                        RecordFactory.newEmptyMetadataWithId(UUID.randomUUID().toString()),
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusMinutes(30).toInstant());

        HealthConnectException e =
                assertThrows(HealthConnectException.class, () -> updateRecords(List.of(record)));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void deleteRecordsById_returnsSecurityError() {
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                verifyDeleteRecords(
                                        List.of(RecordIdFilter.fromId(mRecordClass, "record-id"))));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void deleteRecordsUsingFilter_returnsSecurityError() {
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                verifyDeleteRecords(
                                        mRecordClass,
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(YESTERDAY_11AM.toInstant())
                                                .setEndTime(YESTERDAY_11AM.plusHours(2).toInstant())
                                                .build()));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void getChangeLogToken_returnsSecurityError() {
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                getChangeLogToken(
                                        new ChangeLogTokenRequest.Builder()
                                                .addRecordType(mRecordClass)
                                                .build()));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void readRecordsUsingFilters_returnsSecurityError() {
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                readRecords(
                                        new ReadRecordsRequestUsingFilters.Builder<>(mRecordClass)
                                                .build()));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void readRecordsById_returnsSecurityError() {
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                readRecords(
                                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                                .addId("record-id")
                                                .build()));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void readRecordsByClientId_returnsSecurityError() {
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                readRecords(
                                        new ReadRecordsRequestUsingIds.Builder<>(mRecordClass)
                                                .addClientRecordId("client-record-id")
                                                .build()));
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    private static String getTestPackageName() {
        return ApplicationProvider.getApplicationContext().getPackageName();
    }
}
