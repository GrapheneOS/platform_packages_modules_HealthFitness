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
import android.database.sqlite.SQLiteException;
import android.healthconnect.HealthConnectException;
import android.healthconnect.aidl.HealthConnectExceptionParcel;
import android.healthconnect.aidl.IHealthConnectService;
import android.healthconnect.aidl.IInsertRecordsResponseCallback;
import android.healthconnect.aidl.IReadRecordsResponseCallback;
import android.healthconnect.aidl.InsertRecordsResponseParcel;
import android.healthconnect.aidl.ReadRecordsRequestParcel;
import android.healthconnect.aidl.RecordIdFiltersParcel;
import android.healthconnect.aidl.RecordsParcel;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.util.Slog;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.InsertTransactionRequest;
import com.android.server.healthconnect.storage.request.ReadTransactionRequest;

import java.util.ArrayList;
import java.util.List;
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
    private final Context mContext;

    HealthConnectServiceImpl(
            TransactionManager transactionManager,
            HealthConnectPermissionHelper permissionHelper,
            Context context) {
        mTransactionManager = transactionManager;
        mPermissionHelper = permissionHelper;
        mContext = context;
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
                                                packageName, recordsParcel.getRecords(), mContext));
                        callback.onResult(new InsertRecordsResponseParcel(uuids));
                    } catch (SQLiteException sqLiteException) {
                        Slog.e(TAG, "SqlException: ", sqLiteException);
                        tryAndThrowException(
                                callback, sqLiteException, HealthConnectException.ERROR_IO);
                    } catch (Exception e) {
                        Slog.e(TAG, "Exception: ", e);
                        tryAndThrowException(callback, e, HealthConnectException.ERROR_INTERNAL);
                    }
                });
    }

    /**
     * Read records {@code recordsParcel} from HealthConnect database.
     *
     * @param packageName packageName of calling app.
     * @param request ReadRecordsRequestParcel is parcel for the request object containing {@link
     *     RecordIdFiltersParcel}.
     * @param callback Callback to receive result of performing this operation. The records are
     *     returned in {@link RecordsParcel} . In case of an error or a permission failure the
     *     HealthConnect service, {@link IReadRecordsResponseCallback#onError} will be invoked with
     *     a {@link HealthConnectExceptionParcel}.
     */
    @Override
    public void readRecords(
            @NonNull String packageName,
            @NonNull ReadRecordsRequestParcel request,
            @NonNull IReadRecordsResponseCallback callback) {
        SHARED_EXECUTOR.execute(
                () -> {
                    try {
                        try {
                            List<RecordInternal<?>> recordInternalList =
                                    mTransactionManager.readRecords(
                                            new ReadTransactionRequest(packageName, request));
                            callback.onResult(new RecordsParcel(recordInternalList));
                        } catch (TypeNotPresentException exception) {
                            // All the requested package names are not present, so simply return
                            // an empty list
                            if (ReadTransactionRequest.TYPE_NOT_PRESENT_PACKAGE_NAME.equals(
                                    exception.typeName())) {
                                callback.onResult(new RecordsParcel(new ArrayList<>()));
                            } else {
                                throw exception;
                            }
                        }
                    } catch (SQLiteException sqLiteException) {
                        tryAndThrowException(
                                callback, sqLiteException, HealthConnectException.ERROR_IO);
                    } catch (Exception e) {
                        tryAndThrowException(callback, e, HealthConnectException.ERROR_INTERNAL);
                    }
                });
    }

    private static void tryAndThrowException(
            @NonNull IInsertRecordsResponseCallback callback,
            @NonNull Exception exception,
            @HealthConnectException.ErrorCode int errorCode) {
        try {
            callback.onError(
                    new HealthConnectExceptionParcel(
                            new HealthConnectException(errorCode, exception.getMessage())));
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to send result to the callback", e);
        }
    }

    private static void tryAndThrowException(
            @NonNull IReadRecordsResponseCallback callback,
            @NonNull Exception exception,
            @HealthConnectException.ErrorCode int errorCode) {
        try {
            callback.onError(
                    new HealthConnectExceptionParcel(
                            new HealthConnectException(errorCode, exception.getMessage())));
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to send result to the callback", e);
        }
    }
}
