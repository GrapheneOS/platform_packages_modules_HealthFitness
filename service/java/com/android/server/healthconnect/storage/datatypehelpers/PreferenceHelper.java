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

import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL_UNIQUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A helper class to store user preferences, set in UI APK for the platform.
 *
 * @hide
 */
public final class PreferenceHelper {
    private static final String TABLE_NAME = "preference_table";
    private static final String KEY_COLUMN_NAME = "key";
    private static final String VALUE_COLUMN_NAME = "value";
    private static PreferenceHelper sPreferenceHelper;
    private ConcurrentHashMap<String, String> mPreferences;

    private PreferenceHelper() {}

    public static PreferenceHelper getInstance() {
        if (sPreferenceHelper == null) {
            sPreferenceHelper = new PreferenceHelper();
        }

        return sPreferenceHelper;
    }

    public void insertPreference(String key, String value) {
        TransactionManager.getInitialisedInstance()
                .insertOrReplace(new UpsertTableRequest(TABLE_NAME, getContentValues(key, value)));
        getPreferences().put(key, value);
    }

    @NonNull
    public CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
    }

    @Nullable
    public String getPreference(String key) {
        return getPreferences().get(key);
    }

    private Map<String, String> getPreferences() {
        if (mPreferences == null) {
            populatePreferences();
        }
        return mPreferences;
    }

    @NonNull
    private ContentValues getContentValues(String key, String value) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(KEY_COLUMN_NAME, key);
        contentValues.put(VALUE_COLUMN_NAME, value);
        return contentValues;
    }

    private void populatePreferences() {
        mPreferences = new ConcurrentHashMap<>();
        final TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        try (SQLiteDatabase db = transactionManager.getReadableDb();
                Cursor cursor = transactionManager.read(db, new ReadTableRequest(TABLE_NAME))) {
            while (cursor.moveToNext()) {
                String key = StorageUtils.getCursorString(cursor, KEY_COLUMN_NAME);
                String value = StorageUtils.getCursorString(cursor, VALUE_COLUMN_NAME);
                mPreferences.put(key, value);
            }
        }
    }

    @NonNull
    private List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(KEY_COLUMN_NAME, TEXT_NOT_NULL_UNIQUE));
        columnInfo.add(new Pair<>(VALUE_COLUMN_NAME, TEXT_NULL));

        return columnInfo;
    }
}
