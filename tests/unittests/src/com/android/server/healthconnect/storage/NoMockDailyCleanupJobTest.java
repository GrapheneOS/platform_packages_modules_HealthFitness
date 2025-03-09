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

package com.android.server.healthconnect.storage;

import static com.android.server.healthconnect.storage.datatypehelpers.StepsRecordHelper.STEPS_TABLE_NAME;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.database.Cursor;
import android.health.connect.internal.datatypes.RecordInternal;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.StepsRecordHelper;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.PreferencesManager;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class NoMockDailyCleanupJobTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    private static final String TEST_PACKAGE_NAME = "package.name";

    private TransactionManager mTransactionManager;
    private TransactionTestUtils mTransactionTestUtils;
    private HealthConnectInjector mHealthConnectInjector;
    private DailyCleanupJob mDailyCleanupJob;
    private PreferencesManager mPreferencesManager;

    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mHealthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mDailyCleanupJob = mHealthConnectInjector.getDailyCleanupJob();
        mPreferencesManager = mHealthConnectInjector.getPreferencesManager();
        mTransactionManager = mHealthConnectInjector.getTransactionManager();

        mTransactionTestUtils = new TransactionTestUtils(mHealthConnectInjector);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
    }

    @Test
    public void startDailyCleanup_changeLogsGenerated() {
        String uuid =
                mTransactionTestUtils
                        .insertRecords(TEST_PACKAGE_NAME, createStepsRecord(4000, 5000, 100))
                        .get(0);
        RecordHelper<?> helper = new StepsRecordHelper();
        try (Cursor cursor = mTransactionManager.read(new ReadTableRequest(STEPS_TABLE_NAME))) {
            List<RecordInternal<?>> records =
                    helper.getInternalRecords(
                            cursor,
                            mHealthConnectInjector.getDeviceInfoHelper(),
                            mHealthConnectInjector.getAppInfoHelper());
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getUuid()).isEqualTo(UUID.fromString(uuid));
        }

        mPreferencesManager.setRecordRetentionPeriodInDays(30);
        assertThat(mPreferencesManager.getRecordRetentionPeriodInDays()).isEqualTo(30);
        mDailyCleanupJob.startDailyCleanup();

        try (Cursor cursor = mTransactionManager.read(new ReadTableRequest(STEPS_TABLE_NAME))) {
            List<RecordInternal<?>> records =
                    helper.getInternalRecords(
                            cursor,
                            mHealthConnectInjector.getDeviceInfoHelper(),
                            mHealthConnectInjector.getAppInfoHelper());
            assertThat(records).isEmpty();
        }

        assertThat(mTransactionTestUtils.getAllDeletedUuids())
                .containsExactly(UUID.fromString(uuid));
    }
}
