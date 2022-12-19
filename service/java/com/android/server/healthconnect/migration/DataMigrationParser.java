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

import static android.healthconnect.migration.DataMigrationFields.DM_PERMISSION_FIRST_GRANT_TIME;
import static android.healthconnect.migration.DataMigrationFields.DM_PERMISSION_PACKAGE_NAME;
import static android.healthconnect.migration.DataMigrationFields.DM_PERMISSION_PERMISSION_NAMES;
import static android.healthconnect.migration.DataMigrationFields.DM_RECORD_TYPE;
import static android.healthconnect.migration.MigrationDataEntity.TYPE_PACKAGE_PERMISSIONS;
import static android.healthconnect.migration.MigrationDataEntity.TYPE_RECORD;

import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.content.Context;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.migration.MigrationDataEntity;
import android.os.Bundle;

import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Parses collections of {@link MigrationDataEntity} into meaningful results, that are easier to
 * process.
 *
 * @hide
 */
public final class DataMigrationParser {

    private final Context mUserContext;
    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final RecordHelperProvider mRecordHelperProvider;

    public DataMigrationParser(
            @NonNull Context userContext,
            @NonNull DeviceInfoHelper deviceInfoHelper,
            @NonNull AppInfoHelper appInfoHelper,
            @NonNull RecordHelperProvider recordHelperProvider) {
        mUserContext = userContext;
        mDeviceInfoHelper = deviceInfoHelper;
        mAppInfoHelper = appInfoHelper;
        mRecordHelperProvider = recordHelperProvider;
    }

    /**
     * Parses a collection of {@link MigrationDataEntity} into meaningful results.
     *
     * @param entities a collection of {@link MigrationDataEntity} that should be parsed.
     * @return a list of {@link ParseResult}.
     */
    @NonNull
    public List<ParseResult> parse(@NonNull Collection<MigrationDataEntity> entities) {
        return entities.stream().map(this::parse).toList();
    }

    @NonNull
    private ParseResult parse(@NonNull MigrationDataEntity entity) {
        final Bundle payload = entity.getPayload();

        switch (entity.getTypeId()) {
            case TYPE_PACKAGE_PERMISSIONS:
                return parsePackagePermissions(payload);
            case TYPE_RECORD:
                return parseRecord(payload);
            default:
                throw new IllegalStateException("Unexpected type: " + entity.getTypeId());
        }
    }

    @NonNull
    private GrantPermissions parsePackagePermissions(@NonNull Bundle payload) {
        return new GrantPermissions(
                requireNonNull(payload.getString(DM_PERMISSION_PACKAGE_NAME)),
                requireNonNull(payload.getStringArrayList(DM_PERMISSION_PERMISSION_NAMES)),
                requireNonNull(ofEpochMilli(payload.getLong(DM_PERMISSION_FIRST_GRANT_TIME))));
    }

    @NonNull
    private UpsertData parseRecord(@NonNull Bundle payload) {
        final RecordInternal<?> record =
                mRecordHelperProvider
                        .getRecordHelper(payload.getInt(DM_RECORD_TYPE, -1))
                        .newInternalRecord();
        record.populateUsing(payload);
        StorageUtils.addNameBasedUUIDTo(record);
        mAppInfoHelper.populateAppInfoId(record, mUserContext, false);
        mDeviceInfoHelper.populateDeviceInfoId(record);

        return new UpsertData(
                mRecordHelperProvider
                        .getRecordHelper(record.getRecordType())
                        .getUpsertTableRequest(record));
    }

    /** Represents a result of parsing, should be checked for actual type. */
    interface ParseResult {}

    /** Represents a result of parsing, that should be inserted into the database. */
    public static final class UpsertData implements ParseResult {
        private final UpsertTableRequest mRequest;

        public UpsertData(@NonNull UpsertTableRequest request) {
            requireNonNull(request);

            mRequest = request;
        }

        @NonNull
        public UpsertTableRequest getRequest() {
            return mRequest;
        }
    }

    /** Represents a result of parsing, indicating that permission should be granted. */
    public static final class GrantPermissions implements ParseResult {
        private final String mPackageName;
        private final List<String> mPermissionNames;
        private final Instant mFirstGrantTime;

        public GrantPermissions(
                @NonNull String packageName,
                @NonNull List<String> permissionNames,
                @NonNull Instant firstGrantTime) {
            requireNonNull(packageName);
            requireNonNull(permissionNames);
            requireNonNull(firstGrantTime);

            mPackageName = packageName;
            mPermissionNames = permissionNames;
            mFirstGrantTime = firstGrantTime;
        }

        @NonNull
        public String getPackageName() {
            return mPackageName;
        }

        @NonNull
        public List<String> getPermissionNames() {
            return mPermissionNames;
        }

        @NonNull
        public Instant getFirstGrantTime() {
            return mFirstGrantTime;
        }
    }
}
