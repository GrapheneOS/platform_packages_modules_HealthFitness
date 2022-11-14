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

package com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.storage.utils.StorageUtils.DELIMITER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL_UNIQUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorStringList;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.healthconnect.datatypes.Record;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.utils.RecordMapper;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper for Activity Date Table. The table maps a record to a date on which there was a db write
 * for that record
 *
 * @hide
 */
public final class ActivityDateHelper {
    private static final String TABLE_NAME = "activity_date_table";
    private static final String DATE_COLUMN_NAME = "date";
    private static final String RECORD_TYPE_ID_COLUMN_NAME = "record_type_id";

    private static ActivityDateHelper sActivityDateHelper;

    private ActivityDateHelper() {}

    /** Returns an instance of this class */
    public static ActivityDateHelper getInstance() {
        if (sActivityDateHelper == null) {
            sActivityDateHelper = new ActivityDateHelper();
        }

        return sActivityDateHelper;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    @NonNull
    public CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
    }

    @NonNull
    List<Pair<String, String>> getColumnInfo() {
        // TODO(260554339): Update the db columns to store dates as integers
        return Arrays.asList(
                new Pair<>(RecordHelper.PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT),
                new Pair<>(DATE_COLUMN_NAME, TEXT_NOT_NULL),
                new Pair<>(RECORD_TYPE_ID_COLUMN_NAME, INTEGER_NOT_NULL_UNIQUE));
    }

    /** Called on DB update. */
    public void onUpgrade(int newVersion, @NonNull SQLiteDatabase db) {
        // empty by default
    }

    /** Insert a new activity dates for the given records */
    @NonNull
    public void insertRecordDate(@NonNull List<RecordInternal<?>> recordInternals) {
        Objects.requireNonNull(recordInternals);

        final TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        HashMap<Integer, Set<String>> recordIdToDistinctDatesMap =
                getRecordIdToDistinctDatesMap(recordInternals);
        List<UpsertTableRequest> upsertTableRequests = new ArrayList<>();
        recordIdToDistinctDatesMap.forEach(
                (recordTypeId, dates) ->
                        upsertTableRequests.add(
                                new UpsertTableRequest(
                                        TABLE_NAME, getContentValues(recordTypeId, dates))));

        transactionManager.insertAll(upsertTableRequests);
    }

    /** Returns a list of all dates with database writes for the given record types */
    @NonNull
    public List<LocalDate> getActivityDates(@NonNull List<Class<? extends Record>> recordTypes) {
        RecordMapper recordMapper = RecordMapper.getInstance();
        List<Integer> recordTypeIds =
                recordTypes.stream().map(recordMapper::getRecordType).collect(Collectors.toList());
        Set<String> distinctDates =
                readDates(
                                new ReadTableRequest(TABLE_NAME)
                                        .setWhereClause(
                                                new WhereClauses()
                                                        .addWhereInIntsClause(
                                                                RECORD_TYPE_ID_COLUMN_NAME,
                                                                recordTypeIds)))
                        .values()
                        .stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toSet());

        return distinctDates.stream().map(LocalDate::parse).collect(Collectors.toList());
    }

    /** Returns record id to dates map from a Cursor object */
    @NonNull
    public HashMap<Integer, List<String>> getRecordIdToDatesMap(@NonNull Cursor cursor) {
        HashMap<Integer, List<String>> localDates = new HashMap<>();

        while (cursor.moveToNext()) {
            int recordTypeId = getCursorInt(cursor, RECORD_TYPE_ID_COLUMN_NAME);
            List<String> dates = getCursorStringList(cursor, DATE_COLUMN_NAME, DELIMITER);
            localDates.put(recordTypeId, dates);
        }
        return localDates;
    }

    @NonNull
    private ContentValues getContentValues(int recordTypeId, @NonNull Set<String> dates) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATE_COLUMN_NAME, String.join(DELIMITER, dates));
        contentValues.put(RECORD_TYPE_ID_COLUMN_NAME, recordTypeId);

        return contentValues;
    }

    /**
     * Reads the dates stored in the HealthConnect database.
     *
     * @param request a read request.
     * @return Cursor from table based on ids.
     */
    private HashMap<Integer, List<String>> readDates(@NonNull ReadTableRequest request) {
        final TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        try (SQLiteDatabase db = transactionManager.getReadableDb();
                Cursor cursor = transactionManager.read(db, request)) {

            return getRecordIdToDatesMap(cursor);
        }
    }

    /** Returns a map of updated recordId to date map with comparison to the database */
    @NonNull
    private HashMap<Integer, Set<String>> getRecordIdToDistinctDatesMap(
            @NonNull List<RecordInternal<?>> recordInternals) {

        HashMap<Integer, List<String>> existingDates = readDates(new ReadTableRequest(TABLE_NAME));

        HashMap<Integer, Set<String>> recordIdToDates = new HashMap<>();

        for (RecordInternal<?> recordInternal : recordInternals) {
            Set<String> dates =
                    recordIdToDates.getOrDefault(recordInternal.getRecordType(), new HashSet<>());

            dates.add(recordInternal.getLocalDate().toString());

            if (existingDates.containsKey(recordInternal.getRecordType())) {
                dates.addAll(existingDates.remove(recordInternal.getRecordType()));
            }

            recordIdToDates.put(recordInternal.getRecordType(), dates);
        }
        return recordIdToDates;
    }
}
