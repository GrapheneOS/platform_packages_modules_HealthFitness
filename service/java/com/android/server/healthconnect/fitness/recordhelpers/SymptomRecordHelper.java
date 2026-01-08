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
package com.android.server.healthconnect.fitness.recordhelpers;

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.SymptomRecordInternal;
import android.health.connect.internal.datatypes.utils.SymptomTypePermissionMapper;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A helper class for SymptomsRecord.
 *
 * @hide
 */
public final class SymptomRecordHelper extends IntervalRecordHelper<SymptomRecordInternal> {

    @VisibleForTesting public static final String TABLE_NAME = "symptom_record_table";
    @VisibleForTesting public static final String SYMPTOM_TYPE_COLUMN_NAME = "symptom_type";
    @VisibleForTesting public static final String NOTES_COLUMN_NAME = "notes";
    @VisibleForTesting public static final String SEVERITY_COLUMN_NAME = "severity";
    @VisibleForTesting public static final String COUNT_COLUMN_NAME = "count";
    @VisibleForTesting public static final String TEMPORAL_TYPE_COLUMN_NAME = "temporal_type";

    public SymptomRecordHelper() {
        super(RecordTypeIdentifier.RECORD_TYPE_SYMPTOM);
    }

    @Override
    public Set<String> getPerRecordWritePermissions(RecordInternal<?> record) {
        SymptomRecordInternal symptomsRecord = (SymptomRecordInternal) record;
        String requiredPermission =
                SymptomTypePermissionMapper.getWritePermission(symptomsRecord.getSymptomType());
        return Collections.singleton(requiredPermission);
    }

    @Override
    public Set<String> getGranularReadPermissions() {
        return SymptomTypePermissionMapper.getSymptomTypes().stream()
                .map(SymptomTypePermissionMapper::getReadPermission)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getAllPerRecordWritePermissions() {
        return SymptomTypePermissionMapper.getSymptomTypes().stream()
                .map(SymptomTypePermissionMapper::getWritePermission)
                .collect(Collectors.toSet());
    }

    @Override
    void addAdditionalDeletionFilters(
            DeleteTableRequest deleteTableRequest, Set<String> grantedWritePermissions) {
        WhereClauses whereClauses = new WhereClauses(WhereClauses.LogicalOperator.AND);
        Set<Integer> allowedSymptomTypes =
                SymptomTypePermissionMapper.getSymptomTypes().stream()
                        .filter(
                                (symptomType) ->
                                        grantedWritePermissions.contains(
                                                SymptomTypePermissionMapper.getWritePermission(
                                                        symptomType)))
                        .collect(Collectors.toSet());

        if (allowedSymptomTypes.isEmpty()) {
            // No permissions for any symptom types, return a request that yields empty results by
            // adding a clause that is always false.
            whereClauses.addFalseClause();
        } else {
            whereClauses.addWhereInClause(
                    SYMPTOM_TYPE_COLUMN_NAME,
                    allowedSymptomTypes.stream().map(String::valueOf).collect(Collectors.toList()));
        }
        deleteTableRequest.addExtraWhereClauses(whereClauses);
    }

    @Override
    public String getMainTableName() {
        return TABLE_NAME;
    }

    @Override
    SymptomRecordInternal populateSpecificRecordValue(Cursor cursor) {
        SymptomRecordInternal record = new SymptomRecordInternal();
        record.setSymptomType(getCursorInt(cursor, SYMPTOM_TYPE_COLUMN_NAME));
        record.setNotes(getCursorString(cursor, NOTES_COLUMN_NAME));
        record.setSeverity(getCursorInt(cursor, SEVERITY_COLUMN_NAME));
        record.setCount(getCursorInt(cursor, COUNT_COLUMN_NAME));
        record.setTemporalType(getCursorInt(cursor, TEMPORAL_TYPE_COLUMN_NAME));
        return record;
    }

    @Override
    void populateSpecificContentValues(
            ContentValues contentValues, SymptomRecordInternal symptomRecord) {
        contentValues.put(SYMPTOM_TYPE_COLUMN_NAME, symptomRecord.getSymptomType());
        contentValues.put(NOTES_COLUMN_NAME, symptomRecord.getNotes());
        contentValues.put(SEVERITY_COLUMN_NAME, symptomRecord.getSeverity());
        contentValues.put(COUNT_COLUMN_NAME, symptomRecord.getCount());
        contentValues.put(TEMPORAL_TYPE_COLUMN_NAME, symptomRecord.getTemporalType());
    }

    @Override
    List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Arrays.asList(
                new Pair<>(SYMPTOM_TYPE_COLUMN_NAME, INTEGER),
                new Pair<>(NOTES_COLUMN_NAME, TEXT_NULL),
                new Pair<>(SEVERITY_COLUMN_NAME, INTEGER),
                new Pair<>(COUNT_COLUMN_NAME, INTEGER),
                new Pair<>(TEMPORAL_TYPE_COLUMN_NAME, INTEGER));
    }

