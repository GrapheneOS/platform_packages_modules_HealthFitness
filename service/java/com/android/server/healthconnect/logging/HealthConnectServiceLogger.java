/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.healthconnect.logging;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__API_METHOD_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__DELETE_DATA;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__GET_CHANGES;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__GET_CHANGES_TOKEN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__GET_GRANTED_PERMISSIONS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__INSERT_DATA;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__READ_AGGREGATED_DATA;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__READ_DATA;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__REVOKE_ALL_PERMISSIONS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_METHOD__UPDATE_DATA;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_STATUS__ERROR;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_STATUS__STATUS_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.health.HealthFitnessStatsLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Class to log metrics from HealthConnectService
 *
 * @hide
 */
public class HealthConnectServiceLogger {

    private final int mHealthDataServiceApiMethod;
    private final int mHealthDataServiceApiStatus;
    private final int mErrorCode;
    private final long mDuration;
    private final boolean mHoldsDataManagementPermission;

    /**
     * HealthConnectService ApiMethods supported by logging.
     *
     * @hide
     */
    public static final class ApiMethods {

        public static final int API_METHOD_UNKNOWN =
                HEALTH_CONNECT_API_CALLED__API_METHOD__API_METHOD_UNKNOWN;
        public static final int DELETE_DATA = HEALTH_CONNECT_API_CALLED__API_METHOD__DELETE_DATA;
        public static final int GET_CHANGES = HEALTH_CONNECT_API_CALLED__API_METHOD__GET_CHANGES;
        public static final int GET_CHANGES_TOKEN =
                HEALTH_CONNECT_API_CALLED__API_METHOD__GET_CHANGES_TOKEN;
        public static final int GET_GRANTED_PERMISSIONS =
                HEALTH_CONNECT_API_CALLED__API_METHOD__GET_GRANTED_PERMISSIONS;
        public static final int INSERT_DATA = HEALTH_CONNECT_API_CALLED__API_METHOD__INSERT_DATA;
        public static final int READ_AGGREGATED_DATA =
                HEALTH_CONNECT_API_CALLED__API_METHOD__READ_AGGREGATED_DATA;
        public static final int READ_DATA = HEALTH_CONNECT_API_CALLED__API_METHOD__READ_DATA;
        public static final int REVOKE_ALL_PERMISSIONS =
                HEALTH_CONNECT_API_CALLED__API_METHOD__REVOKE_ALL_PERMISSIONS;
        public static final int UPDATE_DATA = HEALTH_CONNECT_API_CALLED__API_METHOD__UPDATE_DATA;

        @IntDef({
            API_METHOD_UNKNOWN,
            DELETE_DATA,
            GET_CHANGES,
            GET_CHANGES_TOKEN,
            GET_GRANTED_PERMISSIONS,
            INSERT_DATA,
            READ_AGGREGATED_DATA,
            READ_DATA,
            REVOKE_ALL_PERMISSIONS,
            UPDATE_DATA,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface ApiMethod {}
    }

    /**
     * Builder for HealthConnectServiceLogger
     *
     * @hide
     */
    public static class Builder {

        private final long mStartTime;
        private final int mHealthDataServiceApiMethod;
        private int mHealthDataServiceApiStatus;
        private int mErrorCode;
        private long mDuration;
        private final boolean mHoldsDataManagementPermission;

        public Builder(boolean holdsDataManagementPermission, @ApiMethods.ApiMethod int apiMethod) {
            mStartTime = System.currentTimeMillis();
            mHealthDataServiceApiMethod = apiMethod;
            mHealthDataServiceApiStatus = HEALTH_CONNECT_API_CALLED__API_STATUS__STATUS_UNKNOWN;
            mErrorCode = -1; // Means no error
            mHoldsDataManagementPermission = holdsDataManagementPermission;
        }

        /** Set the API was called successfully. */
        public Builder setHealthDataServiceApiStatusSuccess() {
            this.mHealthDataServiceApiStatus = HEALTH_CONNECT_API_CALLED__API_STATUS__SUCCESS;
            return this;
        }

        /**
         * Set the API threw error.
         *
         * @param errorCode Error code thrown by the API.
         */
        public Builder setHealthDataServiceApiStatusError(int errorCode) {
            this.mErrorCode = errorCode;
            this.mHealthDataServiceApiStatus = HEALTH_CONNECT_API_CALLED__API_STATUS__ERROR;
            return this;
        }

        /** Returns an object of {@link HealthConnectServiceLogger}. */
        public HealthConnectServiceLogger build() {
            mDuration = System.currentTimeMillis() - mStartTime;
            return new HealthConnectServiceLogger(this);
        }
    }

    private HealthConnectServiceLogger(@NonNull HealthConnectServiceLogger.Builder builder) {
        Objects.requireNonNull(builder);

        mHealthDataServiceApiMethod = builder.mHealthDataServiceApiMethod;
        mHealthDataServiceApiStatus = builder.mHealthDataServiceApiStatus;
        mErrorCode = builder.mErrorCode;
        mDuration = builder.mDuration;
        mHoldsDataManagementPermission = builder.mHoldsDataManagementPermission;
    }

    /** Log to statsd. */
    public void log() {

        // Do not log API calls made from the controller
        if (mHoldsDataManagementPermission) {
            return;
        }
        HealthFitnessStatsLog.write(
                HEALTH_CONNECT_API_CALLED,
                mHealthDataServiceApiMethod,
                mHealthDataServiceApiStatus,
                mErrorCode,
                mDuration);
    }
}
