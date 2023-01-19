/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.healthconnect.tests.migration;

import static android.content.pm.PackageManager.GET_PERMISSIONS;
import static android.content.pm.PackageManager.PackageInfoFlags;
import static android.healthconnect.HealthPermissions.READ_HEIGHT;
import static android.healthconnect.HealthPermissions.WRITE_HEIGHT;
import static android.healthconnect.datatypes.units.Length.fromMeters;
import static android.healthconnect.datatypes.units.Power.fromWatts;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.HealthPermissions;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsResponse;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.HeightRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.PowerRecord;
import android.healthconnect.datatypes.PowerRecord.PowerRecordSample;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.migration.MigrationEntity;
import android.healthconnect.migration.MigrationException;
import android.healthconnect.migration.PermissionMigrationPayload;
import android.healthconnect.migration.RecordMigrationPayload;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.os.UserHandle;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@RunWith(AndroidJUnit4.class)
public class DataMigrationTest {

    private static final String PACKAGE_NAME = "android.healthconnect.tests.migration";
    private static final String APP_PACKAGE_NAME = "android.healthconnect.tests.migration.app";
    private final Executor mOutcomeExecutor = Executors.newSingleThreadExecutor();
    private final Instant mEndTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private final Instant mStartTime = mEndTime.minus(Duration.ofHours(1));
    private Context mTargetContext;
    private HealthConnectManager mManager;

    private static Bundle bundleOf(Consumer<Bundle> builder) {
        final Bundle bundle = new Bundle();
        builder.accept(bundle);
        return bundle;
    }

    private static <T, E extends RuntimeException> T blockingCall(
            Consumer<OutcomeReceiver<T, E>> action) throws E {
        final BlockingOutcomeReceiver<T, E> outcomeReceiver = new BlockingOutcomeReceiver<>();
        action.accept(outcomeReceiver);
        return outcomeReceiver.await();
    }

    private static Metadata getMetadata() {
        return new Metadata.Builder()
                .setDevice(new Device.Builder().setManufacturer("Device").setModel("Model").build())
                .build();
    }

    @Before
    public void setUp() {
        mTargetContext = InstrumentationRegistry.getTargetContext();
        mManager = mTargetContext.getSystemService(HealthConnectManager.class);
        deleteAllRecords();
    }

    @After
    public void tearDown() {
        deleteAllRecords();
    }

    @Test
    public void migrateHeight_heightSaved() {
        migrate(
                getRecordEntity(
                        "height1",
                        new HeightRecord.Builder(getMetadata(), mEndTime, fromMeters(183D))
                                .build()));

        final HeightRecord record = getRecords(HeightRecord.class).get(0);

        assertThat(record.getHeight().getInMeters()).isEqualTo(183D);
        assertThat(record.getTime()).isEqualTo(mEndTime);
    }

    @Test
    public void migrateSteps_stepsSaved() {
        migrate(
                getRecordEntity(
                        "steps1",
                        new StepsRecord.Builder(getMetadata(), mStartTime, mEndTime, 10).build()));

        final StepsRecord record = getRecords(StepsRecord.class).get(0);

        assertThat(record.getCount()).isEqualTo(10);
        assertThat(record.getStartTime()).isEqualTo(mStartTime);
        assertThat(record.getEndTime()).isEqualTo(mEndTime);
    }

    @Test
    public void migratePower_powerSaved() {
        migrate(
                getRecordEntity(
                        "power1",
                        new PowerRecord.Builder(
                                        getMetadata(),
                                        mStartTime,
                                        mEndTime,
                                        List.of(
                                                new PowerRecordSample(fromWatts(10D), mEndTime),
                                                new PowerRecordSample(fromWatts(20D), mEndTime),
                                                new PowerRecordSample(fromWatts(30D), mEndTime)))
                                .build()));

        final PowerRecord record = getRecords(PowerRecord.class).get(0);

        assertThat(record.getSamples())
                .containsExactly(
                        new PowerRecordSample(fromWatts(10D), mEndTime),
                        new PowerRecordSample(fromWatts(20D), mEndTime),
                        new PowerRecordSample(fromWatts(30D), mEndTime))
                .inOrder();
        assertThat(record.getStartTime()).isEqualTo(mStartTime);
        assertThat(record.getEndTime()).isEqualTo(mEndTime);
    }

