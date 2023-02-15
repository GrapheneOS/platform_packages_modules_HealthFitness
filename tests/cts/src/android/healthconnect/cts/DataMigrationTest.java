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

package android.healthconnect.cts;

import static android.content.pm.PackageManager.GET_PERMISSIONS;
import static android.content.pm.PackageManager.PackageInfoFlags;
import static android.health.connect.HealthPermissions.READ_HEIGHT;
import static android.health.connect.HealthPermissions.WRITE_HEIGHT;
import static android.health.connect.datatypes.units.Length.fromMeters;
import static android.health.connect.datatypes.units.Power.fromWatts;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.health.connect.ApplicationInfoResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.AppInfo;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.PowerRecord.PowerRecordSample;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.migration.AppInfoMigrationPayload;
import android.health.connect.migration.MigrationEntity;
import android.health.connect.migration.MigrationException;
import android.health.connect.migration.PermissionMigrationPayload;
import android.health.connect.migration.RecordMigrationPayload;
import android.os.OutcomeReceiver;
import android.os.UserHandle;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.google.common.truth.Expect;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private static final String PACKAGE_NAME = "android.healthconnect.cts";
    private static final String APP_PACKAGE_NAME = "android.healthconnect.cts.app";
    private static final String PACKAGE_NAME_NOT_INSTALLED = "not.installed.package";
    private static final String APP_NAME = "Test App";
    private static final String APP_NAME_NEW = "Test App 2";

    @Rule public final Expect expect = Expect.create();

    private final Executor mOutcomeExecutor = Executors.newSingleThreadExecutor();
    private final Instant mEndTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private final Instant mStartTime = mEndTime.minus(Duration.ofHours(1));
    private Context mTargetContext;
    private HealthConnectManager mManager;

    private static <T, E extends RuntimeException> T blockingCall(
            Consumer<OutcomeReceiver<T, E>> action) {
        final BlockingOutcomeReceiver<T, E> outcomeReceiver = new BlockingOutcomeReceiver<>();
        action.accept(outcomeReceiver);
        return outcomeReceiver.await();
    }

    private static <T, E extends RuntimeException> T blockingCallWithPermissions(
            Consumer<OutcomeReceiver<T, E>> action, String... permissions) {
        try {
            return runWithShellPermissionIdentity(() -> blockingCall(action), permissions);
        } catch (RuntimeException e) {
            // runWithShellPermissionIdentity wraps and rethrows all exceptions as RuntimeException,
            // but we need the original RuntimeException if there is one.
            final Throwable cause = e.getCause();
            throw cause instanceof RuntimeException ? (RuntimeException) cause : e;
        }
    }

    private static Metadata getMetadata() {
        return new Metadata.Builder()
                .setDevice(new Device.Builder().setManufacturer("Device").setModel("Model").build())
                .build();
    }

    private static byte[] getBitmapBytes(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return stream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException(exception);
        }
    }

    @Before
    public void setUp() {
        mTargetContext = InstrumentationRegistry.getTargetContext();
        mManager = mTargetContext.getSystemService(HealthConnectManager.class);
        clearData();
    }

    @After
    public void tearDown() {
        clearData();
    }

    private void clearData() {
        deleteAllRecords();
        deleteAllStagedRemoteData();
    }

    @Test
    public void migrateHeight_heightSaved() {
        migrate(
                getRecordEntity(
                        "height1",
                        new HeightRecord.Builder(getMetadata(), mEndTime, fromMeters(3D)).build()));

        final HeightRecord record = getRecords(HeightRecord.class).get(0);

        assertThat(record.getHeight().getInMeters()).isEqualTo(3D);
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

    @Test
    public void migratePermissions_invalidPermission_throwsMigrationException() {
        revokeAppPermissions(READ_HEIGHT, WRITE_HEIGHT);

        final String entityId = "permissions1";

        final MigrationEntity entity =
                new MigrationEntity(
                        entityId,
                        new PermissionMigrationPayload.Builder(APP_PACKAGE_NAME, Instant.now())
                                .addPermission("invalid.permission")
                                .build());
        try {
            migrate(entity);
            fail("Expected to fail with MigrationException but didn't");
        } catch (MigrationException e) {
            assertEquals(MigrationException.ERROR_MIGRATE_ENTITY, e.getErrorCode());
            assertEquals(entityId, e.getFailedEntityId());
        }
    }

    @Test
    public void testMigrationStatesFlow() {
        int version = 33;
        runWithShellPermissionIdentity(
                () -> {
                    TestUtils.insertMinDataMigrationSdkExtensionVersion(version);
                    assertThat(TestUtils.isApiBlocked()).isEqualTo(false);
                    TestUtils.startMigration();
                    assertThat(TestUtils.isApiBlocked()).isEqualTo(true);
                    TestUtils.finishMigration();
                    assertThat(TestUtils.isApiBlocked()).isEqualTo(false);
                },
                Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
    }

    @Test
    public void migrateAppInfo_notInstalledAppAndRecordsMigrated_appInfoSaved() {
        final Bitmap icon = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        final byte[] iconBytes = getBitmapBytes(icon);

        migrate(
                getRecordEntity(
                        PACKAGE_NAME_NOT_INSTALLED,
                        "steps1",
                        new StepsRecord.Builder(getMetadata(), mStartTime, mEndTime, 10).build()));

        migrate(
                new MigrationEntity(
                        "appInfo1",
                        new AppInfoMigrationPayload.Builder(PACKAGE_NAME_NOT_INSTALLED, APP_NAME)
                                .setAppIcon(iconBytes)
                                .build()));

        final AppInfo appInfo = getContributorApplicationInfo(PACKAGE_NAME_NOT_INSTALLED);

        expect.that(appInfo).isNotNull();
        expect.that(appInfo.getName()).isEqualTo(APP_NAME);
        expect.that(getBitmapBytes(appInfo.getIcon())).isEqualTo(iconBytes);
    }

    @Test
    public void migrateAppInfo_notInstalledAppAndRecordsNotMigrated_appInfoNotSaved() {
        final Bitmap icon = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        final byte[] iconBytes = getBitmapBytes(icon);

        migrate(
                new MigrationEntity(
                        "appInfo1",
                        new AppInfoMigrationPayload.Builder(
                                        PACKAGE_NAME_NOT_INSTALLED, APP_NAME_NEW)
                                .setAppIcon(iconBytes)
                                .build()));

        final AppInfo appInfo = getContributorApplicationInfo(PACKAGE_NAME_NOT_INSTALLED);

        assertThat(appInfo).isNull();
    }

    @Test
    public void migrateAppInfo_installedAppAndRecordsMigrated_appInfoNotSaved() {
        final Bitmap icon = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        final byte[] iconBytes = getBitmapBytes(icon);

        migrate(
                getRecordEntity(
                        APP_PACKAGE_NAME,
                        "steps1",
                        new StepsRecord.Builder(getMetadata(), mStartTime, mEndTime, 10).build()));

        migrate(
                new MigrationEntity(
                        "appInfo1",
                        new AppInfoMigrationPayload.Builder(APP_PACKAGE_NAME, APP_NAME_NEW)
                                .setAppIcon(iconBytes)
                                .build()));

        final AppInfo appInfo = getContributorApplicationInfo(APP_PACKAGE_NAME);

        expect.that(appInfo).isNotNull();
        expect.that(appInfo.getName()).isNotEqualTo(APP_NAME_NEW);
        expect.that(getBitmapBytes(appInfo.getIcon())).isNotEqualTo(iconBytes);
    }

    private void migrate(MigrationEntity... entities) {
        DataMigrationTest.<Void, MigrationException>blockingCallWithPermissions(
                callback -> mManager.startMigration(mOutcomeExecutor, callback),
                Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);

        DataMigrationTest.<Void, MigrationException>blockingCallWithPermissions(
                callback ->
                        mManager.writeMigrationData(List.of(entities), mOutcomeExecutor, callback),
                Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
    }

    private MigrationEntity getRecordEntity(String entityId, Record record) {
        return getRecordEntity(PACKAGE_NAME, entityId, record);
    }

    private MigrationEntity getRecordEntity(String packageName, String entityId, Record record) {
        return new MigrationEntity(
                entityId,
                new RecordMigrationPayload.Builder(packageName, "Example App", record).build());
    }

    private <T extends Record> List<T> getRecords(Class<T> clazz) {
        final Consumer<OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException>> action =
                callback -> getRecordsAsync(clazz, callback);

        final ReadRecordsResponse<T> response =
                blockingCallWithPermissions(
                        action, HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);

        return response.getRecords();
    }

    private <T extends Record> void getRecordsAsync(
            Class<T> clazz,
            OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException> callback) {
        mManager.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(clazz)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(mStartTime)
                                        .setEndTime(mEndTime)
                                        .build())
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

    private void deleteAllStagedRemoteData() {
        runWithShellPermissionIdentity(
                () ->
                        // TODO(b/241542162): Avoid reflection once TestApi can be called from CTS
                        mManager.getClass().getMethod("deleteAllStagedRemoteData").invoke(mManager),
                "android.permission.DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA");
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

    private AppInfo getContributorApplicationInfo(String packageName) {
        return getContributorApplicationsInfo().stream()
                .filter(ai -> ai.getPackageName().equals(packageName))
                .findFirst()
                .orElse(null);
    }

    private List<AppInfo> getContributorApplicationsInfo() {
        final ApplicationInfoResponse response =
                runWithShellPermissionIdentity(
                        () -> blockingCall(this::getContributorApplicationsInfoAsync));
        return response.getApplicationInfoList();
    }

    private void getContributorApplicationsInfoAsync(
            OutcomeReceiver<ApplicationInfoResponse, HealthConnectException> callback) {
        mManager.getContributorApplicationsInfo(mOutcomeExecutor, callback);
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
