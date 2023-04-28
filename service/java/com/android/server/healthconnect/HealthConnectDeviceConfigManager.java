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

package com.android.server.healthconnect;

import android.annotation.NonNull;
import android.content.Context;
import android.health.connect.ratelimiter.RateLimiter;
import android.health.connect.ratelimiter.RateLimiter.QuotaBucket;
import android.provider.DeviceConfig;

import com.android.internal.annotations.GuardedBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Singleton class to provide values and listen changes of settings flags.
 *
 * @hide
 */
public class HealthConnectDeviceConfigManager implements DeviceConfig.OnPropertiesChangedListener {
    public static final String EXERCISE_ROUTE_FEATURE_FLAG = "exercise_routes_enable";
    public static final String ENABLE_RATE_LIMITER_FLAG = "enable_rate_limiter";
    private static final String MAX_READ_REQUESTS_PER_24H_FOREGROUND_FLAG =
            "max_read_requests_per_24h_foreground";
    private static final String MAX_READ_REQUESTS_PER_24H_BACKGROUND_FLAG =
            "max_read_requests_per_24h_background";
    private static final String MAX_READ_REQUESTS_PER_15M_FOREGROUND_FLAG =
            "max_read_requests_per_15m_foreground";
    private static final String MAX_READ_REQUESTS_PER_15M_BACKGROUND_FLAG =
            "max_read_requests_per_15m_background";
    private static final String MAX_WRITE_REQUESTS_PER_24H_FOREGROUND_FLAG =
            "max_write_requests_per_24h_foreground";
    private static final String MAX_WRITE_REQUESTS_PER_24H_BACKGROUND_FLAG =
            "max_write_requests_per_24h_background";
    private static final String MAX_WRITE_REQUESTS_PER_15M_FOREGROUND_FLAG =
            "max_write_requests_per_15m_foreground";
    private static final String MAX_WRITE_REQUESTS_PER_15M_BACKGROUND_FLAG =
            "max_write_requests_per_15m_background";
    private static final String MAX_WRITE_CHUNK_SIZE_FLAG = "max_write_chunk_size";
    private static final String MAX_WRITE_SINGLE_RECORD_SIZE_FLAG = "max_write_single_record_size";

    // Flag to enable/disable sleep and exercise sessions.
    public static final String SESSION_DATATYPE_FEATURE_FLAG = "session_types_enable";

    public static final boolean EXERCISE_ROUTE_DEFAULT_FLAG_VALUE = true;
    public static final boolean ENABLE_RATE_LIMITER_DEFAULT_FLAG_VALUE = true;
    public static final int QUOTA_BUCKET_PER_15M_FOREGROUND_DEFAULT_FLAG_VALUE = 1000;
    public static final int QUOTA_BUCKET_PER_24H_FOREGROUND_DEFAULT_FLAG_VALUE = 5000;
    public static final int QUOTA_BUCKET_PER_15M_BACKGROUND_DEFAULT_FLAG_VALUE = 300;
    public static final int QUOTA_BUCKET_PER_24H_BACKGROUND_DEFAULT_FLAG_VALUE = 5000;
    public static final int CHUNK_SIZE_LIMIT_IN_BYTES_DEFAULT_FLAG_VALUE = 5000000;
    public static final int RECORD_SIZE_LIMIT_IN_BYTES_DEFAULT_FLAG_VALUE = 1000000;

    public static final boolean SESSION_DATATYPE_DEFAULT_FLAG_VALUE = true;

    private static HealthConnectDeviceConfigManager sDeviceConfigManager;
    private final ReentrantReadWriteLock mLock = new ReentrantReadWriteLock();

    @GuardedBy("mLock")
    private boolean mExerciseRouteEnabled =
            DeviceConfig.getBoolean(
                    DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                    EXERCISE_ROUTE_FEATURE_FLAG,
                    EXERCISE_ROUTE_DEFAULT_FLAG_VALUE);

