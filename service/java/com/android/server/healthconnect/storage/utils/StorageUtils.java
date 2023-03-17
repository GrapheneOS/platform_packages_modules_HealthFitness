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

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.datatypes.AggregationType.SUM;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HYDRATION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NUTRITION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED;

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.CLIENT_RECORD_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.HealthDataCategory;
import android.health.connect.RecordIdFilter;
import android.health.connect.internal.datatypes.InstantRecordInternal;
import android.health.connect.internal.datatypes.IntervalRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.RecordMapper;
import android.health.connect.internal.datatypes.utils.RecordTypeRecordCategoryMapper;
import android.text.TextUtils;

import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;

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
    public static final String BLOB_UNIQUE_NULL = "BLOB UNIQUE";
    public static final String SELECT_ALL = "SELECT * FROM ";
    public static final String LIMIT_SIZE = " LIMIT ";
    public static final int BOOLEAN_FALSE_VALUE = 0;
    public static final int BOOLEAN_TRUE_VALUE = 1;

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

    /** Checks if the value of given column is null */
    public static boolean isNullValue(Cursor cursor, String columnName) {
        return cursor.isNull(cursor.getColumnIndex(columnName));
    }

    public static String getCursorString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndex(columnName));
    }

    public static int getCursorInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName));
    }

    /** Reads integer and converts to true anything apart from 0. */
    public static boolean getIntegerAndConvertToBoolean(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndex(columnName)) != BOOLEAN_FALSE_VALUE;
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

    /** Encodes record properties participating in deduplication into a byte array. */
    @Nullable
    public static byte[] getDedupeByteBuffer(@NonNull RecordInternal<?> record) {
        if (!TextUtils.isEmpty(record.getClientRecordId())) {
            return null; // If dedupe by clientRecordId then don't dedupe by hash
        }

        if (record instanceof InstantRecordInternal<?>) {
            return getDedupeByteBuffer((InstantRecordInternal<?>) record);
        }

        if (record instanceof IntervalRecordInternal<?>) {
            return getDedupeByteBuffer((IntervalRecordInternal<?>) record);
        }

        throw new IllegalArgumentException("Unexpected record type: " + record);
    }

    @NonNull
    private static byte[] getDedupeByteBuffer(@NonNull InstantRecordInternal<?> record) {
        return ByteBuffer.allocate(Long.BYTES * 3)
                .putLong(record.getAppInfoId())
                .putLong(record.getDeviceInfoId())
                .putLong(record.getTimeInMillis())
                .array();
    }

    @Nullable
    private static byte[] getDedupeByteBuffer(@NonNull IntervalRecordInternal<?> record) {
        final int type = record.getRecordType();
        if ((type == RECORD_TYPE_HYDRATION) || (type == RECORD_TYPE_NUTRITION)) {
            return null; // Some records are exempt from deduplication
        }

        return ByteBuffer.allocate(Long.BYTES * 4)
                .putLong(record.getAppInfoId())
                .putLong(record.getDeviceInfoId())
                .putLong(record.getStartTimeInMillis())
                .putLong(record.getEndTimeInMillis())
                .array();
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

    /**
     * Returns if priority of apps needs to be considered to compute the aggregate request for the
     * record type. Priority to be considered only for sleep and Activity categories.
     */
    public static boolean supportsPriority(int recordType, int operationType) {
        if (recordType != RECORD_TYPE_EXERCISE_SESSION) {
            @HealthDataCategory.Type
            Integer recordCategory =
                    RecordTypeRecordCategoryMapper.getRecordCategoryForRecordType(recordType);
            if (recordCategory == ACTIVITY && operationType == SUM) {
                return true;
            }
        }
        return false;
    }

    /** Returns list of app Ids of contributing apps for the record type in the priority order */
    public static List<Long> getAppIdPriorityList(int recordType) {
        return HealthDataCategoryPriorityHelper.getInstance()
                .getAppIdPriorityOrder(
                        RecordTypeRecordCategoryMapper.getRecordCategoryForRecordType(recordType));
    }

    /** Returns if derivation needs to be done to calculate aggregate */
    public static boolean isDerivedType(int recordType) {
        if (recordType == RECORD_TYPE_BASAL_METABOLIC_RATE
                || recordType == RECORD_TYPE_TOTAL_CALORIES_BURNED) {
            return true;
        }
        return false;
    }
}
