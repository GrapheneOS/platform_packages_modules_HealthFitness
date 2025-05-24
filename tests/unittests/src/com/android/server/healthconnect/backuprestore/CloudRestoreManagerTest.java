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

package com.android.server.healthconnect.backuprestore;

import static com.android.server.healthconnect.backuprestore.ProtoTestData.TEST_PACKAGE_NAME;
import static com.android.server.healthconnect.backuprestore.ProtoTestData.generateCoreRecord;
import static com.android.server.healthconnect.backuprestore.ProtoTestData.generateExerciseSession;
import static com.android.server.healthconnect.backuprestore.ProtoTestData.generateIntervalRecord;
import static com.android.server.healthconnect.backuprestore.ProtoTestData.generateRecord;
import static com.android.server.healthconnect.backuprestore.RecordProtoConverter.PROTO_VERSION;
import static com.android.server.healthconnect.common.preferences.PreferencesManager.AUTO_DELETE_DURATION_RECORDS_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.health.connect.HealthDataCategory;
import android.health.connect.backuprestore.BackupMetadata;
import android.health.connect.backuprestore.RestoreChange;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BackupData;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Record;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.PriorityList;
import com.android.server.healthconnect.storage.DatabaseHelper.DatabaseHelpers;
import com.android.server.healthconnect.storage.TransactionManager;

import com.google.common.collect.ImmutableMap;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Unit test for class {@link CloudRestoreManager}. */
@RunWith(AndroidJUnit4.class)
public class CloudRestoreManagerTest {

    private static final String TEST_PACKAGE_NAME_2 = "other.app";
    private static final String TEST_PACKAGE_NAME_3 = "another.app";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private AppInfoHelper mAppInfoHelper;
    private DeviceInfoHelper mDeviceInfoHelper;
    private TransactionManager mTransactionManager;
    private FitnessRecordReadHelper mFitnessRecordReadHelper;
    private FitnessTestUtils mFitnessTestUtils;
    private CloudRestoreManager mCloudRestoreManager;
    private RecordProtoConverter mRecordProtoConverter;
    private HealthDataCategoryPriorityHelper mPriorityHelper;
    private PreferenceHelper mPreferenceHelper;
    private InternalHealthConnectMappings mMappings;
    private DatabaseHelpers mDatabaseHelpers;
    private Instant mTimeStamp;

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mTransactionManager = healthConnectInjector.getTransactionManager();
        mFitnessRecordReadHelper = healthConnectInjector.getFitnessRecordReadHelper();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mDeviceInfoHelper = healthConnectInjector.getDeviceInfoHelper();
        mPriorityHelper = healthConnectInjector.getHealthDataCategoryPriorityHelper();
        mPreferenceHelper = healthConnectInjector.getPreferenceHelper();
        mMappings = healthConnectInjector.getInternalHealthConnectMappings();
        mDatabaseHelpers = healthConnectInjector.getDatabaseHelpers();

        mTimeStamp = Instant.parse("2024-06-04T16:39:12Z");
        Clock fakeClock = Clock.fixed(mTimeStamp, ZoneId.of("UTC"));