    @Override
    protected void addCustomReadTableWhereClauses(
            WhereClauses whereClauses,
            Set<String> grantedGranularPermissions,
            boolean enforceSelfRead) {
        Set<Integer> allowedSymptomTypes =
                SymptomTypePermissionMapper.getSymptomTypes().stream()
                        .filter(
                                (symptomType) ->
                                        grantedGranularPermissions.contains(
                                                        SymptomTypePermissionMapper
                                                                .getReadPermission(symptomType))
                                                || (enforceSelfRead
                                                        && grantedGranularPermissions.contains(
                                                                SymptomTypePermissionMapper
                                                                        .getWritePermission(
                                                                                symptomType))))
                        .collect(Collectors.toSet());

        if (allowedSymptomTypes.isEmpty()) {
            // No permissions for any symptom types, return a request that yields empty results by
            // adding a clause that is always false.
            whereClauses.addFalseClause();
        } else {
            whereClauses.addWhereInClause(
                    SYMPTOM_TYPE_COLUMN_NAME,
                    allowedSymptomTypes.stream().map(String::valueOf).collect(Collectors.toList()));
        }
    }

    @Override
    public void enforcePreUpsertChecks(
            List<RecordInternal<?>> recordInternals, TransactionManager transactionManager) {

        Map<UUID, Integer> uuidToSymptomTypeMap = new HashMap<>();
        List<String> uuids = new ArrayList<>();
        for (RecordInternal<?> recordInternal : recordInternals) {
            SymptomRecordInternal symptomRecordInternal = (SymptomRecordInternal) recordInternal;
            uuids.add(StorageUtils.getHexString(symptomRecordInternal.getUuid()));
            uuidToSymptomTypeMap.put(
                    symptomRecordInternal.getUuid(), symptomRecordInternal.getSymptomType());
        }

        ReadTableRequest readTableRequest =
                new ReadTableRequest(TABLE_NAME)
                        .setColumnNames(List.of(UUID_COLUMN_NAME, SYMPTOM_TYPE_COLUMN_NAME))
                        .setWhereClause(
                                new WhereClauses(AND)
                                        .addWhereInClauseWithoutQuotes(UUID_COLUMN_NAME, uuids));

        try (Cursor cursor = transactionManager.read(readTableRequest)) {
            while (cursor.moveToNext()) {
                UUID recordUuid = StorageUtils.getCursorUUID(cursor, UUID_COLUMN_NAME);
                if (uuidToSymptomTypeMap.containsKey(recordUuid)
                        && uuidToSymptomTypeMap.get(recordUuid)
                                != StorageUtils.getCursorInt(cursor, SYMPTOM_TYPE_COLUMN_NAME)) {
                    throw new IllegalArgumentException("Updating Symptom type is not allowed.");
                }
            }
        }
    }
}
