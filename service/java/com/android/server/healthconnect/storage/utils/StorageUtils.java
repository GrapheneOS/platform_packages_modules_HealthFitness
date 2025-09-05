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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HYDRATION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NUTRITION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SYMPTOM;
import static android.text.TextUtils.isEmpty;

import static com.android.internal.annotations.VisibleForTesting.Visibility.PRIVATE;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.CLIENT_RECORD_ID_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.UUID_COLUMN_NAME;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.RecordIdFilter;
import android.health.connect.internal.datatypes.InstantRecordInternal;
import android.health.connect.internal.datatypes.IntervalRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;

import java.nio.ByteBuffer;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public static final String INTEGER_NOT_NULL = "INTEGER NOT NULL";
    public static final String REAL = "REAL";
    public static final String REAL_NOT_NULL = "REAL NOT NULL";
    public static final String PRIMARY_AUTOINCREMENT = "INTEGER PRIMARY KEY AUTOINCREMENT";
    public static final String PRIMARY = "INTEGER PRIMARY KEY";
    public static final String DELIMITER = ",";
    public static final String BLOB = "BLOB";
    public static final String BLOB_UNIQUE_NULL = "BLOB UNIQUE";
    public static final String BLOB_NULL = "BLOB NULL";
    public static final String BLOB_UNIQUE_NON_NULL = "BLOB NOT NULL UNIQUE";
    public static final String BLOB_NON_NULL = "BLOB NOT NULL";
    public static final String SELECT_ALL = "SELECT * FROM ";
    public static final String SELECT = "SELECT ";
    public static final String FROM = " FROM ";
    public static final String DISTINCT = "DISTINCT ";
    public static final String LIMIT_SIZE = " LIMIT ";
    public static final int BOOLEAN_FALSE_VALUE = 0;
    public static final int BOOLEAN_TRUE_VALUE = 1;
    public static final int UUID_BYTE_SIZE = 16;
    private static final String TAG = "HealthConnectUtils";

    // Returns null if fetching any of the fields resulted in an error
    @Nullable
    public static String getConflictErrorMessageForRecord(
            Cursor cursor, ContentValues contentValues) {
        try {
            return "Updating record with uuid: "
                    + convertBytesToUUID(contentValues.getAsByteArray(UUID_COLUMN_NAME))
                    + " and client record id: "
                    + contentValues.getAsString(CLIENT_RECORD_ID_COLUMN_NAME)
                    + " conflicts with an existing record with uuid: "
                    + getCursorUUID(cursor, UUID_COLUMN_NAME)
                    + "  and client record id: "
                    + getCursorString(cursor, CLIENT_RECORD_ID_COLUMN_NAME);
        } catch (Exception exception) {
            Slog.e(TAG, "", exception);
            return null;
        }
    }

    /**
     * Returns a UUID for the given triple {@code resourceId}, {@code resourceType} and {@code
     * dataSourceId}.
     */
    public static UUID generateMedicalResourceUUID(
            String resourceId, int resourceType, String dataSourceId) {
        final byte[] resourceIdBytes = resourceId.getBytes();
        final byte[] dataSourceIdBytes = dataSourceId.getBytes();

        byte[] bytes =
                ByteBuffer.allocate(
                                resourceIdBytes.length + Integer.BYTES + dataSourceIdBytes.length)
                        .put(resourceIdBytes)
                        .putInt(resourceType)
                        .put(dataSourceIdBytes)
                        .array();
        return UUID.nameUUIDFromBytes(bytes);
    }

    /**
     * Sets UUID for the given record. If {@link RecordInternal#getClientRecordId()} is null or
     * empty, then the UUID is randomly generated. Otherwise, the UUID is generated as a combination
     * of {@link RecordInternal#getPackageName()}, {@link RecordInternal#getClientRecordId()} and
     * {@link RecordInternal#getRecordType()}.
     */
    public static void addNameBasedUUIDTo(RecordInternal<?> recordInternal) {
        final String clientRecordId = recordInternal.getClientRecordId();
        if (isEmpty(clientRecordId)) {
            recordInternal.setUuid(UUID.randomUUID());
            return;
        }

        final UUID uuid =
                getUUID(
                        requireNonNull(recordInternal.getPackageName()),
                        clientRecordId,
                        recordInternal.getRecordType());
        recordInternal.setUuid(uuid);
    }

    /** Updates the uuid using the clientRecordID if the clientRecordId is present. */
    public static void updateNameBasedUUIDIfRequired(RecordInternal<?> recordInternal) {
        final String clientRecordId = recordInternal.getClientRecordId();
        if (isEmpty(clientRecordId)) {
            // If clientRecordID is absent, use the uuid already set in the input record and
            // hence no need to modify it.
            return;
        }

        final UUID uuid =
                getUUID(
                        requireNonNull(recordInternal.getPackageName()),
                        clientRecordId,
                        recordInternal.getRecordType());
        recordInternal.setUuid(uuid);
    }

    /**
     * Returns a UUID for the given {@link RecordIdFilter} and package name. If {@link
     * RecordIdFilter#getClientRecordId()} is null or empty, then the UUID corresponds to {@link
     * RecordIdFilter#getId()}. Otherwise, the UUID is generated as a combination of the package
     * name, {@link RecordIdFilter#getClientRecordId()} and {@link RecordIdFilter#getRecordType()}.
     */
    public static UUID getUUIDFor(RecordIdFilter recordIdFilter, String packageName) {
        final String clientRecordId = recordIdFilter.getClientRecordId();
        if (isEmpty(clientRecordId)) {
            return UUID.fromString(recordIdFilter.getId());
        }

        return getUUID(
                packageName,
                clientRecordId,
                HealthConnectMappings.getInstance().getRecordType(recordIdFilter.getRecordType()));
    }

    public static void addPackageNameTo(RecordInternal<?> recordInternal, String packageName) {
        recordInternal.setPackageName(packageName);
    }

    /** Checks if the value of given column is null */
    public static boolean isNullValue(Cursor cursor, String columnName) {
        return cursor.isNull(cursor.getColumnIndexOrThrow(columnName));
    }

    public static String getCursorString(Cursor cursor, String columnName) {
        return cursor.getString(cursor.getColumnIndexOrThrow(columnName));
    }

    public static UUID getCursorUUID(Cursor cursor, String columnName) {
        return convertBytesToUUID(cursor.getBlob(cursor.getColumnIndexOrThrow(columnName)));
    }

    public static int getCursorInt(Cursor cursor, String columnName) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(columnName));
    }

    /** Reads integer and converts to false anything apart from 1. */
    public static boolean getIntegerAndConvertToBoolean(Cursor cursor, String columnName) {
        String value = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
        if (value == null || value.isEmpty()) {
            return false;
        }
        return Integer.parseInt(value) == BOOLEAN_TRUE_VALUE;
    }

    public static long getCursorLong(Cursor cursor, String columnName) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(columnName));
    }

    public static double getCursorDouble(Cursor cursor, String columnName) {
        return cursor.getDouble(cursor.getColumnIndexOrThrow(columnName));
    }

    public static byte[] getCursorBlob(Cursor cursor, String columnName) {
        return cursor.getBlob(cursor.getColumnIndexOrThrow(columnName));
    }

    public static List<String> getCursorStringList(
            Cursor cursor, String columnName, String delimiter) {
        final String values = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.asList(values.split(delimiter));
    }

    public static List<Integer> getCursorIntegerList(
            Cursor cursor, String columnName, String delimiter) {
        final String stringList = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
        if (stringList == null || stringList.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(stringList.split(delimiter))
                .mapToInt(Integer::valueOf)
                .boxed()
                .toList();
    }

    public static List<Long> getCursorLongList(Cursor cursor, String columnName, String delimiter) {
        final String stringList = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
        if (stringList == null || stringList.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(stringList.split(delimiter)).mapToLong(Long::valueOf).boxed().toList();
    }

    /** [1, 2, 3] -> "1,2,3" */
    public static String flattenIntCollection(Collection<Integer> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(DELIMITER));
    }

    public static String flattenLongList(List<Long> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(DELIMITER));
    }

    private static String getMaxPrimaryKeyQuery(String tableName) {
        return "SELECT MAX("
                + PRIMARY_COLUMN_NAME
                + ") as "
                + PRIMARY_COLUMN_NAME
                + " FROM "
                + tableName;
    }

    /**
     * Reads ZoneOffset using given cursor. Returns null of column name is not present in the table.
     */
    @Nullable
    public static ZoneOffset getZoneOffset(Cursor cursor, String startZoneOffsetColumnName) {
        ZoneOffset zoneOffset = null;
        if (cursor.getColumnIndex(startZoneOffsetColumnName) != -1) {
            zoneOffset =
                    ZoneOffset.ofTotalSeconds(
                            StorageUtils.getCursorInt(cursor, startZoneOffsetColumnName));
        }

        return zoneOffset;
    }

    /** Encodes record properties participating in deduplication into a byte array. */
    @Nullable
    public static byte[] getDedupeByteBuffer(RecordInternal<?> record) {
        if (!isEmpty(record.getClientRecordId())) {
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

    private static byte[] getDedupeByteBuffer(InstantRecordInternal<?> record) {
        return ByteBuffer.allocate(Long.BYTES * 3)
                .putLong(record.getAppInfoId())
                .putLong(record.getDeviceInfoId())
                .putLong(record.getTimeInMillis())
                .array();
    }

    @Nullable
    private static byte[] getDedupeByteBuffer(IntervalRecordInternal<?> record) {
        final int type = record.getRecordType();
        if ((type == RECORD_TYPE_HYDRATION)
                || (type == RECORD_TYPE_NUTRITION)
                || (type == RECORD_TYPE_SYMPTOM)) {
            return null; // Some records are exempt from deduplication
        }

        return ByteBuffer.allocate(Long.BYTES * 4)
                .putLong(record.getAppInfoId())
                .putLong(record.getDeviceInfoId())
                .putLong(record.getStartTimeInMillis())
                .putLong(record.getEndTimeInMillis())
                .array();
    }

    /** Returns a UUID for the given package name, client record id and record type id. */
    private static UUID getUUID(String packageName, String clientRecordId, int recordTypeId) {
        final byte[] packageNameBytes = packageName.getBytes();
        final byte[] clientRecordIdBytes = clientRecordId.getBytes();

        byte[] bytes =
                ByteBuffer.allocate(
                                packageNameBytes.length
                                        + Integer.BYTES
                                        + clientRecordIdBytes.length)
                        .put(packageNameBytes)
                        .putInt(
                                InternalHealthConnectMappings.getInstance()
                                        .getRecordTypeIdForUuid(recordTypeId))
                        .put(clientRecordIdBytes)
                        .array();
        return UUID.nameUUIDFromBytes(bytes);
    }

    public static UUID convertBytesToUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }

    public static byte[] convertUUIDToBytes(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    /** Convert a double value to bytes. */
    public static byte[] convertDoubleToBytes(double value) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[8]);
        byteBuffer.putDouble(value);
        return byteBuffer.array();
    }

    /** Convert bytes to a double. */
    public static double convertBytesToDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    /** Convert an integer value to bytes. */
    public static byte[] convertIntToBytes(int value) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4]);
        byteBuffer.putInt(value);
        return byteBuffer.array();
    }

    /** Convert bytes to an integer. */
    public static int convertBytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    /** Convert bytes to a long. */
    public static byte[] convertLongToBytes(long value) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[8]);
        byteBuffer.putLong(value);
        return byteBuffer.array();
    }

    /** Convert a long value to bytes. */
    public static long convertBytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }

    /**
     * Creates a list of UUIDs from a collection of the string representation of the UUIDs. Any ids
     * which cannot be parsed as UUIDs are ignores. It is the responsibility of the caller to handle
     * the case where a non-empty list becomes empty.
     *
     * @param ids the ids to parse
     * @return a possibly empty list of UUIDs
     */
    public static List<UUID> toUuids(Collection<String> ids) {
        return ids.stream()
                .flatMap(
                        id -> {
                            try {
                                return Stream.of(UUID.fromString(id));
                            } catch (IllegalArgumentException ex) {
                                return Stream.of();
                            }
                        })
                .toList();
    }

    /** Converts a list of {@link UUID} strings to a list of hex strings. */
    public static List<String> convertUuidStringsToHexStrings(List<String> ids) {
        return StorageUtils.getListOfHexStrings(toUuids(ids));
    }

    public static String getHexString(byte[] value) {
        if (value == null) {
            return "";
        }

        final StringBuilder builder = new StringBuilder("x'");
        for (byte b : value) {
            builder.append(String.format("%02x", b));
        }
        builder.append("'");

        return builder.toString();
    }

    public static String getHexString(UUID uuid) {
        return getHexString(convertUUIDToBytes(uuid));
    }

    /** Creates a list of Hex strings for a given list of {@code UUID}s. */
    public static List<String> getListOfHexStrings(List<UUID> uuids) {
        List<String> hexStrings = new ArrayList<>();
        for (UUID uuid : uuids) {
            hexStrings.add(getHexString(convertUUIDToBytes(uuid)));
        }

        return hexStrings;
    }

    /**
     * Returns a byte array containing sublist of the given uuids list, from position {@code
     * start}(inclusive) to {@code end}(exclusive).
     */
    public static byte[] getSingleByteArray(List<UUID> uuids) {
        byte[] allByteArray = new byte[UUID_BYTE_SIZE * uuids.size()];

        ByteBuffer byteBuffer = ByteBuffer.wrap(allByteArray);
        for (UUID uuid : uuids) {
            byteBuffer.put(convertUUIDToBytes(uuid));
        }

        return byteBuffer.array();
    }

    public static List<UUID> getCursorUUIDList(Cursor cursor, String columnName) {
        byte[] bytes = cursor.getBlob(cursor.getColumnIndexOrThrow(columnName));
        return bytesToUuids(bytes);
    }

    /** Turns a byte array to a UUID list. */
    @VisibleForTesting(visibility = PRIVATE)
    public static List<UUID> bytesToUuids(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        List<UUID> uuidList = new ArrayList<>();
        while (byteBuffer.hasRemaining()) {
            long high = byteBuffer.getLong();
            long low = byteBuffer.getLong();
            uuidList.add(new UUID(high, low));
        }
        return uuidList;
    }

    /**
     * Returns a quoted, escaped id if {@code id} is not quoted. Following examples show the
     * expected return values,
     *
     * <p>getNormalisedId("id") -> "'id'"
     *
     * <p>getNormalisedId("'id'") -> "'id'"
     *
     * <p>getNormalisedId("x'id'") -> "x'id'"
     */
    public static String getNormalisedString(String id) {
        StringBuilder result = new StringBuilder();

        String innerId;
        if (id.startsWith("'") && id.endsWith("'")) {
            innerId = id.substring(1, id.length() - 1);
        } else if ((id.startsWith("x'") || id.startsWith("X'")) && id.endsWith("'")) {
            innerId = id.substring(2, id.length() - 1);
            result.append(id.charAt(0));
        } else {
            innerId = id;
        }

        DatabaseUtils.appendEscapedSQLString(result, innerId);

        return result.toString();
    }

    /** Checks whether {@code tableName} exists in the {@code database}. */
    public static boolean checkTableExists(SQLiteDatabase database, String tableName) {
        try (Cursor cursor =
                database.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                        new String[] {tableName})) {
            if (cursor.getCount() == 0) {
                Slog.d(TAG, "Table does not exist: " + tableName);
            }
            return cursor.getCount() > 0;
        }
    }

    /** Checks whether {@code columnName} exists in {@code tableName} in the {@code database}. */
    public static boolean checkColumnExists(
            SQLiteDatabase database, String tableName, String columnName) {
        String query =
                "SELECT COUNT(*) FROM pragma_table_info('"
                        + tableName
                        + "') WHERE name='"
                        + columnName
                        + "';";
        try (Cursor cursor = database.rawQuery(query, null)) {
            return cursor.moveToNext() && cursor.getInt(0) > 0;
        }
    }

    /** Gets the last id for {@code tableName} exists in the {@code database}. */
    public static long getLastRowIdFor(SQLiteDatabase database, String tableName) {
        try (Cursor cursor =
                database.rawQuery(StorageUtils.getMaxPrimaryKeyQuery(tableName), null)) {
            cursor.moveToFirst();
            return cursor.getLong(cursor.getColumnIndexOrThrow(PRIMARY_COLUMN_NAME));
        }
    }

    /** Extracts and holds data from {@link ContentValues}. */
    public static class RecordIdentifierData {
        private final String mClientRecordId;
        private final UUID mUuid;

        public RecordIdentifierData(ContentValues contentValues) {
            mClientRecordId = contentValues.getAsString(CLIENT_RECORD_ID_COLUMN_NAME);
            mUuid = StorageUtils.convertBytesToUUID(contentValues.getAsByteArray(UUID_COLUMN_NAME));
        }

        @Nullable
        public String getClientRecordId() {
            return mClientRecordId;
        }

        @Nullable
        public UUID getUuid() {
            return mUuid;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            if (mClientRecordId != null && !mClientRecordId.isEmpty()) {
                builder.append("clientRecordID : ").append(mClientRecordId).append(" , ");
            }

            if (mUuid != null) {
                builder.append("uuid : ").append(mUuid).append(" , ");
            }
            return builder.toString();
        }
    }
}
