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

import android.health.connect.Constants;
import android.os.UserHandle;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Maintains the states of each onboarding notification, and decide whether it should be sent to the
 * user again.
 *
 * @hide
 */
public final class OnboardingNotificationStateManager {
    private static final String TAG = OnboardingStateManager.class.getSimpleName();
    private static final int NUM_OF_NOTIFICATIONS = 2;
    static final int SHOULD_SHOW_NO_NOTIFICATION = 0;
    static final int SHOULD_SHOW_NO_APP_CONNECTED_NOTIFICATION = 1;
    static final int SHOULD_SHOW_ONE_APP_CONNECTED_NOTIFICATION = 1 << 1;
    static final int SHOULD_SHOW_ALL_NOTIFICATIONS = (1 << NUM_OF_NOTIFICATIONS) - 1;

    @VisibleForTesting
    static final String NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX = "onboarding_notification_state_";

    private final PreferenceHelper mPreferenceHelper;
    private final ReentrantReadWriteLock mStatesLock = new ReentrantReadWriteLock(true);
    private UserHandle mUserHandle;

    public OnboardingNotificationStateManager(
            PreferenceHelper preferenceHelper, UserHandle userHandle) {
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

    int getOnboardingNotificationState() {
        mStatesLock.readLock().lock();
        try {
            String notificationStateStr =
                    mPreferenceHelper.getPreference(getNotificationStateKey());
            if (Objects.isNull(notificationStateStr)) {
                return SHOULD_SHOW_ALL_NOTIFICATIONS;
            }
            return Integer.parseInt(notificationStateStr);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read onboarding notification preference.");
            return SHOULD_SHOW_NO_NOTIFICATION;
        } finally {
            mStatesLock.readLock().unlock();
        }
    }

    /** Updates the onboarding notification shown state. */
    void updateNotificationShownState(int newState) {
        if (newState == getOnboardingNotificationState()) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "The new state is the same as the current state: " + newState);
            }
            return;
        }

        mStatesLock.writeLock().lock();
        try {
            mPreferenceHelper.insertOrReplacePreference(
                    getNotificationStateKey(), String.valueOf(newState));
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    /** Updates the onboarding notification flag so the active bits in {@code flags} are unset. */
    void unsetFlags(int flags) {
        int currentFlags = getOnboardingNotificationState();
        if ((currentFlags & flags) == 0) {
            // all the flags to be unset are not set in the current flags
            return;
        }
        updateNotificationShownState(currentFlags & (~flags));
    }

    /** Clears the onboarding notification state. */
    void clearNotificationShownState() {
        mStatesLock.writeLock().lock();
        try {
            mPreferenceHelper.removeKey(getNotificationStateKey());
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    private String getNotificationStateKey() {
        return NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX + mUserHandle.getIdentifier();
    }
}
