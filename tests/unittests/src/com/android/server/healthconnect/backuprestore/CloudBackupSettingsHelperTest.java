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

import static com.android.server.healthconnect.common.preferences.PreferencesManager.AUTO_DELETE_DURATION_RECORDS_KEY;
import static com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.DistanceUnitProto;
import static com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.EnergyUnitProto;
import static com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.HeightUnitProto;
import static com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.TemperatureUnitProto;
import static com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.WeightUnitProto;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.HealthDataCategory;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.healthconnect.testing.unittest.fakes.FakePreferenceHelper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.AppInfo;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.PriorityList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class CloudBackupSettingsHelperTest {

    private static final String TEST_PACKAGE_NAME = "package.name";
    private static final String TEST_NEW_PACKAGE_NAME = "new.package.name";
    private static final String TEST_APP_NAME = "app.name";
    private static final String TEST_PACKAGE_NAME_2 = "other.app";
    private static final String TEST_PACKAGE_NAME_3 = "another.app";
    private static final String TEST_PACKAGE_NAME_4 = "not.installed.app";

    private PreferenceHelper mPreferenceHelper;
    private HealthDataCategoryPriorityHelper mPriorityHelper;

    private AppInfoHelper mAppInfoHelper;
    private CloudBackupSettingsHelper mCloudBackupSettingsHelper;

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;

    @Before
    public void setUp() throws Exception {
        HealthConnectInjector.resetInstanceForTest();

        Context context = ApplicationProvider.getApplicationContext();
        mPreferenceHelper = new FakePreferenceHelper();

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setPreferenceHelper(mPreferenceHelper)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        FitnessTestUtils fitnessTestUtils = new FitnessTestUtils(healthConnectInjector);
        fitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
        fitnessTestUtils.insertApp(TEST_PACKAGE_NAME_2);
        fitnessTestUtils.insertApp(TEST_PACKAGE_NAME_3);

        mPriorityHelper = healthConnectInjector.getHealthDataCategoryPriorityHelper();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mCloudBackupSettingsHelper =
                new CloudBackupSettingsHelper(mPriorityHelper, mPreferenceHelper, mAppInfoHelper);
    }

    @Test
    public void emptyPriorityList_setsPriorityListAsEmptyList() {
        Map<Integer, List<Long>> priorityMapImmutable =
                mPriorityHelper.getHealthDataCategoryToAppIdPriorityMapImmutable();
        assertThat(priorityMapImmutable).isEmpty();

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        Map<Integer, PriorityList> actualResult = userSettings.getPriorityListMap();
        assertThat(actualResult).isEmpty();
    }

    @Test
    public void oneCategoryPriorityList_setsPriorityListCorrectly() {
        mPriorityHelper.setPriorityOrder(
                HealthDataCategory.ACTIVITY, List.of(TEST_PACKAGE_NAME, TEST_PACKAGE_NAME_2));

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        Map<Integer, PriorityList> actualPriorityList = userSettings.getPriorityListMap();
        Map<Integer, PriorityList> expectedPriorityList =
                Map.of(
                        HealthDataCategory.ACTIVITY,
                        PriorityList.newBuilder()
                                .addPackageName(TEST_PACKAGE_NAME)
                                .addPackageName(TEST_PACKAGE_NAME_2)
                                .build());
        assertThat(actualPriorityList).isEqualTo(expectedPriorityList);
    }

    @Test
    public void appInfoPresent_setsAppInfoCorrectly() {
        mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(TEST_NEW_PACKAGE_NAME, TEST_APP_NAME);

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        Map<String, AppInfo> appInfoMap = userSettings.getAppInfoMap();

        // The first two packages are inserted during the setup.
        Map<String, AppInfo> expectedAppInfoMap =
                Map.of(
                        TEST_PACKAGE_NAME,
                        AppInfo.getDefaultInstance(),
                        TEST_PACKAGE_NAME_2,
                        AppInfo.getDefaultInstance(),
                        TEST_PACKAGE_NAME_3,
                        AppInfo.getDefaultInstance(),
                        TEST_NEW_PACKAGE_NAME,
                        AppInfo.newBuilder().setAppName(TEST_APP_NAME).build());

        assertThat(appInfoMap).isEqualTo(expectedAppInfoMap);
    }

    @Test
    public void appInfoCleared_restoresAppInfoCorrectly() {
        Map<String, AppInfo> appInfoToRestore =
                Map.of(
                        TEST_PACKAGE_NAME,
                        AppInfo.getDefaultInstance(),
                        TEST_PACKAGE_NAME_2,
                        AppInfo.getDefaultInstance(),
                        TEST_PACKAGE_NAME_3,
                        AppInfo.getDefaultInstance(),
                        TEST_PACKAGE_NAME_4,
                        AppInfo.getDefaultInstance(),
                        TEST_NEW_PACKAGE_NAME,
                        AppInfo.newBuilder().setAppName(TEST_APP_NAME).build());

        mCloudBackupSettingsHelper.restoreAppInfo(appInfoToRestore);

        Map<String, AppInfo> restoredAppInfo =
                mCloudBackupSettingsHelper.collectUserSettings().getAppInfoMap();

        assertThat(restoredAppInfo).isEqualTo(appInfoToRestore);
    }

    @Test
    public void defaultUnitPreferences_keepAllEnumsUnspecified() {
        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        assertThat(userSettings.getTemperatureUnitSetting())
                .isEqualTo(TemperatureUnitProto.TEMPERATURE_UNIT_UNSPECIFIED);
        assertThat(userSettings.getEnergyUnitSetting())
                .isEqualTo(EnergyUnitProto.ENERGY_UNIT_UNSPECIFIED);
        assertThat(userSettings.getWeightUnitSetting())
                .isEqualTo(WeightUnitProto.WEIGHT_UNIT_UNSPECIFIED);
        assertThat(userSettings.getHeightUnitSetting())
                .isEqualTo(HeightUnitProto.HEIGHT_UNIT_UNSPECIFIED);
        assertThat(userSettings.getDistanceUnitSetting())
                .isEqualTo(DistanceUnitProto.DISTANCE_UNIT_UNSPECIFIED);
    }

    @Test
    public void autoDeleteSettingsOff_setsAutoDeleteSettingsCorrectly() {
        mPreferenceHelper.insertOrReplacePreference(
                AUTO_DELETE_DURATION_RECORDS_KEY, String.valueOf(0));

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        assertThat(userSettings.getAutoDeleteFrequencyInDays()).isEqualTo("0");
    }

    @Test
    public void autoDeleteSettingsOn_setsAutoDeleteSettingsCorrectly() {
        mPreferenceHelper.insertOrReplacePreference(
                AUTO_DELETE_DURATION_RECORDS_KEY, String.valueOf(90));

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        assertThat(userSettings.getAutoDeleteFrequencyInDays()).isEqualTo("90");
    }

    @Test
    public void autoDeleteSettingsNotSet_doesNotRestore() {
        mPreferenceHelper.removeKey(AUTO_DELETE_DURATION_RECORDS_KEY);
        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();
        mCloudBackupSettingsHelper.restoreUserSettings(userSettings);

        assertThat(mPreferenceHelper.getPreference(AUTO_DELETE_DURATION_RECORDS_KEY))
                .isEqualTo(null);
    }

    @Test
    public void autoDeleteSettingsOff_restoresAutoDeleteSettingsCorrectly() {
        mPreferenceHelper.insertOrReplacePreference(
                AUTO_DELETE_DURATION_RECORDS_KEY, String.valueOf(0));
        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();
        mPreferenceHelper.removeKey(AUTO_DELETE_DURATION_RECORDS_KEY);

        mCloudBackupSettingsHelper.restoreUserSettings(userSettings);

        assertThat(mPreferenceHelper.getPreference(AUTO_DELETE_DURATION_RECORDS_KEY))
                .isEqualTo(String.valueOf(0));
    }

    @Test
    public void autoDeleteSettingsOn_restoresAutoDeleteSettingsCorrectly() {
        mPreferenceHelper.insertOrReplacePreference(
                AUTO_DELETE_DURATION_RECORDS_KEY, String.valueOf(90));
        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();
        mPreferenceHelper.removeKey(AUTO_DELETE_DURATION_RECORDS_KEY);

        mCloudBackupSettingsHelper.restoreUserSettings(userSettings);

        assertThat(mPreferenceHelper.getPreference(AUTO_DELETE_DURATION_RECORDS_KEY))
                .isEqualTo(String.valueOf(90));
    }

    @Test
    public void priorityListsMergedCorrectly() {
        mPriorityHelper.setPriorityOrder(HealthDataCategory.ACTIVITY, List.of(TEST_PACKAGE_NAME));

        Map<Integer, PriorityList> importedPriorityList = new HashMap<>();
        importedPriorityList.put(
                HealthDataCategory.ACTIVITY,
                PriorityList.newBuilder().addPackageName(TEST_PACKAGE_NAME_2).build());
        importedPriorityList.put(
                HealthDataCategory.VITALS,
                PriorityList.newBuilder()
                        .addPackageName(TEST_PACKAGE_NAME_2)
                        .addPackageName(TEST_PACKAGE_NAME)
                        .build());

        mCloudBackupSettingsHelper.mergePriorityLists(importedPriorityList);

        assertThat(
                        mAppInfoHelper.getPackageNames(
                                mPriorityHelper.getAppIdPriorityOrder(HealthDataCategory.ACTIVITY)))
                .isEqualTo(List.of(TEST_PACKAGE_NAME, TEST_PACKAGE_NAME_2));
        assertThat(
                        mAppInfoHelper.getPackageNames(
                                mPriorityHelper.getAppIdPriorityOrder(HealthDataCategory.VITALS)))
                .isEqualTo(List.of(TEST_PACKAGE_NAME_2, TEST_PACKAGE_NAME));
    }
}
