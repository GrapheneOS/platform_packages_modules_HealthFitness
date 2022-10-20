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

import static java.util.Collections.emptySet;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.database.sqlite.SQLiteException;
import android.healthconnect.Constants;
import android.healthconnect.HealthConnectException;
import android.healthconnect.aidl.HealthConnectExceptionParcel;
import android.healthconnect.aidl.IHealthConnectService;
import android.healthconnect.aidl.IInsertRecordsResponseCallback;
import android.healthconnect.aidl.InsertRecordsResponseParcel;
import android.healthconnect.aidl.RecordsParcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.InsertTransactionRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * IHealthConnectService's implementation
 *
 * @hide
 */
final class HealthConnectServiceImpl extends IHealthConnectService.Stub {
    private static final String TAG = "HealthConnectService";
    private static final int MIN_BACKGROUND_EXECUTOR_THREADS = 2;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final boolean DEBUG = false;
    // In order to unblock the binder queue all the async should be scheduled on SHARED_EXECUTOR, as
    // soon as they come.
    private static final Executor SHARED_EXECUTOR =
            new ThreadPoolExecutor(
                    Math.max(
                            MIN_BACKGROUND_EXECUTOR_THREADS,
                            Runtime.getRuntime().availableProcessors()),
                    Math.max(
                            MIN_BACKGROUND_EXECUTOR_THREADS,
                            Runtime.getRuntime().availableProcessors()),
                    KEEP_ALIVE_TIME,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());

    private final TransactionManager mTransactionManager;
    private final HealthConnectPermissionHelper mPermissionHelper;

    HealthConnectServiceImpl(
            TransactionManager transactionManager, HealthConnectPermissionHelper permissionHelper) {
        mTransactionManager = transactionManager;
        mPermissionHelper = permissionHelper;
    }

    @Override
    public void grantHealthPermission(
            @NonNull String packageName, @NonNull String permissionName, @NonNull UserHandle user) {
        mPermissionHelper.grantHealthPermission(packageName, permissionName, user);
    }

    @Override
    public void revokeHealthPermission(
            @NonNull String packageName,
            @NonNull String permissionName,
            @Nullable String reason,
            @NonNull UserHandle user) {
        mPermissionHelper.revokeHealthPermission(packageName, permissionName, reason, user);
    }

    @Override
    public void revokeAllHealthPermissions(
            @NonNull String packageName, @Nullable String reason, @NonNull UserHandle user) {
        mPermissionHelper.revokeAllHealthPermissions(packageName, reason, user);
    }

    @Override
    public List<String> getGrantedHealthPermissions(
            @NonNull String packageName, @NonNull UserHandle user) {
        return mPermissionHelper.getGrantedHealthPermissions(packageName, user);
    }

    /**
     * Inserts {@code recordsParcel} into the HealthConnect database.
     *
     * @param recordsParcel parcel for list of records to be inserted.
     * @param callback Callback to receive result of performing this operation. The keys returned in
     *     {@link InsertRecordsResponseParcel} are the unique IDs of the input records. The values
     *     are in same order as {@code record}. In case of an error or a permission failure the
     *     HealthConnect service, {@link IInsertRecordsResponseCallback#onError} will be invoked
     *     with a {@link HealthConnectExceptionParcel}.
     */
    @Override
    public void insertRecords(
            @NonNull String packageName,
            @NonNull RecordsParcel recordsParcel,
            @NonNull IInsertRecordsResponseCallback callback) {
        SHARED_EXECUTOR.execute(
                () -> {
                    try {
                        List<String> uuids =
                                mTransactionManager.insertAll(
                                        new InsertTransactionRequest(
                                                packageName, recordsParcel.getRecords()));
                        callback.onResult(new InsertRecordsResponseParcel(uuids));
                    } catch (SQLiteException sqLiteException) {
                        tryAndThrowException(
                                callback, sqLiteException, HealthConnectException.ERROR_IO);
                    } catch (Exception e) {
                        tryAndThrowException(callback, e, HealthConnectException.ERROR_INTERNAL);
                    }
                });
    }

    /**
     * Returns a set of health permissions defined within the module and belonging to {@link
     * Constants.HEALTH_PERMISSION_GROUP_NAME}.
     *
     * <p><b>Note:</b> If we, for some reason, fail to retrieve these, we return an empty set rather
     * than crashing the device. This means the health permissions infra will be inactive.
     */
    static Set<String> getDefinedHealthPerms(PackageManager packageManager) {
        PermissionInfo[] permissionInfos =
                getHealthPermissionControllerPermissionInfos(packageManager);
        if (permissionInfos == null) {
            // This should never happen. But if it does, let's mark our permissions infra as
            //   inactive. At least users can use other parts of their phone.
            return emptySet();
        }

        Set<String> definedHealthPerms = new HashSet<>(permissionInfos.length);
        for (PermissionInfo permInfo : permissionInfos) {
            if (Constants.HEALTH_PERMISSION_GROUP_NAME.equals(permInfo.group)) {
                definedHealthPerms.add(permInfo.name);
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "Defined health permissions: " + definedHealthPerms.toString());
        }
        return definedHealthPerms;
    }

    /**
     * Returns a list of permissions defined in the health permission controller APK, {@code null}
     * if it could not be retrieved.
     */
    private static PermissionInfo[] getHealthPermissionControllerPermissionInfos(
            PackageManager packageManager) {
        PackageInfo packageInfo;
        String healthConnectControllerPackageName = null;
        try {
            healthConnectControllerPackageName =
                    packageManager.getPermissionInfo(
                                    Constants.MANAGE_HEALTH_PERMISSIONS_NAME, /* flags= */ 0)
                            .packageName;
            packageInfo =
                    packageManager.getPackageInfo(
                            healthConnectControllerPackageName,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        } catch (PackageManager.NameNotFoundException e) {
            // This should never happen. But if it does, let's log it and return null
            if (healthConnectControllerPackageName == null) {
                // We couldn't find the permission
                Slog.e(
                        TAG,
                        "HealthConnect permission"
                                + Constants.MANAGE_HEALTH_PERMISSIONS_NAME
                                + ") not found");
            } else {
                // we couldn't find the package
                Slog.e(
                        TAG,
                        "HealthConnect permissions APK ("
                                + healthConnectControllerPackageName
                                + ") not found");
            }
            return null;
        }
        if (packageInfo.permissions == null) {
            // This should never happen. But if it does, let's log it and return null.
            Slog.e(
                    TAG,
                    "No HealthConnect permissions defined in APK ("
                            + healthConnectControllerPackageName
                            + ")");
            return null;
        }
        return packageInfo.permissions;
    }

    private static void tryAndThrowException(
            @NonNull IInsertRecordsResponseCallback callback,
            @NonNull Exception exception,
            @HealthConnectException.ErrorCode int errorCode) {
        try {
            callback.onError(
                    new HealthConnectExceptionParcel(
                            new HealthConnectException(errorCode, exception.toString())));
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to send result to the callback", e);
        }
    }
}
