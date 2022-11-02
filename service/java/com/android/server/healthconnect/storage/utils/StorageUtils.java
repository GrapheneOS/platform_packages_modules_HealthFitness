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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.NonNull;
import android.annotation.StringDef;
import android.database.Cursor;
import android.healthconnect.RecordId;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.utils.RecordMapper;

import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;

import java.lang.annotation.Retention;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
    public static final String REAL = "REAL";
    public static final String PRIMARY_AUTOINCREMENT = "INTEGER PRIMARY KEY AUTOINCREMENT";
    public static final String PRIMARY = "INTEGER PRIMARY KEY";
    public static final String BLOB = "BLOB";

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

    public static String getUUIDFor(RecordId recordId, String packageName) {
        byte[] clientIDBlob;
        if (recordId.getClientRecordId() == null || recordId.getClientRecordId().isEmpty()) {
            return recordId.getId();
        }
        clientIDBlob = recordId.getClientRecordId().getBytes();

        byte[] nameBasedUidBytes =
                getUUIDByteBuffer(
                        AppInfoHelper.getInstance().getAppInfoId(packageName),
                        clientIDBlob,
                        RecordMapper.getInstance().getRecordType(recordId.getRecordType()));

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
        return Integer.parseInt(cursor.getString(cursor.getColumnIndex(columnName)));
    }

    public static List<String> getCursorStringList(
            Cursor cursor, String columnName, String delimiter) {
        final String stringList = cursor.getString(cursor.getColumnIndex(columnName));
        if (stringList == null || stringList.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.asList(stringList.split(delimiter));
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
}
