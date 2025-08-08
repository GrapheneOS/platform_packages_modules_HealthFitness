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

import android.health.connect.HealthDataCategory;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.storage.HealthConnectContext;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Manages the state of matchmaking denials for apps.
 *
 * @hide
 */
public final class MatchmakingDenialStateManager {
    private static final String TAG = MatchmakingDenialStateManager.class.getSimpleName();

    @VisibleForTesting static final int MAX_DENIALS_BEFORE_PAUSE = 2;
    private static final Duration PAUSE_DURATION = Duration.ofDays(30);

    @VisibleForTesting static final String DENIAL_PREFERENCE_PREFIX = "matchmaking_denial_state";

    @GuardedBy("this")
    private HealthConnectContext mUserContext;

    private final PreferenceHelper mPreferenceHelper;

    public MatchmakingDenialStateManager(
            HealthConnectContext userContext, PreferenceHelper preferenceHelper) {
        mUserContext = userContext;
        mPreferenceHelper = preferenceHelper;
    }

    /** Setup MatchmakingDenialStateManager for the given user. */
    public synchronized void setupForUser(HealthConnectContext userContext) {
        mUserContext = userContext;
    }

    /**
     * Records a matchmaking denial for the given package.
     *
     * <p>This increments the internal denial counter. If the number of denials reaches the defined
     * limit, a pause period is initiated.
     */
    synchronized void recordMatchmakingDenial(
            String packageName, @HealthDataCategory.Type int dataCategory) {
        DenialState storedValues = getDenialState(packageName, dataCategory);
        int denialCount = storedValues.denialCount();
        Instant pauseStartedTimestamp = storedValues.pauseStartedTimestamp();

        if (shouldResetPauseStartedTimestamp(pauseStartedTimestamp)) {
            denialCount = 1;
            pauseStartedTimestamp = Instant.EPOCH;
        } else {
            denialCount++;
            if (denialCount >= MAX_DENIALS_BEFORE_PAUSE
                    && pauseStartedTimestamp.equals(Instant.EPOCH)) {
                pauseStartedTimestamp = Instant.now();
            }
        }

        DenialState newDenialState = new DenialState(denialCount, pauseStartedTimestamp);
        updateDenialState(packageName, dataCategory, newDenialState);
    }

    /**
     * Checks if the matchmaking feature is currently paused for the given package.
     *
     * <p>This method also cleans up expired pause states by resetting the denial counter if the
     * pause period has elapsed.
     *
     * @return {@code true} if the number of denials has reached the limit and the pause period has
     *     not yet expired, {@code false} otherwise.
     */
    synchronized boolean isMatchmakingPaused(
            String packageName, @HealthDataCategory.Type int dataCategory) {
        DenialState storedValues = getDenialState(packageName, dataCategory);
        if (shouldResetPauseStartedTimestamp(storedValues.pauseStartedTimestamp())) {
            resetDenialState(packageName, dataCategory);
            return false;
        }
        return storedValues.denialCount() >= MAX_DENIALS_BEFORE_PAUSE;
    }

    private String getPreferenceKey(String packageName, @HealthDataCategory.Type int dataCategory) {
        return String.join(
                "_",
                DENIAL_PREFERENCE_PREFIX,
                packageName,
                String.valueOf(dataCategory),
                String.valueOf(mUserContext.getUser().getIdentifier()));
    }

    /**
     * Checks if the pause started timestamp is set and has expired.
     *
     * <p>A timestamp of {@link Instant#EPOCH} is considered not set.
     *
     * @param storedTimestamp The timestamp when the pause was started.
     * @return true if the timestamp is set and older than the pause duration, false otherwise.
     */
    private boolean shouldResetPauseStartedTimestamp(Instant storedTimestamp) {
        if (storedTimestamp.equals(Instant.EPOCH)) {
            return false;
        }
        return storedTimestamp.plus(PAUSE_DURATION).isBefore(Instant.now());
    }

    private DenialState getDenialState(
            String packageName, @HealthDataCategory.Type int dataCategory) {
        String concatenatedValueAndTimestamp =
                mPreferenceHelper.getPreference(getPreferenceKey(packageName, dataCategory));
        if (concatenatedValueAndTimestamp == null) {
            return new DenialState();
        }
        return DenialState.fromPreferenceString(concatenatedValueAndTimestamp);
    }

    private void resetDenialState(String packageName, @HealthDataCategory.Type int dataCategory) {
        updateDenialState(packageName, dataCategory, new DenialState(0, Instant.EPOCH));
    }

    private void updateDenialState(
            String packageName,
            @HealthDataCategory.Type int dataCategory,
            DenialState denialState) {
        mPreferenceHelper.insertOrReplacePreference(
                getPreferenceKey(packageName, dataCategory), denialState.toPreferenceString());
    }

    /** Represents the state of a matchmaking denial. */
    @VisibleForTesting
    record DenialState(int denialCount, Instant pauseStartedTimestamp) {
        private static final int DENIAL_COUNT_BYTES = Integer.BYTES;
        private static final int TIMESTAMP_BYTES = Long.BYTES;

        DenialState {
            if (denialCount < 0) {
                Slog.e(TAG, "Denial count cannot be negative. Resetting to 0.");
                denialCount = 0;
            }
        }

        DenialState() {
            this(0, Instant.EPOCH);
        }

        /**
         * Serializes the {@link DenialState} to a string for storage.
         *
         * <p>Note: The timestamp is stored with millisecond precision, any finer precision will be
         * lost.
         */
        String toPreferenceString() {
            ByteBuffer buffer = ByteBuffer.allocate(DENIAL_COUNT_BYTES + TIMESTAMP_BYTES);
            buffer.putInt(denialCount);
            buffer.putLong(pauseStartedTimestamp.toEpochMilli());
            return Base64.getEncoder().encodeToString(buffer.array());
        }

        /** Deserializes a string from storage into a {@link DenialState} object. */
        static DenialState fromPreferenceString(String preferenceString) {
            if (preferenceString == null) {
                return new DenialState();
            }
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(preferenceString);
            } catch (IllegalArgumentException e) {
                Slog.e(
                        TAG,
                        "Invalid Base64 format in denial state preference string: "
                                + preferenceString,
                        e);
                return new DenialState();
            }
            if (bytes.length != DENIAL_COUNT_BYTES + TIMESTAMP_BYTES) {
                Slog.e(TAG, "Malformed denial state preference string: " + preferenceString);
                return new DenialState();
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int counter = buffer.getInt();
            long timestampEpochMilli = buffer.getLong();
            return new DenialState(counter, Instant.ofEpochMilli(timestampEpochMilli));
        }
    }
}
