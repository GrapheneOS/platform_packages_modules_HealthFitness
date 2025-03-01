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

import static android.health.connect.HealthPermissions.WRITE_EXERCISE_ROUTE;

import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createExerciseSessionRecordWithRoute;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.HealthPermissions;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.request.ReadTransactionRequest;
import com.android.server.healthconnect.testing.fixtures.EnvironmentFixture;
import com.android.server.healthconnect.testing.fixtures.SQLiteDatabaseFixture;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(AndroidJUnit4.class)
public class TransactionManagerExerciseRoutesTest {
    private static final String TEST_PACKAGE_NAME = "package.name";
    private static final String FOO_PACKAGE_NAME = "package.foo";
    private static final String BAR_PACKAGE_NAME = "package.bar";
    private static final String UNKNOWN_PACKAGE_NAME = "package.unknown";
    private static final Set<String> WRITE_EXERCISE_ROUTE_EXTRA_PERM = Set.of(WRITE_EXERCISE_ROUTE);

    @Rule(order = 1)
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .addStaticMockFixtures(EnvironmentFixture::new, SQLiteDatabaseFixture::new)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    private TransactionTestUtils mTransactionTestUtils;
    private TransactionManager mTransactionManager;
    private AppInfoHelper mAppInfoHelper;
    private AccessLogsHelper mAccessLogsHelper;
    private DeviceInfoHelper mDeviceInfoHelper;
    private ReadAccessLogsHelper mReadAccessLogsHelper;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .build();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();
        mDeviceInfoHelper = healthConnectInjector.getDeviceInfoHelper();
        mReadAccessLogsHelper = healthConnectInjector.getReadAccessLogsHelper();

        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(FOO_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(BAR_PACKAGE_NAME);
    }

