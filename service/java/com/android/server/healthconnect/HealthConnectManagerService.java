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

import android.annotation.Nullable;
import android.content.Context;
import android.health.connect.ratelimiter.RateLimiter;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.server.SystemService;
import com.android.server.healthconnect.exportimport.ExportImportJobs;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.migration.MigratorPackageChangesReceiver;
import com.android.server.healthconnect.storage.HealthConnectContext;

import java.util.Objects;

/**
 * HealthConnect system service scaffold.
 *
 * @hide
 */
public class HealthConnectManagerService extends SystemService {
    private static final String TAG = "HealthConnectManagerService";
    private final Context mContext;
    private final HealthConnectServiceImpl mHealthConnectService;
    private final UserManager mUserManager;
    private final HealthConnectInjector mHealthConnectInjector;
    private final RateLimiter mRateLimiter;

    private UserHandle mCurrentForegroundUser;

    public HealthConnectManagerService(Context context) {
        super(context);
        mRateLimiter = new RateLimiter();
        mContext = context;
        mCurrentForegroundUser = context.getUser();
        mUserManager = context.getSystemService(UserManager.class);

        HealthConnectInjector.setInstance(new HealthConnectInjectorImpl(context));
        mHealthConnectInjector = HealthConnectInjector.getInstance();
        mHealthConnectService =
                new HealthConnectServiceImpl(
                        mContext,
                        mHealthConnectInjector.getTimeSource(),
                        mHealthConnectInjector.getInternalHealthConnectMappings(),
                        mHealthConnectInjector.getTransactionManager(),
                        mHealthConnectInjector.getHealthConnectPermissionHelper(),
                        mHealthConnectInjector.getFirstGrantTimeManager(),
                        mHealthConnectInjector.getMigrationEntityHelper(),
                        mHealthConnectInjector.getMigrationStateManager(),
                        mHealthConnectInjector.getMigrationUiStateManager(),
                        mHealthConnectInjector.getMigrationCleaner(),
                        mHealthConnectInjector.getFitnessRecordReadHelper(),
                        mHealthConnectInjector.getFitnessRecordDeleteHelper(),
                        mHealthConnectInjector.getMedicalResourceHelper(),
                        mHealthConnectInjector.getMedicalDataSourceHelper(),
                        mHealthConnectInjector.getExportManager(),
                        mHealthConnectInjector.getExportImportSettingsStorage(),
                        mHealthConnectInjector.getExportImportNotificationSender(),
                        mHealthConnectInjector.getBackupRestore(),
                        mHealthConnectInjector.getAccessLogsHelper(),
                        mHealthConnectInjector.getHealthDataCategoryPriorityHelper(),
                        mHealthConnectInjector.getActivityDateHelper(),
                        mHealthConnectInjector.getChangeLogsHelper(),
                        mHealthConnectInjector.getChangeLogsRequestHelper(),
                        mHealthConnectInjector.getPriorityMigrationHelper(),
                        mHealthConnectInjector.getAppInfoHelper(),
                        mHealthConnectInjector.getDeviceInfoHelper(),
                        mHealthConnectInjector.getPreferenceHelper(),
                        mHealthConnectInjector.getDatabaseHelpers(),
                        mHealthConnectInjector.getPreferencesManager(),
                        mHealthConnectInjector.getReadAccessLogsHelper(),
                        mHealthConnectInjector.getAppOpsManagerLocal(),
                        mHealthConnectInjector.getThreadScheduler(),
                        mRateLimiter,
                        mHealthConnectInjector.getEnvironmentDataDirectory(),
                        mHealthConnectInjector.getExportImportLogger(),
                        mHealthConnectInjector.getHealthFitnessStatsLog(),
                        mHealthConnectInjector.getBackupRestoreLogger());
    }

    @Override
    public void onStart() {
        mHealthConnectInjector
                .getPermissionPackageChangesOrchestrator()
                .registerBroadcastReceiver(mContext);
        new MigratorPackageChangesReceiver(mHealthConnectInjector.getMigrationStateManager())
                .registerBroadcastReceiver(mContext);
        publishBinderService(Context.HEALTHCONNECT_SERVICE, mHealthConnectService);
    }

    /**
     * NOTE: Don't put any code that uses DB in onUserSwitching, such code should be part of
     * switchToSetupForUser which is only called once DB is in usable state.
     */
    @Override
    public void onUserSwitching(@Nullable TargetUser from, TargetUser to) {
        if (from != null && mUserManager.isUserUnlocked(from.getUserHandle())) {
            // We need to cancel any pending timers for the foreground user before it goes into the
            // background.
            mHealthConnectService.cancelBackupRestoreTimeouts();
        }

        HealthConnectThreadScheduler threadScheduler = mHealthConnectInjector.getThreadScheduler();
        threadScheduler.shutdownThreadPools();
        mRateLimiter.clearCache();
        HealthConnectDailyJobs.cancelAllJobs(mContext);
        mHealthConnectInjector.getDatabaseHelpers().clearAllCache();
        mHealthConnectInjector.getTransactionManager().shutDownCurrentUser();
        mHealthConnectInjector.getMigrationStateManager().shutDownCurrentUser(mContext);
        threadScheduler.resetThreadPools();

        mCurrentForegroundUser = to.getUserHandle();

        if (mUserManager.isUserUnlocked(to.getUserHandle())) {
            // The user is already in unlocked state, so we should proceed with our setup right now,
            // as we won't be getting a onUserUnlocked callback
            setupForCurrentForegroundUser();
        }
    }

