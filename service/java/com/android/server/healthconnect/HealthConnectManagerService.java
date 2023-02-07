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
import android.annotation.Nullable;
import android.content.Context;
import android.health.connect.HealthConnectManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;

import com.android.server.SystemService;
import com.android.server.healthconnect.migration.MigrationBroadcast;
import com.android.server.healthconnect.permission.FirstGrantTimeDatastore;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.permission.PackagePermissionChangesMonitor;
import com.android.server.healthconnect.storage.AutoDeleteService;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

import java.util.Objects;

/**
 * HealthConnect system service scaffold.
 *
 * @hide
 */
public class HealthConnectManagerService extends SystemService {
    private static final String TAG = "HealthConnectManagerService";
    private final Context mContext;
    private final PackagePermissionChangesMonitor mPackageMonitor;
    private final HealthConnectServiceImpl mHealthConnectService;
    private final TransactionManager mTransactionManager;
    private final UserManager mUserManager;
    private UserHandle mCurrentUser;

    public HealthConnectManagerService(Context context) {
        super(context);
        HealthPermissionIntentAppsTracker permissionIntentTracker =
                new HealthPermissionIntentAppsTracker(context);
        FirstGrantTimeManager firstGrantTimeManager =
                new FirstGrantTimeManager(
                        context, permissionIntentTracker, FirstGrantTimeDatastore.createInstance());
        HealthConnectPermissionHelper permissionHelper =
                new HealthConnectPermissionHelper(
                        context,
                        context.getPackageManager(),
                        HealthConnectManager.getHealthPermissions(context),
                        permissionIntentTracker,
                        firstGrantTimeManager);
        mPackageMonitor =
                new PackagePermissionChangesMonitor(permissionIntentTracker, firstGrantTimeManager);
        mCurrentUser = context.getUser();
        mContext = context;
        mUserManager = mContext.getSystemService(UserManager.class);
        mTransactionManager =
                TransactionManager.getInstance(
                        new HealthConnectUserContext(mContext, mCurrentUser));
        mHealthConnectService =
                new HealthConnectServiceImpl(
                        mTransactionManager, permissionHelper, firstGrantTimeManager, mContext);
    }

    @Override
    public void onStart() {
        mPackageMonitor.registerBroadcastReceiver(mContext);
        publishBinderService(Context.HEALTHCONNECT_SERVICE, mHealthConnectService);
    }

    @Override
    public void onUserSwitching(@Nullable TargetUser from, @NonNull TargetUser to) {
        HealthConnectThreadScheduler.shutdownThreadPools();
        AppInfoHelper.getInstance().clearCache();
        DeviceInfoHelper.getInstance().clearCache();
        HealthDataCategoryPriorityHelper.getInstance().clearCache();
        PreferenceHelper.getInstance().clearCache();
        mTransactionManager.onUserSwitching();
    }

    @Override
    public void onUserUnlocking(@NonNull TargetUser user) {
        Objects.requireNonNull(user);

        HealthConnectThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        // TODO(b/267255123): Send broadcast up to 10 times with a 60s delay
                        // (configurable)
                        MigrationBroadcast migrationBroadcast = new MigrationBroadcast(mContext);
                        migrationBroadcast.sendInvocationBroadcast();
                    } catch (Exception e) {
                        Slog.e(TAG, "Sending migration broadcast failed", e);
                    }
                });

        if (user.getUserHandle().equals(mCurrentUser)) {
            // The current setup in place is for {@code user} only, so just ignore this request
            return;
        }

        mCurrentUser = user.getUserHandle();
        mTransactionManager.onUserUnlocking(new HealthConnectUserContext(mContext, mCurrentUser));
        HealthConnectThreadScheduler.resetThreadPools();
        HealthConnectThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        AutoDeleteService.schedule(mContext, mCurrentUser.getIdentifier());
                    } catch (Exception e) {
                        Slog.e(TAG, "Auto delete schedule failed", e);
                    }
                });
    }

    @Override
    public boolean isUserSupported(@NonNull TargetUser user) {
        return !mUserManager.isManagedProfile(user.getUserHandle().getIdentifier());
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