    @Test
    public void readRecordsByIds_doesNotReturnRoutesOfOtherApps() {
        ExerciseSessionRecordInternal fooSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(10000));
        ExerciseSessionRecordInternal barSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(11000));
        ExerciseSessionRecordInternal ownSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        String fooUuid = mTransactionTestUtils.insertRecords(FOO_PACKAGE_NAME, fooSession).get(0);
        String barUuid = mTransactionTestUtils.insertRecords(BAR_PACKAGE_NAME, barSession).get(0);
        String ownUuid = mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, ownSession).get(0);
        List<UUID> allUuids = Stream.of(fooUuid, barUuid, ownUuid).map(UUID::fromString).toList();
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        mAppInfoHelper,
                        TEST_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, allUuids),
                        /* startDateAccessMillis= */ 0,
                        WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        /* isInForeground= */ true,
                        /* isReadingSelfData= */ false);

        List<RecordInternal<?>> returnedRecords =
                mTransactionManager.readRecordsByIds(
                        request,
                        mAppInfoHelper,
                        mDeviceInfoHelper,
                        mAccessLogsHelper,
                        mReadAccessLogsHelper,
                        /* shouldRecordAccessLog= */ false);

        Map<String, ExerciseSessionRecordInternal> idToSessionMap =
                returnedRecords.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> record.getUuid().toString(),
                                        ExerciseSessionRecordInternal.class::cast));
        assertThat(idToSessionMap.get(fooUuid).getRoute()).isNull();
        assertThat(idToSessionMap.get(barUuid).getRoute()).isNull();
        assertThat(idToSessionMap.get(ownUuid).getRoute()).isEqualTo(ownSession.getRoute());
        assertThat(idToSessionMap.get(fooUuid).hasRoute()).isTrue();
        assertThat(idToSessionMap.get(barUuid).hasRoute()).isTrue();
        assertThat(idToSessionMap.get(ownUuid).hasRoute()).isTrue();
    }

    @Test
    public void readRecordsByIds_unknownApp_doesNotReturnRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        UUID uuid =
                UUID.fromString(
                        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session).get(0));
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        mAppInfoHelper,
                        UNKNOWN_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, List.of(uuid)),
                        /* startDateAccessMillis= */ 0,
                        WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        /* isInForeground= */ true,
                        /* isReadingSelfData= */ false);

        List<RecordInternal<?>> returnedRecords =
                mTransactionManager.readRecordsByIds(
                        request,
                        mAppInfoHelper,
                        mDeviceInfoHelper,
                        mAccessLogsHelper,
                        mReadAccessLogsHelper,
                        /* shouldRecordAccessLog= */ false);

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isNull();
    }

    @Test
    public void readRecordsByIds_nullPackageName_doesNotReturnRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        UUID uuid =
                UUID.fromString(
                        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session).get(0));
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        mAppInfoHelper,
                        null,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, List.of(uuid)),
                        /* startDateAccessMillis= */ 0,
                        WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        /* isInForeground= */ true,
                        /* isReadingSelfData= */ false);

        List<RecordInternal<?>> returnedRecords =
                mTransactionManager.readRecordsByIds(
                        request,
                        mAppInfoHelper,
                        mDeviceInfoHelper,
                        mAccessLogsHelper,
                        mReadAccessLogsHelper,
                        /* shouldRecordAccessLog= */ false);

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isNull();
    }

    @Test
    public void readRecordsByIds_unknownApp_withReadRoutePermission_returnsRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        UUID uuid =
                UUID.fromString(
                        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session).get(0));
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        mAppInfoHelper,
                        UNKNOWN_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, List.of(uuid)),
                        /* startDateAccessMillis= */ 0,
                        Set.of(HealthPermissions.READ_EXERCISE_ROUTE),
                        /* isInForeground= */ true,
                        /* isReadingSelfData= */ false);

        List<RecordInternal<?>> returnedRecords =
                mTransactionManager.readRecordsByIds(
                        request,
                        mAppInfoHelper,
                        mDeviceInfoHelper,
                        mAccessLogsHelper,
                        mReadAccessLogsHelper,
                        /* shouldRecordAccessLog= */ false);

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isEqualTo(session.getRoute());
    }

    @Test
    public void readRecordsAndPageToken_byFilters_doesNotReturnRoutesOfOtherApps() {
        ExerciseSessionRecordInternal fooSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(10000));
        ExerciseSessionRecordInternal barSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(11000));
        ExerciseSessionRecordInternal ownSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        String fooUuid = mTransactionTestUtils.insertRecords(FOO_PACKAGE_NAME, fooSession).get(0);
        String barUuid = mTransactionTestUtils.insertRecords(BAR_PACKAGE_NAME, barSession).get(0);
        String ownUuid = mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, ownSession).get(0);
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        mAppInfoHelper,
                        TEST_PACKAGE_NAME,
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .setTimeRangeFilter(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.ofEpochSecond(100000))
                                                .build())
                                .build()
                                .toReadRecordsRequestParcel(),
                        /* startDateAccessMillis= */ 0,
                        /* enforceSelfRead= */ false,
                        WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        /* isInForeground= */ true);

        List<RecordInternal<?>> returnedRecords =
                mTransactionManager.readRecordsAndPageTokenWithoutAccessLogs(
                                request,
                                mAppInfoHelper,
                                mDeviceInfoHelper,
                                /* packageNamesByAppIds= */ null)
                        .first;

        Map<String, ExerciseSessionRecordInternal> idToSessionMap =
                returnedRecords.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> record.getUuid().toString(),
                                        ExerciseSessionRecordInternal.class::cast));

        assertThat(idToSessionMap.get(fooUuid).getRoute()).isNull();
        assertThat(idToSessionMap.get(barUuid).getRoute()).isNull();
        assertThat(idToSessionMap.get(ownUuid).getRoute()).isEqualTo(ownSession.getRoute());
        assertThat(idToSessionMap.get(fooUuid).hasRoute()).isTrue();
        assertThat(idToSessionMap.get(barUuid).hasRoute()).isTrue();
        assertThat(idToSessionMap.get(ownUuid).hasRoute()).isTrue();
    }

    @Test
    public void readRecordsAndPageToken_byFilters_unknownApp_doesNotReturnRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session);
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        mAppInfoHelper,
                        UNKNOWN_PACKAGE_NAME,
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .setTimeRangeFilter(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.ofEpochSecond(100000))
                                                .build())
                                .build()
                                .toReadRecordsRequestParcel(),
                        /* startDateAccessMillis= */ 0,
                        /* enforceSelfRead= */ false,
                        WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        /* isInForeground= */ true);

        List<RecordInternal<?>> returnedRecords =
                mTransactionManager.readRecordsAndPageTokenWithoutAccessLogs(
                                request,
                                mAppInfoHelper,
                                mDeviceInfoHelper,
                                /* packageNamesByAppIds= */ null)
                        .first;

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isNull();
    }

    @Test
    public void readRecordsAndPageToken_byFilters_nullPackageName_doesNotReturnRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session);
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        mAppInfoHelper,
                        null,
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .setTimeRangeFilter(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.ofEpochSecond(100000))
                                                .build())
                                .build()
                                .toReadRecordsRequestParcel(),
                        /* startDateAccessMillis= */ 0,
                        /* enforceSelfRead= */ false,
                        WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        /* isInForeground= */ true);

        List<RecordInternal<?>> returnedRecords =
                mTransactionManager.readRecordsAndPageTokenWithoutAccessLogs(
                                request,
                                mAppInfoHelper,
                                mDeviceInfoHelper,
                                /* packageNamesByAppIds= */ null)
                        .first;

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isNull();
    }

    @Test
    public void readRecordsAndPageToken_byFilters_withReadRoutePermission_returnsRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session);
        ReadTransactionRequest request =
                new ReadTransactionRequest(
                        mAppInfoHelper,
                        UNKNOWN_PACKAGE_NAME,
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .setTimeRangeFilter(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.ofEpochSecond(100000))
                                                .build())
                                .build()
                                .toReadRecordsRequestParcel(),
                        /* startDateAccessMillis= */ 0,
                        /* enforceSelfRead= */ false,
                        Set.of(HealthPermissions.READ_EXERCISE_ROUTE),
                        /* isInForeground= */ true);

        List<RecordInternal<?>> returnedRecords =
                mTransactionManager.readRecordsAndPageTokenWithoutAccessLogs(
                                request,
                                mAppInfoHelper,
                                mDeviceInfoHelper,
                                /* packageNamesByAppIds= */ null)
                        .first;

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isEqualTo(session.getRoute());
    }
}
