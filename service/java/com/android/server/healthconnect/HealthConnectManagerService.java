/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.healthconnect.HealthConnectManager;
import android.util.Slog;

import com.android.server.SystemService;
import com.android.server.healthconnect.permission.FirstGrantTimeDatastore;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.permission.PackagePermissionChangesMonitor;
import com.android.server.healthconnect.storage.AutoDeleteService;
import com.android.server.healthconnect.storage.TransactionManager;

import java.util.Objects;

/**
 * HealthConnect system service scaffold.
 *
 * @hide
 */
public class HealthConnectManagerService extends SystemService {
    private static final String TAG = "HealthConnectManagerService";
    private final Context mContext;
    private final HealthConnectPermissionHelper mPermissionHelper;
    private final TransactionManager mTransactionManager;
    private final HealthPermissionIntentAppsTracker mPermissionIntentTracker;
    private final PackagePermissionChangesMonitor mPackageMonitor;
    private final FirstGrantTimeManager mFirstGrantTimeManager;

    public HealthConnectManagerService(Context context) {
        super(context);
        mPermissionIntentTracker = new HealthPermissionIntentAppsTracker(context);
        mPermissionHelper =
                new HealthConnectPermissionHelper(
                        context,
                        context.getPackageManager(),
                        HealthConnectManager.getHealthPermissions(context),
                        mPermissionIntentTracker);
        mFirstGrantTimeManager =
                new FirstGrantTimeManager(
                        context,
                        mPermissionIntentTracker,
                        FirstGrantTimeDatastore.createInstance());
        mPackageMonitor =
                new PackagePermissionChangesMonitor(
                        mPermissionIntentTracker, mFirstGrantTimeManager);
        mTransactionManager = TransactionManager.getInstance(getContext());
        mContext = context;
    }

    @Override
    public void onStart() {
        mFirstGrantTimeManager.initializeState();
        mPackageMonitor.registerBroadcastReceiver(mContext);
        publishBinderService(
                Context.HEALTHCONNECT_SERVICE,
                new HealthConnectServiceImpl(mTransactionManager, mPermissionHelper, mContext));
    }

    @Override
    public void onUserUnlocking(@NonNull TargetUser user) {
        Objects.requireNonNull(user);
        try {
            AutoDeleteService.schedule(mContext, user.getUserHandle().getIdentifier());
        } catch (Exception e) {
            Slog.e(TAG, "Auto delete schedule failed", e);
        }
    }

    @Override
    public void onUserStopping(@NonNull TargetUser user) {
        Objects.requireNonNull(user);
        try {
            AutoDeleteService.stop(mContext, user.getUserHandle().getIdentifier());
        } catch (Exception e) {
            Slog.e(TAG, "Auto delete stop failed", e);
        }
    }
}
