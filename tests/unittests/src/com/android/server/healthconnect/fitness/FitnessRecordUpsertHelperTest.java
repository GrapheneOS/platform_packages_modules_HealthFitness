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

package com.android.server.healthconnect.fitness;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.SymptomRecord;
import android.health.connect.internal.datatypes.SymptomRecordInternal;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.healthconnect.testing.unittest.RecordInternalFactory;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.ArrayMap;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class FitnessRecordUpsertHelperTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String TEST_PACKAGE_NAME = "package.name";

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private TransactionManager mTransactionManager;
    private FitnessRecordUpsertHelper mFitnessRecordUpsertHelper;
    private AccessLogsHelper mAccessLogsHelper;
    private UserHandle mUserHandle;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setAppOpLogsHelper(mock(AppOpLogsHelper.class))
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mFitnessRecordUpsertHelper = healthConnectInjector.getFitnessRecordUpsertHelper();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();
        mUserHandle = context.getUser();

        FitnessTestUtils fitnessTestUtils = new FitnessTestUtils(healthConnectInjector);
        fitnessTestUtils.insertApp("package.name");
    }

    @Test
    public void insertRecords_insertsChangeLogs_insertsAccessLogs() {
        mFitnessRecordUpsertHelper.insertRecords(
                TEST_PACKAGE_NAME,
                List.of(
                        RecordInternalFactory.buildStepsRecord(500, 750, 100)
                                .setPackageName(TEST_PACKAGE_NAME)),
                new ArrayMap<>(),
                /* shouldGenerateAccessLogs= */ true);

        assertThat(mTransactionManager.count(new ReadTableRequest(ChangeLogsHelper.TABLE_NAME)))
                .isEqualTo(1);
        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isNotEmpty();
    }

    @Test
    public void insertRecords_insertAccessLogsFalse_noAccessLogsInserted() {
        mFitnessRecordUpsertHelper.insertRecords(
                TEST_PACKAGE_NAME,
                List.of(
                        RecordInternalFactory.buildStepsRecord(500, 750, 100)
                                .setPackageName(TEST_PACKAGE_NAME)),
                new ArrayMap<>(),
                /* shouldGenerateAccessLogs= */ false);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void insertRecordsUnrestricted_insertsChangeLogs_noAccessLogs() {
        mFitnessRecordUpsertHelper.insertRecordsUnrestricted(
                List.of(
                        RecordInternalFactory.buildStepsRecord(500, 750, 100)
                                .setPackageName(TEST_PACKAGE_NAME)
                                .setUuid(UUID.randomUUID())),
                /* shouldGenerateChangeLog= */ true);

        assertThat(mTransactionManager.count(new ReadTableRequest(ChangeLogsHelper.TABLE_NAME)))
                .isEqualTo(1);
        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags({Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB})
    public void insertSymptomsRecords_updateSymptomType_throwsException() {
        SymptomRecordInternal symptomRecordInternal = new SymptomRecordInternal();
        symptomRecordInternal.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG);
        // Set to generate same UUID for the record on inserting again
        symptomRecordInternal.setClientRecordId("123");

        String uuid =
                mFitnessRecordUpsertHelper
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                List.of(symptomRecordInternal),
                                new ArrayMap<>(),
                                /* shouldGenerateAccessLogs= */ true)
                        .get(0);

        symptomRecordInternal.setUuid(uuid);
        symptomRecordInternal.setSymptomType(SymptomRecord.SYMPTOM_TYPE_COUGH);

        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFitnessRecordUpsertHelper.insertRecords(
                                        TEST_PACKAGE_NAME,
                                        List.of(
                                                symptomRecordInternal,
                                                new SymptomRecordInternal()
                                                        .setSymptomType(
                                                                SymptomRecord.SYMPTOM_TYPE_ACNE)),
                                        new ArrayMap<>(),
                                        /* shouldGenerateAccessLogs= */ true));

        assertThat(thrown).hasMessageThat().isEqualTo("Updating Symptom type is not allowed.");
    }

    @Test
    @EnableFlags({Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB})
    public void insertSymptomsRecords_doNotUpdateSymptomType_successfulInsert() {
        SymptomRecordInternal symptomRecordInternal = new SymptomRecordInternal();
        symptomRecordInternal.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG);
        // Set to generate same UUID for the record on inserting again
        symptomRecordInternal.setClientRecordId("123");

        String uuid =
                mFitnessRecordUpsertHelper
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                List.of(symptomRecordInternal),
                                new ArrayMap<>(),
                                /* shouldGenerateAccessLogs= */ true)
                        .get(0);

        symptomRecordInternal.setUuid(uuid);
        symptomRecordInternal.setNotes("Testing");

        List<String> uuids =
                mFitnessRecordUpsertHelper.insertRecords(
                        TEST_PACKAGE_NAME,
                        List.of(
                                symptomRecordInternal,
                                new SymptomRecordInternal()
                                        .setSymptomType(SymptomRecord.SYMPTOM_TYPE_ACNE)),
                        new ArrayMap<>(),
                        /* shouldGenerateAccessLogs= */ true);

        assertThat(uuids).contains(uuid);
    }

    @Test
    @EnableFlags({Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB})
    public void updateSymptomsRecords_updateSymptomType_throwsException() {
        SymptomRecordInternal symptomRecordInternal = new SymptomRecordInternal();
        symptomRecordInternal.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG);
        // Set to generate same UUID for the record on inserting again
        symptomRecordInternal.setClientRecordId("123");

        String uuid =
                mFitnessRecordUpsertHelper
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                List.of(symptomRecordInternal),
                                new ArrayMap<>(),
                                /* shouldGenerateAccessLogs= */ true)
                        .get(0);

        symptomRecordInternal.setUuid(uuid);
        symptomRecordInternal.setSymptomType(SymptomRecord.SYMPTOM_TYPE_COUGH);

        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mFitnessRecordUpsertHelper.updateRecords(
                                        TEST_PACKAGE_NAME,
                                        List.of(symptomRecordInternal),
                                        new ArrayMap<>()));

        assertThat(thrown).hasMessageThat().isEqualTo("Updating Symptom type is not allowed.");
    }

    @Test
    @EnableFlags({Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB})
    public void updateSymptomsRecords_doNotUpdateSymptomType_successfulUpdate() {
        SymptomRecordInternal symptomRecordInternal = new SymptomRecordInternal();
        symptomRecordInternal.setSymptomType(SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG);
        // Set to generate same UUID for the record on inserting again
        symptomRecordInternal.setClientRecordId("123");

        String uuid =
                mFitnessRecordUpsertHelper
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                List.of(symptomRecordInternal),
                                new ArrayMap<>(),
                                /* shouldGenerateAccessLogs= */ true)
                        .get(0);

        symptomRecordInternal.setUuid(uuid);
        symptomRecordInternal.setNotes("Testing");

        List<String> uuids =
                mFitnessRecordUpsertHelper.updateRecords(
                        TEST_PACKAGE_NAME, List.of(symptomRecordInternal), new ArrayMap<>());

        assertThat(uuids).containsExactly(uuid);
    }
}
