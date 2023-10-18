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

import static android.health.connect.HealthPermissions.WRITE_EXERCISE_ROUTE;
import static android.healthconnect.cts.device.HealthConnectDeviceTest.APP_A_WITH_READ_WRITE_PERMS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.READ_RECORDS_SIZE;
import static android.healthconnect.cts.lib.MultiAppTestUtils.RECORD_IDS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.SUCCESS;
import static android.healthconnect.cts.lib.MultiAppTestUtils.insertRecordAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.insertSessionNoRouteAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.readRecordsAs;
import static android.healthconnect.cts.lib.MultiAppTestUtils.updateRouteAs;
import static android.healthconnect.cts.utils.TestUtils.READ_EXERCISE_ROUTE_PERMISSION;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.deleteTestData;
import static android.healthconnect.cts.utils.TestUtils.getChangeLogToken;
import static android.healthconnect.cts.utils.TestUtils.getChangeLogs;
import static android.healthconnect.cts.utils.TestUtils.getExerciseSessionRecord;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readRecords;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static com.android.compatibility.common.util.FeatureUtil.AUTOMOTIVE_FEATURE;
import static com.android.compatibility.common.util.FeatureUtil.hasSystemFeature;

import static com.google.common.truth.Truth.assertThat;

import android.app.UiAutomation;
import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.healthconnect.cts.utils.TestUtils.RecordTypeAndRecordIds;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExerciseRouteAccessTest {

    private UiAutomation mAutomation;

    @Before
    public void setUp() {
        mAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        Assume.assumeFalse(hasSystemFeature(AUTOMOTIVE_FEATURE));

        mAutomation.grantRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);
    }

    @After
    public void tearDown() throws InterruptedException {
        deleteTestData();
        deleteAllStagedRemoteData();

        mAutomation.grantRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);
    }

    @Test
    public void readRecords_usingFilters_cannotAccessOtherAppRoute() throws Exception {
        assertThat(insertRecordAs(APP_A_WITH_READ_WRITE_PERMS).getBoolean(SUCCESS)).isTrue();

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void readRecords_usingFilters_withReadExerciseRoutePermission_canAccessOtherAppRoute()
            throws Exception {
        assertThat(insertRecordAs(APP_A_WITH_READ_WRITE_PERMS).getBoolean(SUCCESS)).isTrue();
        mAutomation.adoptShellPermissionIdentity(READ_EXERCISE_ROUTE_PERMISSION);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNotNull();
    }

    @Test
    public void readRecords_usingFilters_canAccessOwnRoute() throws Exception {
        ExerciseSessionRecord record =
                getExerciseSessionRecord(
                        getApplicationContext().getPackageName(), 0.0, /* withRoute= */ true);
        insertRecords(List.of(record), getApplicationContext());

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(record.getRoute());
    }

    @Test
    public void readRecords_usingFilters_mixedOwnAndOtherAppSession() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
        String otherAppSessionId = getInsertedSessionId(bundle);
        ExerciseSessionRecord ownSession =
                getExerciseSessionRecord(
                        getApplicationContext().getPackageName(), 0.0, /* withRoute= */ true);
        String ownSessionId =
                insertRecords(List.of(ownSession), getApplicationContext())
                        .get(0)
                        .getMetadata()
                        .getId();

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());

        Map<String, ExerciseSessionRecord> idToRecordMap =
                records.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> record.getMetadata().getId(),
                                        Function.identity()));
        assertThat(records).isNotNull();
        assertThat(records).hasSize(2);
        assertThat(idToRecordMap.get(otherAppSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(otherAppSessionId).getRoute()).isNull();
        assertThat(idToRecordMap.get(ownSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(ownSessionId).getRoute()).isEqualTo(ownSession.getRoute());
    }

    @Test
    public void readRecords_usingIds_cannotAccessOtherAppRoute() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
        String sessionId = getInsertedSessionId(bundle);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                                .addId(sessionId)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void readRecords_usingIds_withReadExerciseRoutePermission_canAccessOtherAppRoute()
            throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
        String sessionId = getInsertedSessionId(bundle);
        mAutomation.adoptShellPermissionIdentity(READ_EXERCISE_ROUTE_PERMISSION);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                                .addId(sessionId)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNotNull();
    }

    @Test
    public void readRecords_usingIds_canAccessOwnRoute() throws Exception {
        ExerciseSessionRecord record =
                getExerciseSessionRecord(
                        getApplicationContext().getPackageName(), 0.0, /* withRoute= */ true);
        String sessionId =
                insertRecords(List.of(record), getApplicationContext())
                        .get(0)
                        .getMetadata()
                        .getId();

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                                .addId(sessionId)
                                .build());

        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(record.getRoute());
    }

    @Test
    public void readRecords_usingIds__mixedOwnAndOtherAppSession() throws Exception {
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
        String otherAppSessionId = getInsertedSessionId(bundle);
        ExerciseSessionRecord ownSession =
                getExerciseSessionRecord(
                        getApplicationContext().getPackageName(), 0.0, /* withRoute= */ true);
        String ownSessionId =
                insertRecords(List.of(ownSession), getApplicationContext())
                        .get(0)
                        .getMetadata()
                        .getId();

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                                .addId(otherAppSessionId)
                                .addId(ownSessionId)
                                .build());

        Map<String, ExerciseSessionRecord> idToRecordMap =
                records.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> record.getMetadata().getId(),
                                        Function.identity()));
        assertThat(records).isNotNull();
        assertThat(records).hasSize(2);
        assertThat(idToRecordMap.get(otherAppSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(otherAppSessionId).getRoute()).isNull();
        assertThat(idToRecordMap.get(ownSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(ownSessionId).getRoute()).isEqualTo(ownSession.getRoute());
    }

    @Test
    public void getChangelogs_cannotAccessOtherAppRoute() throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(ExerciseSessionRecord.class)
                                        .build(),
                                getApplicationContext())
                        .getToken();
        assertThat(insertRecordAs(APP_A_WITH_READ_WRITE_PERMS).getBoolean(SUCCESS)).isTrue();

        ChangeLogsResponse response =
                getChangeLogs(
                        new ChangeLogsRequest.Builder(token).build(), getApplicationContext());

        List<ExerciseSessionRecord> records =
                response.getUpsertedRecords().stream()
                        .map(ExerciseSessionRecord.class::cast)
                        .toList();
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void getChangelogs_withReadExerciseRoutePermission_canAccessOtherAppRoute()
            throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(ExerciseSessionRecord.class)
                                        .build(),
                                getApplicationContext())
                        .getToken();
        assertThat(insertRecordAs(APP_A_WITH_READ_WRITE_PERMS).getBoolean(SUCCESS)).isTrue();
        mAutomation.adoptShellPermissionIdentity(READ_EXERCISE_ROUTE_PERMISSION);

        ChangeLogsResponse response =
                getChangeLogs(
                        new ChangeLogsRequest.Builder(token).build(), getApplicationContext());

        List<ExerciseSessionRecord> records =
                response.getUpsertedRecords().stream()
                        .map(ExerciseSessionRecord.class::cast)
                        .toList();
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNotNull();
    }

    @Test
    public void getChangelogs_canAccessOwnRoute() throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(ExerciseSessionRecord.class)
                                        .build(),
                                getApplicationContext())
                        .getToken();
        ExerciseSessionRecord record =
                getExerciseSessionRecord(
                        getApplicationContext().getPackageName(), 0.0, /* withRoute= */ true);
        insertRecords(List.of(record), getApplicationContext());

        ChangeLogsResponse response =
                getChangeLogs(
                        new ChangeLogsRequest.Builder(token).build(), getApplicationContext());

        List<ExerciseSessionRecord> records =
                response.getUpsertedRecords().stream()
                        .map(ExerciseSessionRecord.class::cast)
                        .toList();
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(record.getRoute());
    }

    @Test
    public void getChangelogs_mixedOwnAndOtherAppSession() throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(ExerciseSessionRecord.class)
                                        .build(),
                                getApplicationContext())
                        .getToken();
        Bundle bundle = insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
        String otherAppSessionId = getInsertedSessionId(bundle);
        ExerciseSessionRecord ownSession =
                getExerciseSessionRecord(
                        getApplicationContext().getPackageName(), 0.0, /* withRoute= */ true);
        String ownSessionId =
                insertRecords(List.of(ownSession), getApplicationContext())
                        .get(0)
                        .getMetadata()
                        .getId();

        ChangeLogsResponse response =
                getChangeLogs(
                        new ChangeLogsRequest.Builder(token).build(), getApplicationContext());

        Map<String, ExerciseSessionRecord> idToRecordMap =
                response.getUpsertedRecords().stream()
                        .map(ExerciseSessionRecord.class::cast)
                        .collect(
                                Collectors.toMap(
                                        record -> record.getMetadata().getId(),
                                        Function.identity()));
        assertThat(response.getUpsertedRecords()).isNotNull();
        assertThat(response.getUpsertedRecords()).hasSize(2);
        assertThat(idToRecordMap.get(otherAppSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(otherAppSessionId).getRoute()).isNull();
        assertThat(idToRecordMap.get(ownSessionId).hasRoute()).isTrue();
        assertThat(idToRecordMap.get(ownSessionId).getRoute()).isEqualTo(ownSession.getRoute());
    }

    @Test
    public void testRouteInsert_cannotInsertRouteWithoutPerm() throws Exception {
        mAutomation.revokeRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);

        try {
            insertRecordAs(APP_A_WITH_READ_WRITE_PERMS);
            Assert.fail("Should have thrown an Security Exception!");
        } catch (HealthConnectException e) {
            assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        } finally {
            mAutomation.grantRuntimePermission(
                    APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);
        }
    }

    @Test
    public void testRouteUpdate_updateRouteWithPerm_noRouteAfterUpdate() throws Exception {
        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isEmpty();

        assertThat(insertRecordAs(APP_A_WITH_READ_WRITE_PERMS).getBoolean(SUCCESS)).isTrue();
        records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();

        assertThat(updateRouteAs(APP_A_WITH_READ_WRITE_PERMS).getBoolean(SUCCESS)).isTrue();

        records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isFalse();

        // Check that the route has been actually deleted, so no exceptions from incorrect record
        // state.
        Bundle bundle =
                readRecordsAs(
                        APP_A_WITH_READ_WRITE_PERMS,
                        new ArrayList<>(List.of(ExerciseSessionRecord.class.getName())));
        assertThat(bundle.getBoolean(SUCCESS)).isTrue();
        assertThat(bundle.getInt(READ_RECORDS_SIZE)).isEqualTo(1);
    }

    @Test
    public void testRouteUpdate_updateRouteWithoutPerm_hasRouteAfterUpdate() throws Exception {
        assertThat(insertRecordAs(APP_A_WITH_READ_WRITE_PERMS).getBoolean(SUCCESS)).isTrue();
        mAutomation.revokeRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);

        updateRouteAs(APP_A_WITH_READ_WRITE_PERMS);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
    }

    @Test
    public void testRouteUpsert_insertRecordNoRouteWithoutRoutePerm_hasRouteAfterInsert()
            throws Exception {
        assertThat(insertRecordAs(APP_A_WITH_READ_WRITE_PERMS).getBoolean(SUCCESS)).isTrue();
        mAutomation.revokeRuntimePermission(
                APP_A_WITH_READ_WRITE_PERMS.getPackageName(), WRITE_EXERCISE_ROUTE);

        insertSessionNoRouteAs(APP_A_WITH_READ_WRITE_PERMS);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
    }

    @Test
    public void testRouteUpsert_insertRecordNoRouteWithRoutePerm_noRouteAfterInsert()
            throws Exception {
        assertThat(insertRecordAs(APP_A_WITH_READ_WRITE_PERMS).getBoolean(SUCCESS)).isTrue();
        insertSessionNoRouteAs(APP_A_WITH_READ_WRITE_PERMS);

        List<ExerciseSessionRecord> records =
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());
        assertThat(records).isNotNull();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isFalse();
    }

    private static String getInsertedSessionId(Bundle bundle) {
        List<String> ids =
                ((List<RecordTypeAndRecordIds>) bundle.getSerializable(RECORD_IDS))
                        .stream()
                                .filter(
                                        it ->
                                                it.getRecordType()
                                                        .equals(
                                                                ExerciseSessionRecord.class
                                                                        .getName()))
                                .map(RecordTypeAndRecordIds::getRecordIds)
                                .flatMap(Collection::stream)
                                .toList();
        assertThat(ids).hasSize(1);
        return ids.get(0);
    }
}
