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

import android.health.connect.Constants;
import android.os.UserHandle;
import android.util.Slog;

import androidx.annotation.VisibleForTesting;

import com.android.server.healthconnect.common.preferences.PreferenceHelper;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Maintains the state of the native steps notification, and decide whether it should be sent to the
 * user again.
 *
 * @hide
 */
public final class NativeStepsNotificationStateManager {
    private static final String TAG = NativeStepsNotificationStateManager.class.getSimpleName();

    static final String NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX =
            "native_steps_notification_was_seen_";

    private final PreferenceHelper mPreferenceHelper;
    private final ReentrantReadWriteLock mStatesLock = new ReentrantReadWriteLock(true);
    private UserHandle mUserHandle;

    public NativeStepsNotificationStateManager(
            PreferenceHelper preferenceHelper, UserHandle userHandle) {
        mPreferenceHelper = preferenceHelper;
        mUserHandle = userHandle;
    }

    /** Set the notification to seen */
    public void disable() {
        updateNotificationWasSeen(true);
    }

    /** Return if notification can still be displayed */
    public boolean isEnabled() {
        return !getWasSeen();
    }

    /** Return current was seen state */
    @VisibleForTesting
    public boolean getWasSeen() {
        mStatesLock.readLock().lock();
        try {
            String wasSeenString =
                    mPreferenceHelper.getPreference(getWasSeenNotificationPreferenceKey());
            if (Objects.isNull(wasSeenString)) {
                return false;
            }
            return Boolean.parseBoolean(wasSeenString);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read native steps notification preference.");
            return true;
        } finally {
            mStatesLock.readLock().unlock();
        }
    }

    /** Update the was seen state */
    @VisibleForTesting
    public void updateNotificationWasSeen(boolean newState) {
        if (preferenceKeyExists() && newState == getWasSeen()) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "The new state is the same as the current state: " + newState);
            }
            return;
        }

        mStatesLock.writeLock().lock();
        try {
            mPreferenceHelper.insertOrReplacePreference(
                    getWasSeenNotificationPreferenceKey(), String.valueOf(newState));
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    /** Return if the preference for the notification exists */
    @VisibleForTesting
    public boolean preferenceKeyExists() {
        mStatesLock.readLock().lock();
        try {
            String wasSeenString =
                    mPreferenceHelper.getPreference(getWasSeenNotificationPreferenceKey());
            return Objects.nonNull(wasSeenString);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read native steps notification preference.");
            return false;
        } finally {
            mStatesLock.readLock().unlock();
        }
    }

    /** Initialize new with new user */
    @VisibleForTesting
    public void setupForUser(UserHandle userHandle) {
        mStatesLock.writeLock().lock();
        try {
            mUserHandle = userHandle;
        } finally {
            mStatesLock.writeLock().unlock();
        }
    }

    @VisibleForTesting
    public String getWasSeenNotificationPreferenceKey() {
        return NOTIFICATION_STATE_PREFERENCE_KEY_PREFIX + mUserHandle.getIdentifier();
    }
}
