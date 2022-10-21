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
import android.healthconnect.internal.datatypes.RecordInternal;

import java.lang.annotation.Retention;
import java.nio.ByteBuffer;
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

    public static void addNameBasedUUIDTo(@NonNull RecordInternal<?> recordInternal) {
        byte[] clientIDBlob;
        if (recordInternal.getClientRecordId() == null
                || recordInternal.getClientRecordId().isEmpty()) {
            clientIDBlob = UUID.randomUUID().toString().getBytes();
        } else {
            clientIDBlob = recordInternal.getClientRecordId().getBytes();
        }
        // TODO(b/249527913): Update with app ID once available
        byte[] appIdBlob = recordInternal.getPackageName().getBytes();

        ByteBuffer nameBasedUidBytes =
                ByteBuffer.allocate(appIdBlob.length + 4 + clientIDBlob.length)
                        .put(appIdBlob)
                        .putInt(recordInternal.getRecordType())
                        .put(clientIDBlob);

        recordInternal.setUuid(UUID.nameUUIDFromBytes(nameBasedUidBytes.array()).toString());
    }

    public static void addPackageNameTo(
            @NonNull RecordInternal<?> recordInternal, @NonNull String packageName) {
        recordInternal.setPackageName(packageName);
    }

    @Retention(SOURCE)
    @StringDef({TEXT_NOT_NULL, TEXT_NOT_NULL_UNIQUE, TEXT_NULL, INTEGER, PRIMARY_AUTOINCREMENT})
    public @interface SQLiteType {}
}
