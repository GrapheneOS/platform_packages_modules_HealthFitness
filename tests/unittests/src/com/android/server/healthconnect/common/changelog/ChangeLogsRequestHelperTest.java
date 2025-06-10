/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.healthconnect.common.changelog;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ChangeLogsRequestHelperTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private ChangeLogsRequestHelper mChangeLogsRequestHelper;

    private static final String TEST_PACKAGE_NAME = "com.example.test";
    private static final long TEST_LATEST_CHANGE_LOG_ROW_ID = 10L;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mChangeLogsRequestHelper = injector.getChangeLogsRequestHelper();
    }

    @Test
    @EnableFlags({FLAG_CLOUD_BACKUP_AND_RESTORE, FLAG_CLOUD_BACKUP_AND_RESTORE_DB})
    public void getChangeLogRetentionDuration_cloudBackupEnabled_returnsNewRetention() {
        assertThat(ChangeLogsRequestHelper.getChangeLogRetentionDuration())
                .isEqualTo(Duration.ofDays(90));
    }

    @Test
    @DisableFlags({FLAG_CLOUD_BACKUP_AND_RESTORE, FLAG_CLOUD_BACKUP_AND_RESTORE_DB})
    public void getChangeLogRetentionDuration_cloudBackupDisabled_returnsDefaultRetention() {
        assertThat(ChangeLogsRequestHelper.getChangeLogRetentionDuration())
                .isEqualTo(Duration.ofDays(32));
    }

    @Test
    public void getTokenAndGetRequest_handlesRecordTypesCorrectly() {
        ChangeLogTokenRequest apiRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addDataOriginFilter(
                                new DataOrigin.Builder().setPackageName("com.filter.app1").build())
                        .build();

        String token =
                mChangeLogsRequestHelper.getToken(
                        TEST_LATEST_CHANGE_LOG_ROW_ID, TEST_PACKAGE_NAME, apiRequest);
        assertThat(token).isNotEmpty();

        ChangeLogsRequestHelper.TokenRequest retrievedRequest =
                mChangeLogsRequestHelper.getRequest(TEST_PACKAGE_NAME, token);
        assertThat(retrievedRequest.getRequestingPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(retrievedRequest.getRowIdChangeLogs()).isEqualTo(TEST_LATEST_CHANGE_LOG_ROW_ID);
        assertThat(retrievedRequest.getPackageNamesToFilter()).containsExactly("com.filter.app1");
        assertThat(retrievedRequest.getRecordTypes())
                .containsExactly(RecordTypeIdentifier.RECORD_TYPE_STEPS);
        assertThat(retrievedRequest.getMedicalResourceTypes()).isEmpty();
    }

    @Test
    @EnableFlags({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB
    })
    public void getTokenAndGetRequest_phrEnabled_handlesMedicalResourceTypesCorrectly() {
        ChangeLogTokenRequest apiRequest =
                new ChangeLogTokenRequest.Builder()
                        .addMedicalResourceType(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES)
                        .addDataOriginFilter(
                                new DataOrigin.Builder().setPackageName("com.filter.app2").build())
                        .build();

        String token =
                mChangeLogsRequestHelper.getToken(
                        TEST_LATEST_CHANGE_LOG_ROW_ID, TEST_PACKAGE_NAME, apiRequest);
        assertThat(token).isNotEmpty();

        ChangeLogsRequestHelper.TokenRequest retrievedRequest =
                mChangeLogsRequestHelper.getRequest(TEST_PACKAGE_NAME, token);
        assertThat(retrievedRequest.getRequestingPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(retrievedRequest.getRowIdChangeLogs()).isEqualTo(TEST_LATEST_CHANGE_LOG_ROW_ID);
        assertThat(retrievedRequest.getPackageNamesToFilter()).containsExactly("com.filter.app2");
        assertThat(retrievedRequest.getRecordTypes()).isEmpty();
        assertThat(retrievedRequest.getMedicalResourceTypes())
                .containsExactly(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES);
    }

    @Test
    public void getRequest_blankToken_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mChangeLogsRequestHelper.getRequest(TEST_PACKAGE_NAME, ""));
    }

    @Test
    public void getRequest_tokenNotFound_throwsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> mChangeLogsRequestHelper.getRequest(TEST_PACKAGE_NAME, "nonexistent_token"));
    }

    @Test
    public void getRequest_mismatchedPackageName_throwsIllegalArgumentException() {
        ChangeLogTokenRequest originalApiRequest =
                new ChangeLogTokenRequest.Builder().addRecordType(StepsRecord.class).build();
        String token =
                mChangeLogsRequestHelper.getToken(
                        TEST_LATEST_CHANGE_LOG_ROW_ID, TEST_PACKAGE_NAME, originalApiRequest);

        assertThrows(
                IllegalArgumentException.class,
                () -> mChangeLogsRequestHelper.getRequest("com.another.package", token));
    }

    @Test
    public void getNextPageToken_insertsCorrectValues() {
        ChangeLogsRequestHelper.TokenRequest originalTokenRequest =
                new ChangeLogsRequestHelper.TokenRequest(
                        List.of("com.filter.app1"),
                        List.of(RecordTypeIdentifier.RECORD_TYPE_STEPS),
                        Collections.emptyList(),
                        TEST_PACKAGE_NAME,
                        TEST_LATEST_CHANGE_LOG_ROW_ID);
        long nextRowId = 20L;

        String nextPageToken =
                mChangeLogsRequestHelper.getNextPageToken(originalTokenRequest, nextRowId);
        assertThat(nextPageToken).isNotEmpty();

        ChangeLogsRequestHelper.TokenRequest retrievedRequest =
                mChangeLogsRequestHelper.getRequest(TEST_PACKAGE_NAME, nextPageToken);
        assertThat(retrievedRequest.getRequestingPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(retrievedRequest.getRowIdChangeLogs()).isEqualTo(nextRowId);
        assertThat(retrievedRequest.getPackageNamesToFilter()).containsExactly("com.filter.app1");
        assertThat(retrievedRequest.getRecordTypes())
                .containsExactly(RecordTypeIdentifier.RECORD_TYPE_STEPS);
        assertThat(retrievedRequest.getMedicalResourceTypes()).isEmpty();
    }

    @Test
    @EnableFlags({
        FLAG_PHR_CHANGE_LOGS,
        FLAG_PHR_CHANGE_LOGS_DB,
        FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB
    })
    public void getNextPageToken_phrEnabled_handlesMedicalResourceTypes() {
        ChangeLogsRequestHelper.TokenRequest originalTokenRequest =
                new ChangeLogsRequestHelper.TokenRequest(
                        List.of("com.filter.app2"),
                        Collections.emptyList(),
                        List.of(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES),
                        TEST_PACKAGE_NAME,
                        TEST_LATEST_CHANGE_LOG_ROW_ID);
        long nextRowId = 30L;

        String nextPageToken =
                mChangeLogsRequestHelper.getNextPageToken(originalTokenRequest, nextRowId);
        assertThat(nextPageToken).isNotEmpty();

        ChangeLogsRequestHelper.TokenRequest retrievedRequest =
                mChangeLogsRequestHelper.getRequest(TEST_PACKAGE_NAME, nextPageToken);
        assertThat(retrievedRequest.getRequestingPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(retrievedRequest.getRowIdChangeLogs()).isEqualTo(nextRowId);
        assertThat(retrievedRequest.getPackageNamesToFilter()).containsExactly("com.filter.app2");
        assertThat(retrievedRequest.getRecordTypes()).isEmpty();
        assertThat(retrievedRequest.getMedicalResourceTypes())
                .containsExactly(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES);
    }
}
