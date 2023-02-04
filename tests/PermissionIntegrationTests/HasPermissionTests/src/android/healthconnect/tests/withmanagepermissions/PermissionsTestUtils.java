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

package android.healthconnect.tests.withmanagepermissions;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;
import android.health.connect.migration.MigrationException;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PermissionsTestUtils {
    private static final String TAG = "HCPermissionsTestUtils";

    /**
     * Skips test if the test app doesn't hold {@link HealthPermissions.MANAGE_HEALTH_PERMISSIONS}.
     * Should be called from the test or test setup methods.
     *
     * <p>{@link HealthPermissions.MANAGE_HEALTH_PERMISSIONS} is protected at signature level, and
     * should be granted automatically if it's possible to hold. This method simply check if test
     * package holds the permission.
     */
    public static void assumeHoldManageHealthPermissionsPermission(Context context) {
        assumeTrue(
                "Skipping test - test cannot hold "
                        + HealthPermissions.MANAGE_HEALTH_PERMISSIONS
                        + " for the build-under-test. This is likely because the build uses "
                        + "mainline prebuilts and therefore does not have a compatible signature "
                        + "with this test app. See the test class Javadoc for more info.",
                context.checkSelfPermission(HealthPermissions.MANAGE_HEALTH_PERMISSIONS)
                        == PackageManager.PERMISSION_GRANTED);
    }

    public static void startMigration() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);

        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        service.startMigration(
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, MigrationException>() {
                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(MigrationException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    }

    public static void finishMigration() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);

        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        service.finishMigration(
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, MigrationException>() {

                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(MigrationException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    }

    public static void deleteAllStagedRemoteData() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        runWithShellPermissionIdentity(
                () ->
                        // TODO(b/241542162): Avoid reflection once TestApi can be called from CTS
                        service.getClass().getMethod("deleteAllStagedRemoteData").invoke(service),
                "android.permission.DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA");
    }
}
