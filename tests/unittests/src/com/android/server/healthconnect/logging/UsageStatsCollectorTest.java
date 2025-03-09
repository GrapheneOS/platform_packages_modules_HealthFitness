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

package com.android.server.healthconnect.logging;

import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_CONDITIONS;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.WRITE_STEPS;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.server.healthconnect.logging.UsageStatsCollector.EXPORT_PERIOD_PREFERENCE_KEY;
import static com.android.server.healthconnect.logging.UsageStatsCollector.USER_MOST_RECENT_ACCESS_LOG_TIME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectManager;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.storage.utils.PreferencesManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UsageStatsCollectorTest {

    @Rule(order = 1)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule(order = 2)
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .mockStatic(HealthConnectManager.class)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    @Mock private MedicalResourceHelper mMedicalResourceHelper;
    @Mock private MedicalDataSourceHelper mMedicalDataSourceHelper;
    @Mock private AccessLogsHelper mAccessLogsHelper;
    @Mock private PreferencesManager mPreferencesManager;
    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private PackageManager mPackageManager;
    @Mock private PackageInfo mPackageInfoConnectedApp;
    @Mock private PackageInfo mPackageInfoPhrConnectedApp;
    @Mock private PackageInfo mPackageInfoNotHoldingPermission;
    @Mock private PackageInfo mPackageInfoNotConnectedApp;
    private UsageStatsCollector mUsageStatsCollector;

    private static final String NOT_HEALTH_PERMISSION = "NOT_HEALTH_PERMISSION";
    private static final String CONNECTED_APP_PACKAGE_NAME = "connected.app";
    private static final String PHR_CONNECTED_APP_PACKAGE_NAME = "phr.connected.app";
    private static final String NOT_CONNECTED_APP_PACKAGE_NAME = "not.connected.app";
    private static final String NOT_HOLDING_HC_PERMISSIONS_APP_PACKAGE_NAME =
            "not.holding.permission.app";

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        Context context = spy(ApplicationProvider.getApplicationContext());
        UserHandle userHandle = ApplicationProvider.getApplicationContext().getUser();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setPreferencesManager(mPreferencesManager)
                        .setPreferenceHelper(mPreferenceHelper)
                        .setAccessLogsHelper(mAccessLogsHelper)
                        .setMedicalDataSourceHelper(mMedicalDataSourceHelper)
                        .setMedicalResourceHelper(mMedicalResourceHelper)
                        .build();
        doReturn(context).when(context).createContextAsUser(eq(userHandle), anyInt());
        mUsageStatsCollector =
                healthConnectInjector.getUsageStatsCollector(
                        HealthConnectContext.create(
                                context,
                                userHandle,
                                /* databaseDirName= */ null,
                                healthConnectInjector.getEnvironmentDataDirectory()));

        doReturn(mPackageManager).when(context).getPackageManager();
        doReturn(
                        List.of(
                                mPackageInfoConnectedApp,
                                mPackageInfoNotHoldingPermission,
                                mPackageInfoNotConnectedApp,
                                mPackageInfoPhrConnectedApp))
                .when(mPackageManager)
                .getInstalledPackages(any());
        ExtendedMockito.doReturn(Set.of(READ_STEPS, WRITE_STEPS, READ_MEDICAL_DATA_CONDITIONS))
                .when(() -> HealthConnectManager.getHealthPermissions(any()));

        mPackageInfoConnectedApp.requestedPermissions = new String[] {READ_STEPS, WRITE_STEPS};
        mPackageInfoConnectedApp.requestedPermissionsFlags =
                new int[] {
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        mPackageInfoConnectedApp.packageName = CONNECTED_APP_PACKAGE_NAME;

        mPackageInfoNotHoldingPermission.requestedPermissions =
                new String[] {NOT_HEALTH_PERMISSION};
        mPackageInfoNotHoldingPermission.requestedPermissionsFlags =
                new int[] {PackageInfo.REQUESTED_PERMISSION_GRANTED};
        mPackageInfoNotHoldingPermission.packageName = NOT_HOLDING_HC_PERMISSIONS_APP_PACKAGE_NAME;

        mPackageInfoNotConnectedApp.requestedPermissions = new String[] {READ_STEPS};
        mPackageInfoNotConnectedApp.requestedPermissionsFlags =
                new int[] {PackageInfo.REQUESTED_PERMISSION_NEVER_FOR_LOCATION};
        mPackageInfoNotConnectedApp.packageName = NOT_CONNECTED_APP_PACKAGE_NAME;

        mPackageInfoPhrConnectedApp.requestedPermissions =
                new String[] {READ_MEDICAL_DATA_CONDITIONS, WRITE_STEPS};
        mPackageInfoPhrConnectedApp.requestedPermissionsFlags =
                new int[] {
                    PackageInfo.REQUESTED_PERMISSION_GRANTED,
                    PackageInfo.REQUESTED_PERMISSION_GRANTED
                };
        mPackageInfoPhrConnectedApp.packageName = PHR_CONNECTED_APP_PACKAGE_NAME;
    }

    @Test
    public void testGetNumberOfCompatibleApps() {
        assertThat(mUsageStatsCollector.getNumberOfAppsCompatibleWithHealthConnect()).isEqualTo(3);
    }

    @Test
    public void testGetAppsConnectedWithHealthConnect() {
        Map<String, List<String>> expectedResult =
                Map.of(
                        CONNECTED_APP_PACKAGE_NAME,
                        List.of(READ_STEPS, WRITE_STEPS),
                        PHR_CONNECTED_APP_PACKAGE_NAME,
                        List.of(READ_MEDICAL_DATA_CONDITIONS, WRITE_STEPS));

        assertThat(mUsageStatsCollector.getPackagesHoldingHealthPermissions())
                .isEqualTo(expectedResult);
    }

    @Test
    public void phrLastReadTimeStampNull_testUserIsNotPhrMonthlyActiveUser() {
        when(mPreferencesManager.getPhrLastReadMedicalResourcesApiTimeStamp()).thenReturn(null);

        assertThat(mUsageStatsCollector.isPhrMonthlyActiveUser()).isEqualTo(false);
    }

    @Test
    public void phrLastReadTimeStampBefore30Days_testUserIsNotPhrMonthlyActiveUser() {
        when(mPreferencesManager.getPhrLastReadMedicalResourcesApiTimeStamp())
                .thenReturn(subtractDaysToInstantNow(/* days= */ 40));

        assertThat(mUsageStatsCollector.isPhrMonthlyActiveUser()).isEqualTo(false);
    }

    @Test
    public void phrLastReadTimeStampWithin30Days_testUserIsPhrMonthlyActiveUser() {
        when(mPreferencesManager.getPhrLastReadMedicalResourcesApiTimeStamp())
                .thenReturn(subtractDaysToInstantNow(/* days= */ 10));

        assertThat(mUsageStatsCollector.isPhrMonthlyActiveUser()).isEqualTo(true);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void flagEnabled_testConnectedPhrAppsCount() {
        assertThat(mUsageStatsCollector.getGrantedPhrAppsCount()).isEqualTo(1);
    }

    @Test
    @DisableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void flagDisabled_testConnectedPhrAppsCount() {
        assertThat(mUsageStatsCollector.getGrantedPhrAppsCount()).isEqualTo(0);
    }

    @Test
    public void exportFrequencyPreferenceNull_testExportFrequency() {
        when(mPreferenceHelper.getPreference(eq(EXPORT_PERIOD_PREFERENCE_KEY))).thenReturn(null);

        assertThat(mUsageStatsCollector.getExportFrequency()).isEqualTo(0);
    }

    @Test
    public void exportFrequencyPreferenceNonNull_testExportFrequency() {
        when(mPreferenceHelper.getPreference(eq(EXPORT_PERIOD_PREFERENCE_KEY)))
                .thenReturn(String.valueOf(1));

        assertThat(mUsageStatsCollector.getExportFrequency()).isEqualTo(1);
    }

    @Test
    public void mostRecentAccessLogReadTimeStampNull_testUserIsNotMonthlyActiveUser() {
        when(mPreferenceHelper.getPreference(eq(USER_MOST_RECENT_ACCESS_LOG_TIME)))
                .thenReturn(null);

        assertThat(mUsageStatsCollector.isUserMonthlyActive()).isEqualTo(false);
    }

    @Test
    public void mostRecentAccessLogReadTimeStampBefore30Days_testUserIsNotMonthlyActiveUser() {
        when(mPreferenceHelper.getPreference(eq(USER_MOST_RECENT_ACCESS_LOG_TIME)))
                .thenReturn(
                        String.valueOf(subtractDaysToInstantNow(/* days= */ 40).toEpochMilli()));

        assertThat(mUsageStatsCollector.isUserMonthlyActive()).isEqualTo(false);
    }

    @Test
    public void mostRecentAccessLogReadTimeStampWithin30Days_testUserIsNotMonthlyActiveUser() {
        when(mPreferenceHelper.getPreference(eq(USER_MOST_RECENT_ACCESS_LOG_TIME)))
                .thenReturn(
                        String.valueOf(subtractDaysToInstantNow(/* days= */ 10).toEpochMilli()));

        assertThat(mUsageStatsCollector.isUserMonthlyActive()).isEqualTo(true);
    }

    @Test
    public void noAccessLog_testDoNotUpdateLatestAccessLogsTimeStamp() {
        when(mAccessLogsHelper.getLatestUpsertOrReadOperationAccessLogTimeStamp())
                .thenReturn(Long.MIN_VALUE);

        mUsageStatsCollector.upsertLastAccessLogTimeStamp();

        verify(mPreferenceHelper, times(0)).insertOrReplacePreference(any(), any());
    }

    @Test
    public void accessLogPresent_testUpdateLatestAccessLogsTimeStamp() {
        when(mAccessLogsHelper.getLatestUpsertOrReadOperationAccessLogTimeStamp()).thenReturn(1L);

        mUsageStatsCollector.upsertLastAccessLogTimeStamp();

        verify(mPreferenceHelper, times(1))
                .insertOrReplacePreference(
                        eq(USER_MOST_RECENT_ACCESS_LOG_TIME), eq(String.valueOf(1L)));
    }

    @Test
    public void testGetMedicalResourcesCount() {
        when(mMedicalResourceHelper.getMedicalResourcesCount()).thenReturn(1);

        assertThat(mUsageStatsCollector.getMedicalResourcesCount()).isEqualTo(1);
    }

    @Test
    public void testGetMedicalDataSourcesCount() {
        when(mMedicalDataSourceHelper.getMedicalDataSourcesCount()).thenReturn(1);

        assertThat(mUsageStatsCollector.getMedicalDataSourcesCount()).isEqualTo(1);
    }

    private Instant subtractDaysToInstantNow(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }
}