    @GuardedBy("mLock")
    private boolean mSessionDatatypeEnabled =
            DeviceConfig.getBoolean(
                    DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                    SESSION_DATATYPE_FEATURE_FLAG,
                    SESSION_DATATYPE_DEFAULT_FLAG_VALUE);

    @NonNull
    static void initializeInstance(Context context) {
        if (sDeviceConfigManager == null) {
            sDeviceConfigManager = new HealthConnectDeviceConfigManager();
            DeviceConfig.addOnPropertiesChangedListener(
                    DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                    context.getMainExecutor(),
                    sDeviceConfigManager);
        }
    }

    /** Returns initialised instance of this class. */
    @NonNull
    public static HealthConnectDeviceConfigManager getInitialisedInstance() {
        Objects.requireNonNull(sDeviceConfigManager);

        return sDeviceConfigManager;
    }

    /** Returns if operations with exercise route are enabled. */
    public boolean isExerciseRouteFeatureEnabled() {
        mLock.readLock().lock();
        try {
            return mExerciseRouteEnabled;
        } finally {
            mLock.readLock().unlock();
        }
    }

    @GuardedBy("mLock")
    private boolean mRateLimiterEnabled =
            DeviceConfig.getBoolean(
                    DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                    ENABLE_RATE_LIMITER_FLAG,
                    ENABLE_RATE_LIMITER_DEFAULT_FLAG_VALUE);

    /** Returns if operations with sessions datatypes are enabled. */
    public boolean isSessionDatatypeFeatureEnabled() {
        mLock.readLock().lock();
        try {
            return mSessionDatatypeEnabled;
        } finally {
            mLock.readLock().unlock();
        }
    }

