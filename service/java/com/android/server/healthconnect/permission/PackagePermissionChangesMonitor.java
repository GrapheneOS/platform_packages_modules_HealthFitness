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

package com.android.server.healthconnect.permission;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.UserHandle;
import android.util.Log;

import com.android.modules.utils.BackgroundThread;

/**
 * Tracks installing/uninstalling, updates and changes of packages.
 *
 * @hide
 */
public class PackagePermissionChangesMonitor extends BroadcastReceiver {
    private static final String TAG = "HealthPackageChangesMonitor";
    static final IntentFilter sPackageFilter = buildPackageChangeFilter();
    private final HealthPermissionIntentAppsTracker mPermissionIntentTracker;

    public PackagePermissionChangesMonitor(
            HealthPermissionIntentAppsTracker permissionIntentTracker) {
        mPermissionIntentTracker = permissionIntentTracker;
    }

    /**
     * Register broadcast receiver to track package changes.
     *
     * @hide
     */
    public void registerBroadcastReceiver(Context context) {
        context.registerReceiverForAllUsers(
                this, sPackageFilter, null, BackgroundThread.getHandler());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = getPackageName(intent);
        UserHandle userHandle = getUserHandle(intent);
        if (packageName == null || userHandle == null) {
            Log.w(TAG, "can't extract info from the input intent");
            return;
        }
        mPermissionIntentTracker.onPackageChanged(packageName, userHandle);
    }

    private static IntentFilter buildPackageChangeFilter() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme(/* scheme= */ "package");
        return filter;
    }

    private String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        String pkg = uri != null ? uri.getSchemeSpecificPart() : null;
        return pkg;
    }

    private UserHandle getUserHandle(Intent intent) {
        final int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
        if (uid >= 0) {
            return UserHandle.getUserHandleForUid(uid);
        } else {
            Log.w(TAG, "UID extra is missing from intent");
            return null;
        }
    }
}
