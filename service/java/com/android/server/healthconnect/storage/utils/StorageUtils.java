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

package com.android.server.healthconnect.storage.utils;

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.CLIENT_RECORD_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.content.ContentValues;
import android.database.Cursor;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.utils.RecordMapper;

import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;

import java.lang.annotation.Retention;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An util class for HC storage
 *
 * @hide
 */
public final class StorageUtils {
    public static final String TEXT_NOT_NULL = "TEXT NOT NULL";
    public static final String TEXT_NOT_NULL_UNIQUE = "TEXT NOT NULL UNIQUE";
    public static final String TEXT_NULL = "TEXT";
    public static final String INTEGER = "INTEGER";
    public static final String INTEGER_UNIQUE = "INTEGER UNIQUE";
    public static final String INTEGER_NOT_NULL_UNIQUE = "INTEGER NOT NULL UNIQUE";
    public static final String INTEGER_NOT_NULL = "INTEGER NOT NULL";
    public static final String REAL = "REAL";
    public static final String REAL_NOT_NULL = "REAL NOT NULL";
    public static final String PRIMARY_AUTOINCREMENT = "INTEGER PRIMARY KEY AUTOINCREMENT";
    public static final String PRIMARY = "INTEGER PRIMARY KEY";
    public static final String DELIMITER = ",";
    public static final String BLOB = "BLOB";
    public static final String SELECT_ALL = "SELECT * FROM ";
    public static final String LIMIT_SIZE = " LIMIT ";

    public static void addNameBasedUUIDTo(@NonNull RecordInternal<?> recordInternal) {
        byte[] clientIDBlob;
        if (recordInternal.getClientRecordId() == null
                || recordInternal.getClientRecordId().isEmpty()) {
            clientIDBlob = UUID.randomUUID().toString().getBytes();
        } else {
            clientIDBlob = recordInternal.getClientRecordId().getBytes();
        }

        byte[] nameBasedUidBytes =
                getUUIDByteBuffer(
                        recordInternal.getAppInfoId(),
                        clientIDBlob,
                        recordInternal.getRecordType());

        recordInternal.setUuid(UUID.nameUUIDFromBytes(nameBasedUidBytes).toString());
    }

    /** Updates the uuid using the clientRecordID if the clientRecordId is present. */
    public static void updateNameBasedUUIDIfRequired(@NonNull RecordInternal<?> recordInternal) {
        byte[] clientIDBlob;
        if (recordInternal.getClientRecordId() == null
                || recordInternal.getClientRecordId().isEmpty()) {
            // If clientRecordID is absent, use the uuid already set in the input record and
            // hence no need to modify it.
            return;
        }
        clientIDBlob = recordInternal.getClientRecordId().getBytes();
        byte[] nameBasedUidBytes =
                getUUIDByteBuffer(
                        recordInternal.getAppInfoId(),
                        clientIDBlob,
                        recordInternal.getRecordType());

        recordInternal.setUuid(UUID.nameUUIDFromBytes(nameBasedUidBytes).toString());
    }

    public static String getUUIDFor(RecordIdFilter recordIdFilter, String packageName) {
        byte[] clientIDBlob;
        if (recordIdFilter.getClientRecordId() == null
                || recordIdFilter.getClientRecordId().isEmpty()) {
            return recordIdFilter.getId();
        }
        clientIDBlob = recordIdFilter.getClientRecordId().getBytes();

        byte[] nameBasedUidBytes =
                getUUIDByteBuffer(
                        AppInfoHelper.getInstance().getAppInfoId(packageName),
                        clientIDBlob,
                        RecordMapper.getInstance().getRecordType(recordIdFilter.getRecordType()));

        return UUID.nameUUIDFromBytes(nameBasedUidBytes).toString();
    }

    public static void addPackageNameTo(
            @NonNull RecordInternal<?> recordInternal, @NonNull String packageName) {
        recordInternal.setPackageName(packageName);
    }

