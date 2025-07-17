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
package com.android.server.healthconnect.onboarding.matchingapps;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.onboarding.matchingapps.MatchmakingDenialStateManager.DenialState;
import com.android.server.healthconnect.storage.HealthConnectContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RunWith(AndroidJUnit4.class)
public class MatchmakingDenialStateManagerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private PreferenceHelper mPreferenceHelper;

    private MatchmakingDenialStateManager mMatchmakingDenialStateManager;

    private static final String PACKAGE_NAME = "com.example.app";
    private static final String PACKAGE_NAME_2 = "com.example.app2";
    private static final String PREFERENCE_KEY =
            MatchmakingDenialStateManager.DENIAL_PREFERENCE_PREFIX + PACKAGE_NAME + "0";
    private static final String PREFERENCE_KEY_2 =
            MatchmakingDenialStateManager.DENIAL_PREFERENCE_PREFIX + PACKAGE_NAME_2 + "0";

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        HealthConnectContext userContext =
                HealthConnectContext.create(context, context.getUser(), null, null);
        mMatchmakingDenialStateManager =
                new MatchmakingDenialStateManager(userContext, mPreferenceHelper);
    }

    @After
    public void tearDown() {
        clearInvocations(mPreferenceHelper);
    }

    @Test
    public void isMatchmakingPaused_noPreviousDenials_returnsFalse() {
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(null);

        assertThat(mMatchmakingDenialStateManager.isMatchmakingPaused(PACKAGE_NAME)).isFalse();
        verify(mPreferenceHelper, never()).insertOrReplacePreference(anyString(), anyString());
    }

    @Test
    public void isMatchmakingPaused_denialsBelowLimit_returnsFalse() {
        String preferenceValue = new DenialState(4, Instant.now()).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        assertThat(mMatchmakingDenialStateManager.isMatchmakingPaused(PACKAGE_NAME)).isFalse();
    }

    @Test
    public void isMatchmakingPaused_denialsAtLimit_returnsTrue() {
        String preferenceValue = new DenialState(5, Instant.now()).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        assertThat(mMatchmakingDenialStateManager.isMatchmakingPaused(PACKAGE_NAME)).isTrue();
    }

    @Test
    public void isMatchmakingPaused_emptyCounterTimestamp_returnsFalseAndDoesNotWrite() {
        String preferenceValue = new DenialState(0, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        assertThat(mMatchmakingDenialStateManager.isMatchmakingPaused(PACKAGE_NAME)).isFalse();

        verify(mPreferenceHelper, never()).insertOrReplacePreference(anyString(), anyString());
    }

    @Test
    public void isMatchmakingPaused_pauseExpired_resetsCounterAndReturnsFalse() {
        Instant expiredTimestamp = Instant.now().minus(40, ChronoUnit.DAYS);
        String preferenceValue = new DenialState(10, expiredTimestamp).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        assertThat(mMatchmakingDenialStateManager.isMatchmakingPaused(PACKAGE_NAME)).isFalse();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(0);
        assertThat(captured.pauseStartedTimestamp()).isEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_noPreviousDenials_setsCounterToOne() {
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(null);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(PACKAGE_NAME);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(1);
        assertThat(captured.pauseStartedTimestamp()).isEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_previousDenials_incrementsCounter() {
        String preferenceValue = new DenialState(2, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(PACKAGE_NAME);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(3);
        assertThat(captured.pauseStartedTimestamp()).isEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_reachesLimit_updatesTimestamp() {
        String preferenceValue = new DenialState(4, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(PACKAGE_NAME);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(5);
        assertThat(captured.pauseStartedTimestamp()).isNotEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_denialsAboveLimit_doesNotUpdateTimestamp() {
        Instant initialPauseTime = Instant.parse("2025-07-20T10:00:00Z");
        String preferenceValue = new DenialState(5, initialPauseTime).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(PACKAGE_NAME);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(6);
        assertThat(captured.pauseStartedTimestamp()).isEqualTo(initialPauseTime);
    }

    @Test
    public void denialState_fromPreferenceString_invalidString_returnsEmpty() {
        DenialState denialState = DenialState.fromPreferenceString("invalid-string");

        assertThat(denialState.denialCount()).isEqualTo(0);
        assertThat(denialState.pauseStartedTimestamp()).isEqualTo(Instant.EPOCH);
    }

    @Test
    public void denialState_fromPreferenceString_nullString_returnsEmpty() {
        DenialState denialState = DenialState.fromPreferenceString(null);

        assertThat(denialState.denialCount()).isEqualTo(0);
    }

    @Test
    public void denialState_constructor_negativeDenialCount_coercesToZero() {
        DenialState denialState = new DenialState(-1, Instant.EPOCH);

        assertThat(denialState.denialCount()).isEqualTo(0);
    }

    @Test
    public void denialState_toPreferenceString_fromPreferenceString() {
        Instant now = Instant.ofEpochMilli(1234567890L);
        DenialState denialState = new DenialState(3, now);
        assertThat(DenialState.fromPreferenceString(denialState.toPreferenceString()))
                .isEqualTo(denialState);
    }

    @Test
    public void denialState_serialization_truncatesToMillis() {
        Instant initialTime = Instant.parse("2025-07-20T10:00:00.123456789Z");
        DenialState denialState = new DenialState(1, initialTime);

        String preferenceString = denialState.toPreferenceString();
        DenialState capturedDenialState = DenialState.fromPreferenceString(preferenceString);

        assertThat(capturedDenialState.pauseStartedTimestamp())
                .isEqualTo(Instant.parse("2025-07-20T10:00:00.123Z"));
    }

    @Test
    public void preferenceKey_isCreatedCorrectly() {
        mMatchmakingDenialStateManager.isMatchmakingPaused(PACKAGE_NAME);

        verify(mPreferenceHelper).getPreference(eq(PREFERENCE_KEY));
    }

    @Test
    public void recordMatchmakingDenial_forOnePackage_doesNotAffectOtherPackage() {
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(null);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(PACKAGE_NAME);

        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), anyString());
        verify(mPreferenceHelper, never())
                .insertOrReplacePreference(eq(PREFERENCE_KEY_2), anyString());
    }

    @Test
    public void recordMatchmakingDenial_denialsAtLimitAndTimestampNotSet_setsTimestamp() {
        String preferenceValue = new DenialState(4, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(PACKAGE_NAME);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(5);
        assertThat(captured.pauseStartedTimestamp()).isNotEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_denialsAboveLimitAndTimestampNotSet_setsTimestamp() {
        String preferenceValue = new DenialState(10, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(PACKAGE_NAME);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(11);
        assertThat(captured.pauseStartedTimestamp()).isNotEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_forSecondPackage_doesNotAffectFirstPackage() {
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(null);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(PACKAGE_NAME_2);

        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY_2), anyString());
        verify(mPreferenceHelper, never())
                .insertOrReplacePreference(eq(PREFERENCE_KEY), anyString());
    }
}
