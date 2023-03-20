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

package com.android.server.healthconnect.permission;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiAutomation;
import android.content.Context;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.os.OutcomeReceiver;
import android.os.Process;
import android.os.UserHandle;

import androidx.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FirstGrantTimeUnitTest {

    private static final String SELF_PACKAGE_NAME = "com.android.healthconnect.unittests";
    private static final UserHandle CURRENT_USER = Process.myUserHandle();

    @Mock private HealthPermissionIntentAppsTracker mTracker;

    private FirstGrantTimeManager mGrantTimeManager;

    private final UiAutomation mUiAutomation =
            InstrumentationRegistry.getInstrumentation().getUiAutomation();

    @Mock private FirstGrantTimeDatastore mDatastore;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getContext();
        MockitoAnnotations.initMocks(this);
        when(mDatastore.readForUser(CURRENT_USER)).thenReturn(new UserGrantTimeState(1));

        mUiAutomation.adoptShellPermissionIdentity(
                "android.permission.OBSERVE_GRANT_REVOKE_PERMISSIONS");
        mGrantTimeManager = new FirstGrantTimeManager(context, mTracker, mDatastore);
        mUiAutomation.dropShellPermissionIdentity();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknownPackage_throwsException() {
        mGrantTimeManager.getFirstGrantTime("android.unknown_package", CURRENT_USER);
    }

    @Test
    public void testCurrentPackage_intentNotSupported_grantTimeIsNull() {
        when(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(false);
        assertThat(mGrantTimeManager.getFirstGrantTime(SELF_PACKAGE_NAME, CURRENT_USER)).isNull();
    }

    @Test
    public void testCurrentPackage_intentSupported_grantTimeIsNotNull() {
        when(mTracker.supportsPermissionUsageIntent(SELF_PACKAGE_NAME, CURRENT_USER))
                .thenReturn(true);
        assertThat(mGrantTimeManager.getFirstGrantTime(SELF_PACKAGE_NAME, CURRENT_USER))
                .isNotNull();
        assertThat(mGrantTimeManager.getFirstGrantTime(SELF_PACKAGE_NAME, CURRENT_USER))
                .isGreaterThan(Instant.now().minusSeconds((long) 1e3));
        assertThat(mGrantTimeManager.getFirstGrantTime(SELF_PACKAGE_NAME, CURRENT_USER))
                .isLessThan(Instant.now().plusSeconds((long) 1e3));
        verify(mDatastore).writeForUser(ArgumentMatchers.any(), ArgumentMatchers.eq(CURRENT_USER));
        verify(mDatastore).readForUser(CURRENT_USER);
    }

    @Test(expected = HealthConnectException.class)
    public <T extends Record> void testReadRecords_WithNoIntent() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setTimeRangeFilter(filter)
                        .build();
        readRecords(request);
    }

    private static <T extends Record> List<T> readRecords(ReadRecordsRequest<T> request)
            throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        CountDownLatch latch = new CountDownLatch(1);
        assertThat(service).isNotNull();
        assertThat(request.getRecordType()).isNotNull();
        AtomicReference<List<T>> response = new AtomicReference<>();
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();
        service.readRecords(
                request,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(ReadRecordsResponse<T> result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        healthConnectExceptionAtomicReference.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        if (healthConnectExceptionAtomicReference.get() != null) {
            throw healthConnectExceptionAtomicReference.get();
        }
        return response.get();
    }
}
