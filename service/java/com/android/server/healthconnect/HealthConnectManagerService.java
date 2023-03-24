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
import android.health.connect.ratelimiter.RateLimiter;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;

import com.android.server.SystemService;
import com.android.server.healthconnect.migration.MigrationBroadcastScheduler;
import com.android.server.healthconnect.migration.MigrationCleaner;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.migration.MigratorPackageChangesReceiver;
import com.android.server.healthconnect.migration.PriorityMigrationHelper;
import com.android.server.healthconnect.permission.FirstGrantTimeDatastore;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.permission.PermissionPackageChangesOrchestrator;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MigrationEntityHelper;
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
    private final PermissionPackageChangesOrchestrator mPermissionPackageChangesOrchestrator;
    private final HealthConnectServiceImpl mHealthConnectService;
    private final TransactionManager mTransactionManager;
    private final UserManager mUserManager;
    private final MigrationBroadcastScheduler mMigrationBroadcastScheduler;
    private UserHandle mCurrentForegroundUser;

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
        mPermissionPackageChangesOrchestrator =
                new PermissionPackageChangesOrchestrator(
                        permissionIntentTracker, firstGrantTimeManager, permissionHelper);
        mUserManager = context.getSystemService(UserManager.class);
        mCurrentForegroundUser = context.getUser();
        mContext = context;
        mTransactionManager =
                TransactionManager.getInstance(
                        new HealthConnectUserContext(mContext, mCurrentForegroundUser));
        HealthConnectDeviceConfigManager.initializeInstance(context);
        mMigrationBroadcastScheduler =
                new MigrationBroadcastScheduler(mCurrentForegroundUser.getIdentifier());
        final MigrationStateManager migrationStateManager =
                MigrationStateManager.initializeInstance(mCurrentForegroundUser.getIdentifier());
        migrationStateManager.setMigrationBroadcastScheduler(mMigrationBroadcastScheduler);
        final MigrationCleaner migrationCleaner =
                new MigrationCleaner(
                        mTransactionManager,
                        MigrationEntityHelper.getInstance(),
                        PriorityMigrationHelper.getInstance());
        mHealthConnectService =
                new HealthConnectServiceImpl(
                        mTransactionManager,
                        permissionHelper,
                        migrationCleaner,
                        firstGrantTimeManager,
                        migrationStateManager,
                        mContext);
    }

    @Override
    public void onStart() {
        mPermissionPackageChangesOrchestrator.registerBroadcastReceiver(mContext);
        new MigratorPackageChangesReceiver(MigrationStateManager.getInitialisedInstance())
                .registerBroadcastReceiver(mContext);
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
        RateLimiter.clearCache();
        HealthConnectThreadScheduler.resetThreadPools();
        MigrationStateManager.getInitialisedInstance()
                .onUserSwitching(mContext, to.getUserHandle().getIdentifier());

        mCurrentForegroundUser = to.getUserHandle();
        if (mUserManager.isUserUnlocked(to.getUserHandle())) {
            // The user is already in unlocked state, so we should proceed with our setup right now,
            // as we won't be getting a onUserUnlocked callback
            switchToSetupForUser(to.getUserHandle());
        }
    }

    // NOTE: The only scenario in which onUserUnlocked's code should be triggered is if the
    // foreground user is unlocked. If {@code user} is not a foreground user, the following
    // code should only be triggered when the {@code user} actually gets unlocked. And in
    // such cases onUserSwitching will be triggered for {@code user} and this code will be
    // triggered then.
    @Override
    public void onUserUnlocked(@NonNull TargetUser user) {
        Objects.requireNonNull(user);
        if (!user.getUserHandle().equals(mCurrentForegroundUser)) {
            // Ignore unlocking requests for non-foreground users
            return;
        }

        switchToSetupForUser(user.getUserHandle());
    }

    @Override
    public boolean isUserSupported(@NonNull TargetUser user) {
        UserManager userManager =
                getUserContext(mContext, user.getUserHandle()).getSystemService(UserManager.class);
        return !(Objects.requireNonNull(userManager).isProfile());
    }

    private void switchToSetupForUser(UserHandle user) {
        // Note: This is for test setup debugging, please don't surround with DEBUG flag
        Slog.d(TAG, "switchToSetupForUser: " + user);
        mTransactionManager.onUserUnlocked(
                new HealthConnectUserContext(mContext, mCurrentForegroundUser));
        mHealthConnectService.onUserSwitching(mCurrentForegroundUser);
        mMigrationBroadcastScheduler.setUserId(mCurrentForegroundUser.getIdentifier());
        HealthConnectDailyJobs.cancelAllJobs(mContext);

        HealthConnectThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        HealthConnectDailyJobs.schedule(
                                mContext, mCurrentForegroundUser.getIdentifier());
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to scheduled Health Connect daily service.", e);
                    }
                });

        HealthConnectThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        mMigrationBroadcastScheduler.prescheduleNewJobs(mContext);
                    } catch (Exception e) {
                        Slog.e(TAG, "Migration broadcast schedule failed", e);
                    }
                });

        HealthConnectThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        MigrationStateManager.getInitialisedInstance()
                                .switchToSetupForUser(mContext);
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to start user unlocked state changes actions", e);
                    }
                });
        HealthConnectThreadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        PreferenceHelper.getInstance().initializePreferences();
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to initialize preferences cache", e);
                    }
                });
    }

    @NonNull
    private static Context getUserContext(@NonNull Context context, @NonNull UserHandle user) {
        if (Process.myUserHandle().equals(user)) {
            return context;
        } else {
            return context.createContextAsUser(user, 0);
        }
    }
}
