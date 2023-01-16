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
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.migration.MigrationEntity;
import android.healthconnect.migration.MigrationPayload;
import android.healthconnect.migration.PermissionMigrationPayload;
import android.healthconnect.migration.RecordMigrationPayload;
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
    public void apply(@NonNull Collection<MigrationEntity> entities) {
        migrateRecords(entities);
        migratePermissions(entities);
    }

    private void migrateRecords(@NonNull Collection<MigrationEntity> entities) {
        final List<UpsertTableRequest> requests = parseRecords(entities);
        if (!requests.isEmpty()) {
            mTransactionManager.insertAll(requests);
        }
    }

    @NonNull
    private List<UpsertTableRequest> parseRecords(@NonNull Collection<MigrationEntity> entities) {
        final List<UpsertTableRequest> list = new ArrayList<>();

        for (MigrationEntity entity : entities) {
            final MigrationPayload payload = entity.getPayload();
            if (payload instanceof RecordMigrationPayload) {
                list.add(parseRecord((RecordMigrationPayload) payload));
            }
        }

        return list;
    }

    @NonNull
    private UpsertTableRequest parseRecord(@NonNull RecordMigrationPayload payload) {
        final RecordInternal<?> record = payload.getRecord();
        StorageUtils.addNameBasedUUIDTo(record);
        mAppInfoHelper.populateAppInfoId(record, mUserContext, false);
        mDeviceInfoHelper.populateDeviceInfoId(record);

        return mRecordHelperProvider
                .getRecordHelper(record.getRecordType())
                .getUpsertTableRequest(record);
    }

    @NonNull
    private void migratePermissions(@NonNull Collection<MigrationEntity> entities) {
        for (MigrationEntity entity : entities) {
            final MigrationPayload payload = entity.getPayload();
            if (payload instanceof PermissionMigrationPayload) {
                migratePermissions((PermissionMigrationPayload) payload);
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
}
