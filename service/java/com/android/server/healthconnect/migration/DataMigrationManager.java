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

package com.android.server.healthconnect.migration;

import static android.os.UserHandle.getUserHandleForUid;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.PackageInfoFlags;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.migration.AppInfoMigrationPayload;
import android.health.connect.migration.MigrationEntity;
import android.health.connect.migration.MigrationPayload;
import android.health.connect.migration.PermissionMigrationPayload;
import android.health.connect.migration.RecordMigrationPayload;
import android.os.UserHandle;

import com.android.internal.annotations.GuardedBy;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MigrationEntityHelper;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.Collection;

/**
 * Controls the data migration flow. Accepts and applies collections of {@link MigrationEntity}.
 *
 * @hide
 */
public final class DataMigrationManager {

    private static final Object sLock = new Object();

    private final Context mUserContext;
    private final TransactionManager mTransactionManager;
    private final HealthConnectPermissionHelper mPermissionHelper;
    private final FirstGrantTimeManager mFirstGrantTimeManager;
    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final MigrationEntityHelper mMigrationEntityHelper;
    private final RecordHelperProvider mRecordHelperProvider;

    public DataMigrationManager(
            @NonNull Context userContext,
            @NonNull TransactionManager transactionManager,
            @NonNull HealthConnectPermissionHelper permissionHelper,
            @NonNull FirstGrantTimeManager firstGrantTimeManager,
            @NonNull DeviceInfoHelper deviceInfoHelper,
            @NonNull AppInfoHelper appInfoHelper,
            @NonNull MigrationEntityHelper migrationEntityHelper,
            @NonNull RecordHelperProvider recordHelperProvider) {
        mUserContext = userContext;
        mTransactionManager = transactionManager;
        mPermissionHelper = permissionHelper;
        mFirstGrantTimeManager = firstGrantTimeManager;
        mDeviceInfoHelper = deviceInfoHelper;
        mAppInfoHelper = appInfoHelper;
        mMigrationEntityHelper = migrationEntityHelper;
        mRecordHelperProvider = recordHelperProvider;
    }

    /**
     * Parses and applies the provided migration entities.
     *
     * @param entities a collection of {@link MigrationEntity} to be applied.
     */
    public void apply(@NonNull Collection<MigrationEntity> entities) throws EntityWriteException {
        synchronized (sLock) { // Ensure only one migration process at a time
            final SQLiteDatabase db = mTransactionManager.getWritableDb();
            db.beginTransaction();
            try {
                for (MigrationEntity entity : entities) {
                    migrateEntity(db, entity);
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    /** Migrates the provided {@link MigrationEntity}. Must be called inside a DB transaction. */
    @GuardedBy("sLock")
    private void migrateEntity(@NonNull SQLiteDatabase db, @NonNull MigrationEntity entity)
            throws EntityWriteException {
        try {
            if (!insertEntityIdIfNotPresent(db, entity.getEntityId())) {
                return;
            }

            final MigrationPayload payload = entity.getPayload();
            if (payload instanceof RecordMigrationPayload) {
                migrateRecord(db, (RecordMigrationPayload) payload);
            } else if (payload instanceof PermissionMigrationPayload) {
                migratePermissions((PermissionMigrationPayload) payload);
            } else if (payload instanceof AppInfoMigrationPayload) {
                migrateAppInfo((AppInfoMigrationPayload) payload);
            } else {
                throw new IllegalArgumentException("Unsupported payload type: " + payload);
            }
        } catch (RuntimeException e) {
            throw new EntityWriteException(entity.getEntityId(), e);
        }
    }

    @GuardedBy("sLock")
    private void migrateRecord(
            @NonNull SQLiteDatabase db, @NonNull RecordMigrationPayload payload) {
        mTransactionManager.insertRecord(db, parseRecord(payload));
    }

    @NonNull
    private UpsertTableRequest parseRecord(@NonNull RecordMigrationPayload payload) {
        final RecordInternal<?> record = payload.getRecordInternal();
        StorageUtils.addNameBasedUUIDTo(record);
        mAppInfoHelper.populateAppInfoId(record, mUserContext, false);
        mDeviceInfoHelper.populateDeviceInfoId(record);

        return mRecordHelperProvider
                .getRecordHelper(record.getRecordType())
                .getUpsertTableRequest(record);
    }

    @GuardedBy("sLock")
    private void migratePermissions(@NonNull PermissionMigrationPayload payload) {
        final String packageName = payload.getHoldingPackageName();
        final UserHandle appUserHandle = getUserHandle(packageName);
        if ((appUserHandle != null)
                && !mPermissionHelper.hasGrantedHealthPermissions(packageName, appUserHandle)) {
            for (String permissionName : payload.getPermissions()) {
                mPermissionHelper.grantHealthPermission(packageName, permissionName, appUserHandle);
            }

            mFirstGrantTimeManager.setFirstGrantTime(
                    packageName, payload.getFirstGrantTime(), mUserContext.getUser());
        }
    }

    @GuardedBy("sLock")
    private void migrateAppInfo(@NonNull AppInfoMigrationPayload payload) {
        mAppInfoHelper.updateAppInfoIfNotInstalled(
                mUserContext, payload.getPackageName(), payload.getAppName(), payload.getAppIcon());
    }

    @Nullable
    private UserHandle getUserHandle(@NonNull String packageName) {
        final ApplicationInfo ai = getApplicationInfo(packageName);

        return ai != null ? getUserHandleForUid(ai.uid) : null;
    }

    @Nullable
    private ApplicationInfo getApplicationInfo(@NonNull String packageName) {
        try {
            return mUserContext
                    .getPackageManager()
                    .getPackageInfo(packageName, PackageInfoFlags.of(0L))
                    .applicationInfo;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Inserts the provided {@code entity} into the database if it doesn't exist yet. Used for data
     * deduplication.
     *
     * @return {@code true} if inserted successfully, {@code false} otherwise.
     */
    @GuardedBy("sLock")
    private boolean insertEntityIdIfNotPresent(
            @NonNull SQLiteDatabase db, @NonNull String entityId) {
        final UpsertTableRequest request = mMigrationEntityHelper.getInsertRequest(entityId);
        return mTransactionManager.insertOrIgnore(db, request) != -1;
    }

    /** Indicates an error during entity migration. */
    public static final class EntityWriteException extends Exception {
        private final String mEntityId;

        private EntityWriteException(@NonNull String entityId, @Nullable Throwable cause) {
            super("Error writing entity: " + entityId, cause);

            mEntityId = entityId;
        }

        /**
         * Returns an identifier of the failed entity, as specified in {@link
         * MigrationEntity#getEntityId()}.
         */
        @NonNull
        public String getEntityId() {
            return mEntityId;
        }
    }
}
