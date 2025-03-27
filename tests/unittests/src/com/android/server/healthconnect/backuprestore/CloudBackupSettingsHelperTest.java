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

import static com.android.server.healthconnect.backuprestore.CloudBackupSettingsHelper.AUTO_DELETE_PREF_KEY;
import static com.android.server.healthconnect.backuprestore.CloudBackupSettingsHelper.DISTANCE_UNIT_PREF_KEY;
import static com.android.server.healthconnect.backuprestore.CloudBackupSettingsHelper.ENERGY_UNIT_PREF_KEY;
import static com.android.server.healthconnect.backuprestore.CloudBackupSettingsHelper.HEIGHT_UNIT_PREF_KEY;
import static com.android.server.healthconnect.backuprestore.CloudBackupSettingsHelper.TEMPERATURE_UNIT_PREF_KEY;
import static com.android.server.healthconnect.backuprestore.CloudBackupSettingsHelper.WEIGHT_UNIT_PREF_KEY;
import static com.android.server.healthconnect.proto.backuprestore.Settings.AutoDeleteFrequencyProto;
import static com.android.server.healthconnect.proto.backuprestore.Settings.DistanceUnitProto;
import static com.android.server.healthconnect.proto.backuprestore.Settings.EnergyUnitProto;
import static com.android.server.healthconnect.proto.backuprestore.Settings.HeightUnitProto;
import static com.android.server.healthconnect.proto.backuprestore.Settings.TemperatureUnitProto;
import static com.android.server.healthconnect.proto.backuprestore.Settings.WeightUnitProto;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.HealthDataCategory;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.proto.backuprestore.Settings;
import com.android.server.healthconnect.proto.backuprestore.Settings.AppInfo;
import com.android.server.healthconnect.proto.backuprestore.Settings.PriorityList;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.testing.fakes.FakePreferenceHelper;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

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
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    @Before
    public void setUp() throws Exception {
        HealthConnectInjector.resetInstanceForTest();

        Context context = ApplicationProvider.getApplicationContext();
        mPreferenceHelper = new FakePreferenceHelper();

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setPreferenceHelper(mPreferenceHelper)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        TransactionTestUtils transactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        transactionTestUtils.insertApp(TEST_PACKAGE_NAME);
        transactionTestUtils.insertApp(TEST_PACKAGE_NAME_2);
        transactionTestUtils.insertApp(TEST_PACKAGE_NAME_3);

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
    public void defaultUnitPreferences_setsUnitPreferencesCorrectly() {
        mPreferenceHelper.insertOrReplacePreference(
                TEMPERATURE_UNIT_PREF_KEY, TemperatureUnitProto.CELSIUS.toString());
        mPreferenceHelper.insertOrReplacePreference(
                ENERGY_UNIT_PREF_KEY, EnergyUnitProto.CALORIE.toString());
        mPreferenceHelper.insertOrReplacePreference(
                HEIGHT_UNIT_PREF_KEY, HeightUnitProto.CENTIMETERS.toString());
        mPreferenceHelper.insertOrReplacePreference(
                WEIGHT_UNIT_PREF_KEY, WeightUnitProto.POUND.toString());
        mPreferenceHelper.insertOrReplacePreference(
                DISTANCE_UNIT_PREF_KEY, DistanceUnitProto.KILOMETERS.toString());

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        assertThat(userSettings.getTemperatureUnitSetting())
                .isEqualTo(TemperatureUnitProto.CELSIUS);
        assertThat(userSettings.getEnergyUnitSetting()).isEqualTo(EnergyUnitProto.CALORIE);
        assertThat(userSettings.getWeightUnitSetting()).isEqualTo(WeightUnitProto.POUND);
        assertThat(userSettings.getHeightUnitSetting()).isEqualTo(HeightUnitProto.CENTIMETERS);
        assertThat(userSettings.getDistanceUnitSetting()).isEqualTo(DistanceUnitProto.KILOMETERS);
    }

    @Test
    public void nonDefaultUnitPreference_setsUnitPreferencesCorrectly() {
        mPreferenceHelper.insertOrReplacePreference(
                TEMPERATURE_UNIT_PREF_KEY, TemperatureUnitProto.KELVIN.toString());
        mPreferenceHelper.insertOrReplacePreference(
                ENERGY_UNIT_PREF_KEY, EnergyUnitProto.KILOJOULE.toString());
        mPreferenceHelper.insertOrReplacePreference(
                HEIGHT_UNIT_PREF_KEY, HeightUnitProto.FEET.toString());
        mPreferenceHelper.insertOrReplacePreference(
                WEIGHT_UNIT_PREF_KEY, WeightUnitProto.POUND.toString());
        mPreferenceHelper.insertOrReplacePreference(
                DISTANCE_UNIT_PREF_KEY, DistanceUnitProto.MILES.toString());

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        assertThat(userSettings.getTemperatureUnitSetting()).isEqualTo(TemperatureUnitProto.KELVIN);
        assertThat(userSettings.getEnergyUnitSetting()).isEqualTo(EnergyUnitProto.KILOJOULE);
        assertThat(userSettings.getWeightUnitSetting()).isEqualTo(WeightUnitProto.POUND);
        assertThat(userSettings.getHeightUnitSetting()).isEqualTo(HeightUnitProto.FEET);
        assertThat(userSettings.getDistanceUnitSetting()).isEqualTo(DistanceUnitProto.MILES);
    }

    @Test
    public void autoDeleteSettingsOff_setsAutoDeleteSettingsCorrectly() {
        mPreferenceHelper.insertOrReplacePreference(
                AUTO_DELETE_PREF_KEY, AutoDeleteFrequencyProto.AUTO_DELETE_RANGE_NEVER.toString());

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        assertThat(userSettings.getAutoDeleteFrequency())
                .isEqualTo(AutoDeleteFrequencyProto.AUTO_DELETE_RANGE_NEVER);
    }

    @Test
    public void autoDeleteSettingsThreeMonths_setsAutoDeleteSettingsCorrectly() {
        mPreferenceHelper.insertOrReplacePreference(
                AUTO_DELETE_PREF_KEY,
                AutoDeleteFrequencyProto.AUTO_DELETE_RANGE_THREE_MONTHS.toString());

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        assertThat(userSettings.getAutoDeleteFrequency())
                .isEqualTo(AutoDeleteFrequencyProto.AUTO_DELETE_RANGE_THREE_MONTHS);
    }

    @Test
    public void autoDeleteSettingsEighteenMonths_setsAutoDeleteSettingsCorrectly() {
        mPreferenceHelper.insertOrReplacePreference(
                AUTO_DELETE_PREF_KEY,
                AutoDeleteFrequencyProto.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS.toString());

        Settings userSettings = mCloudBackupSettingsHelper.collectUserSettings();

        assertThat(userSettings.getAutoDeleteFrequency())
                .isEqualTo(AutoDeleteFrequencyProto.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS);
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
