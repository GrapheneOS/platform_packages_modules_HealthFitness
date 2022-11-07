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

package android.healthconnect;

import android.annotation.CallbackExecutor;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SdkConstant;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.annotation.UserHandleAware;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.healthconnect.aidl.HealthConnectExceptionParcel;
import android.healthconnect.aidl.IHealthConnectService;
import android.healthconnect.aidl.IInsertRecordsResponseCallback;
import android.healthconnect.aidl.IReadRecordsResponseCallback;
import android.healthconnect.aidl.InsertRecordsResponseParcel;
import android.healthconnect.aidl.ReadRecordsRequestParcel;
import android.healthconnect.aidl.RecordsParcel;
import android.healthconnect.datatypes.Record;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.utils.InternalExternalRecordConverter;
import android.os.Binder;
import android.os.OutcomeReceiver;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * This class provides APIs to interact with the centralized HealthConnect storage maintained by the
 * system.
 *
 * <p>HealthConnect is an offline, on-device storage that unifies data from multiple devices and
 * apps into an ecosystem featuring.
 *
 * <ul>
 *   <li>APIs to insert data of various types into the system.
 * </ul>
 *
 * <p>The basic unit of data in HealthConnect is represented as a {@link Record} object, which is
 * the base class for all the other data types such as {@link
 * android.healthconnect.datatypes.StepsRecord}.
 */
@SystemService(Context.HEALTHCONNECT_SERVICE)
public class HealthConnectManager {
    /**
     * Used in conjunction with {@link android.content.Intent#ACTION_VIEW_PERMISSION_USAGE} to
     * launch UI to show an app’s health permission rationale/data policy.
     *
     * <p><b>Note:</b> Used by apps to define an intent filter in conjunction with {@link
     * android.content.Intent#ACTION_VIEW_PERMISSION_USAGE} that the HC UI can link out to.
     */
    // We use intent.category prefix to be compatible with HealthPermissions strings definitions.
    @SdkConstant(SdkConstant.SdkConstantType.INTENT_CATEGORY)
    public static final String CATEGORY_HEALTH_PERMISSIONS =
            "android.intent.category.HEALTH_PERMISSIONS";
    /**
     * Activity action: Launch UI to manage (e.g. grant/revoke) health permissions.
     *
     * <p>Shows a list of apps which request at least one permission of the Health permission group.
     *
     * <p>Input: {@link android.content.Intent#EXTRA_PACKAGE_NAME} string extra with the name of the
     * app requesting the action. Optional: Adding package name extras launches a UI to manager
     * (e.g. grant/revoke) for this app.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_HEALTH_PERMISSIONS =
            "android.healthconnect.action.MANAGE_HEALTH_PERMISSIONS";
    /**
     * Activity action: Launch UI to show and manage (e.g. grant/revoke) health permissions and
     * health data (e.g. delete) for an app.
     *
     * <p>Input: {@link android.content.Intent#EXTRA_PACKAGE_NAME} string extra with the name of the
     * app requesting the action must be present. An app can open only its own page.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_MANAGE_HEALTH_PERMISSIONS_AND_DATA =
            "android.healthconnect.action.MANAGE_HEALTH_PERMISSIONS_AND_DATA";
    /**
     * Activity action: Launch UI to health connect home settings screen.
     *
     * <p>shows a list of recent apps that accessed (e.g. read/write) health data and allows the
     * user to access health permissions and health data.
     *
     * @hide
     */
    @SystemApi
    @SdkConstant(SdkConstant.SdkConstantType.ACTIVITY_INTENT_ACTION)
    public static final String ACTION_HEALTH_HOME_SETTINGS =
            "android.healthconnect.action.HEALTH_HOME_SETTINGS";

    private static final String TAG = "HealthConnectManager";
    private static final String HEALTH_PERMISSION_PREFIX = "android.permission.health.";
    private static volatile Set<String> sHealthPermissions;
    private final Context mContext;
    private final IHealthConnectService mService;
    private final InternalExternalRecordConverter mInternalExternalRecordConverter;

    /** @hide */
    HealthConnectManager(@NonNull Context context, @NonNull IHealthConnectService service) {
        mContext = context;
        mService = service;
        mInternalExternalRecordConverter = InternalExternalRecordConverter.getInstance();
    }

