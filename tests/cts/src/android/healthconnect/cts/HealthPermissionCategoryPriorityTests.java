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

import static android.healthconnect.HealthDataCategory.ACTIVITY;
import static android.healthconnect.HealthDataCategory.BODY_MEASUREMENTS;
import static android.healthconnect.HealthDataCategory.CYCLE_TRACKING;
import static android.healthconnect.HealthDataCategory.NUTRITION;
import static android.healthconnect.HealthDataCategory.SLEEP;
import static android.healthconnect.HealthDataCategory.VITALS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.GetDataOriginPriorityOrderResponse;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.UpdateDataOriginPriorityOrderRequest;
import android.healthconnect.datatypes.DataOrigin;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class HealthPermissionCategoryPriorityTests {
    private static final Set<Integer> sAllDataCategories =
            Set.of(ACTIVITY, BODY_MEASUREMENTS, CYCLE_TRACKING, NUTRITION, SLEEP, VITALS);
    private static final String TAG = "PermissionCategoryPriorityTests";

    @Test
    public void testGetPriority() throws InterruptedException {
        for (Integer permissionCategory : sAllDataCategories) {
            assertThat(getPriority(permissionCategory)).isNotNull();
        }
    }

    @Test
    public void testUpdatePriority_incorrectValues() throws InterruptedException {
        for (Integer permissionCategory : sAllDataCategories) {
            GetDataOriginPriorityOrderResponse currentPriority = getPriority(permissionCategory);
            assertThat(currentPriority).isNotNull();
            updatePriority(permissionCategory, Arrays.asList("a", "b", "c"));
            GetDataOriginPriorityOrderResponse newPriority = getPriority(permissionCategory);
            assertThat(currentPriority.getDataOriginInPriorityOrder().size())
                    .isEqualTo(newPriority.getDataOriginInPriorityOrder().size());

            List<String> currentPriorityString =
                    currentPriority.getDataOriginInPriorityOrder().stream()
                            .map(DataOrigin::getPackageName)
                            .collect(Collectors.toList());
            List<String> newPriorityString =
                    newPriority.getDataOriginInPriorityOrder().stream()
                            .map(DataOrigin::getPackageName)
                            .collect(Collectors.toList());
            assertThat(currentPriorityString.equals(newPriorityString)).isTrue();
        }
    }

    // TODO(b/257638480): Add more tests with some actual priorities, after grant permission tests
    // are in place

    private GetDataOriginPriorityOrderResponse getPriority(int permissionCategory)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<GetDataOriginPriorityOrderResponse> response = new AtomicReference<>();
        service.getDataOriginsInPriorityOrder(
                permissionCategory,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(GetDataOriginPriorityOrderResponse result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, "Exception: ", exception);
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();

        return response.get();
    }

    private void updatePriority(int permissionCategory, List<String> packageNames)
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        List<DataOrigin> dataOrigins =
                packageNames.stream()
                        .map(
                                (packageName) ->
                                        new DataOrigin.Builder()
                                                .setPackageName(packageName)
                                                .build())
                        .collect(Collectors.toList());

        CountDownLatch latch = new CountDownLatch(1);
        service.updateDataOriginPriorityOrder(
                new UpdateDataOriginPriorityOrderRequest(dataOrigins, permissionCategory),
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, "Exception: ", exception);
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
    }

    // TODO(b/261618513): Test actual priority order by using other test apps
}