    /** Updates rate limiting quota values. */
    public void updateRateLimiterValues() {
        Map<Integer, Integer> quotaBucketToMaxApiCallQuotaMap = new HashMap<>();
        Map<String, Integer> quotaBucketToMaxMemoryQuotaMap = new HashMap<>();
        quotaBucketToMaxApiCallQuotaMap.put(
                QuotaBucket.QUOTA_BUCKET_READS_PER_24H_FOREGROUND,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_READ_REQUESTS_PER_24H_FOREGROUND_FLAG,
                        QUOTA_BUCKET_PER_24H_FOREGROUND_DEFAULT_FLAG_VALUE));
        quotaBucketToMaxApiCallQuotaMap.put(
                QuotaBucket.QUOTA_BUCKET_READS_PER_24H_BACKGROUND,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_READ_REQUESTS_PER_24H_BACKGROUND_FLAG,
                        QUOTA_BUCKET_PER_24H_BACKGROUND_DEFAULT_FLAG_VALUE));
        quotaBucketToMaxApiCallQuotaMap.put(
                QuotaBucket.QUOTA_BUCKET_READS_PER_15M_FOREGROUND,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_READ_REQUESTS_PER_15M_FOREGROUND_FLAG,
                        QUOTA_BUCKET_PER_15M_FOREGROUND_DEFAULT_FLAG_VALUE));
        quotaBucketToMaxApiCallQuotaMap.put(
                QuotaBucket.QUOTA_BUCKET_READS_PER_15M_BACKGROUND,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_READ_REQUESTS_PER_15M_BACKGROUND_FLAG,
                        QUOTA_BUCKET_PER_15M_BACKGROUND_DEFAULT_FLAG_VALUE));
        quotaBucketToMaxApiCallQuotaMap.put(
                QuotaBucket.QUOTA_BUCKET_WRITES_PER_24H_FOREGROUND,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_WRITE_REQUESTS_PER_24H_FOREGROUND_FLAG,
                        QUOTA_BUCKET_PER_24H_FOREGROUND_DEFAULT_FLAG_VALUE));
        quotaBucketToMaxApiCallQuotaMap.put(
                QuotaBucket.QUOTA_BUCKET_WRITES_PER_24H_BACKGROUND,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_WRITE_REQUESTS_PER_24H_BACKGROUND_FLAG,
                        QUOTA_BUCKET_PER_24H_BACKGROUND_DEFAULT_FLAG_VALUE));
        quotaBucketToMaxApiCallQuotaMap.put(
                QuotaBucket.QUOTA_BUCKET_WRITES_PER_15M_FOREGROUND,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_WRITE_REQUESTS_PER_15M_FOREGROUND_FLAG,
                        QUOTA_BUCKET_PER_15M_FOREGROUND_DEFAULT_FLAG_VALUE));
        quotaBucketToMaxApiCallQuotaMap.put(
                QuotaBucket.QUOTA_BUCKET_WRITES_PER_15M_BACKGROUND,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_WRITE_REQUESTS_PER_15M_BACKGROUND_FLAG,
                        QUOTA_BUCKET_PER_15M_BACKGROUND_DEFAULT_FLAG_VALUE));
        quotaBucketToMaxMemoryQuotaMap.put(
                RateLimiter.CHUNK_SIZE_LIMIT_IN_BYTES,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_WRITE_CHUNK_SIZE_FLAG,
                        CHUNK_SIZE_LIMIT_IN_BYTES_DEFAULT_FLAG_VALUE));
        quotaBucketToMaxMemoryQuotaMap.put(
                RateLimiter.RECORD_SIZE_LIMIT_IN_BYTES,
                DeviceConfig.getInt(
                        DeviceConfig.NAMESPACE_HEALTH_FITNESS,
                        MAX_WRITE_SINGLE_RECORD_SIZE_FLAG,
                        RECORD_SIZE_LIMIT_IN_BYTES_DEFAULT_FLAG_VALUE));
        RateLimiter.updateApiCallQuotaMap(quotaBucketToMaxApiCallQuotaMap);
        RateLimiter.updateMemoryQuotaMap(quotaBucketToMaxMemoryQuotaMap);
        mLock.readLock().lock();
        try {
            RateLimiter.updateEnableRateLimiterFlag(mRateLimiterEnabled);
        } finally {
            mLock.readLock().unlock();
        }
    }

    @Override
    public void onPropertiesChanged(DeviceConfig.Properties properties) {
        if (!properties.getNamespace().equals(DeviceConfig.NAMESPACE_HEALTH_FITNESS)) {
            return;
        }
        for (String name : properties.getKeyset()) {
            if (name == null) {
                continue;
            }

            if (name.equals(EXERCISE_ROUTE_FEATURE_FLAG)) {
                mLock.writeLock().lock();
                try {
                    mExerciseRouteEnabled =
                            properties.getBoolean(
                                    EXERCISE_ROUTE_FEATURE_FLAG, EXERCISE_ROUTE_DEFAULT_FLAG_VALUE);
                } finally {
                    mLock.writeLock().unlock();
                }
            } else if (name.equals(SESSION_DATATYPE_FEATURE_FLAG)) {
                mLock.writeLock().lock();
                try {
                    mSessionDatatypeEnabled =
                            properties.getBoolean(
                                    SESSION_DATATYPE_FEATURE_FLAG,
                                    SESSION_DATATYPE_DEFAULT_FLAG_VALUE);
                } finally {
                    mLock.writeLock().unlock();
                }
            } else if (name.equals(ENABLE_RATE_LIMITER_FLAG)) {
                mLock.writeLock().lock();
                try {
                    mRateLimiterEnabled =
                            properties.getBoolean(
                                    ENABLE_RATE_LIMITER_FLAG,
                                    ENABLE_RATE_LIMITER_DEFAULT_FLAG_VALUE);
                    RateLimiter.updateEnableRateLimiterFlag(mRateLimiterEnabled);
                } finally {
                    mLock.writeLock().unlock();
                }
            }
        }
    }
}
