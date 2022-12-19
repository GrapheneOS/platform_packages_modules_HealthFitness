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
import android.healthconnect.migration.MigrationDataEntity;
import android.os.UserHandle;

import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.storage.TransactionManager;

import java.util.Collection;
import java.util.List;

/**
 * Controls the data migration flow. Accepts and applies collections of {@link MigrationDataEntity}.
 *
 * @hide
 */
public final class DataMigrationManager {

    private final Context mUserContext;
    private final TransactionManager mTransactionManager;
    private final HealthConnectPermissionHelper mPermissionHelper;
    private final FirstGrantTimeManager mFirstGrantTimeManager;
    private final DataMigrationParser mParser;

    public DataMigrationManager(
            @NonNull Context userContext,
            @NonNull TransactionManager transactionManager,
            @NonNull HealthConnectPermissionHelper permissionHelper,
            @NonNull FirstGrantTimeManager firstGrantTimeManager,
            @NonNull DataMigrationParser parser) {
        mUserContext = userContext;
        mTransactionManager = transactionManager;
        mPermissionHelper = permissionHelper;
        mFirstGrantTimeManager = firstGrantTimeManager;
        mParser = parser;
    }

    /**
     * Parses and applies the provided migration entities.
     *
     * @param entities a collection of {@link MigrationDataEntity} to be applied.
     */
    public void apply(@NonNull Collection<MigrationDataEntity> entities) {
        final List<DataMigrationParser.ParseResult> results = mParser.parse(entities);
        updateDatabase(results);
        updatePermissions(results);
    }

    private void updateDatabase(@NonNull List<DataMigrationParser.ParseResult> results) {
        mTransactionManager.insertAll(
                results.stream()
                        .filter(result -> result instanceof DataMigrationParser.UpsertData)
                        .map(result -> (DataMigrationParser.UpsertData) result)
                        .map(DataMigrationParser.UpsertData::getRequest)
                        .toList());
    }

    private void updatePermissions(@NonNull List<DataMigrationParser.ParseResult> results) {
        results.stream()
                .filter(result -> result instanceof DataMigrationParser.GrantPermissions)
                .map(result -> (DataMigrationParser.GrantPermissions) result)
                .forEach(this::updatePermissions);
    }

    private void updatePermissions(DataMigrationParser.GrantPermissions result) {
        final String packageName = result.getPackageName();
        final UserHandle appUserHandle = getUserHandle(packageName);
        if ((appUserHandle != null)
                && !mPermissionHelper.hasGrantedHealthPermissions(packageName, appUserHandle)) {
            for (String permissionName : result.getPermissionNames()) {
                mPermissionHelper.grantHealthPermission(packageName, permissionName, appUserHandle);
            }

            mFirstGrantTimeManager.setFirstGrantTime(
                    result.getPackageName(), result.getFirstGrantTime(), mUserContext.getUser());
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