    @Test
    public void migratePermissions_permissionsGranted() {
        revokeAppPermissions(READ_HEIGHT, WRITE_HEIGHT);

        migrate(
                new MigrationEntity(
                        "permissions1",
                        new PermissionMigrationPayload.Builder(APP_PACKAGE_NAME, Instant.now())
                                .addPermission(READ_HEIGHT)
                                .addPermission(WRITE_HEIGHT)
                                .build()));

        assertThat(getGrantedAppPermissions()).containsAtLeast(READ_HEIGHT, WRITE_HEIGHT);
    }

    private void migrate(MigrationEntity... entities) {
        runWithShellPermissionIdentity(
                () ->
                        DataMigrationTest.<Void, MigrationException>blockingCall(
                                outcomeReceiver ->
                                        mManager.writeMigrationData(
                                                List.of(entities), Runnable::run, outcomeReceiver)),
                Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
    }

    private MigrationEntity getRecordEntity(String entityId, Record record) {
        return new MigrationEntity(
                entityId,
                new RecordMigrationPayload.Builder(PACKAGE_NAME, "Example App", record).build());
    }

    private <T extends Record> List<T> getRecords(Class<T> clazz) {
        return runWithShellPermissionIdentity(
                () ->
                        DataMigrationTest
                                .<ReadRecordsResponse<T>, HealthConnectException>blockingCall(
                                        callback -> getRecordsAsync(clazz, callback))
                                .getRecords(),
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);
    }

    private <T extends Record> void getRecordsAsync(
            Class<T> clazz,
            OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException> callback) {
        mManager.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(clazz)
                        .setTimeRangeFilter(
                                new TimeRangeFilter.Builder(mStartTime, mEndTime).build())
                        .build(),
                Executors.newSingleThreadExecutor(),
                callback);
    }

    private void deleteAllRecords() {
        runWithShellPermissionIdentity(
                () -> blockingCall(this::deleteAllRecordsAsync),
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);
    }

    private void deleteAllRecordsAsync(OutcomeReceiver<Void, HealthConnectException> callback) {
        mManager.deleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder().setPackageName(PACKAGE_NAME).build())
                        .build(),
                mOutcomeExecutor,
                callback);
    }

    private void revokeAppPermissions(String... permissions) {
        final PackageManager pm = mTargetContext.getPackageManager();
        final UserHandle user = mTargetContext.getUser();

        runWithShellPermissionIdentity(
                () -> {
                    for (String permission : permissions) {
                        pm.revokeRuntimePermission(APP_PACKAGE_NAME, permission, user);
                    }
                });
    }

    private List<String> getGrantedAppPermissions() {
        final PackageInfo pi = getAppPackageInfo();
        final String[] requestedPermissions = pi.requestedPermissions;
        final int[] requestedPermissionsFlags = pi.requestedPermissionsFlags;

        if (requestedPermissions == null) {
            return List.of();
        }

        final List<String> permissions = new ArrayList<>();

        for (int i = 0; i < requestedPermissions.length; i++) {
            if ((requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) {
                permissions.add(requestedPermissions[i]);
            }
        }

        return permissions;
    }

    private PackageInfo getAppPackageInfo() {
        return runWithShellPermissionIdentity(
                () ->
                        mTargetContext
                                .getPackageManager()
                                .getPackageInfo(
                                        APP_PACKAGE_NAME, PackageInfoFlags.of(GET_PERMISSIONS)));
    }

    @SuppressWarnings("NewClassNamingConvention") // False positive
    private static class BlockingOutcomeReceiver<T, E extends Throwable>
            implements OutcomeReceiver<T, E> {
        private final Object[] mResult = new Object[1];
        private final Throwable[] mError = new Throwable[1];
        private final CountDownLatch mCountDownLatch = new CountDownLatch(1);

        @Override
        public void onResult(T result) {
            mResult[0] = result;
            mCountDownLatch.countDown();
        }

        @Override
        public void onError(E error) {
            mError[0] = error;
            mCountDownLatch.countDown();
        }

        public T await() throws E {
            try {
                if (!mCountDownLatch.await(10, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Timeout waiting for outcome");
                }

                if (mError[0] != null) {
                    //noinspection unchecked
                    throw (E) mError[0];
                }

                //noinspection unchecked
                return (T) mResult[0];
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
