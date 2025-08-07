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
package com.android.server.healthconnect.device.notification;

import static com.android.server.healthconnect.device.notification.NativeStepsNotificationStateManager.NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX;

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
public class NativeStepsNotificationStateManagerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private UserHandle mUserHandle;

    private com.android.server.healthconnect.device.notification.NativeStepsNotificationStateManager
            mNativeStepsNotificationStateManager;

    private static final int USER_ID_INT = (int) (Math.random() * 100);
    private static final String PREF_KEY = NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX + USER_ID_INT;

    @Before
    public void setUp() {
        when(mUserHandle.getIdentifier()).thenReturn(USER_ID_INT);

        mNativeStepsNotificationStateManager =
                new NativeStepsNotificationStateManager(mPreferenceHelper, mUserHandle);
    }

    @After
    public void tearDown() throws TimeoutException {
        clearInvocations(mPreferenceHelper, mUserHandle);
    }

    @Test
    public void setupForUser_preferenceKeyUpdated() {
        int newUserId = USER_ID_INT + 1;
        when(mUserHandle.getIdentifier()).thenReturn(newUserId);
        mNativeStepsNotificationStateManager.setupForUser(mUserHandle);

        String expectedPrefKey = NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX + newUserId;

        clearInvocations(mPreferenceHelper);
        mNativeStepsNotificationStateManager.getWasSeen();
        verify(mPreferenceHelper).getPreference(eq(expectedPrefKey));

        clearInvocations(mPreferenceHelper);
        mNativeStepsNotificationStateManager.preferenceKeyExists();
        verify(mPreferenceHelper).getPreference(eq(expectedPrefKey));

        clearInvocations(mPreferenceHelper);
        mNativeStepsNotificationStateManager.updateNotificationWasSeen(false);
        verify(mPreferenceHelper).getPreference(eq(expectedPrefKey));

        clearInvocations(mPreferenceHelper);
        mNativeStepsNotificationStateManager.updateNotificationWasSeen(true);
        verify(mPreferenceHelper).getPreference(eq(expectedPrefKey));
    }

    @Test
    public void getWasSeen_returnsValueFromPreference() {
        setNotificationWasSeenInPreference(false);
        verifyNotificationWasSeen(false);

        setNotificationWasSeenInPreference(true);
        verifyNotificationWasSeen(true);
    }

    @Test
    public void getWasSeen_referenceUnset_returnsFalse() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(null);
        assertThat(mNativeStepsNotificationStateManager.getWasSeen()).isEqualTo(false);
    }

    @Test
    public void getWasSeen_referenceUnset_returnsPreferenceKeyDoesNotExist() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(null);
        assertThat(mNativeStepsNotificationStateManager.preferenceKeyExists()).isEqualTo(false);
    }

    @Test
    public void getWasSeen_referenceSet_returnsPreferenceKeyExists() {
        setNotificationWasSeenInPreference(false);
        assertThat(mNativeStepsNotificationStateManager.preferenceKeyExists()).isEqualTo(true);
    }

    @Test
    public void getWasSeen_failedToRead_returnsTrue() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY)))
                .thenThrow(new RuntimeException("test exception"));

        assertThat(mNativeStepsNotificationStateManager.getWasSeen()).isEqualTo(true);
    }

    @Test
    public void updateNotificationWasSeen_keyDoesNotExist_preferenceUpdated() {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(null);

        mNativeStepsNotificationStateManager.updateNotificationWasSeen(true);

        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREF_KEY), eq(String.valueOf(true)));
    }

    @Test
    public void updateNotificationWasSeenState_keyExists_same_preferenceNotUpdated() {
        setNotificationWasSeenInPreference(false);

        mNativeStepsNotificationStateManager.updateNotificationWasSeen(false);

        verify(mPreferenceHelper, never()).insertOrReplacePreference(eq(PREF_KEY), any());
    }

    @Test
    public void updateNotificationWasSeenState_keyExists_same_preferenceUpdated() {
        setNotificationWasSeenInPreference(false);

        mNativeStepsNotificationStateManager.updateNotificationWasSeen(true);

        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREF_KEY), eq(String.valueOf(true)));
    }

    private void setNotificationWasSeenInPreference(boolean wasSeen) {
        when(mPreferenceHelper.getPreference(eq(PREF_KEY))).thenReturn(String.valueOf(wasSeen));
    }

    private void verifyNotificationWasSeen(boolean expectedState) {
        assertThat(mNativeStepsNotificationStateManager.getWasSeen()).isEqualTo(expectedState);
    }
}
