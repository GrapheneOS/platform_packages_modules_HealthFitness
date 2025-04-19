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

package com.android.server.healthconnect.storage.request;

import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.database.SQLException;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.storage.utils.StorageUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AlterTableRequestTest {
    private static final String TABLE_NAME = "sample_table";
    private static final String COLUMN_NAME_1 = "sample_column_1";
    private static final String COLUMN_NAME_2 = "sample_column_2";
    private static final String COLUMN_TYPE = StorageUtils.INTEGER;

    @Test
    public void testAlterTableRequest_getAlterTableAddColumnsCommand_addSingleColumn() {
        List<Pair<String, String>> columnInfo = List.of(Pair.create(COLUMN_NAME_1, COLUMN_TYPE));

        AlterTableRequest alterTableRequest = new AlterTableRequest(TABLE_NAME, columnInfo);

        assertThat(alterTableRequest.getAddColumnsCommands())
                .containsExactly("ALTER TABLE sample_table ADD COLUMN sample_column_1 INTEGER;");
    }

    @Test
    public void testAlterTableRequest_getAlterTableAddColumnsCommand_addMultipleColumns() {
        List<Pair<String, String>> columnInfo =
                List.of(
                        Pair.create(COLUMN_NAME_1, COLUMN_TYPE),
                        Pair.create(COLUMN_NAME_2, COLUMN_TYPE));

        AlterTableRequest alterTableRequest = new AlterTableRequest(TABLE_NAME, columnInfo);

        assertThat(alterTableRequest.getAddColumnsCommands())
                .containsExactly(
                        "ALTER TABLE sample_table ADD COLUMN sample_column_1 INTEGER;",
                        "ALTER TABLE sample_table ADD COLUMN sample_column_2 INTEGER;");
    }

    @Test
    public void testAlterTableRequest_notNullColumnUsed_expectException() {
        List<Pair<String, String>> columnInfo = List.of(Pair.create(COLUMN_NAME_1, TEXT_NOT_NULL));
        AlterTableRequest alterTableRequest = new AlterTableRequest(TABLE_NAME, columnInfo);

        assertThrows(SQLException.class, alterTableRequest::getAddColumnsCommands);
    }
}
