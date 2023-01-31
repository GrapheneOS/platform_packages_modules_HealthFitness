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
import android.health.connect.migration.MigrationEntity;
import android.health.connect.migration.MigrationPayload;
import android.health.connect.migration.PermissionMigrationPayload;
import android.health.connect.migration.RecordMigrationPayload;
import android.os.UserHandle;

import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Controls the data migration flow. Accepts and applies collections of {@link MigrationEntity}.
 *
 * @hide
 */
public final class DataMigrationManager {

    private final Context mUserContext;
    private final TransactionManager mTransactionManager;
    private final HealthConnectPermissionHelper mPermissionHelper;
    private final FirstGrantTimeManager mFirstGrantTimeManager;
    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final RecordHelperProvider mRecordHelperProvider;

    public DataMigrationManager(
            @NonNull Context userContext,
            @NonNull TransactionManager transactionManager,
            @NonNull HealthConnectPermissionHelper permissionHelper,
            @NonNull FirstGrantTimeManager firstGrantTimeManager,
            @NonNull DeviceInfoHelper deviceInfoHelper,
            @NonNull AppInfoHelper appInfoHelper,
            @NonNull RecordHelperProvider recordHelperProvider) {
        mUserContext = userContext;
        mTransactionManager = transactionManager;
        mPermissionHelper = permissionHelper;
        mFirstGrantTimeManager = firstGrantTimeManager;
        mDeviceInfoHelper = deviceInfoHelper;
        mAppInfoHelper = appInfoHelper;
        mRecordHelperProvider = recordHelperProvider;
    }

    /**
     * Parses and applies the provided migration entities.
     *
     * @param entities a collection of {@link MigrationEntity} to be applied.
     */
    public void apply(@NonNull Collection<MigrationEntity> entities) throws EntityWriteException {
        migrateRecords(entities);
        migratePermissions(entities);
    }

    private void migrateRecords(@NonNull Collection<MigrationEntity> entities)
            throws EntityWriteException {
        final List<EntityInsertRequest> requests = parseRecords(entities);
        if (requests.isEmpty()) {
            return;
        }

        final SQLiteDatabase db = mTransactionManager.getWritableDb();
        db.beginTransaction();
        try {
            for (EntityInsertRequest request : requests) {
                try {
                    mTransactionManager.insertRecord(db, request.request);
                } catch (RuntimeException e) {
                    throw new EntityWriteException(request.entityId, e);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @NonNull
    private List<EntityInsertRequest> parseRecords(@NonNull Collection<MigrationEntity> entities)
            throws EntityWriteException {
        final List<EntityInsertRequest> list = new ArrayList<>();

        for (MigrationEntity entity : entities) {
            final MigrationPayload payload = entity.getPayload();
            if (payload instanceof RecordMigrationPayload) {
                final UpsertTableRequest request;
                try {
                    request = parseRecord((RecordMigrationPayload) payload);
                } catch (RuntimeException e) {
                    throw new EntityWriteException(entity.getEntityId(), e);
                }
                list.add(new EntityInsertRequest(entity.getEntityId(), request));
            }
        }

        return list;
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

    @NonNull
    private void migratePermissions(@NonNull Collection<MigrationEntity> entities)
            throws EntityWriteException {
        for (MigrationEntity entity : entities) {
            final MigrationPayload payload = entity.getPayload();
            if (payload instanceof PermissionMigrationPayload) {
                try {
                    migratePermissions((PermissionMigrationPayload) payload);
                } catch (RuntimeException e) {
                    throw new EntityWriteException(entity.getEntityId(), e);
                }
            }
        }
    }

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

    private static final class EntityInsertRequest {
        @NonNull public final String entityId;
        @NonNull public final UpsertTableRequest request;

        private EntityInsertRequest(@NonNull String entityId, @NonNull UpsertTableRequest request) {
            this.entityId = entityId;
            this.request = request;
        }
    }
}
