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

package com.android.server.healthconnect.storage.request;

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.cts.utils.DataFactory.getBasalMetabolicRateRecord;
import static android.healthconnect.cts.utils.DataFactory.getStepsRecord;

import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.testing.fixtures.EnvironmentFixture;
import com.android.server.healthconnect.testing.fixtures.SQLiteDatabaseFixture;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class UpsertTransactionRequestTest {

    private static final String TEST_PACKAGE_NAME = "package.name";

    @Rule(order = 1)
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .addStaticMockFixtures(EnvironmentFixture::new, SQLiteDatabaseFixture::new)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    private TransactionManager mTransactionManager;
    private InternalHealthConnectMappings mInternalHealthConnectMappings;
    private AppInfoHelper mAppInfoHelper;
    private DeviceInfoHelper mDeviceInfoHelper;
    private AccessLogsHelper mAccessLogsHelper;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .build();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mInternalHealthConnectMappings = healthConnectInjector.getInternalHealthConnectMappings();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mDeviceInfoHelper = healthConnectInjector.getDeviceInfoHelper();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();

        TransactionTestUtils transactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        transactionTestUtils.insertApp("package.name");
    }

    @Test
    public void getPackageName_expectCorrectName() {
        UpsertTransactionRequest request1 =
                UpsertTransactionRequest.createForInsert(
                        "package.name.1",
                        List.of(),
                        mTransactionManager,
                        mInternalHealthConnectMappings,
                        mDeviceInfoHelper,
                        mAppInfoHelper,
                        mAccessLogsHelper,
                        /* extraPermsStateMap= */ new ArrayMap<>());
        assertThat(request1.getPackageName()).isEqualTo("package.name.1");

        UpsertTransactionRequest request2 =
                UpsertTransactionRequest.createForUpdate(
                        "package.name.2",
                        List.of(),
                        mTransactionManager,
                        mInternalHealthConnectMappings,
                        mDeviceInfoHelper,
                        mAppInfoHelper,
                        mAccessLogsHelper,
                        /* extraPermsStateMap= */ new ArrayMap<>());
        assertThat(request2.getPackageName()).isEqualTo("package.name.2");

        UpsertTransactionRequest request3 =
                UpsertTransactionRequest.createForRestore(
                        List.of(),
                        mTransactionManager,
                        mInternalHealthConnectMappings,
                        mDeviceInfoHelper,
                        mAppInfoHelper);
        assertThat(request3.getPackageName()).isNull();
    }

    @Test
    public void getRecordTypeIds_expectCorrectRecordTypeIds() {
        List<RecordInternal<?>> records =
                List.of(
                        getStepsRecord().toRecordInternal(),
                        getBasalMetabolicRateRecord().toRecordInternal());
        UpsertTransactionRequest request =
                UpsertTransactionRequest.createForUpdate(
                        TEST_PACKAGE_NAME,
                        records,
                        mTransactionManager,
                        mInternalHealthConnectMappings,
                        mDeviceInfoHelper,
                        mAppInfoHelper,
                        mAccessLogsHelper,
                        /* extraPermsStateMap= */ new ArrayMap<>());

        assertThat(request.getRecordTypeIds())
                .containsExactly(RECORD_TYPE_STEPS, RECORD_TYPE_BASAL_METABOLIC_RATE);
    }

    @Test
    public void execute_forInsert_insertsChangeLogs_insertsAccessLogs() {
        UpsertTransactionRequest upsertTransactionRequest =
                UpsertTransactionRequest.createForInsert(
                        TEST_PACKAGE_NAME,
                        List.of(createStepsRecord(500, 750, 100).setPackageName(TEST_PACKAGE_NAME)),
                        mTransactionManager,
                        mInternalHealthConnectMappings,
                        mDeviceInfoHelper,
                        mAppInfoHelper,
                        mAccessLogsHelper,
                        new ArrayMap<>());
        upsertTransactionRequest.execute();

        assertThat(mTransactionManager.count(new ReadTableRequest(ChangeLogsHelper.TABLE_NAME)))
                .isEqualTo(1);
        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs();
        assertThat(result).isNotEmpty();
    }

    @Test
    public void execute_forRestore_insertsChangeLogs_withNoAccessLogs() {
        UpsertTransactionRequest.createForRestore(
                        List.of(
                                createStepsRecord(500, 750, 100)
                                        .setPackageName(TEST_PACKAGE_NAME)
                                        .setUuid(UUID.randomUUID())),
                        mTransactionManager,
                        mInternalHealthConnectMappings,
                        mDeviceInfoHelper,
                        mAppInfoHelper)
                .execute();

        assertThat(mTransactionManager.count(new ReadTableRequest(ChangeLogsHelper.TABLE_NAME)))
                .isEqualTo(1);
        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs();
        assertThat(result).isEmpty();
    }
}
