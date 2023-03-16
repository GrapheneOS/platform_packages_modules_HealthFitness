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
import android.provider.DeviceConfig;

import com.android.internal.annotations.GuardedBy;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Singleton class to provide values and listen changes of settings flags.
 *
 * @hide
 */
public class HealthConnectDeviceConfigManager implements DeviceConfig.OnPropertiesChangedListener {
    public static final String EXERCISE_ROUTE_FEATURE_FLAG = "exercise_routes_enable";

    // Flag to enable/disable sleep and exercise sessions.
    public static final String SESSION_DATATYPE_FEATURE_FLAG = "session_types_enable";

    public static final boolean EXERCISE_ROUTE_DEFAULT_FLAG_VALUE = true;

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

    /** Returns if operations with sessions datatypes are enabled. */
    public boolean isSessionDatatypeFeatureEnabled() {
        mLock.readLock().lock();
        try {
            return mSessionDatatypeEnabled;
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
            }
        }
    }
}
