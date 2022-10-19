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

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parent class for all the helper classes for all the records
 *
 * @hide
 */
public abstract class RecordHelper<T extends RecordInternal<?>> {
    private static final String PRIMARY_COLUMN_NAME = "row_id";
    private static final String UUID_COLUMN_NAME = "uuid";
    private static final String PACKAGE_NAME_COLUMN_NAME = "package_name";
    private static final String LAST_MODIFIED_TIME_COLUMN_NAME = "last_modified_time";
    private static final String CLIENT_RECORD_ID_COLUMN_NAME = "client_record_id";
    private static final String CLIENT_RECORD_VERSION_COLUMN_NAME = "client_record_version";
    private static final String MANUFACTURER_COLUMN_NAME = "manufacturer";
    private static final String MODEL_COLUMN_NAME = "model";
    private static final String DEVICE_TYPE_COLUMN_NAME = "device_type_column_name";
    private static final String CREATE_TABLE_COMMAND = "CREATE TABLE IF NOT EXISTS ";
    private final int mRecordIdentifier;

    RecordHelper() {
        HelperFor annotation = this.getClass().getAnnotation(HelperFor.class);
        Objects.requireNonNull(annotation);
        mRecordIdentifier = annotation.recordIdentifier();
    }

    @NonNull
    public final String getCreateTableCommand() {
        final StringBuilder builder = new StringBuilder(CREATE_TABLE_COMMAND);
        builder.append(getTableName());
        builder.append(" (");
        getColumnInfo()
                .forEach(
                        columnInfo -> {
                            builder.append(columnInfo.first)
                                    .append(" ")
                                    .append(columnInfo.second)
                                    .append(", ");
                        });
        builder.setLength(builder.length() - 2); // Remove the last 2 char i.e. ", "
        builder.append(")");

        return builder.toString();
    }

    @NonNull
    public final ContentValues getContentValuesFor(@NonNull T recordInternal) {
        ContentValues recordContentValues = new ContentValues();

        recordContentValues.put(UUID_COLUMN_NAME, recordInternal.getUuid());
        recordContentValues.put(PACKAGE_NAME_COLUMN_NAME, recordInternal.getPackageName());
        recordContentValues.put(
                LAST_MODIFIED_TIME_COLUMN_NAME, recordInternal.getLastModifiedTime());
        recordContentValues.put(CLIENT_RECORD_ID_COLUMN_NAME, recordInternal.getClientRecordId());
        recordContentValues.put(
                CLIENT_RECORD_VERSION_COLUMN_NAME, recordInternal.getClientRecordVersion());
        recordContentValues.put(MANUFACTURER_COLUMN_NAME, recordInternal.getManufacturer());
        recordContentValues.put(MODEL_COLUMN_NAME, recordInternal.getModel());
        recordContentValues.put(DEVICE_TYPE_COLUMN_NAME, recordInternal.getDeviceType());

        populateContentValues(recordContentValues, recordInternal);

        return recordContentValues;
    }

    public int getRecordIdentifier() {
        return mRecordIdentifier;
    }

    // Called on DB update. Inheriting classes should implement this if they need to add new
    // columns.
    public void onUpgrade(int newVersion, @NonNull SQLiteDatabase db) {
        // empty by default
    }

    /** Returns the table name to be created corresponding to this helper */
    @NonNull
    public abstract String getTableName();

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    @NonNull
    abstract List<Pair<String, SQLiteType>> getSpecificColumnInfo();

    abstract void populateContentValues(
            @NonNull ContentValues contentValues, @NonNull T recordInternal);

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    @NonNull
    private List<Pair<String, SQLiteType>> getColumnInfo() {
        ArrayList<Pair<String, SQLiteType>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, SQLiteType.PRIMARY_AUTOINCREMENT));
        columnInfo.add(new Pair<>(UUID_COLUMN_NAME, SQLiteType.TEXT_NOT_NULL_UNIQUE));
        columnInfo.add(new Pair<>(PACKAGE_NAME_COLUMN_NAME, SQLiteType.TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(LAST_MODIFIED_TIME_COLUMN_NAME, SQLiteType.INTEGER));
        columnInfo.add(new Pair<>(CLIENT_RECORD_ID_COLUMN_NAME, SQLiteType.TEXT_NULL));
        columnInfo.add(new Pair<>(CLIENT_RECORD_VERSION_COLUMN_NAME, SQLiteType.TEXT_NULL));
        columnInfo.add(new Pair<>(MANUFACTURER_COLUMN_NAME, SQLiteType.TEXT_NULL));
        columnInfo.add(new Pair<>(MODEL_COLUMN_NAME, SQLiteType.TEXT_NULL));
        columnInfo.add(new Pair<>(DEVICE_TYPE_COLUMN_NAME, SQLiteType.INTEGER));

        columnInfo.addAll(getSpecificColumnInfo());

        return columnInfo;
    }

    public enum SQLiteType {
        TEXT_NOT_NULL("TEXT NOT NULL"),
        TEXT_NOT_NULL_UNIQUE("TEXT NOT NULL UNIQUE"),
        TEXT_NULL("TEXT"),
        INTEGER("INTEGER"),
        // Note: This is meant to be only used by this class
        PRIMARY_AUTOINCREMENT("INTEGER PRIMARY KEY AUTOINCREMENT");

        private final String text;

        SQLiteType(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
