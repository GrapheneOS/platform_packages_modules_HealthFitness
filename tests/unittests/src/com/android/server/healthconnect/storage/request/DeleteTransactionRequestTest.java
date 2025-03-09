/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SPEED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.android.server.healthconnect.storage.request.DeleteTransactionRequest.HEALTH_CONNECT_PACKAGE_NAME;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.RecordIdFilter;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.RecordIdFiltersParcel;
import android.health.connect.datatypes.BloodGlucoseRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.StepsRecord;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DeleteTransactionRequestTest {
    private static final String TEST_PACKAGE_NAME = "package.name";

    private AppInfoHelper mAppInfoHelper;
    private InternalHealthConnectMappings mInternalHealthConnectMappings;

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mAppInfoHelper = injector.getAppInfoHelper();
        mInternalHealthConnectMappings = injector.getInternalHealthConnectMappings();
    }

    @Test
    public void getPackageName_fromConstructor_nameSetCorrectly() {
        DeleteUsingFiltersRequest deleteRequest = new DeleteUsingFiltersRequest.Builder().build();
        DeleteUsingFiltersRequestParcel parcel = new DeleteUsingFiltersRequestParcel(deleteRequest);

        DeleteTransactionRequest request =
                new DeleteTransactionRequest(TEST_PACKAGE_NAME, parcel, mAppInfoHelper);
        assertThat(request.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
    }

    @Test
    public void getPackageName_autoDelete_isHcPackageName() {
        RecordHelper<?> helper = mInternalHealthConnectMappings.getRecordHelper(RECORD_TYPE_STEPS);
        DeleteTableRequest deleteRequest = helper.getDeleteRequestForAutoDelete(30);

        DeleteTransactionRequest request = new DeleteTransactionRequest(List.of(deleteRequest));
        assertThat(request.getPackageName()).isEqualTo(HEALTH_CONNECT_PACKAGE_NAME);
    }

    @Test
    public void getRecordTypeIds_deleteById_idsSetCorrectly() {
        List<RecordIdFilter> recordIdFilters =
                List.of(
                        RecordIdFilter.fromClientRecordId(StepsRecord.class, "stepId"),
                        RecordIdFilter.fromClientRecordId(HeartRateRecord.class, "heartId"));
        DeleteUsingFiltersRequestParcel parcel =
                new DeleteUsingFiltersRequestParcel(
                        new RecordIdFiltersParcel(recordIdFilters), TEST_PACKAGE_NAME);

        DeleteTransactionRequest request =
                new DeleteTransactionRequest(TEST_PACKAGE_NAME, parcel, mAppInfoHelper);
        assertThat(request.getRecordTypeIds())
                .containsExactly(RECORD_TYPE_STEPS, RECORD_TYPE_HEART_RATE);
    }

    @Test
    public void getRecordTypeIds_deleteByFilter_idsSetCorrectly() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(SpeedRecord.class)
                        .addRecordType(BloodGlucoseRecord.class)
                        .build();
        DeleteUsingFiltersRequestParcel parcel = new DeleteUsingFiltersRequestParcel(deleteRequest);

        DeleteTransactionRequest request =
                new DeleteTransactionRequest(TEST_PACKAGE_NAME, parcel, mAppInfoHelper);
        assertThat(request.getRecordTypeIds())
                .containsExactly(RECORD_TYPE_SPEED, RECORD_TYPE_BLOOD_GLUCOSE);
    }

    @Test
    public void getRecordTypeIds_autoDelete_expectsEmpty() {
        RecordHelper<?> helper =
                mInternalHealthConnectMappings.getRecordHelper(RECORD_TYPE_DISTANCE);
        DeleteTableRequest deleteRequest = helper.getDeleteRequestForAutoDelete(30);

        DeleteTransactionRequest request = new DeleteTransactionRequest(List.of(deleteRequest));
        assertThat(request.getRecordTypeIds()).isEmpty();
    }
}
