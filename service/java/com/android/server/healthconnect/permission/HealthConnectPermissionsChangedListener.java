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

package com.android.server.healthconnect.permission;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;

/**
 * Listener for permission changes happening across all apps for all users on device.
 *
 * @hide
 */
public class HealthConnectPermissionsChangedListener
        implements PackageManager.OnPermissionsChangedListener {

    private final Context mContext;
    private final FirstGrantTimeManager mFirstGrantTimeManager;

    public HealthConnectPermissionsChangedListener(
            Context context, FirstGrantTimeManager firstGrantTimeManager) {
        mContext = context;
        mFirstGrantTimeManager = firstGrantTimeManager;
    }

    /**
     * Registers a {@link PackageManager.OnPermissionsChangedListener} that updates first grant
     * times based on permission changes.
     */
    @SuppressLint("MissingPermission")
    public void registerPermissionsChangeListener() {
        PackageManager packageManager = mContext.getPackageManager();
        packageManager.addOnPermissionsChangeListener(this);
    }

    @Override
    public void onPermissionsChanged(int uid) {
        mFirstGrantTimeManager.updateFirstGrantTimesFromPermissionState(
                UserHandle.getUserHandleForUid(uid), uid, /* sync= */ false);
    }
}
