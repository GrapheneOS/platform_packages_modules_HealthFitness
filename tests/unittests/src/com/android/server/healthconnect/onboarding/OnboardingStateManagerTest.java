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

package com.android.server.healthconnect.onboarding;

import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_HIDE;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED;
import static android.health.connect.HealthConnectOnboardingState.ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED;
import static android.health.connect.HealthPermissionCategory.ACTIVE_CALORIES_BURNED;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_READ;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SPEED;

import static com.android.server.healthconnect.onboarding.OnboardingStateManager.ONBOARDING_STATE_PREFERENCE_KEY_PREFIX;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.health.connect.HealthConnectOnboardingState;
import android.health.connect.HealthPermissions;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.internal.datatypes.AppInfoInternal;
import android.os.UserHandle;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.PackageInfoUtils;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

/** Test class for the OnboardingStateManager class. */
@RunWith(AndroidJUnit4.class)
public class OnboardingStateManagerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private HealthConnectPermissionHelper mHealthConnectPermissionHelper;
    @Mock private MockListener mMockListener;
    @Mock private UserHandle mUserHandle;
    @Mock private AppInfoHelper mAppInfoHelper;
    @Mock private AccessLogsHelper mAccessLogsHelper;
    @Mock PackageInfoUtils mPackageInfoUtils;

    private OnboardingStateManager mOnboardingStateManager;
    private Map<String, AppInfoInternal> mFakeAppInfoMap;
    private List<AccessLog> mFakeAccessLogs;
    private Context mContext;

    private static final int USER_ID_INT = (int) (Math.random() * 100);
    private static final String PREF_KEY = ONBOARDING_STATE_PREFERENCE_KEY_PREFIX + USER_ID_INT;
    private static final String APP_PKG_1 = "com.example.app1";
    private static final String APP_PKG_2 = "com.example.app2";
    private static final String APP_PKG_3 = "com.example.app3";
    private static final String APP_PKG_4 = "com.example.app4";
    private static final String SYSTEM_APP_PKG = "com.system.app";
    private static final Instant NOW = Instant.now();
    private static final long EIGHT_DAYS_AGO = NOW.minus(Duration.ofDays(8)).toEpochMilli();
    private static final long SEVEN_DAYS_AGO = NOW.minus(Duration.ofDays(7)).toEpochMilli();
    private static final long SIX_DAYS_AGO = NOW.minus(Duration.ofDays(6)).toEpochMilli();

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        setAppConnectedFitnessPermission(APP_PKG_1, /* isConnected= */ false);
        setAppConnectedFitnessPermission(APP_PKG_2, /* isConnected= */ false);
        setAppConnectedFitnessPermission(APP_PKG_3, /* isConnected= */ false);
        setAppConnectedFitnessPermission(APP_PKG_4, /* isConnected= */ false);

        when(mHealthConnectPermissionHelper.hasNonUserSensitiveHealthPermission(
                        eq(SYSTEM_APP_PKG), any(), any()))
                .thenReturn(true);

        mFakeAppInfoMap = new HashMap<>(5);
        mFakeAppInfoMap.put(APP_PKG_1, createAppInfo(APP_PKG_1, false));
        mFakeAppInfoMap.put(APP_PKG_2, createAppInfo(APP_PKG_2, false));
        mFakeAppInfoMap.put(APP_PKG_3, createAppInfo(APP_PKG_3, false));
        mFakeAppInfoMap.put(APP_PKG_4, createAppInfo(APP_PKG_4, false));
        mFakeAppInfoMap.put(SYSTEM_APP_PKG, createAppInfo(SYSTEM_APP_PKG, false));
        when(mAppInfoHelper.getAppInfoMap()).thenReturn(mFakeAppInfoMap);

        mFakeAccessLogs = new ArrayList<>();
        when(mAccessLogsHelper.queryAccessLogs(eq(mUserHandle))).thenReturn(mFakeAccessLogs);

        when(mUserHandle.getIdentifier()).thenReturn(USER_ID_INT);

        mOnboardingStateManager =
                new OnboardingStateManager(
                        mContext,
                        mPreferenceHelper,
                        mHealthConnectPermissionHelper,
                        mPackageInfoUtils,
                        mAppInfoHelper,
                        mAccessLogsHelper,
                        mUserHandle);
        mOnboardingStateManager.setupForUser(mUserHandle);
        mOnboardingStateManager.addStateChangedListener(mMockListener::onOnboardingStateChanged);
    }

    @After
    public void tearDown() throws TimeoutException {
        clearInvocations(mPreferenceHelper, mMockListener);
        mFakeAppInfoMap.clear();
        mFakeAccessLogs.clear();
    }

    @Test
    public void testGetOnboardingState_returnsCorrectStateFromPreference() {
        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);

        int state = mOnboardingStateManager.getOnboardingState();

        assertEquals(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED, state);
    }

    @Test
    public void testGetOnboardingState_returnsHideWhenPreferenceIsNull() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(null);

        int state = mOnboardingStateManager.getOnboardingState();

        assertEquals(ONBOARDING_BANNER_STATE_HIDE, state);
    }

    @Test
    public void updateAndGetOnboardingState_noCompatibleApps_returnsHide() {
        setCompatibleApps(emptyList());

        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        clearInvocations(mPreferenceHelper);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_HIDE);
        verifyStateChange(ONBOARDING_BANNER_STATE_HIDE);
    }

    @Test
    public void updateAndGetOnboardingState_allAppsConnected_returnsHide() {
        setAppConnectedFitnessPermission(APP_PKG_1, /* isConnected= */ true);
        setAppConnectedFitnessPermission(APP_PKG_2, /* isConnected= */ true);
        setAppConnectedFitnessPermission(APP_PKG_3, /* isConnected= */ true);
        setAppConnectedFitnessPermission(APP_PKG_4, /* isConnected= */ true);

        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO),
                        createPackageInfo(APP_PKG_2, SEVEN_DAYS_AGO),
                        createPackageInfo(APP_PKG_3, SIX_DAYS_AGO),
                        createPackageInfo(APP_PKG_4, NOW.toEpochMilli())));

        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
        clearInvocations(mPreferenceHelper, mMockListener);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_HIDE);
        verifyStateChange(ONBOARDING_BANNER_STATE_HIDE);
    }

    @Test
    public void updateAndGetOnboardingState_onlyAppsWithoutFitnessPermissions_returnsHide() {
        setAppConnectedFitnessPermission(APP_PKG_1, /* isConnected= */ false);
        setAppConnectedFitnessPermission(APP_PKG_2, /* isConnected= */ false);
        setAppConnectedFitnessPermission(APP_PKG_3, /* isConnected= */ false);
        setAppRequestsFitnessPermission(APP_PKG_1, false);
        setAppRequestsFitnessPermission(APP_PKG_2, false);
        setAppRequestsFitnessPermission(APP_PKG_3, false);

        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO),
                        createPackageInfo(APP_PKG_2, SEVEN_DAYS_AGO),
                        createPackageInfo(APP_PKG_3, SIX_DAYS_AGO)));
        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
        clearInvocations(mPreferenceHelper, mMockListener);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_HIDE);
        verifyStateChange(ONBOARDING_BANNER_STATE_HIDE);
    }

    @Test
    public void updateAndGetOnboardingState_twoConnectedApps_returnsHide() {
        setAppConnectedFitnessPermission(APP_PKG_1, /* isConnected= */ true);
        setAppConnectedFitnessPermission(APP_PKG_2, /* isConnected= */ true);

        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, SIX_DAYS_AGO), // connected
                        createPackageInfo(APP_PKG_2, SEVEN_DAYS_AGO), // connected
                        createPackageInfo(APP_PKG_3, EIGHT_DAYS_AGO), // candidate
                        createPackageInfo(APP_PKG_4, EIGHT_DAYS_AGO))); // candidate

        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        clearInvocations(mPreferenceHelper, mMockListener);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_HIDE);
        verifyStateChange(ONBOARDING_BANNER_STATE_HIDE);
    }

    @Test
    public void updateAndGetOnboardingState_zeroConnected_noCandidate_returnsHide() {
        setAppRequestsFitnessPermission(APP_PKG_1, true);
        setAppRequestsFitnessPermission(APP_PKG_2, true);
        setAppRequestsFitnessPermission(APP_PKG_3, true);
        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, SIX_DAYS_AGO), // too new
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO), // has data
                        createPackageInfo(APP_PKG_3, EIGHT_DAYS_AGO))); // has access log
        setAppUsedWithData(APP_PKG_2);
        setAppsUsedWithAccessLog(APP_PKG_3);

        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        clearInvocations(mPreferenceHelper, mMockListener);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_HIDE);
        verifyStateChange(ONBOARDING_BANNER_STATE_HIDE);
    }

    @Test
    public void updateAndGetOnboardingState_zeroConnected_oneCandidate_returnsHide() {
        setAppRequestsFitnessPermission(APP_PKG_1, true);
        setAppRequestsFitnessPermission(APP_PKG_2, true);
        setAppRequestsFitnessPermission(APP_PKG_3, true);
        setAppRequestsFitnessPermission(APP_PKG_4, true);
        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO), // candidate
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO), // has access log
                        createPackageInfo(APP_PKG_3, EIGHT_DAYS_AGO), // has data
                        createPackageInfo(APP_PKG_4, SIX_DAYS_AGO))); // too new
        setAppsUsedWithAccessLog(APP_PKG_2);
        setAppUsedWithData(APP_PKG_3);

        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        clearInvocations(mPreferenceHelper, mMockListener);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_HIDE);
        verifyStateChange(ONBOARDING_BANNER_STATE_HIDE);
    }

    @Test
    public void
            updateAndGetOnboardingState_zeroConnected_oneCandidateDeniedPermission_returnsHide() {
        setAppRequestsFitnessPermission(APP_PKG_1, true);
        setAppRequestsFitnessPermission(APP_PKG_2, true);
        setAppRequestsFitnessPermission(APP_PKG_3, true);
        setAppRequestsFitnessPermission(APP_PKG_4, true);
        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO), // candidate
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO), // has access log
                        createPackageInfo(APP_PKG_3, EIGHT_DAYS_AGO), // has data
                        createPackageInfo(APP_PKG_4, EIGHT_DAYS_AGO))); // has denied permission
        setAppsUsedWithAccessLog(APP_PKG_2);
        setAppUsedWithData(APP_PKG_3);
        setAppUsedWithPermission(APP_PKG_4, true);

        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        clearInvocations(mPreferenceHelper, mMockListener);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_HIDE);
        verifyStateChange(ONBOARDING_BANNER_STATE_HIDE);
    }

    @Test
    public void updateAndGetOnboardingState_zeroConnected_twoCandidates_returnsZeroConnected() {
        setAppRequestsFitnessPermission(APP_PKG_1, true);
        setAppRequestsFitnessPermission(APP_PKG_2, true);
        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO),
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO)));

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
        verifyStateChange(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
    }

    @Test
    public void updateAndGetOnboardingState_oneConnected_noCandidate_returnsHide() {
        setAppConnectedFitnessPermission(APP_PKG_1, /* isConnected= */ true);
        setAppRequestsFitnessPermission(APP_PKG_2, true);
        setAppRequestsFitnessPermission(APP_PKG_3, true);
        setAppRequestsFitnessPermission(APP_PKG_4, true);

        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, NOW.toEpochMilli()), // connected
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO), // has access log
                        createPackageInfo(APP_PKG_3, EIGHT_DAYS_AGO), // has data
                        createPackageInfo(APP_PKG_4, SIX_DAYS_AGO))); // too new

        setAppsUsedWithAccessLog(APP_PKG_2);
        setAppUsedWithData(APP_PKG_3);

        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        clearInvocations(mPreferenceHelper);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_HIDE);
        verifyStateChange(ONBOARDING_BANNER_STATE_HIDE);
    }

    @Test
    public void updateAndGetOnboardingState_oneConnected_oneCandidate_returnsOneConnected() {
        setAppConnectedFitnessPermission(APP_PKG_1, /* isConnected= */ true);
        setAppRequestsFitnessPermission(APP_PKG_2, true);

        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO), // connected
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO))); // candidate

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        verifyStateChange(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
    }

    @Test
    public void updateAndGetOnboardingState_zeroAppToOneAppConnected_updatesAndNotifies() {
        setAppConnectedFitnessPermission(APP_PKG_2, /* isConnected= */ true);
        setAppRequestsFitnessPermission(APP_PKG_1, true);
        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO), // candidate
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO))); // connected

        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
        clearInvocations(mPreferenceHelper, mMockListener);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        verifyStateChange(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
    }

    @Test
    public void updateAndGetOnboardingState_stateDoesNotChange_notNotified() {
        setAppConnectedFitnessPermission(APP_PKG_1, /* isConnected= */ true);
        setAppConnectedFitnessPermission(APP_PKG_2, /* isConnected= */ true);

        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO),
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO)));

        setOnboardingStateInPreference(ONBOARDING_BANNER_STATE_HIDE);
        clearInvocations(mPreferenceHelper, mMockListener);

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_HIDE);
        verifyNoStateChange();
    }

    @Test
    public void updateAndGetOnboardingState_onlySystemAppConnected_returnsZeroConnected() {
        setAppConnectedFitnessPermission(SYSTEM_APP_PKG, true);
        setAppRequestsFitnessPermission(APP_PKG_1, true);
        setAppRequestsFitnessPermission(APP_PKG_2, true);
        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO), // candidate
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO), // candidate
                        createPackageInfo(SYSTEM_APP_PKG, EIGHT_DAYS_AGO))); // system app

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
        verifyStateChange(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
    }

    @Test
    public void updateAndGetOnboardingState_systemAndOneOtherAppConnected_returnsOneConnected() {
        setAppConnectedFitnessPermission(SYSTEM_APP_PKG, true);
        setAppConnectedFitnessPermission(APP_PKG_1, /* isConnected= */ true);
        setAppRequestsFitnessPermission(APP_PKG_2, true);

        setCompatibleApps(
                ImmutableList.of(
                        createPackageInfo(SYSTEM_APP_PKG, EIGHT_DAYS_AGO), // system app
                        createPackageInfo(APP_PKG_1, EIGHT_DAYS_AGO), // connected
                        createPackageInfo(APP_PKG_2, EIGHT_DAYS_AGO))); // candidate

        assertThat(mOnboardingStateManager.updateAndGetOnboardingState())
                .isEqualTo(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        verifyStateChange(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
    }

    /**
     * Verifies that the preference was updated to the expected state and the listener was notified.
     */
    private void verifyStateChange(int expectedState) {
        verify(mPreferenceHelper)
                .insertOrReplacePreference(eq(PREF_KEY), eq(String.valueOf(expectedState)));
        verify(mMockListener).onOnboardingStateChanged(expectedState);
    }

    /** Verifies that the preference was not updated and the listener was not notified. */
    private void verifyNoStateChange() {
        verify(mPreferenceHelper, never()).insertOrReplacePreferencesTransaction(any());
        verify(mMockListener, never()).onOnboardingStateChanged(any(Integer.class));
    }

    /** Sets a mock onboarding state for the preference helper to return. */
    private void setOnboardingStateInPreference(
            @HealthConnectOnboardingState.OnboardingState int state) {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(String.valueOf(state));
    }

    /** Registers a list of packages as HC compatible apps. */
    private void setCompatibleApps(List<PackageInfo> compatibleApps) {
        when(mPackageInfoUtils.getPackagesCompatibleWithHealthConnect(mContext, mUserHandle))
                .thenReturn(compatibleApps);
    }

    /** Sets the connected state for the given package name. */
    private void setAppConnectedFitnessPermission(String packageName, boolean isConnected) {
        when(mHealthConnectPermissionHelper.hasGrantedFitnessPermission(
                        argThat(
                                argument ->
                                        argument != null
                                                && packageName.equals(argument.packageName))))
                .thenReturn(isConnected);
        if (isConnected) {
            setAppRequestsFitnessPermission(packageName, true);
        }
    }

    private void setAppRequestsFitnessPermission(String packageName, boolean requests) {
        when(mHealthConnectPermissionHelper.isRequestingFitnessPermission(
                        argThat(
                                argument ->
                                        argument != null
                                                && packageName.equals(argument.packageName))))
                .thenReturn(requests);
    }

    private void setAppUsedWithData(String packageName) {
        mFakeAppInfoMap.put(packageName, createAppInfo(packageName, /* isAppUsed= */ true));
    }

    private void setAppsUsedWithAccessLog(String packageName) {
        AccessLog log =
                new AccessLog(packageName, List.of(RECORD_TYPE_SPEED), 8765, OPERATION_TYPE_READ);
        mFakeAccessLogs.add(log);
    }

    private void setAppUsedWithPermission(String packageName, boolean isUsed) {
        when(mHealthConnectPermissionHelper.hasDeniedFitnessPermission(
                        argThat(
                                argument ->
                                        argument != null
                                                && packageName.equals(argument.packageName)),
                        any()))
                .thenReturn(isUsed);
    }

    /** Helper method to create a AppInfoInternal object. */
    private AppInfoInternal createAppInfo(String packageName, boolean isAppUsed) {
        return new AppInfoInternal(
                /* id= */ 0,
                packageName,
                "appName",
                /* icon= */ null,
                isAppUsed ? Set.of(ACTIVE_CALORIES_BURNED) : emptySet());
    }

    /** Helper method to create a PackageInfo object. */
    private PackageInfo createPackageInfo(String packageName, long firstInstallTime) {
        String[] defaultPermissions = {
            HealthPermissions.READ_ACTIVE_CALORIES_BURNED,
            HealthPermissions.READ_STEPS,
            HealthPermissions.WRITE_BLOOD_PRESSURE,
            HealthPermissions.READ_HEALTH_DATA_HISTORY,
            HealthPermissions.WRITE_MEDICAL_DATA
        };
        return createPackageInfo(packageName, firstInstallTime, defaultPermissions);
    }

    private PackageInfo createPackageInfo(
            String packageName, long firstInstallTime, String[] requestedPermissions) {
        PackageInfo pkgInfo = new PackageInfo();
        pkgInfo.packageName = packageName;
        pkgInfo.firstInstallTime = firstInstallTime;
        pkgInfo.requestedPermissions = requestedPermissions;
        return pkgInfo;
    }

    public static final class MockListener {
        void onOnboardingStateChanged(@HealthConnectOnboardingState.OnboardingState int state) {}
    }
}