    /**
     * Returns {@code true} if the given permission protects access to health connect data.
     *
     * @hide
     */
    @SystemApi
    public static boolean isHealthPermission(
            @NonNull Context context, @NonNull final String permission) {
        if (permission == null || !permission.startsWith(HEALTH_PERMISSION_PREFIX)) {
            return false;
        }
        return getHealthPermissions(context).contains(permission);
    }

    /**
     * Returns a set of health permissions defined within the module and belonging to {@link
     * HealthPermissions#HEALTH_PERMISSION_GROUP}.
     *
     * <p><b>Note:</b> If we, for some reason, fail to retrieve these, we return an empty set rather
     * than crashing the device. This means the health permissions infra will be inactive.
     *
     * @hide
     */
    @NonNull
    public static Set<String> getHealthPermissions(@NonNull Context context) {
        if (sHealthPermissions != null) {
            return sHealthPermissions;
        }

        PackageInfo packageInfo = null;
        try {
            final PackageManager pm = context.getApplicationContext().getPackageManager();
            final PermissionGroupInfo permGroupInfo =
                    pm.getPermissionGroupInfo(
                            HealthPermissions.HEALTH_PERMISSION_GROUP, /* flags= */ 0);
            packageInfo =
                    pm.getPackageInfo(
                            permGroupInfo.packageName,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Health permission group or HC package not found", ex);
            sHealthPermissions = Collections.emptySet();
            return sHealthPermissions;
        }

        Set<String> permissions = new HashSet<>();
        for (PermissionInfo perm : packageInfo.permissions) {
            if (HealthPermissions.HEALTH_PERMISSION_GROUP.equals(perm.group)) {
                permissions.add(perm.name);
            }
        }
        sHealthPermissions = permissions;
        return sHealthPermissions;
    }

    /**
     * Grant a runtime permission to an application which the application does not already have. The
     * permission must have been requested by the application. If the application is not allowed to
     * hold the permission, a {@link java.lang.SecurityException} is thrown. If the package or
     * permission is invalid, a {@link java.lang.IllegalArgumentException} is thrown.
     *
     * @hide
     */
    @RequiresPermission(HealthPermissions.MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public void grantHealthPermission(@NonNull String packageName, @NonNull String permissionName) {
        try {
            mService.grantHealthPermission(packageName, permissionName, mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Revoke a health permission that was previously granted by {@link
     * #grantHealthPermission(String, String)} The permission must have been requested by the
     * application. If the application is not allowed to hold the permission, a {@link
     * java.lang.SecurityException} is thrown. If the package or permission is invalid, a {@link
     * java.lang.IllegalArgumentException} is thrown.
     *
     * @hide
     */
    @RequiresPermission(HealthPermissions.MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public void revokeHealthPermission(
            @NonNull String packageName, @NonNull String permissionName, @Nullable String reason) {
        try {
            mService.revokeHealthPermission(
                    packageName, permissionName, reason, mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Revokes all health permissions that were previously granted by {@link
     * #grantHealthPermission(String, String)} If the package is invalid, a {@link
     * java.lang.IllegalArgumentException} is thrown.
     *
     * @hide
     */
    @RequiresPermission(HealthPermissions.MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public void revokeAllHealthPermissions(@NonNull String packageName, @Nullable String reason) {
        try {
            mService.revokeAllHealthPermissions(packageName, reason, mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Returns a list of health permissions that were previously granted by {@link
     * #grantHealthPermission(String, String)}.
     *
     * @hide
     */
    @RequiresPermission(HealthPermissions.MANAGE_HEALTH_PERMISSIONS)
    @UserHandleAware
    public List<String> getGrantedHealthPermissions(@NonNull String packageName) {
        try {
            return mService.getGrantedHealthPermissions(packageName, mContext.getUser());
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * Inserts {@code records} into the HealthConnect database. The records returned in {@link
     * InsertRecordsResponse} contains the unique IDs of the input records. The values are in same
     * order as {@code records}. In case of an error or a permission failure the HealthConnect
     * service, {@link OutcomeReceiver#onError} will be invoked with a {@link
     * HealthConnectException}.
     *
     * @param records list of records to be inserted.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     *     <p>TODO(b/251194265): User permission checks once available.
     */
    public void insertRecords(
            @NonNull List<Record> records,
            @NonNull @CallbackExecutor Executor executor,
            @NonNull OutcomeReceiver<InsertRecordsResponse, HealthConnectException> callback) {
        Objects.requireNonNull(records);
        /*
         TODO(b/251454017): Use executor to return results after "Hidden API flags are
         inconsistent" error is fixed
        */
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            List<RecordInternal<?>> recordInternals =
                    mInternalExternalRecordConverter.getInternalRecords(records);
            mService.insertRecords(
                    mContext.getPackageName(),
                    new RecordsParcel(recordInternals),
                    new IInsertRecordsResponseCallback.Stub() {
                        @Override
                        public void onResult(InsertRecordsResponseParcel parcel) {
                            Binder.clearCallingIdentity();
                            callback.onResult(
                                    new InsertRecordsResponse(
                                            getRecordsWithUids(records, parcel.getUids())));
                        }

                        @Override
                        public void onError(HealthConnectExceptionParcel exception) {
                            Binder.clearCallingIdentity();
                            callback.onError(exception.getHealthConnectException());
                        }
                    });
        } catch (ArithmeticException | ClassCastException invalidArgumentException) {
            callback.onError(
                    new HealthConnectException(
                            HealthConnectException.ERROR_INVALID_ARGUMENT,
                            invalidArgumentException.getMessage()));
        } catch (IllegalAccessException
                | InstantiationException
                | InvocationTargetException
                | NoSuchMethodException exception) {
            callback.onError(
                    new HealthConnectException(
                            HealthConnectException.ERROR_INTERNAL, exception.getMessage()));
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * API to read records based on {@link RecordIdFilter}.
     *
     * @param request ReadRecordsRequestUsingIds request containing a list of {@link RecordIdFilter}
     *     and recordType to perform read operation.
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     *     <p>TODO(b/251194265): User permission checks once available.
     */
    public <T extends Record> void readRecords(
            @NonNull ReadRecordsRequestUsingIds<T> request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.readRecords(
                    mContext.getPackageName(),
                    new ReadRecordsRequestParcel(request),
                    getReadCallback(executor, callback));
        } catch (RemoteException remoteException) {
            remoteException.rethrowFromSystemServer();
        }
    }

    /**
     * API to read records based on {@link ReadRecordsRequestUsingFilters}.
     *
     * @param request Read request based on {@link ReadRecordsRequestUsingFilters}
     * @param executor Executor on which to invoke the callback.
     * @param callback Callback to receive result of performing this operation.
     *     <p>TODO(b/251194265): User permission checks once available.
     */
    public <T extends Record> void readRecords(
            @NonNull ReadRecordsRequestUsingFilters<T> request,
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException> callback) {
        Objects.requireNonNull(request);
        Objects.requireNonNull(executor);
        Objects.requireNonNull(callback);
        try {
            mService.readRecords(
                    mContext.getPackageName(),
                    new ReadRecordsRequestParcel(request),
                    getReadCallback(executor, callback));
        } catch (RemoteException remoteException) {
            remoteException.rethrowFromSystemServer();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Record> IReadRecordsResponseCallback.Stub getReadCallback(
            @NonNull Executor executor,
            @NonNull OutcomeReceiver<ReadRecordsResponse<T>, HealthConnectException> callback) {
        return new IReadRecordsResponseCallback.Stub() {
            @Override
            public void onResult(RecordsParcel parcel) {
                Binder.clearCallingIdentity();
                try {
                    List<T> externalRecords =
                            (List<T>)
                                    mInternalExternalRecordConverter.getExternalRecords(
                                            parcel.getRecords());
                    executor.execute(
                            () -> callback.onResult(new ReadRecordsResponse<>(externalRecords)));
                } catch (ClassCastException castException) {
                    HealthConnectException healthConnectException =
                            new HealthConnectException(
                                    HealthConnectException.ERROR_INTERNAL,
                                    castException.getMessage());
                    returnError(
                            executor,
                            new HealthConnectExceptionParcel(healthConnectException),
                            callback);
                }
            }

            @Override
            public void onError(HealthConnectExceptionParcel exception) {
                returnError(executor, exception, callback);
            }
        };
    }

    private List<Record> getRecordsWithUids(List<Record> records, List<String> uids) {
        int i = 0;
        for (Record record : records) {
            record.getMetadata().setId(uids.get(i++));
        }

        return records;
    }

    private void returnError(
            Executor executor,
            HealthConnectExceptionParcel exception,
            OutcomeReceiver<?, HealthConnectException> callback) {
        Binder.clearCallingIdentity();
        executor.execute(() -> callback.onError(exception.getHealthConnectException()));
    }
}
