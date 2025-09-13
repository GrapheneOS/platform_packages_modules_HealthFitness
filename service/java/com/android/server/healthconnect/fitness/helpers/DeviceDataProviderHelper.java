/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.server.healthconnect.fitness.helpers;

import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.DEVICE_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;

import android.util.Pair;

import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.DatabaseHelper;
import com.android.server.healthconnect.storage.request.CreateTableRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to help with the DB transaction for storing Device Data Provider metadata and state.
 *
 * @hide
 */
public class DeviceDataProviderHelper extends DatabaseHelper {

    public static final String TABLE_NAME = "device_data_provider_table";
    public static final String SOURCE_PACKAGE_NAME = "source_package_name";
    public static final String DATA_TYPE = "data_type";
    public static final String IS_AVAILABLE = "is_available";
    public static final String IS_USER_ENABLED = "is_user_enabled";

    public DeviceDataProviderHelper(DatabaseHelpers databaseHelpers) {
        super(databaseHelpers);
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo())
                .addForeignKey(
                        /* referencedTable= */ DeviceInfoHelper.TABLE_NAME,
                        /* columnNames= */ List.of(DEVICE_INFO_ID_COLUMN_NAME),
                        /* referencedColumnNames= */ List.of(RecordHelper.PRIMARY_COLUMN_NAME));
    }

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    private static List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(RecordHelper.PRIMARY_COLUMN_NAME, PRIMARY));
        columnInfo.add(new Pair<>(DEVICE_INFO_ID_COLUMN_NAME, INTEGER_NOT_NULL)); // Foreign key
        columnInfo.add(new Pair<>(SOURCE_PACKAGE_NAME, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(DATA_TYPE, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(IS_AVAILABLE, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(IS_USER_ENABLED, INTEGER_NOT_NULL));

        return columnInfo;
    }

    @Override
    protected String getMainTableName() {
        return TABLE_NAME;
    }
}