        mRecordProtoConverter = new RecordProtoConverter();
        mCloudRestoreManager =
                new CloudRestoreManager(
                        mTransactionManager,
                        healthConnectInjector.getFitnessRecordUpsertHelper(),
                        mFitnessRecordReadHelper,
                        mMappings,
                        mDeviceInfoHelper,
                        mAppInfoHelper,
                        mPriorityHelper,
                        mPreferenceHelper,
                        fakeClock,
                        healthConnectInjector.getBackupRestoreLogger());
        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);
    }

    @Test
    public void canRestore() {
        assertThat(mCloudRestoreManager.canRestore(PROTO_VERSION - 1)).isTrue();
        assertThat(mCloudRestoreManager.canRestore(PROTO_VERSION)).isTrue();
        assertThat(mCloudRestoreManager.canRestore(PROTO_VERSION + 1)).isFalse();
    }

    @Test
    public void restoreChanges_restoresChanges() {
        Record stepsRecord = generateRecord(RecordTypeIdentifier.RECORD_TYPE_STEPS);
        RestoreChange stepsChange =
                new RestoreChange(
                        BackupData.newBuilder().setRecord(stepsRecord).build().toByteArray());
        Record bloodPressureRecord =
                generateRecord(RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE);
        RestoreChange bloodPressureChange =
                new RestoreChange(
                        BackupData.newBuilder()
                                .setRecord(bloodPressureRecord)
                                .build()
                                .toByteArray());

        mCloudRestoreManager.restoreChanges(List.of(stepsChange, bloodPressureChange));

        List<RecordInternal<?>> records =
                mFitnessTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                List.of(UUID.fromString(stepsRecord.getUuid())),
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                List.of(UUID.fromString(bloodPressureRecord.getUuid()))));
        assertThat(records).hasSize(2);
        assertThat(mRecordProtoConverter.toRecordProto(records.get(0))).isEqualTo(stepsRecord);
        assertThat(mRecordProtoConverter.toRecordProto(records.get(1)))
                .isEqualTo(bloodPressureRecord);
        assertThat(mAppInfoHelper.getRecordTypesToContributingPackagesMap())
                .containsExactly(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        Set.of(stepsRecord.getPackageName()),
                        RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                        Set.of(bloodPressureRecord.getPackageName()));
    }

    @Test
    public void restoreChanges_duplicatedChangesIgnored() {
        Record stepsRecord = generateRecord(RecordTypeIdentifier.RECORD_TYPE_STEPS);
        RestoreChange stepsChange =
                new RestoreChange(
                        BackupData.newBuilder().setRecord(stepsRecord).build().toByteArray());
        Record bloodPressureRecord =
                generateRecord(RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE);
        RestoreChange bloodPressureChange =
                new RestoreChange(
                        BackupData.newBuilder()
                                .setRecord(bloodPressureRecord)
                                .build()
                                .toByteArray());

        // First restore
        mCloudRestoreManager.restoreChanges(List.of(stepsChange, bloodPressureChange));

        // Second restore does not throw any exceptions
        mCloudRestoreManager.restoreChanges(List.of(stepsChange, bloodPressureChange));
        List<RecordInternal<?>> records =
                mFitnessTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                List.of(UUID.fromString(stepsRecord.getUuid())),
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                List.of(UUID.fromString(bloodPressureRecord.getUuid()))));
        assertThat(records).hasSize(2);
        assertThat(mRecordProtoConverter.toRecordProto(records.get(0))).isEqualTo(stepsRecord);
        assertThat(mRecordProtoConverter.toRecordProto(records.get(1)))
                .isEqualTo(bloodPressureRecord);
    }

    @Test
    public void whenRestoreSettingsCalled_withUnspecifiedEnums_settingsSuccessfullyRestored() {
        CloudBackupSettingsHelper cloudBackupSettingsHelper =
                new CloudBackupSettingsHelper(mPriorityHelper, mPreferenceHelper, mAppInfoHelper);
        setupInitialSettings();
        Settings settingsToRestore = createSettingsToRestore();
        mCloudRestoreManager.restoreSettings(new BackupMetadata(settingsToRestore.toByteArray()));

        Settings currentSettings = cloudBackupSettingsHelper.collectUserSettings();
        mDatabaseHelpers.clearAllData(mTransactionManager);

        Map<Integer, PriorityList> expectedPriorityList =
                Map.of(
                        HealthDataCategory.ACTIVITY,
                        PriorityList.newBuilder()
                                .addPackageName(TEST_PACKAGE_NAME)
                                .addPackageName(TEST_PACKAGE_NAME_2)
                                .addPackageName(TEST_PACKAGE_NAME_3)
                                .build());
        assertSettingsCorrectlyUpdated(settingsToRestore, currentSettings, expectedPriorityList);
    }

    @Test
    public void restoreChanges_exerciseSession_withMissingTrainingPlan_removesReference() {
        Record exerciseSessionRecord =
                generateRecord(RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION);
        Record sessionWithPlanReference =
                exerciseSessionRecord.toBuilder()
                        .setIntervalRecord(
                                exerciseSessionRecord.getIntervalRecord().toBuilder()
                                        .setExerciseSession(
                                                exerciseSessionRecord
                                                        .getIntervalRecord()
                                                        .getExerciseSession()
                                                        .toBuilder()
                                                        .setPlannedExerciseSessionId(
                                                                UUID.randomUUID().toString())))
                        .build();

        mCloudRestoreManager.restoreChanges(
                List.of(
                        new RestoreChange(
                                BackupData.newBuilder()
                                        .setRecord(sessionWithPlanReference)
                                        .build()
                                        .toByteArray())));

        var restoredSession = readExerciseSession(exerciseSessionRecord.getUuid());
        assertThat(mRecordProtoConverter.toRecordProto(restoredSession))
                .isEqualTo(exerciseSessionRecord);
    }

    @Test
    public void restoreChanges_exerciseSession_withTrainingPlanInChanges_keepsReference() {
        Record plannedExerciseSessionRecord =
                generateRecord(RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION);
        Record exerciseSessionRecord =
                generateCoreRecord()
                        .setIntervalRecord(
                                generateIntervalRecord()
                                        .setExerciseSession(
                                                generateExerciseSession().toBuilder()
                                                        .setPlannedExerciseSessionId(
                                                                plannedExerciseSessionRecord
                                                                        .getUuid())))
                        .build();

        mCloudRestoreManager.restoreChanges(
                List.of(
                        new RestoreChange(
                                BackupData.newBuilder()
                                        .setRecord(plannedExerciseSessionRecord)
                                        .build()
                                        .toByteArray()),
                        new RestoreChange(
                                BackupData.newBuilder()
                                        .setRecord(exerciseSessionRecord)
                                        .build()
                                        .toByteArray())));

        var restoredSession = readExerciseSession(exerciseSessionRecord.getUuid());
        assertThat(mRecordProtoConverter.toRecordProto(restoredSession))
                .isEqualTo(exerciseSessionRecord);
    }

    @Test
    public void restoreChanges_exerciseSession_withTrainingPlanRestoredEarlier_keepsReference() {
        Record plannedExerciseSessionRecord =
                generateRecord(RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION);
        mCloudRestoreManager.restoreChanges(
                List.of(
                        new RestoreChange(
                                BackupData.newBuilder()
                                        .setRecord(plannedExerciseSessionRecord)
                                        .build()
                                        .toByteArray())));
        Record exerciseSessionRecord =
                generateCoreRecord()
                        .setIntervalRecord(
                                generateIntervalRecord()
                                        .setExerciseSession(
                                                generateExerciseSession().toBuilder()
                                                        .setPlannedExerciseSessionId(
                                                                plannedExerciseSessionRecord
                                                                        .getUuid())))
                        .build();
        mCloudRestoreManager.restoreChanges(
                List.of(
                        new RestoreChange(
                                BackupData.newBuilder()
                                        .setRecord(exerciseSessionRecord)
                                        .build()
                                        .toByteArray())));

        var restoredSession = readExerciseSession(exerciseSessionRecord.getUuid());
        assertThat(mRecordProtoConverter.toRecordProto(restoredSession))
                .isEqualTo(exerciseSessionRecord);
    }

    @Test
    public void restoreInvalidSettings_throwsException() {
        BackupMetadata backupSettings = new BackupMetadata(new byte[] {45, 36});
        assertThrows(
                IllegalArgumentException.class,
                () -> mCloudRestoreManager.restoreSettings(backupSettings));
    }

    @Test
    public void restoreNotSupportedSettings_keepsOriginalSettings() {
        setupInitialSettings();

        int invalidEnumValue = 999;

        Settings settings =
                Settings.newBuilder()
                        .setEnergyUnitSettingValue(invalidEnumValue)
                        .setTemperatureUnitSettingValue(invalidEnumValue)
                        .setHeightUnitSettingValue(invalidEnumValue)
                        .setWeightUnitSettingValue(invalidEnumValue)
                        .setDistanceUnitSettingValue(invalidEnumValue)
                        .build();

        BackupMetadata backupSettings = new BackupMetadata(settings.toByteArray());
        mCloudRestoreManager.restoreSettings(backupSettings);

        assertThat(mPreferenceHelper.getPreference(AUTO_DELETE_DURATION_RECORDS_KEY))
                .isEqualTo("90");
    }

    @Test
    public void restoreInvalidChanges_skipsInvalidChange() {
        RestoreChange restoreChange = new RestoreChange(new byte[] {45, 36});
        // test that no exceptions are thrown
        mCloudRestoreManager.restoreChanges(List.of(restoreChange));
    }

    private void setupInitialSettings() {
        mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(TEST_PACKAGE_NAME, "app name 1");
        mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(TEST_PACKAGE_NAME_2, "app name 2");
        mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(TEST_PACKAGE_NAME_3, "app name 3");
        mPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(TEST_PACKAGE_NAME, TEST_PACKAGE_NAME_2));
        mPreferenceHelper.insertOrReplacePreference(AUTO_DELETE_DURATION_RECORDS_KEY, "90");
    }

    private Settings createSettingsToRestore() {
        Map<String, Settings.AppInfo> appInfoMap =
                Map.of(
                        TEST_PACKAGE_NAME,
                                Settings.AppInfo.newBuilder().setAppName("app name 1").build(),
                        TEST_PACKAGE_NAME_2,
                                Settings.AppInfo.newBuilder().setAppName("app name 2").build(),
                        TEST_PACKAGE_NAME_3,
                                Settings.AppInfo.newBuilder().setAppName("app name 3").build());

        Map<Integer, PriorityList> priorityListMap =
                Map.of(
                        HealthDataCategory.ACTIVITY,
                        PriorityList.newBuilder()
                                .addPackageName(TEST_PACKAGE_NAME_2)
                                .addPackageName(TEST_PACKAGE_NAME_3)
                                .addPackageName(TEST_PACKAGE_NAME)
                                .build());

        return Settings.newBuilder()
                .putAllAppInfo(appInfoMap)
                .putAllPriorityList(priorityListMap)
                .setAutoDeleteFrequencyInDays("30")
                .build();
    }

    private void assertSettingsCorrectlyUpdated(
            Settings settingsFromBackup,
            Settings restoredSettings,
            Map<Integer, PriorityList> expectedMergedPriorityList) {

        assertThat(settingsFromBackup.getAutoDeleteFrequencyInDays())
                .isEqualTo(restoredSettings.getAutoDeleteFrequencyInDays());

        assertThat(expectedMergedPriorityList.get(HealthDataCategory.ACTIVITY).getPackageNameList())
                .isEqualTo(
                        restoredSettings
                                .getPriorityListMap()
                                .get(HealthDataCategory.ACTIVITY)
                                .getPackageNameList());
    }

    @NotNull
    private RecordInternal<?> readExerciseSession(String sessionId) {
        List<RecordInternal<?>> records =
                mFitnessRecordReadHelper.readRecordsUnrestricted(
                        mTransactionManager,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION,
                                List.of(UUID.fromString(sessionId))));
        assertThat(records.size()).isEqualTo(1);
        return records.get(0);
    }
}
