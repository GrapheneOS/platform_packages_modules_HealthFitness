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

package android.healthconnect.cts.route;

import static android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES;
import static android.health.connect.HealthPermissions.WRITE_EXERCISE_ROUTE;
import static android.healthconnect.cts.route.ExerciseRouteTestHelper.ROUTES_READER_WRITER_APP_PACKAGE_NAME;
import static android.healthconnect.cts.route.ExerciseRouteTestHelper.ROUTE_WRITER_APP_PACKAGE_NAME;
import static android.healthconnect.cts.route.ExerciseRouteTestHelper.assertCorrectHealthPermissions;
import static android.healthconnect.cts.route.ExerciseRouteTestHelper.getExerciseSessionWithAnotherRoute;
import static android.healthconnect.cts.route.ExerciseRouteTestHelper.getExerciseSessionWithRoute;
import static android.healthconnect.cts.route.ExerciseRouteTestHelper.getExerciseSessionWithoutRoute;
import static android.healthconnect.cts.route.ExerciseRouteTestHelper.readAllExerciseSessionRecordsPrivileged;
import static android.healthconnect.testing.cts.TestUtils.connectAppsWithGrantedPermissions;
import static android.healthconnect.testing.cts.TestUtils.deleteAllDataFromHealthConnect;
import static android.healthconnect.testing.shared.DataFactory.getEmptyMetadata;
import static android.healthconnect.testing.shared.DataFactory.getMetadataForClientId;
import static android.healthconnect.testing.shared.DataFactory.getMetadataForId;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy;
import android.healthconnect.testing.cts.testapphelpers.TestAppRule;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class ExerciseRouteNoReadWritePermissionTest {

    @Rule(order = 0)
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Rule(order = 1)
    public final TestAppRule mRouteWriterAppRule =
            new TestAppRule.Builder(ROUTE_WRITER_APP_PACKAGE_NAME).build();

    @Rule(order = 2)
    public final TestAppRule mRoutesReaderWriterAppRule =
            new TestAppRule.Builder(ROUTES_READER_WRITER_APP_PACKAGE_NAME).build();

    private final TestAppProxy mRouteWriterApp = mRouteWriterAppRule.getProxy();
    private final TestAppProxy mRoutesReaderWriteApp = mRoutesReaderWriterAppRule.getProxy();

    @Before
    public void setUp() throws Exception {
        assertCorrectHealthPermissions();
        connectAppsWithGrantedPermissions();
    }

    @After
    public void tearDown() throws InterruptedException {
        deleteAllDataFromHealthConnect();
    }

    @Test
    public void insertRecords_canNotInsertRoute() {
        ExerciseSessionRecord otherAppSession = getExerciseSessionWithRoute(getEmptyMetadata());

        mRouteWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> mRouteWriterApp.insertRecords(otherAppSession));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void insertRecords_withSameClientId_withoutRoute_routeDoesNotGetDeleted()
            throws Exception {
        ExerciseSessionRecord sessionWithRoute =
                getExerciseSessionWithRoute(getMetadataForClientId("client id"));
        mRouteWriterApp.insertRecords(sessionWithRoute);
        ExerciseSessionRecord updatedSessionWithoutRoute =
                getExerciseSessionWithoutRoute(getMetadataForClientId("client id"));

        mRouteWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        mRouteWriterApp.insertRecords(updatedSessionWithoutRoute);

        List<ExerciseSessionRecord> records = readAllExerciseSessionRecordsPrivileged();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(sessionWithRoute.getRoute());
    }

    @Test
    public void insertRecords_withSameClientId_updatedRoute_throws_routeDoesNotGetUpdated()
            throws Exception {
        ExerciseSessionRecord sessionWithRoute =
                getExerciseSessionWithRoute(getMetadataForClientId("client id"));
        mRouteWriterApp.insertRecords(sessionWithRoute);
        ExerciseSessionRecord sessionWithUpdatedRoute =
                getExerciseSessionWithAnotherRoute(getMetadataForClientId("client id"));

        mRouteWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> mRouteWriterApp.insertRecords(sessionWithUpdatedRoute));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        List<ExerciseSessionRecord> records = readAllExerciseSessionRecordsPrivileged();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(sessionWithRoute.getRoute());
    }

    @Test
    public void insertRecords_withSameClientId_addedRoute_throws_routeDoesNotGetAdded()
            throws Exception {
        ExerciseSessionRecord sessionWithoutRoute =
                getExerciseSessionWithoutRoute(getMetadataForClientId("client id"));
        mRouteWriterApp.insertRecords(sessionWithoutRoute);
        ExerciseSessionRecord sessionWithAddedRoute =
                getExerciseSessionWithRoute(getMetadataForClientId("client id"));

        mRouteWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> mRouteWriterApp.insertRecords(sessionWithAddedRoute));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        List<ExerciseSessionRecord> records = readAllExerciseSessionRecordsPrivileged();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isFalse();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void updateRecords_withoutRoute_routeDoesNotGetDeleted() throws Exception {
        ExerciseSessionRecord sessionWithRoute = getExerciseSessionWithRoute(getEmptyMetadata());
        String sessionId = mRouteWriterApp.insertRecords(sessionWithRoute).get(0);
        ExerciseSessionRecord updatedSessionWithoutRoute =
                getExerciseSessionWithoutRoute(getMetadataForId(sessionId));

        mRouteWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        mRouteWriterApp.updateRecords(updatedSessionWithoutRoute);

        List<ExerciseSessionRecord> records = readAllExerciseSessionRecordsPrivileged();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(sessionWithRoute.getRoute());
    }

    @Test
    public void updateRecords_withUpdatedRoute_throws_routeDoesNotGetUpdated() throws Exception {
        ExerciseSessionRecord sessionWithRoute = getExerciseSessionWithRoute(getEmptyMetadata());
        String sessionId = mRouteWriterApp.insertRecords(sessionWithRoute).get(0);
        ExerciseSessionRecord sessionWithUpdatedRoute =
                getExerciseSessionWithAnotherRoute(getMetadataForId(sessionId));

        mRouteWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> mRouteWriterApp.updateRecords(sessionWithUpdatedRoute));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        List<ExerciseSessionRecord> records = readAllExerciseSessionRecordsPrivileged();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isEqualTo(sessionWithRoute.getRoute());
    }

    @Test
    public void updateRecords_withAddedRoute_throws_routeDoesNotGetAdded() throws Exception {
        ExerciseSessionRecord sessionWithoutRoute =
                getExerciseSessionWithoutRoute(getEmptyMetadata());
        String sessionId = mRouteWriterApp.insertRecords(sessionWithoutRoute).get(0);
        ExerciseSessionRecord sessionWithAddedRoute =
                getExerciseSessionWithRoute(getMetadataForId(sessionId));

        mRouteWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        HealthConnectException e =
                assertThrows(
                        HealthConnectException.class,
                        () -> mRouteWriterApp.updateRecords(sessionWithAddedRoute));

        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
        List<ExerciseSessionRecord> records = readAllExerciseSessionRecordsPrivileged();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isFalse();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void readRecords_usingFilters_canAccessOwnRoute() throws Exception {
        ExerciseSessionRecord sessionWithRoute = getExerciseSessionWithRoute(getEmptyMetadata());
        mRoutesReaderWriteApp.insertRecords(sessionWithRoute);

        mRoutesReaderWriterAppRule.revokeHealthPermission(READ_EXERCISE_ROUTES);
        mRoutesReaderWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        List<ExerciseSessionRecord> records =
                mRoutesReaderWriteApp.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                                .build());

        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void readRecords_usingIds_canAccessOwnRoute() throws Exception {
        ExerciseSessionRecord sessionWithRoute = getExerciseSessionWithRoute(getEmptyMetadata());
        String sessionId = mRoutesReaderWriteApp.insertRecords(sessionWithRoute).get(0);

        mRoutesReaderWriterAppRule.revokeHealthPermission(READ_EXERCISE_ROUTES);
        mRoutesReaderWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        List<ExerciseSessionRecord> records =
                mRoutesReaderWriteApp.readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                                .addId(sessionId)
                                .build());

        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNull();
    }

    @Test
    public void getChangelogs_canAccessOwnRoute() throws Exception {
        String token =
                mRoutesReaderWriteApp.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(ExerciseSessionRecord.class)
                                .build());
        ExerciseSessionRecord sessionWithRoute = getExerciseSessionWithRoute(getEmptyMetadata());
        mRoutesReaderWriteApp.insertRecords(sessionWithRoute);

        mRoutesReaderWriterAppRule.revokeHealthPermission(READ_EXERCISE_ROUTES);
        mRoutesReaderWriterAppRule.revokeHealthPermission(WRITE_EXERCISE_ROUTE);
        ChangeLogsResponse response =
                mRoutesReaderWriteApp.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        List<ExerciseSessionRecord> records =
                response.getUpsertedRecords().stream()
                        .map(ExerciseSessionRecord.class::cast)
                        .toList();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).hasRoute()).isTrue();
        assertThat(records.get(0).getRoute()).isNull();
    }
}
