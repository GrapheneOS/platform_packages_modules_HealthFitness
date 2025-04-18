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

import android.database.SQLException;
import android.util.Pair;
import android.util.Slog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Creates a alter table request and alter statements for it.
 *
 * @hide
 */
public final class AlterTableRequest {
    public static final String TAG = "HealthConnectAlter";
    private static final String NOT_NULL = "NOT NULL";
    private static final String ALTER_TABLE_COMMAND = "ALTER TABLE ";
    private static final String ADD_COLUMN_COMMAND = " ADD COLUMN ";
    private final String mTableName;
    private final List<Pair<String, String>> mColumnInfo;

    private final Map<String, Pair<String, String>> mForeignKeyConstraints = new HashMap<>();

    public AlterTableRequest(String tableName, List<Pair<String, String>> columnInfo) {
        mTableName = tableName;
        mColumnInfo = columnInfo;
    }

    /**
     * Adds a foreign key constraint between one column and another. Deletion behavior is to set
     * dangling references to null.
     */
    public AlterTableRequest addForeignKeyConstraint(
            String column, String referencedTable, String referencedColumn) {
        mForeignKeyConstraints.put(column, new Pair<>(referencedTable, referencedColumn));
        return this;
    }

    /** Returns a list of alter table SQL statements to add new columns */
    public List<String> getAddColumnsCommands() {
        List<String> statements = new ArrayList<>();
        for (int i = 0; i < mColumnInfo.size(); i++) {
            StringBuilder statement = new StringBuilder(ALTER_TABLE_COMMAND);
            statement.append(mTableName);
            String columnName = mColumnInfo.get(i).first;
            String columnType = mColumnInfo.get(i).second;
            statement.append(ADD_COLUMN_COMMAND).append(columnName).append(" ").append(columnType);
            if (mForeignKeyConstraints.containsKey(columnName)) {
                statement.append(" ");
                statement.append("REFERENCES ");
                statement.append(mForeignKeyConstraints.get(columnName).first);
                statement.append("(");
                statement.append(mForeignKeyConstraints.get(columnName).second);
                statement.append(")");
                statement.append(" ON DELETE SET NULL");
            }
            statement.append(";");
            statements.add(statement.toString());
        }
        Slog.d(TAG, "Alter table: " + statements);

        // Check on the final commands for now as it's more broad. Should this become a problem
        // later, we can change/move the check to narrow scope such as only check on the
        // `columnInfo` list passed to the constructor
        checkNoNotNullColumns(statements);
        return statements;
    }

    /** Returns a list of alter table SQL statements to add generated columns */
    public static String getAddGeneratedColumnsCommands(
            String tableName, CreateTableRequest.GeneratedColumnInfo generatedColumnInfo) {
        String request =
                ALTER_TABLE_COMMAND
                        + tableName
                        + ADD_COLUMN_COMMAND
                        + generatedColumnInfo.getColumnName()
                        + " "
                        + generatedColumnInfo.getColumnType()
                        + " GENERATED ALWAYS AS ("
                        + generatedColumnInfo.getExpression()
                        + ")";

        Slog.d(TAG, "Alter table generated: " + request);
        return request;
    }

    private static void checkNoNotNullColumns(List<String> alterTableCommands) {
        for (String command : alterTableCommands) {
            if (command == null) {
                continue;
            }
            String upperCased = command.toUpperCase(Locale.ROOT);
            if (upperCased.contains(NOT_NULL)) {
                throw new SQLException(
                        String.format(
                                "Alter table command \"%s\" must not contain \"%s\"",
                                upperCased, NOT_NULL));
            }
        }
    }
}
