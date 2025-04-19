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

import static com.android.server.healthconnect.onboarding.OnboardingStateManager.ONBOARDING_STATE_PREFERENCE_KEY_PREFIX;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.health.connect.HealthConnectOnboardingState;
import android.os.UserHandle;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.preferences.PreferenceHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.TimeoutException;

/** Test class for the OnboardingStateManager class. */
@RunWith(AndroidJUnit4.class)
public class OnboardingStateManagerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private MockListener mMockListener;
    private OnboardingStateManager mOnboardingStateManager;
    @Mock UserHandle mUserHandle;

    public OnboardingStateManagerTest() {}

    @Before
    public void setUp() {
        mOnboardingStateManager = new OnboardingStateManager(mPreferenceHelper, mUserHandle);
        mOnboardingStateManager.setupForUser(mUserHandle);
        mOnboardingStateManager.addStateChangedListener(mMockListener::onOnboardingStateChanged);
    }

    @After
    public void tearDown() throws TimeoutException {
        clearInvocations(mPreferenceHelper);
    }

    @Test
    public void testUpdateOnboardingState_fromHideToZero_stateIsUpdatedAndListenerNotified() {
        setOnboardingState(ONBOARDING_BANNER_STATE_HIDE);

        mOnboardingStateManager.updateOnboardingState(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);

        verifyStateChange(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
        verify(mMockListener, times(1))
                .onOnboardingStateChanged(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);
    }

    @Test
    public void testUpdateOnboardingState_fromZeroToOne_stateIsUpdatedAndListenerNotified() {
        setOnboardingState(ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED);

        mOnboardingStateManager.updateOnboardingState(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);

        verifyStateChange(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
        verify(mMockListener).onOnboardingStateChanged(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);
    }

    @Test
    public void testUpdateOnboardingState_toInvalidState_throwsException() {
        setOnboardingState(ONBOARDING_BANNER_STATE_HIDE);
        int invalidState = 99;

        assertThrows(
                IllegalArgumentException.class,
                () -> {
                    mOnboardingStateManager.updateOnboardingState(invalidState);
                });

        verify(mPreferenceHelper, never()).insertOrReplacePreferencesTransaction(any());
        verify(mMockListener, never()).onOnboardingStateChanged(any(Integer.class));
    }

    @Test
    public void testGetOnboardingState_returnsCorrectStateFromPreference() {
        setOnboardingState(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED);

        int state = mOnboardingStateManager.getOnboardingState();

        assertEquals(ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED, state);
    }

    @Test
    public void testGetOnboardingState_returnsHideWhenPreferenceIsNull() {
        when(mPreferenceHelper.getPreference(
                        eq(ONBOARDING_STATE_PREFERENCE_KEY_PREFIX + mUserHandle.getIdentifier())))
                .thenReturn(null);

        int state = mOnboardingStateManager.getOnboardingState();

        assertEquals(ONBOARDING_BANNER_STATE_HIDE, state);
    }

    private void setOnboardingState(int state) {
        when(mPreferenceHelper.getPreference(
                        eq(ONBOARDING_STATE_PREFERENCE_KEY_PREFIX + mUserHandle.getIdentifier())))
                .thenReturn(String.valueOf(state));
    }

    private void verifyStateChange(int state) {
        verify(mPreferenceHelper)
                .insertOrReplacePreference(
                        eq(ONBOARDING_STATE_PREFERENCE_KEY_PREFIX + mUserHandle.getIdentifier()),
                        eq(String.valueOf(state)));

        verify(mMockListener).onOnboardingStateChanged(state);
    }

    private void verifyNoStateChange() {
        verify(mPreferenceHelper, never()).insertOrReplacePreferencesTransaction(any());
    }

    public static class MockListener {
        void onOnboardingStateChanged(@HealthConnectOnboardingState.OnboardingState int state) {}
    }
}
