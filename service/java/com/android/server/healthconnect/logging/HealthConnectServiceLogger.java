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
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__NOT_DEFINED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__NOT_USED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_ABOVE_3000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_BW_1000_TO_2000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_BW_2000_TO_3000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_BW_500_TO_1000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_UNDER_500;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_ABOVE_5000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_BW_1000_TO_2000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_BW_2000_TO_3000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_BW_3000_TO_4000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_BW_4000_TO_5000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_UNDER_1000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_ABOVE_4000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_BW_1000_TO_2000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_BW_2000_TO_3000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_BW_3000_TO_4000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_UNDER_1000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_ABOVE_6000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_BW_2000_TO_3000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_BW_3000_TO_4000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_BW_4000_TO_5000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_BW_5000_TO_6000;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_UNDER_2000;
import static android.health.connect.ratelimiter.RateLimiter.QuotaBucket.QUOTA_BUCKET_READS_PER_15M_BACKGROUND;
import static android.health.connect.ratelimiter.RateLimiter.QuotaBucket.QUOTA_BUCKET_READS_PER_15M_FOREGROUND;
import static android.health.connect.ratelimiter.RateLimiter.QuotaBucket.QUOTA_BUCKET_READS_PER_24H_BACKGROUND;
import static android.health.connect.ratelimiter.RateLimiter.QuotaBucket.QUOTA_BUCKET_READS_PER_24H_FOREGROUND;
import static android.health.connect.ratelimiter.RateLimiter.QuotaBucket.QUOTA_BUCKET_WRITES_PER_15M_BACKGROUND;
import static android.health.connect.ratelimiter.RateLimiter.QuotaBucket.QUOTA_BUCKET_WRITES_PER_15M_FOREGROUND;
import static android.health.connect.ratelimiter.RateLimiter.QuotaBucket.QUOTA_BUCKET_WRITES_PER_24H_BACKGROUND;
import static android.health.connect.ratelimiter.RateLimiter.QuotaBucket.QUOTA_BUCKET_WRITES_PER_24H_FOREGROUND;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.health.HealthFitnessStatsLog;
import android.health.connect.ratelimiter.RateLimiter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
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
    private final int mRateLimit;
    private final int mNumberOfRecords;

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
     * Rate limiting ranges differentiated by Foreground/Background.
     *
     * @hide
     */
    public static final class RateLimitingRanges {

        public static final int NOT_USED = HEALTH_CONNECT_API_CALLED__RATE_LIMIT__NOT_USED;
        public static final int NOT_DEFINED = HEALTH_CONNECT_API_CALLED__RATE_LIMIT__NOT_DEFINED;
        public static final int FOREGROUND_15_MIN_UNDER_1000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_UNDER_1000;
        public static final int FOREGROUND_15_MIN_BW_1000_TO_2000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_BW_1000_TO_2000;
        public static final int FOREGROUND_15_MIN_BW_2000_TO_3000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_BW_2000_TO_3000;
        public static final int FOREGROUND_15_MIN_BW_3000_TO_4000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_BW_3000_TO_4000;
        public static final int FOREGROUND_15_MIN_ABOVE_4000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_15_MIN_ABOVE_4000;
        public static final int BACKGROUND_15_MIN_UNDER_500 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_UNDER_500;
        public static final int BACKGROUND_15_MIN_BW_500_TO_1000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_BW_500_TO_1000;
        public static final int BACKGROUND_15_MIN_BW_1000_TO_2000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_BW_1000_TO_2000;
        public static final int BACKGROUND_15_MIN_BW_2000_TO_3000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_BW_2000_TO_3000;
        public static final int BACKGROUND_15_MIN_ABOVE_3000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_15_MIN_ABOVE_3000;
        public static final int FOREGROUND_24_HRS_UNDER_2000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_UNDER_2000;
        public static final int FOREGROUND_24_HRS_BW_2000_TO_3000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_BW_2000_TO_3000;
        public static final int FOREGROUND_24_HRS_BW_3000_TO_4000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_BW_3000_TO_4000;
        public static final int FOREGROUND_24_HRS_BW_4000_TO_5000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_BW_4000_TO_5000;
        public static final int FOREGROUND_24_HRS_BW_5000_TO_6000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_BW_5000_TO_6000;
        public static final int FOREGROUND_24_HRS_ABOVE_6000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_FOREGROUND_24_HRS_ABOVE_6000;
        public static final int BACKGROUND_24_HRS_UNDER_1000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_UNDER_1000;
        public static final int BACKGROUND_24_HRS_BW_1000_TO_2000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_BW_1000_TO_2000;
        public static final int BACKGROUND_24_HRS_BW_2000_TO_3000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_BW_2000_TO_3000;
        public static final int BACKGROUND_24_HRS_BW_3000_TO_4000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_BW_3000_TO_4000;
        public static final int BACKGROUND_24_HRS_BW_4000_TO_5000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_BW_4000_TO_5000;
        public static final int BACKGROUND_24_HRS_ABOVE_5000 =
                HEALTH_CONNECT_API_CALLED__RATE_LIMIT__RATE_LIMIT_BACKGROUND_24_HRS_ABOVE_5000;

        private static final Map<Integer, Integer> sForeground15min =
                Map.of(
                        0,
                        FOREGROUND_15_MIN_UNDER_1000,
                        1,
                        FOREGROUND_15_MIN_BW_1000_TO_2000,
                        2,
                        BACKGROUND_15_MIN_BW_2000_TO_3000,
                        3,
                        FOREGROUND_15_MIN_BW_3000_TO_4000);

        private static final Map<Integer, Integer> sForeground24hour =
                Map.of(
                        0,
                        FOREGROUND_24_HRS_UNDER_2000,
                        1,
                        FOREGROUND_24_HRS_UNDER_2000,
                        2,
                        FOREGROUND_24_HRS_BW_2000_TO_3000,
                        3,
                        FOREGROUND_24_HRS_BW_3000_TO_4000,
                        4,
                        FOREGROUND_24_HRS_BW_4000_TO_5000,
                        5,
                        FOREGROUND_24_HRS_BW_5000_TO_6000);

        private static final Map<Integer, Integer> sBackground15Min =
                Map.of(
                        0,
                        BACKGROUND_15_MIN_BW_500_TO_1000,
                        1,
                        BACKGROUND_15_MIN_BW_1000_TO_2000,
                        2,
                        BACKGROUND_15_MIN_BW_2000_TO_3000);

        private static final Map<Integer, Integer> sBackground24Hour =
                Map.of(
                        0,
                        BACKGROUND_24_HRS_UNDER_1000,
                        1,
                        BACKGROUND_24_HRS_BW_1000_TO_2000,
                        2,
                        BACKGROUND_24_HRS_BW_2000_TO_3000,
                        3,
                        BACKGROUND_24_HRS_BW_3000_TO_4000,
                        4,
                        BACKGROUND_24_HRS_BW_4000_TO_5000);

        @IntDef({
            NOT_USED,
            FOREGROUND_15_MIN_UNDER_1000,
            FOREGROUND_15_MIN_BW_1000_TO_2000,
            FOREGROUND_15_MIN_BW_2000_TO_3000,
            FOREGROUND_15_MIN_BW_3000_TO_4000,
            FOREGROUND_15_MIN_ABOVE_4000,
            BACKGROUND_15_MIN_UNDER_500,
            BACKGROUND_15_MIN_BW_500_TO_1000,
            BACKGROUND_15_MIN_BW_1000_TO_2000,
            BACKGROUND_15_MIN_BW_2000_TO_3000,
            BACKGROUND_15_MIN_ABOVE_3000,
            FOREGROUND_24_HRS_UNDER_2000,
            FOREGROUND_24_HRS_BW_2000_TO_3000,
            FOREGROUND_24_HRS_BW_3000_TO_4000,
            FOREGROUND_24_HRS_BW_4000_TO_5000,
            FOREGROUND_24_HRS_BW_5000_TO_6000,
            FOREGROUND_24_HRS_ABOVE_6000,
            BACKGROUND_24_HRS_UNDER_1000,
            BACKGROUND_24_HRS_BW_1000_TO_2000,
            BACKGROUND_24_HRS_BW_2000_TO_3000,
            BACKGROUND_24_HRS_BW_3000_TO_4000,
            BACKGROUND_24_HRS_BW_4000_TO_5000,
            BACKGROUND_24_HRS_ABOVE_5000
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface RateLimit {}
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
        private int mRateLimit;
        private int mNumberOfRecords;
        private final boolean mHoldsDataManagementPermission;

        public Builder(boolean holdsDataManagementPermission, @ApiMethods.ApiMethod int apiMethod) {
            mStartTime = System.currentTimeMillis();
            mHealthDataServiceApiMethod = apiMethod;
            mHealthDataServiceApiStatus = HEALTH_CONNECT_API_CALLED__API_STATUS__STATUS_UNKNOWN;
            mErrorCode = 0; // Means no error
            mHoldsDataManagementPermission = holdsDataManagementPermission;
            mRateLimit = RateLimitingRanges.NOT_USED;
            mNumberOfRecords = 0;
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

        /**
         * Set the rate limiting range if used.
         *
         * @param quotaBucket Quota bucket.
         * @param quotaLimit Bucket limit.
         */
        public Builder setRateLimit(
                @RateLimiter.QuotaBucket.Type int quotaBucket, float quotaLimit) {
            this.mRateLimit = calculateRateLimitEnum(quotaBucket, quotaLimit);
            return this;
        }

        /**
         * Set the number of records involved in the API call.
         *
         * @param numberOfRecords Number of records.
         */
        public Builder setNumberOfRecords(int numberOfRecords) {
            this.mNumberOfRecords = numberOfRecords;
            return this;
        }

        /** Returns an object of {@link HealthConnectServiceLogger}. */
        public HealthConnectServiceLogger build() {
            mDuration = System.currentTimeMillis() - mStartTime;
            return new HealthConnectServiceLogger(this);
        }

        private int calculateRateLimitEnum(
                @RateLimiter.QuotaBucket.Type int quotaBucket, float quotaLimit) {
            int quotient = (int) (quotaLimit / 1000);
            switch (quotaBucket) {
                case QUOTA_BUCKET_READS_PER_15M_FOREGROUND:
                case QUOTA_BUCKET_WRITES_PER_15M_FOREGROUND:
                    return RateLimitingRanges.sForeground15min.getOrDefault(
                            quotient, RateLimitingRanges.FOREGROUND_15_MIN_ABOVE_4000);
                case QUOTA_BUCKET_READS_PER_24H_FOREGROUND:
                case QUOTA_BUCKET_WRITES_PER_24H_FOREGROUND:
                    return RateLimitingRanges.sForeground24hour.getOrDefault(
                            quotient, RateLimitingRanges.FOREGROUND_24_HRS_ABOVE_6000);
                case QUOTA_BUCKET_READS_PER_15M_BACKGROUND:
                case QUOTA_BUCKET_WRITES_PER_15M_BACKGROUND:
                    if (quotaLimit < 500) {
                        return RateLimitingRanges.BACKGROUND_15_MIN_UNDER_500;
                    }
                    return RateLimitingRanges.sBackground15Min.getOrDefault(
                            quotient, RateLimitingRanges.BACKGROUND_15_MIN_ABOVE_3000);
                case QUOTA_BUCKET_READS_PER_24H_BACKGROUND:
                case QUOTA_BUCKET_WRITES_PER_24H_BACKGROUND:
                    return RateLimitingRanges.sBackground24Hour.getOrDefault(
                            quotient, RateLimitingRanges.BACKGROUND_24_HRS_ABOVE_5000);
            }
            return RateLimitingRanges.NOT_DEFINED;
        }
    }

    private HealthConnectServiceLogger(@NonNull HealthConnectServiceLogger.Builder builder) {
        Objects.requireNonNull(builder);

        mHealthDataServiceApiMethod = builder.mHealthDataServiceApiMethod;
        mHealthDataServiceApiStatus = builder.mHealthDataServiceApiStatus;
        mErrorCode = builder.mErrorCode;
        mDuration = builder.mDuration;
        mHoldsDataManagementPermission = builder.mHoldsDataManagementPermission;
        mRateLimit = builder.mRateLimit;
        mNumberOfRecords = builder.mNumberOfRecords;
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
                mDuration,
                mNumberOfRecords,
                mRateLimit);
    }
}
