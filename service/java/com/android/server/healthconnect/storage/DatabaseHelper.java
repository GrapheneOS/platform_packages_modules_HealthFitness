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

package com.android.server.healthconnect.storage;

import com.android.server.healthconnect.storage.request.DeleteTableRequest;

import java.util.HashSet;
import java.util.Set;

/**
 * Parent class for the database helper classes containing common methods
 *
 * @hide
 */
public abstract class DatabaseHelper {

    protected DatabaseHelper(DatabaseHelpers databaseHelpers) {
        databaseHelpers.add(this);
    }

    /** Deletes all entries from the database and clears the cache for the helper class. */
    public synchronized void clearData(TransactionManager transactionManager) {
        transactionManager.delete(new DeleteTableRequest(getMainTableName()));
        clearCache();
    }

    protected void clearCache() {}

    protected abstract String getMainTableName();

    /** A collection of {@link DatabaseHelper}. */
    public static final class DatabaseHelpers {

        private final Set<DatabaseHelper> mDatabaseHelpers = new HashSet<>();

        /**
         * Deletes all entries from the database and clears the cache for all the helper class.
         *
         * <p>This function is only used for testing, do not use in production.
         */
        public void clearAllData(TransactionManager transactionManager) {
            for (DatabaseHelper databaseHelper : mDatabaseHelpers) {
                databaseHelper.clearData(transactionManager);
            }
        }

        /** Clears cache in all the helpers. */
        public void clearAllCache() {
            for (DatabaseHelper databaseHelper : mDatabaseHelpers) {
                databaseHelper.clearCache();
            }
        }

        private void add(DatabaseHelper databaseHelper) {
            mDatabaseHelpers.add(databaseHelper);
        }
    }
}
