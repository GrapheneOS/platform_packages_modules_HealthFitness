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
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEIGHT;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_POWER;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.migration.DataMigrationFields.DM_APP_INFO_APP_NAME;
import static android.healthconnect.migration.DataMigrationFields.DM_APP_INFO_PACKAGE_NAME;
import static android.healthconnect.migration.DataMigrationFields.DM_DEVICE_INFO_MANUFACTURER;
import static android.healthconnect.migration.DataMigrationFields.DM_DEVICE_INFO_MODEL;
import static android.healthconnect.migration.DataMigrationFields.DM_DEVICE_INFO_TYPE;
import static android.healthconnect.migration.DataMigrationFields.DM_PERMISSION_FIRST_GRANT_TIME;
import static android.healthconnect.migration.DataMigrationFields.DM_PERMISSION_PACKAGE_NAME;
import static android.healthconnect.migration.DataMigrationFields.DM_PERMISSION_PERMISSION_NAMES;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_APP_INFO;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_DEVICE_INFO;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_END_TIME;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_HEIGHT_HEIGHT;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_LAST_MODIFIED_TIME;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_POWER_WATTS;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_SAMPLES;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_SAMPLE_TIME;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_START_TIME;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_STEPS_COUNT;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_TIME;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_TYPE;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.truth.Truth.assertThat;

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
import android.healthconnect.datatypes.PowerRecord;
import android.healthconnect.datatypes.PowerRecord.PowerRecordSample;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.units.Power;
import android.healthconnect.migration.MigrationDataEntity;
import android.healthconnect.migration.MigrationException;
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

    private static void fillRecordBundle(Bundle bundle, @RecordTypeIdentifier.RecordType int type) {
        bundle.putInt(DM_RECORD_TYPE, type);
        bundle.putBundle(DM_RECORD_APP_INFO, getAppInfoBundle());
        bundle.putBundle(DM_RECORD_DEVICE_INFO, getDeviceInfoBundle());
        bundle.putLong(DM_RECORD_LAST_MODIFIED_TIME, 0L);
    }

    private static Bundle getAppInfoBundle() {
        return bundleOf(
                appInfo -> {
                    appInfo.putString(DM_APP_INFO_PACKAGE_NAME, PACKAGE_NAME);
                    appInfo.putString(DM_APP_INFO_APP_NAME, "Example App");
                });
    }

    private static Bundle getDeviceInfoBundle() {
        return bundleOf(
                deviceInfo -> {
                    deviceInfo.putString(DM_DEVICE_INFO_MANUFACTURER, "Test Manufacturer");
                    deviceInfo.putString(DM_DEVICE_INFO_MODEL, "Test Model");
                    deviceInfo.putInt(DM_DEVICE_INFO_TYPE, Device.DEVICE_TYPE_PHONE);
                });
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
                getInstantRecordEntity(
                        "height1",
                        RECORD_TYPE_HEIGHT,
                        mEndTime,
                        bundle -> bundle.putDouble(DM_RECORD_HEIGHT_HEIGHT, 183D)));

        final HeightRecord record = getRecords(HeightRecord.class).get(0);

        assertThat(record.getHeight().getInMeters()).isEqualTo(183D);
        assertThat(record.getTime()).isEqualTo(mEndTime);
    }

    @Test
    public void migrateSteps_stepsSaved() {
        migrate(
                getIntervalRecordEntity(
                        "steps1",
                        RECORD_TYPE_STEPS,
                        mStartTime,
                        mEndTime,
                        bundle -> bundle.putLong(DM_RECORD_STEPS_COUNT, 10)));

        final StepsRecord record = getRecords(StepsRecord.class).get(0);

        assertThat(record.getCount()).isEqualTo(10);
        assertThat(record.getStartTime()).isEqualTo(mStartTime);
        assertThat(record.getEndTime()).isEqualTo(mEndTime);
    }

    @Test
    public void migratePower_powerSaved() {
        migrate(
                getSeriesRecordEntity(
                        "power1",
                        RECORD_TYPE_POWER,
                        mStartTime,
                        mEndTime,
                        newArrayList(
                                getSampleBundle(b -> b.putDouble(DM_RECORD_POWER_WATTS, 10D)),
                                getSampleBundle(b -> b.putDouble(DM_RECORD_POWER_WATTS, 20D)),
                                getSampleBundle(b -> b.putDouble(DM_RECORD_POWER_WATTS, 30D)))));

        final PowerRecord record = getRecords(PowerRecord.class).get(0);

        assertThat(record.getSamples())
                .containsExactly(
                        new PowerRecordSample(Power.fromWatts(10D), mEndTime),
                        new PowerRecordSample(Power.fromWatts(20D), mEndTime),
                        new PowerRecordSample(Power.fromWatts(30D), mEndTime))
                .inOrder();
        assertThat(record.getStartTime()).isEqualTo(mStartTime);
        assertThat(record.getEndTime()).isEqualTo(mEndTime);
    }

    @Test
    public void migratePermissions_permissionsGranted() {
        revokeAppPermissions(READ_HEIGHT, WRITE_HEIGHT);

        migrate(
                new MigrationDataEntity(
                        MigrationDataEntity.TYPE_PACKAGE_PERMISSIONS,
                        "permissions1",
                        bundleOf(
                                bundle -> {
                                    bundle.putString(DM_PERMISSION_PACKAGE_NAME, APP_PACKAGE_NAME);
                                    bundle.putLong(
                                            DM_PERMISSION_FIRST_GRANT_TIME,
                                            Instant.now().toEpochMilli());
                                    bundle.putStringArrayList(
                                            DM_PERMISSION_PERMISSION_NAMES,
                                            newArrayList(WRITE_HEIGHT, READ_HEIGHT));
                                })));

        assertThat(getGrantedAppPermissions()).containsAtLeast(READ_HEIGHT, WRITE_HEIGHT);
    }

    private void migrate(MigrationDataEntity... entities) {
        DataMigrationTest.<Void, MigrationException>blockingCall(
                outcomeReceiver ->
                        mManager.writeMigrationData(
                                List.of(entities), Runnable::run, outcomeReceiver));
    }

    private MigrationDataEntity getInstantRecordEntity(
            String uniqueId,
            @RecordTypeIdentifier.RecordType int type,
            Instant time,
            Consumer<Bundle> builder) {
        return new MigrationDataEntity(
                MigrationDataEntity.TYPE_RECORD,
                uniqueId,
                bundleOf(
                        bundle -> {
                            fillRecordBundle(bundle, type);
                            bundle.putLong(DM_RECORD_TIME, time.toEpochMilli());
                            builder.accept(bundle);
                        }));
    }

    private MigrationDataEntity getIntervalRecordEntity(
            String uniqueId,
            @RecordTypeIdentifier.RecordType int type,
            Instant startTime,
            Instant endTime,
            Consumer<Bundle> builder) {
        return new MigrationDataEntity(
                MigrationDataEntity.TYPE_RECORD,
                uniqueId,
                bundleOf(
                        bundle -> {
                            fillRecordBundle(bundle, type);
                            bundle.putLong(DM_RECORD_START_TIME, startTime.toEpochMilli());
                            bundle.putLong(DM_RECORD_END_TIME, endTime.toEpochMilli());
                            builder.accept(bundle);
                        }));
    }

    private MigrationDataEntity getSeriesRecordEntity(
            String uniqueId,
            @RecordTypeIdentifier.RecordType int type,
            Instant startTime,
            Instant endTime,
            ArrayList<Bundle> samples) {
        return getIntervalRecordEntity(
                uniqueId,
                type,
                startTime,
                endTime,
                bundle -> bundle.putParcelableArrayList(DM_RECORD_SAMPLES, samples));
    }

    private Bundle getSampleBundle(Consumer<Bundle> builder) {
        return bundleOf(
                bundle -> {
                    bundle.putLong(DM_RECORD_SAMPLE_TIME, mEndTime.toEpochMilli());
                    builder.accept(bundle);
                });
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
