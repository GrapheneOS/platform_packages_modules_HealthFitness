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

import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_ALL_NOTIFICATIONS;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_NO_NOTIFICATION;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationStateManager.SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@RunWith(AndroidJUnit4.class)
public class OnboardingNotificationStateManagerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private UserHandle mUserHandle;

    private OnboardingNotificationStateManager mOnboardingNotificationStateManager;

    private static final int USER_ID_INT = (int) (Math.random() * 100);
    private static final String PREF_KEY = NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX + USER_ID_INT;

    @Before
    public void setUp() {
        when(mUserHandle.getIdentifier()).thenReturn(USER_ID_INT);

        mOnboardingNotificationStateManager =
                new OnboardingNotificationStateManager(mPreferenceHelper, mUserHandle);
    }

    @After
    public void tearDown() throws TimeoutException {
        clearInvocations(mPreferenceHelper, mUserHandle);
    }

    @Test
    public void setupForUser_preferenceKeyUpdated() {
        int newUserId = USER_ID_INT + 1;
        when(mUserHandle.getIdentifier()).thenReturn(newUserId);
        mOnboardingNotificationStateManager.setupForUser(mUserHandle);

        String expectedPrefKey = NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX + newUserId;

        clearInvocations(mPreferenceHelper);
        mOnboardingNotificationStateManager.getOnboardingNotificationState();
        verify(mPreferenceHelper).getPreference(eq(expectedPrefKey));

        clearInvocations(mPreferenceHelper);
        mOnboardingNotificationStateManager.updateNotificationShownState(
                SHOULD_SHOW_ALL_NOTIFICATIONS);
        verify(mPreferenceHelper).getPreference(eq(expectedPrefKey));

        clearInvocations(mPreferenceHelper);
        mOnboardingNotificationStateManager.clearNotificationShownState();
        verify(mPreferenceHelper).removeKey(eq(expectedPrefKey));
    }

    @Test
    public void getOnboardingNotificationState_returnsValueFromPreference() {
        for (int state = 0; state <= SHOULD_SHOW_ALL_NOTIFICATIONS; state++) {
            setNotificationStateInPreference(state);
            verifyNotificationState(state);
        }
    }

    @Test
    public void getOnboardingNotificationState_referenceUnset_returnsShowAll() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(null);
        assertThat(mOnboardingNotificationStateManager.getOnboardingNotificationState())
                .isEqualTo(SHOULD_SHOW_ALL_NOTIFICATIONS);
    }

    @Test
    public void getOnboardingNotificationState_failedToRead_returnsShowAll() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY)))
                .thenThrow(new RuntimeException("test exception"));

        assertThat(mOnboardingNotificationStateManager.getOnboardingNotificationState())
                .isEqualTo(SHOULD_SHOW_NO_NOTIFICATION);
    }

    @Test
    public void updateNotificationShownState_sameState_preferenceNotUpdated() {
        int state = SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION;
        setNotificationStateInPreference(state);

        mOnboardingNotificationStateManager.updateNotificationShownState(state);

        verify(mPreferenceHelper, never()).insertOrReplacePreference(eq(PREF_KEY), any());
    }

    @Test
    public void updateNotificationShownState_differentState_preferenceUpdated() {
        setNotificationStateInPreference(SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION);

        int newState = SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION;
        mOnboardingNotificationStateManager.updateNotificationShownState(newState);

        verify(mPreferenceHelper)
                .insertOrReplacePreference(eq(PREF_KEY), eq(String.valueOf(newState)));
    }

    @Test
    public void clearNotificationShownState_preferenceRemoved() {
        mOnboardingNotificationStateManager.clearNotificationShownState();

        verify(mPreferenceHelper).removeKey(PREF_KEY);
    }

    @Test
    public void unsetFlags_preferenceUpdated() {
        setNotificationStateInPreference(SHOULD_SHOW_ALL_NOTIFICATIONS);

        clearInvocations(mPreferenceHelper);
        mOnboardingNotificationStateManager.unsetFlags(SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION);
        verify(mPreferenceHelper)
                .insertOrReplacePreference(
                        eq(PREF_KEY),
                        eq(String.valueOf(SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION)));

        clearInvocations(mPreferenceHelper);
        mOnboardingNotificationStateManager.unsetFlags(SHOULD_SHOW_ALL_NOTIFICATIONS);
        verify(mPreferenceHelper)
                .insertOrReplacePreference(
                        eq(PREF_KEY), eq(String.valueOf(SHOULD_SHOW_NO_NOTIFICATION)));
    }

    @Test
    public void unsetFlags_flagsNotSetInCurrentState_preferenceNotUpdated() {
        setNotificationStateInPreference(SHOULD_SHOW_NO_NOTIFICATION);

        mOnboardingNotificationStateManager.unsetFlags(SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION);
        verify(mPreferenceHelper, never()).insertOrReplacePreference(eq(PREF_KEY), any());

        mOnboardingNotificationStateManager.unsetFlags(SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION);
        verify(mPreferenceHelper, never()).insertOrReplacePreference(eq(PREF_KEY), any());
    }

    private void setNotificationStateInPreference(int state) {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(String.valueOf(state));
    }

    private void verifyNotificationState(int expectedState) {
        assertThat(mOnboardingNotificationStateManager.getOnboardingNotificationState())
                .isEqualTo(expectedState);
    }
}