    // NOTE: The only scenario in which onUserUnlocked's code should be triggered is if the
    // foreground user is unlocked. If {@code user} is not a foreground user, the following
    // code should only be triggered when the {@code user} actually gets unlocked. And in
    // such cases onUserSwitching will be triggered for {@code user} and this code will be
    // triggered then.
    @Override
    public void onUserUnlocked(TargetUser user) {
        Objects.requireNonNull(user);
        if (!user.getUserHandle().equals(mCurrentForegroundUser)) {
            // Ignore unlocking requests for non-foreground users
            return;
        }

        setupForCurrentForegroundUser();
    }

    @Override
    public boolean isUserSupported(TargetUser user) {
        UserManager userManager =
                getUserContext(mContext, user.getUserHandle()).getSystemService(UserManager.class);
        return !(Objects.requireNonNull(userManager).isProfile());
    }

    private void setupForCurrentForegroundUser() {
        Slog.d(TAG, "setupForCurrentForegroundUser: " + mCurrentForegroundUser);
        HealthConnectContext hcContext =
                HealthConnectContext.create(
                        mContext,
                        mCurrentForegroundUser,
                        /* databaseDirName= */ null,
                        mHealthConnectInjector.getEnvironmentDataDirectory());

        mHealthConnectService.setupForUser(mCurrentForegroundUser);
        mHealthConnectInjector.getTransactionManager().setupForUser(hcContext);
        mHealthConnectInjector.getMigrationStateManager().setupForUser(mCurrentForegroundUser);
        mHealthConnectInjector
                .getMigrationBroadcastScheduler()
                .setupForUser(mCurrentForegroundUser);
        mHealthConnectInjector.getMigrationUiStateManager().setupForUser(mCurrentForegroundUser);
        mHealthConnectInjector
                .getPermissionPackageChangesOrchestrator()
                .setupForUser(mCurrentForegroundUser);
        mHealthConnectInjector
                .getHealthPermissionIntentAppsTracker()
                .setupForUser(mCurrentForegroundUser);
        mHealthConnectInjector.getBackupRestore().setupForUser(mCurrentForegroundUser);
        mHealthConnectInjector.getAppInfoHelper().setupForUser(hcContext);
        mHealthConnectInjector.getHealthDataCategoryPriorityHelper().setupForUser(hcContext);

        if (Flags.clearCachesAfterSwitchingUser()) {
            // Clear preferences cache again after the user switching is done as there's a race
            // condition with tasks re-populating the preferences cache between clearing the cache
            // and TransactionManager switching user, see b/355426144.
            mHealthConnectInjector.getPreferenceHelper().clearCache();
        }

        HealthConnectThreadScheduler threadScheduler = mHealthConnectInjector.getThreadScheduler();
        threadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        HealthConnectDailyJobs.schedule(mContext, mCurrentForegroundUser);
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to schedule Health Connect daily service.", e);
                    }
                });

        threadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        mHealthConnectInjector
                                .getMigrationBroadcastScheduler()
                                .scheduleNewJobs(
                                        mContext,
                                        mHealthConnectInjector.getMigrationStateManager());
                    } catch (Exception e) {
                        Slog.e(TAG, "Migration broadcast schedule failed", e);
                    }
                });

        threadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        mHealthConnectInjector
                                .getMigrationStateManager()
                                .switchToSetupForUser(mContext);
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to start user unlocked state changes actions", e);
                    }
                });
        threadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        mHealthConnectInjector.getPreferenceHelper().initializePreferences();
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to initialize preferences cache", e);
                    }
                });

        threadScheduler.scheduleInternalTask(
                () -> {
                    try {
                        ExportImportJobs.schedulePeriodicJobIfNotScheduled(
                                mCurrentForegroundUser,
                                mContext,
                                mHealthConnectInjector.getExportImportSettingsStorage(),
                                mHealthConnectInjector.getExportManager());
                    } catch (Exception e) {
                        Slog.e(TAG, "Failed to schedule periodic export job.", e);
                    }
                });

        if (Flags.stepTrackingEnabled()) {
            threadScheduler.scheduleInternalTask(
                    () -> {
                        try {
                            mHealthConnectInjector.getTrackerManager().initialize();
                        } catch (Exception e) {
                            Slog.e(TAG, "Failed to initialize steps tracker.", e);
                        }
                    });
        }
    }

    private static Context getUserContext(Context context, UserHandle user) {
        if (Process.myUserHandle().equals(user)) {
            return context;
        } else {
            return context.createContextAsUser(user, 0);
        }
    }
}
