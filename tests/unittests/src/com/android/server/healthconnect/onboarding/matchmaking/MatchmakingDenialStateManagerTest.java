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
package com.android.server.healthconnect.onboarding.matchmaking;

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.HealthDataCategory.SLEEP;

import static com.android.server.healthconnect.onboarding.matchmaking.MatchmakingDenialStateManager.MAX_DENIALS_BEFORE_PAUSE;
import static com.android.server.healthconnect.onboarding.matchmaking.MatchmakingDenialStateManager.PAUSE_DURATION;

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
import com.android.server.healthconnect.onboarding.matchmaking.MatchmakingDenialStateManager.DenialState;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RunWith(AndroidJUnit4.class)
public class MatchmakingDenialStateManagerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private PreferenceHelper mPreferenceHelper;

    private MatchmakingDenialStateManager mMatchmakingDenialStateManager;

    private static final String CALLING_PACKAGE_NAME = "com.example.calling_app";
    private static final String MATCHING_PACKAGE_NAME = "com.example.matching_app";
    private static final String MATCHING_PACKAGE_NAME_2 = "com.example.matching_app2";
    private static final String PREFERENCE_KEY =
            String.join(
                    "_",
                    MatchmakingDenialStateManager.DENIAL_PREFERENCE_PREFIX,
                    CALLING_PACKAGE_NAME,
                    MATCHING_PACKAGE_NAME,
                    String.valueOf(ACTIVITY));
    private static final String PREFERENCE_KEY_2 =
            String.join(
                    "_",
                    MatchmakingDenialStateManager.DENIAL_PREFERENCE_PREFIX,
                    CALLING_PACKAGE_NAME,
                    MATCHING_PACKAGE_NAME_2,
                    String.valueOf(ACTIVITY));
    private static final String PREFERENCE_KEY_3 =
            String.join(
                    "_",
                    MatchmakingDenialStateManager.DENIAL_PREFERENCE_PREFIX,
                    CALLING_PACKAGE_NAME,
                    MATCHING_PACKAGE_NAME,
                    String.valueOf(SLEEP));

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

        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY))
                .isFalse();
        verify(mPreferenceHelper, never()).insertOrReplacePreference(anyString(), anyString());
    }

    @Test
    public void isMatchmakingPaused_denialsBelowLimit_returnsFalse() {
        String preferenceValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE - 1, Instant.now()).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY))
                .isFalse();
    }

    @Test
    public void isMatchmakingPaused_denialsAtLimit_returnsTrue() {
        String preferenceValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE, Instant.now()).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY))
                .isTrue();
    }

    @Test
    public void isMatchmakingPaused_emptyCounterTimestamp_returnsFalseAndDoesNotWrite() {
        String preferenceValue = new DenialState(0, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY))
                .isFalse();

        verify(mPreferenceHelper, never()).insertOrReplacePreference(anyString(), anyString());
    }

    @Test
    public void isMatchmakingPaused_pauseExpired_removesStoredStateAndReturnsFalse() {
        Instant expiredTimestamp = Instant.now().minus(10, ChronoUnit.DAYS).minus(PAUSE_DURATION);
        String preferenceValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE + 5, expiredTimestamp)
                        .toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY))
                .isFalse();

        verify(mPreferenceHelper).removeKey(eq(PREFERENCE_KEY));
    }

    @Test
    public void isMatchmakingPaused_oneStateExpired_onlyResetsExpiredState() {
        Instant expiredTimestamp = Instant.now().minus(10, ChronoUnit.DAYS).minus(PAUSE_DURATION);
        String expiredStateValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE, expiredTimestamp).toPreferenceString();
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY)).thenReturn(expiredStateValue);
        Instant activeTimestamp = Instant.now().minus(1, ChronoUnit.DAYS);
        String activeStateValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE, activeTimestamp).toPreferenceString();
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY_3)).thenReturn(activeStateValue);

        boolean isPaused =
                mMatchmakingDenialStateManager.isMatchmakingPaused(
                        CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        assertThat(isPaused).isFalse();
        verify(mPreferenceHelper).removeKey(eq(PREFERENCE_KEY));
        verify(mPreferenceHelper, never())
                .insertOrReplacePreference(eq(PREFERENCE_KEY_3), anyString());
    }

    @Test
    public void recordMatchmakingDenial_noPreviousDenials_setsCounterToOne() {
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(null);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(1);
        assertThat(captured.pauseStartedTimestamp()).isEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_previousDenials_incrementsCounter() {
        String preferenceValue = new DenialState(0, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY)).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(1);
        assertThat(captured.pauseStartedTimestamp()).isEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_reachesLimit_updatesTimestamp() {
        String preferenceValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE - 1, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY)).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(MAX_DENIALS_BEFORE_PAUSE);
        assertThat(captured.pauseStartedTimestamp()).isNotEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_denialsAboveLimit_doesNotUpdateTimestamp() {
        Instant initialPauseTime = Instant.now().minus(Duration.ofDays(7));
        String preferenceValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE + 1, initialPauseTime)
                        .toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(MAX_DENIALS_BEFORE_PAUSE + 2);
        assertThat(captured.pauseStartedTimestamp())
                .isEqualTo(initialPauseTime.truncatedTo(ChronoUnit.MILLIS));
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
        mMatchmakingDenialStateManager.isMatchmakingPaused(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        verify(mPreferenceHelper).getPreference(eq(PREFERENCE_KEY));
    }

    @Test
    public void recordMatchmakingDenial_forOnePackage_doesNotAffectOtherPackage() {
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(null);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), anyString());
        verify(mPreferenceHelper, never())
                .insertOrReplacePreference(eq(PREFERENCE_KEY_2), anyString());
    }

    @Test
    public void recordMatchmakingDenial_denialsAtLimitAndTimestampNotSet_setsTimestamp() {
        String preferenceValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE - 1, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(MAX_DENIALS_BEFORE_PAUSE);
        assertThat(captured.pauseStartedTimestamp()).isNotEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_denialsAboveLimitAndTimestampNotSet_setsTimestamp() {
        String preferenceValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE + 5, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(preferenceValue);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(MAX_DENIALS_BEFORE_PAUSE + 6);
        assertThat(captured.pauseStartedTimestamp()).isNotEqualTo(Instant.EPOCH);
    }

    @Test
    public void recordMatchmakingDenial_forSecondPackage_doesNotAffectFirstPackage() {
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(null);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME_2, ACTIVITY);

        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY_2), anyString());
        verify(mPreferenceHelper, never())
                .insertOrReplacePreference(eq(PREFERENCE_KEY), anyString());
    }

    @Test
    public void recordMatchmakingDenial_forOneCategory_doesNotAffectOtherCategory() {
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(null);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, SLEEP);

        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY_3), anyString());
        verify(mPreferenceHelper, never())
                .insertOrReplacePreference(eq(PREFERENCE_KEY), anyString());
        verify(mPreferenceHelper, never())
                .insertOrReplacePreference(eq(PREFERENCE_KEY_2), anyString());
    }

    @Test
    public void recordMatchmakingDenial_twoDenials_triggersPauseAtCorrectLimit() {
        when(mPreferenceHelper.getPreference(anyString())).thenReturn(null);
        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        String preferenceValue = new DenialState(1, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY)).thenReturn(preferenceValue);
        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        preferenceValue = new DenialState(2, Instant.now()).toPreferenceString();
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY)).thenReturn(preferenceValue);
        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY))
                .isTrue();
    }

    @Test
    public void recordMatchmakingDenial_differentCallingApp_doesNotAffectFirst() {
        // Second calling app gets denied
        String callingPackageName2 = "calling.package.name.2";
        String preferenceKeyForSecondApp =
                String.join(
                        "_",
                        MatchmakingDenialStateManager.DENIAL_PREFERENCE_PREFIX,
                        callingPackageName2,
                        MATCHING_PACKAGE_NAME,
                        String.valueOf(ACTIVITY));
        String preferenceValueForSecondApp =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE - 1, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(preferenceKeyForSecondApp))
                .thenReturn(preferenceValueForSecondApp);
        // First calling app not denied
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY)).thenReturn(null);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                callingPackageName2, MATCHING_PACKAGE_NAME, ACTIVITY);

        verify(mPreferenceHelper, never())
                .insertOrReplacePreference(eq(PREFERENCE_KEY), anyString());
        verify(mPreferenceHelper)
                .insertOrReplacePreference(eq(preferenceKeyForSecondApp), anyString());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper)
                .insertOrReplacePreference(eq(preferenceKeyForSecondApp), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());

        String updatedValueForSecondApp =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE, Instant.now()).toPreferenceString();
        when(mPreferenceHelper.getPreference(preferenceKeyForSecondApp))
                .thenReturn(updatedValueForSecondApp);
        assertThat(captured.denialCount()).isEqualTo(MAX_DENIALS_BEFORE_PAUSE);
        assertThat(captured.pauseStartedTimestamp()).isNotEqualTo(Instant.EPOCH);
        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY))
                .isFalse();
        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                callingPackageName2, MATCHING_PACKAGE_NAME, ACTIVITY))
                .isTrue();
    }

    @Test
    public void recordMatchmakingDenial_nonMatchedApp_isNotAffected() {
        // Calling app gets denied
        String preferenceValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE - 1, Instant.EPOCH).toPreferenceString();
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY)).thenReturn(preferenceValue);
        // Non matched app not affected
        String otherPackageName = "non.matched.package.name";
        String expectedKeyForOtherApp =
                String.join(
                        "_",
                        MatchmakingDenialStateManager.DENIAL_PREFERENCE_PREFIX,
                        CALLING_PACKAGE_NAME,
                        otherPackageName,
                        String.valueOf(ACTIVITY));
        when(mPreferenceHelper.getPreference(expectedKeyForOtherApp)).thenReturn(null);

        mMatchmakingDenialStateManager.recordMatchmakingDenial(
                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mPreferenceHelper).insertOrReplacePreference(eq(PREFERENCE_KEY), captor.capture());
        DenialState captured = DenialState.fromPreferenceString(captor.getValue());
        assertThat(captured.denialCount()).isEqualTo(MAX_DENIALS_BEFORE_PAUSE);
        assertThat(captured.pauseStartedTimestamp()).isNotEqualTo(Instant.EPOCH);
        String updatedPreferenceValue =
                new DenialState(MAX_DENIALS_BEFORE_PAUSE, Instant.now()).toPreferenceString();
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY)).thenReturn(updatedPreferenceValue);
        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                CALLING_PACKAGE_NAME, MATCHING_PACKAGE_NAME, ACTIVITY))
                .isTrue();
        assertThat(
                        mMatchmakingDenialStateManager.isMatchmakingPaused(
                                CALLING_PACKAGE_NAME, otherPackageName, ACTIVITY))
                .isFalse();
    }
}
