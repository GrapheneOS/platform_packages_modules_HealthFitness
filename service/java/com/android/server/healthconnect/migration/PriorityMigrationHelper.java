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

import static com.android.server.healthconnect.storage.utils.StorageUtils.DELIMITER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_UNIQUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.HealthDataCategory;
import android.util.Pair;

import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to get migrate priority of the apps for each {@link HealthDataCategory} from
 * migration aware apk to module.
 *
 * @hide
 */
public final class PriorityMigrationHelper {

    private static final String PRE_MIGRATION_TABLE_NAME = "pre_migration_category_priority_table";

    private static final String CATEGORY_COLUMN_NAME = "category";
    private static final String PRIORITY_ORDER_COLUMN_NAME = "priority_order";

    private static final int DB_VERSION_TABLE_CREATED = 5;

    private static final Object sPriorityMigrationHelperLock = new Object();
    private Map<Integer, List<Long>> mPreMigrationPriorityCache;

    private static volatile PriorityMigrationHelper sPriorityMigrationHelper;

    private final HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;

    private final Object mPriorityMigrationHelperInstanceLock = new Object();

    private PriorityMigrationHelper() {
        mHealthDataCategoryPriorityHelper = HealthDataCategoryPriorityHelper.getInstance();
    }

    /** Creates(if it was not already created) and returns instance of PriorityMigrationHelper. */
    @NonNull
    public static PriorityMigrationHelper getInstance() {
        if (sPriorityMigrationHelper == null) {
            synchronized (sPriorityMigrationHelperLock) {
                if (sPriorityMigrationHelper == null) {
                    sPriorityMigrationHelper = new PriorityMigrationHelper();
                }
            }
        }

        return sPriorityMigrationHelper;
    }

    /**
     * Populate the pre-migration priority table by copying entries from priority table at the start
     * of migration.
     */
    public void populatePreMigrationPriority() {
        // TODO(b/272443882) handle case where multiple startMigration should be no-op
        populatePreMigrationTable();
    }

    /**
     * Returns priority order stored for data category in module at the time migration was started.
     */
    public List<Long> getPreMigrationPriority(int dataCategory) {
        synchronized (mPriorityMigrationHelperInstanceLock) {
            if (mPreMigrationPriorityCache == null) {
                cachePreMigrationTable();
            }

            return Collections.unmodifiableList(
                    mPreMigrationPriorityCache.getOrDefault(dataCategory, new ArrayList<>()));
        }
    }

    /**
     * Read pre-migration table and populate cache which would be used for writing priority
     * migration.
     */
    private void cachePreMigrationTable() {
        Map<Integer, List<Long>> preMigrationCategoryPriorityMap = new HashMap<>();
        TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        try (Cursor cursor =
                transactionManager.read(new ReadTableRequest(PRE_MIGRATION_TABLE_NAME))) {
            while (cursor.moveToNext()) {
                int dataCategory = cursor.getInt(cursor.getColumnIndex(CATEGORY_COLUMN_NAME));
                List<Long> appIdsInOrder =
                        StorageUtils.getCursorLongList(
                                cursor, PRIORITY_ORDER_COLUMN_NAME, DELIMITER);
                preMigrationCategoryPriorityMap.put(dataCategory, appIdsInOrder);
            }
        }
        mPreMigrationPriorityCache = preMigrationCategoryPriorityMap;
    }

    /** Delete pre-migration priority data when migration is finished. */
    public void clearData(@NonNull TransactionManager transactionManager) {
        synchronized (mPriorityMigrationHelperInstanceLock) {
            transactionManager.delete(new DeleteTableRequest(PRE_MIGRATION_TABLE_NAME));
            mPreMigrationPriorityCache = null;
        }
    }

    /** Returns a requests for creating pre-migration priority table. */
    @NonNull
    public CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(PRE_MIGRATION_TABLE_NAME, getColumnInfo());
    }

    /** Upgrades the database to the latest version. */
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion) {
        if (oldVersion < DB_VERSION_TABLE_CREATED) {
            HealthConnectDatabase.createTable(db, getCreateTableRequest());
            return; // No more queries running after this, the table is already on latest schema
        }

        // Add more upgrades here
    }

    /**
     * Populate the pre-migration priority table if table is newly created by copying entries from
     * priority table.
     */
    private void populatePreMigrationTable() {
        synchronized (mPriorityMigrationHelperInstanceLock) {
            Map<Integer, List<Long>> existingPriority =
                    mHealthDataCategoryPriorityHelper
                            .getHealthDataCategoryToAppIdPriorityMapImmutable();

            if (existingPriority.isEmpty()) {
                return;
            }

            TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
            existingPriority.forEach(
                    (category, priority) -> {
                        if (!priority.isEmpty()) {
                            UpsertTableRequest request =
                                    new UpsertTableRequest(
                                            PRE_MIGRATION_TABLE_NAME,
                                            getContentValuesFor(category, priority));
                            transactionManager.insert(request);
                        }
                    });
        }
    }

    /**
     * This implementation should return the column names with which the table should be created.
     */
    @NonNull
    private List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(CATEGORY_COLUMN_NAME, INTEGER_UNIQUE));
        columnInfo.add(new Pair<>(PRIORITY_ORDER_COLUMN_NAME, TEXT_NOT_NULL));

        return columnInfo;
    }

    /** Create content values for storing priority in the database. */
    private ContentValues getContentValuesFor(
            @HealthDataCategory.Type int dataCategory, List<Long> priorityList) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CATEGORY_COLUMN_NAME, dataCategory);
        contentValues.put(PRIORITY_ORDER_COLUMN_NAME, StorageUtils.flattenLongList(priorityList));

        return contentValues;
    }
}
