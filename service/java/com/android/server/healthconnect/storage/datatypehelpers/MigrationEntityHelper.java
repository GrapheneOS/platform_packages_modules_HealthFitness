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

package com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.request.UpsertTableRequest.TYPE_STRING;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL_UNIQUE;

import android.content.ContentValues;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to help with the DB transaction for storing migration entity identifiers, user for
 * deduplication logic during the migration process.
 *
 * @hide
 */
public final class MigrationEntityHelper extends DatabaseHelper {

    public MigrationEntityHelper(DatabaseHelpers databaseHelpers) {
        super(databaseHelpers);
    }

    @VisibleForTesting public static final String TABLE_NAME = "migration_entity_table";
    private static final String COLUMN_ENTITY_ID = "entity_id";

    /** Returns a request to create a table for this helper. */
    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
    }

    @Override
    protected String getMainTableName() {
        return TABLE_NAME;
    }

    private static List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY));
        columnInfo.add(new Pair<>(COLUMN_ENTITY_ID, TEXT_NOT_NULL_UNIQUE));

        return columnInfo;
    }

    /** Returns a request to insert the provided {@code entityId}. */
    public UpsertTableRequest getInsertRequest(String entityId) {
        final ContentValues values = new ContentValues();
        values.put(COLUMN_ENTITY_ID, entityId);
        return new UpsertTableRequest(
                TABLE_NAME,
                values,
                Collections.singletonList(new Pair<>(COLUMN_ENTITY_ID, TYPE_STRING)));
    }
}
