/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.server.healthconnect.phr.storage;

import static com.android.server.healthconnect.phr.storage.MedicalResourceHelper.getMedicalResource;

import static java.util.stream.Collectors.toMap;

import android.annotation.Nullable;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.util.Slog;

import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Common change logs operations for {@link MedicalResourceHelper} and {@link
 * MedicalDataSourceHelper}.
 */
final class MedicalChangeLogsHelper {

    private static final String TAG = "MedicalChangeLogsHelper";

    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;
    private final MedicalDataSourceHelper mMedicalDataSourceHelper;

    MedicalChangeLogsHelper(
            TransactionManager transactionManager,
            AppInfoHelper appInfoHelper,
            MedicalDataSourceHelper medicalDataSourceHelper) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mMedicalDataSourceHelper = medicalDataSourceHelper;
    }

    /**
     * Generates deletion change logs for the given medical resources.
     *
     * @param db the database to write the change logs to
     * @param readTableRequest the request to read the resources that will be deleted
     * @param appId the app id of the app that owns the resources, or null if the resources are
     *     deleted from the UI (in which case the app id is determined from the data source of each
     *     resource). If present, all the resources that will be read by the request must be owned
     *     by the appId, as they will be attributed to it in the deletion logs.
     */
    void generateDeletionChangeLogsForMedicalResources(
            SQLiteDatabase db, ReadTableRequest readTableRequest, @Nullable Long appId) {
        List<MedicalResource> resources = new ArrayList<>();
        try (Cursor cursor = mTransactionManager.read(db, readTableRequest)) {
            while (cursor.moveToNext()) {
                resources.add(getMedicalResource(cursor));
            }
        }
        generateDeletionChangeLogsForMedicalResources(db, resources, appId);
    }

    /**
     * Generates deletion change logs for the given medical resources.
     *
     * @param db the database to write the change logs to
     * @param resourcesToDelete the resources that will be deleted
     * @param appId the app id of the app that owns the resources, or null if the resources are
     *     deleted from the UI (in which case the app id is determined from the data source of each
     *     resource). If present, all the resources to be deleted must be owned by the appId, as
     *     they will be attributed to it in the deletion logs.
     */
    void generateDeletionChangeLogsForMedicalResources(
            SQLiteDatabase db, List<MedicalResource> resourcesToDelete, @Nullable Long appId) {
        var deletionChangeLogs = ChangeLogsHelper.ChangeLogsTableRequests.ofDeletion(Instant.now());

        if (appId == null) {
            // If we're not deleting resources of a specific app (for example deleting from the UI),
            // we need to check the app ids of the data sources the resources belong to.
            List<UUID> dataSourceIds =
                    resourcesToDelete.stream()
                            .map(MedicalResource::getDataSourceId)
                            .map(UUID::fromString)
                            .distinct()
                            .toList();
            Map<String, Long> dataSourcesToAppIdsMap =
                    mMedicalDataSourceHelper
                            .getMedicalDataSourcesByIdsWithoutPermissionChecks(dataSourceIds)
                            .stream()
                            .collect(
                                    toMap(
                                            MedicalDataSource::getId,
                                            dataSource ->
                                                    mAppInfoHelper.getAppInfoId(
                                                            dataSource.getPackageName())));
            resourcesToDelete.forEach(
                    resource -> {
                        var resourceAppId = dataSourcesToAppIdsMap.get(resource.getDataSourceId());
                        if (resourceAppId == null) {
                            Slog.e(
                                    TAG,
                                    "No app id found for resource, it will be skipped: "
                                            + resource);
                            return;
                        }
                        deletionChangeLogs.addMedicalResourceInfo(
                                resource.getType(), resourceAppId, resource.getId());
                    });
        } else {
            resourcesToDelete.forEach(
                    resource ->
                            deletionChangeLogs.addMedicalResourceInfo(
                                    resource.getType(), appId, resource.getId()));
        }

        for (UpsertTableRequest insertRequestsForChangeLog :
                deletionChangeLogs.getUpsertTableRequests()) {
            mTransactionManager.insertOrThrowOnConflict(db, insertRequestsForChangeLog);
        }
    }
}
