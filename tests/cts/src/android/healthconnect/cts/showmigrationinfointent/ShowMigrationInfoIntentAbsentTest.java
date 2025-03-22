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

package android.healthconnect.cts.showmigrationinfointent;

import static android.Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA;
import static android.healthconnect.cts.utils.TestOutcomeReceiver.outcomeExecutor;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import android.content.Context;
import android.health.connect.HealthConnectManager;
import android.health.connect.migration.MigrationException;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestOutcomeReceiver;
import android.healthconnect.cts.utils.TestUtils;
import android.os.Build;
import android.os.ext.SdkExtensions;
import android.platform.test.annotations.AppModeFull;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class ShowMigrationInfoIntentAbsentTest {
    private HealthConnectManager mManager;

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        mManager = context.getSystemService(HealthConnectManager.class);
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() {
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testInsertMinDataMigrationSdkExtensionVersion_throwsException() {
        TestOutcomeReceiver<Void, MigrationException> receiver = new TestOutcomeReceiver<>();
        int version = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) + 1;
        runWithShellPermissionIdentity(
                () -> {
                    mManager.insertMinDataMigrationSdkExtensionVersion(
                            version, outcomeExecutor(), receiver);
                    receiver.assertAndGetException();
                },
                MIGRATE_HEALTH_CONNECT_DATA);
    }

    @Test
    public void testStartMigration_throwsException() {
        TestOutcomeReceiver<Void, MigrationException> receiver = new TestOutcomeReceiver<>();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.startMigration(outcomeExecutor(), receiver);
                    receiver.assertAndGetException();
                },
                MIGRATE_HEALTH_CONNECT_DATA);
    }

    @Test
    public void testFinishMigration_throwsException() {
        TestOutcomeReceiver<Void, MigrationException> receiver = new TestOutcomeReceiver<>();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.finishMigration(outcomeExecutor(), receiver);
                    receiver.assertAndGetException();
                },
                MIGRATE_HEALTH_CONNECT_DATA);
    }

    @Test
    public void testWriteMigrationData_throwsException() {
        TestOutcomeReceiver<Void, MigrationException> receiver = new TestOutcomeReceiver<>();
        runWithShellPermissionIdentity(
                () -> {
                    mManager.writeMigrationData(List.of(), outcomeExecutor(), receiver);
                    receiver.assertAndGetException();
                },
                MIGRATE_HEALTH_CONNECT_DATA);
    }
}