    public static String getCursorString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    public static int getCursorInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    public static long getCursorLong(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndex(columnName));
    }

    public static double getCursorDouble(Cursor cursor, String columnName) {
        return cursor.getDouble(cursor.getColumnIndex(columnName));
    }

    public static byte[] getCursorBlob(Cursor cursor, String columnName) {
        return cursor.getBlob(cursor.getColumnIndex(columnName));
    }

    public static List<String> getCursorStringList(
            Cursor cursor, String columnName, String delimiter) {
        final String values = cursor.getString(cursor.getColumnIndex(columnName));
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.asList(values.split(delimiter));
    }

    public static List<Integer> getCursorIntegerList(
            Cursor cursor, String columnName, String delimiter) {
        final String stringList = cursor.getString(cursor.getColumnIndex(columnName));
        if (stringList == null || stringList.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(stringList.split(delimiter))
                .mapToInt(Integer::valueOf)
                .boxed()
                .toList();
    }

    public static List<Long> getCursorLongList(Cursor cursor, String columnName, String delimiter) {
        final String stringList = cursor.getString(cursor.getColumnIndex(columnName));
        if (stringList == null || stringList.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(stringList.split(delimiter)).mapToLong(Long::valueOf).boxed().toList();
    }

    public static String flattenIntList(List<Integer> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(DELIMITER));
    }

    public static String flattenLongList(List<Long> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(DELIMITER));
    }

    public static String flattenIntArray(int[] values) {
        return Arrays.stream(values)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(DELIMITER));
    }

    @Nullable
    public static String getMaxPrimaryKeyQuery(@NonNull String tableName) {
        return "SELECT MAX("
                + PRIMARY_COLUMN_NAME
                + ") as "
                + PRIMARY_COLUMN_NAME
                + " FROM "
                + tableName;
    }

    public static long getPeriodDelta(Period period) {
        return period.getDays();
    }

    public static LocalDateTime getPeriodLocalDateTime(long period) {
        return LocalDateTime.of(LocalDate.ofEpochDay(period), LocalTime.MIN);
    }

    public static Instant getDurationInstant(long duration) {
        return Instant.ofEpochMilli(duration);
    }

    public static long getDurationDelta(Duration duration) {
        return Math.max(duration.toMillis(), 1); // to millis
    }

    private static byte[] getUUIDByteBuffer(long appId, byte[] clientIDBlob, int recordId) {
        return ByteBuffer.allocate(Long.BYTES + Integer.BYTES + clientIDBlob.length)
                .putLong(appId)
                .putInt(recordId)
                .put(clientIDBlob)
                .array();
    }

    @Retention(SOURCE)
    @StringDef({
        TEXT_NOT_NULL,
        TEXT_NOT_NULL_UNIQUE,
        TEXT_NULL,
        INTEGER,
        PRIMARY_AUTOINCREMENT,
        PRIMARY,
        BLOB
    })
    public @interface SQLiteType {}

    /** Extracts and holds data from {@link ContentValues}. */
    public static class RecordIdentifierData {
        private String mClientRecordId = "";
        private String mUuid = "";
        private final String mAppInfoId = "";

        public RecordIdentifierData(ContentValues contentValues) {
            mClientRecordId = contentValues.getAsString(CLIENT_RECORD_ID_COLUMN_NAME);
            mUuid = contentValues.getAsString(UUID_COLUMN_NAME);
        }

        @Nullable
        public String getClientRecordId() {
            return mClientRecordId;
        }

        @Nullable
        public String getUuid() {
            return mUuid;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            if (mClientRecordId != null && !mClientRecordId.isEmpty()) {
                builder.append("clientRecordID : ").append(mClientRecordId).append(" , ");
            }

            if (mUuid != null && !mUuid.isEmpty()) {
                builder.append("uuid : ").append(mUuid).append(" , ");
            }
            return builder.toString();
        }
    }
}
