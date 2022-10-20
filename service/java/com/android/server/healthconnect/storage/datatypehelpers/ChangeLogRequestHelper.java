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

import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;

import android.content.ContentValues;
import android.healthconnect.aidl.ChangeLogTokenRequestParcel;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A class to interact with the DB table that stores the information about the change log requests
 * i.e. {@code TABLE_NAME}
 *
 * <p>This class returns the row_id of the change_log_request_table as a token, that can later be
 * used to recreate the request.
 *
 * @hide
 */
public final class ChangeLogRequestHelper {
    private static final String TABLE_NAME = "change_log_request_table";
    private static final String PACKAGE_FILTERS_COLUMN_NAME = "package_filters";
    private static final String RECORD_IDS_COLUMN_NAME = "record_ids";
    private static final String PACKAGE_NAME_COLUMN_NAME = "package_name";
    private static ChangeLogRequestHelper sChangeLogRequestHelper;

    private ChangeLogRequestHelper() {}

    public static ChangeLogRequestHelper getInstance() {
        if (sChangeLogRequestHelper == null) {
            sChangeLogRequestHelper = new ChangeLogRequestHelper();
        }

        return sChangeLogRequestHelper;
    }

    public CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnsInfo());
    }

    public long getToken(ChangeLogTokenRequestParcel request, String packageName) {
        ContentValues contentValues = new ContentValues();

        /**
         * Store package names here as a package name and not as {@link AppInfoHelper.AppInfo#mId}
         * as ID might not be available right now but might become available when the actual request
         * for this token comes
         */
        contentValues.put(
                PACKAGE_FILTERS_COLUMN_NAME, request.getPackageNamesToFilter().toString());
        contentValues.put(RECORD_IDS_COLUMN_NAME, Arrays.toString(request.getRecordTypes()));
        contentValues.put(PACKAGE_NAME_COLUMN_NAME, packageName);

        return TransactionManager.getInitialisedInstance()
                .insert(new UpsertTableRequest(TABLE_NAME, contentValues));
    }

    private List<Pair<String, String>> getColumnsInfo() {
        List<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(RecordHelper.PRIMARY_COLUMN_NAME, PRIMARY));
        columnInfo.add(new Pair<>(PACKAGE_FILTERS_COLUMN_NAME, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(PACKAGE_NAME_COLUMN_NAME, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(RECORD_IDS_COLUMN_NAME, TEXT_NOT_NULL));

        return columnInfo;
    }
}
