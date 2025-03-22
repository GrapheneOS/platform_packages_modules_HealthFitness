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

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.HealthDataCategory.BODY_MEASUREMENTS;
import static android.health.connect.HealthDataCategory.CYCLE_TRACKING;
import static android.health.connect.HealthDataCategory.NUTRITION;
import static android.health.connect.HealthDataCategory.SLEEP;
import static android.health.connect.HealthDataCategory.VITALS;
import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.healthconnect.cts.utils.HealthConnectReceiver.callAndGetResponseWithShellPermissionIdentity;
import static android.healthconnect.cts.utils.TestOutcomeReceiver.outcomeExecutor;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.health.connect.FetchDataOriginsPriorityOrderResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.UpdateDataOriginPriorityOrderRequest;
import android.health.connect.datatypes.DataOrigin;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.HealthConnectReceiver;
import android.healthconnect.cts.utils.TestUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class HealthPermissionCategoryPriorityTests {
    private static final Set<Integer> sAllDataCategories =
            Set.of(ACTIVITY, BODY_MEASUREMENTS, CYCLE_TRACKING, NUTRITION, SLEEP, VITALS);

    public static final String PACKAGE_NAME = "android.healthconnect.cts";

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private HealthConnectManager mManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mManager = requireNonNull(context.getSystemService(HealthConnectManager.class));
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() {
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testGetPriority() throws InterruptedException {
        for (Integer permissionCategory : sAllDataCategories) {
            FetchDataOriginsPriorityOrderResponse response =
                    callAndGetResponseWithShellPermissionIdentity(
                            (executor, receiver) ->
                                    mManager.fetchDataOriginsPriorityOrder(
                                            permissionCategory, executor, receiver),
                            MANAGE_HEALTH_DATA_PERMISSION);

            assertThat(response).isNotNull();
        }
    }

    @Test
    public void testGetPriority_no_perm() throws InterruptedException {
        for (Integer permissionCategory : sAllDataCategories) {
            HealthConnectReceiver<FetchDataOriginsPriorityOrderResponse> receiver =
                    new HealthConnectReceiver<>();
            mManager.fetchDataOriginsPriorityOrder(permissionCategory, outcomeExecutor(), receiver);

            HealthConnectException healthConnectException = receiver.assertAndGetException();
            assertThat(healthConnectException.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_SECURITY);
        }
    }

    @Test
    public void testUpdatePriority_withNewApps_updatesCorrectly() throws InterruptedException {
        for (Integer permissionCategory : sAllDataCategories) {
            FetchDataOriginsPriorityOrderResponse currentPriority =
                    callAndGetResponseWithShellPermissionIdentity(
                            (executor, receiver) ->
                                    mManager.fetchDataOriginsPriorityOrder(
                                            permissionCategory, executor, receiver),
                            MANAGE_HEALTH_DATA_PERMISSION);
            assertThat(currentPriority).isNotNull();
            // The initial priority list is empty at this stage because permissions have
            // been granted through packageManager
            // TODO (b/314092270) - remove when the priority list is updated via the package
            // manager
            assertThat(currentPriority.getDataOriginsPriorityOrder()).isEmpty();

            UpdateDataOriginPriorityOrderRequest updateRequest =
                    createUpdateRequest(List.of(PACKAGE_NAME), permissionCategory);
            Void unused =
                    callAndGetResponseWithShellPermissionIdentity(
                            (executor, receiver) ->
                                    mManager.updateDataOriginPriorityOrder(
                                            updateRequest, executor, receiver),
                            MANAGE_HEALTH_DATA_PERMISSION);

            FetchDataOriginsPriorityOrderResponse newPriority =
                    callAndGetResponseWithShellPermissionIdentity(
                            (executor, receiver) ->
                                    mManager.fetchDataOriginsPriorityOrder(
                                            permissionCategory, executor, receiver),
                            MANAGE_HEALTH_DATA_PERMISSION);

            assertThat(newPriority.getDataOriginsPriorityOrder()).hasSize(1);
            assertThat(newPriority.getDataOriginsPriorityOrder().get(0).getPackageName())
                    .isEqualTo(PACKAGE_NAME);
        }
    }

    @Test
    public void testUpdatePriority_no_perm() throws InterruptedException {
        for (Integer permissionCategory : sAllDataCategories) {
            UpdateDataOriginPriorityOrderRequest updateRequest =
                    createUpdateRequest(List.of("a", "b", "c"), permissionCategory);
            HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
            mManager.updateDataOriginPriorityOrder(updateRequest, outcomeExecutor(), receiver);

            HealthConnectException healthConnectException = receiver.assertAndGetException();
            assertThat(healthConnectException.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_SECURITY);
        }
    }

    // TODO(b/261618513): Test actual priority order by using other test apps

    private static UpdateDataOriginPriorityOrderRequest createUpdateRequest(
            List<String> packageNames, int dataCategory) {
        List<DataOrigin> dataOrigins =
                packageNames.stream()
                        .map(
                                (packageName) ->
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName)
                                                .build())
                        .toList();
        return new UpdateDataOriginPriorityOrderRequest(dataOrigins, dataCategory);
    }
}
