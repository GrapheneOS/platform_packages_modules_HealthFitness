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

package com.android.server.healthconnect.logging;

import android.annotation.Nullable;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import com.android.server.healthconnect.fitness.recordhelpers.InstantRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SeriesRecordHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;

import java.util.Collection;

/**
 * Helper class to collect Health Connect database stats for logging.
 *
 * @hide
 */
public class DatabaseStatsCollector {

    private static final long NO_DATA = -1;

    private final TransactionManager mTransactionManager;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings =
            InternalHealthConnectMappings.getInstance();

    public DatabaseStatsCollector(TransactionManager transactionManager) {
        mTransactionManager = transactionManager;
    }

    /** Get the size of Health Connect database. */
    public long getDatabaseSize() {
        return mTransactionManager.getDatabaseSize();
    }

    /** Get the number of interval record entries in Health Connect database. */
    long getNumberOfIntervalRecordRows() {
        long count = 0L;
        for (RecordHelper<?> recordHelper : mInternalHealthConnectMappings.getRecordHelpers()) {
            if (recordHelper instanceof IntervalRecordHelper
                    && !(recordHelper instanceof SeriesRecordHelper)) {
                count += queryNumEntries(recordHelper.getMainTableName());
            }
        }
        return count;
    }

    /** Get the number of series record entries in Health Connect database. */
    long getNumberOfSeriesRecordRows() {
        long count = 0L;
        for (RecordHelper<?> recordHelper : mInternalHealthConnectMappings.getRecordHelpers()) {
            if (recordHelper instanceof SeriesRecordHelper) {
                count += queryNumEntries(recordHelper.getMainTableName());
            }
        }
        return count;
    }

    /** Get the number of instant record entries in Health Connect database. */
    long getNumberOfInstantRecordRows() {
        long count = 0L;
        for (RecordHelper<?> recordHelper : mInternalHealthConnectMappings.getRecordHelpers()) {
            if (recordHelper instanceof InstantRecordHelper) {
                count += queryNumEntries(recordHelper.getMainTableName());
            }
        }
        return count;
    }

    /** Get the number of change log entries in Health Connect database. */
    long getNumberOfChangeLogs() {
        return queryNumEntries(ChangeLogsHelper.TABLE_NAME);
    }

    /** Get the number of rows in the given table. */
    private long queryNumEntries(String tableName) {
        return mTransactionManager.queryNumEntries(tableName);
    }

    /**
     * Reads the total number of bytes of disk used to store the given tables. See <a
     * href="https://sqlite.org/dbstat.html">SQLite {@code dbstat} documentation</a>.
     *
     * @param tables a collection of table names to add together
     * @return the total number of bytes used to store those tables, or null if this information
     *     cannot be read
     */
    @Nullable
    Long getFileBytes(Collection<String> tables) {
        if (tables.isEmpty()) {
            return 0L;
        }
        StringBuilder sql = new StringBuilder("SELECT SUM(pgsize) FROM dbstat WHERE name IN (");
        String[] args = new String[tables.size()];
        int index = 0;
        for (String table : tables) {
            sql.append("?,");
            args[index++] = table;
        }
        sql.setCharAt(sql.length() - 1, ')');
        try {
            long bytes =
                    mTransactionManager.runAsTransaction(
                            db -> {
                                try (Cursor cursor = db.rawQuery(sql.toString(), args)) {
                                    if (!cursor.moveToFirst()) {
                                        return NO_DATA;
                                    }
                                    return cursor.getLong(0);
                                }
                            });
            if (bytes == NO_DATA) {
                return null;
            }
            return bytes;
        } catch (SQLiteException e) {
            // This can happen if the dbstat table does not exist. If so, carry on.
            return null;
        }
    }
}
