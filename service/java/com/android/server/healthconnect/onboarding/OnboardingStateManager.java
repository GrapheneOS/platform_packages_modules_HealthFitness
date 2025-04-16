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

import android.health.connect.Constants;
import android.health.connect.HealthConnectOnboardingState;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sends the appropriate notification based on the onboarding state, and returns the current
 * onboarding state of user to the caller.
 *
 * @hide
 */
public final class OnboardingStateManager {
    private static final String TAG = OnboardingStateManager.class.getSimpleName();

    @VisibleForTesting
    static final String ONBOARDING_STATE_PREFERENCE_KEY_PREFIX = "onboarding_state_";

    private final PreferenceHelper mPreferenceHelper;
    private final ReentrantReadWriteLock mStatesLock = new ReentrantReadWriteLock(true);
    private UserHandle mUserHandle;
    private final Set<StateChangedListener> mStateChangedListeners = new CopyOnWriteArraySet<>();

    public OnboardingStateManager(PreferenceHelper preferenceHelper, UserHandle userHandle) {
        mPreferenceHelper = preferenceHelper;
        mUserHandle = userHandle;
    }

    /** Re-initialize this class instance with the new user */
    public void setupForUser(UserHandle userHandle) {
        mStatesLock.writeLock().lock();
        try {
            mUserHandle = userHandle;
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    /** Registers {@link StateChangedListener} for observing onboarding state changes. */
    public void addStateChangedListener(StateChangedListener listener) {
        mStatesLock.writeLock().lock();
        try {
            mStateChangedListeners.add(listener);
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    /** Returns the current onboarding state. */
    @HealthConnectOnboardingState.OnboardingState
    public int getOnboardingState() {
        mStatesLock.readLock().lock();
        try {
            String onboardingStateStr =
                    mPreferenceHelper.getPreference(getOnboardingStatePreferenceKey());
            if (Objects.isNull(onboardingStateStr)) {
                return ONBOARDING_BANNER_STATE_HIDE;
            }
            return Integer.parseInt(onboardingStateStr);
        } finally {
            mStatesLock.readLock().unlock();
        }
    }

    /** Updates the onboarding state. */
    public void updateOnboardingState(@HealthConnectOnboardingState.OnboardingState int state) {
        mStatesLock.writeLock().lock();
        try {
            updateOnboardingStateGuarded(state);
        } finally {
            mStatesLock.writeLock().unlock();
        }

        updateListeners(state);
    }

    /** Atomically updates the onboarding state. */
    private void updateOnboardingStateGuarded(
            @HealthConnectOnboardingState.OnboardingState int state) {
        if (state == getOnboardingState()) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "The new state same as the current state.");
            }
            return;
        }

        switch (state) {
            case ONBOARDING_BANNER_STATE_ZERO_APPS_CONNECTED,
                    ONBOARDING_BANNER_STATE_ONE_APP_CONNECTED,
                    ONBOARDING_BANNER_STATE_HIDE:
                updateOnboardingStatePreference(state);
                return;
            default:
                throw new IllegalArgumentException(
                        "Cannot update onboarding state. Unknown state: " + state);
        }
    }

    /** Updates the onboarding state preference and the timeout reached preferences. */
    private void updateOnboardingStatePreference(
            @HealthConnectOnboardingState.OnboardingState int onboardingState) {
        mStatesLock.writeLock().lock();
        try {
            mPreferenceHelper.insertOrReplacePreference(
                    getOnboardingStatePreferenceKey(), String.valueOf(onboardingState));

        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    private String getOnboardingStatePreferenceKey() {
        return ONBOARDING_STATE_PREFERENCE_KEY_PREFIX + mUserHandle.getIdentifier();
    }

    private void updateListeners(
            @HealthConnectOnboardingState.OnboardingState int onboardingState) {
        for (StateChangedListener listener : mStateChangedListeners) {
            listener.onChanged(onboardingState);
        }
    }

    /**
     * A listener for observing onboarding state changes.
     *
     * @see OnboardingStateManager#addStateChangedListener(StateChangedListener)
     */
    public interface StateChangedListener {

        /**
         * Called on every onboarding state change.
         *
         * @param state the new onboarding state.
         */
        void onChanged(@HealthConnectOnboardingState.OnboardingState int state);
    }
}
