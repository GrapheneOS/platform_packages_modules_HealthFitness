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

package android.healthconnect.internal.datatypes.utils;

import android.annotation.NonNull;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.RecordInternal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A helper class used to convert internal and external data types.
 *
 * @hide
 */
public final class InternalExternalRecordConverter {
    private static InternalExternalRecordConverter sInternalExternalRecordConverter;

    private final Map<Integer, Class<? extends RecordInternal<?>>>
            mRecordIdToInternalRecordClassMap;
    private final Map<Integer, Class<? extends Record>> mRecordIdToExternalRecordClassMap;

    private InternalExternalRecordConverter() {
        // Add any new data type here to facilitate its conversion.
        mRecordIdToInternalRecordClassMap =
                RecordMapper.getInstance().getRecordIdToInternalRecordClassMap();
        mRecordIdToExternalRecordClassMap =
                RecordMapper.getInstance().getRecordIdToExternalRecordClassMap();
    }

    @NonNull
    public static InternalExternalRecordConverter getInstance() {
        if (sInternalExternalRecordConverter == null) {
            sInternalExternalRecordConverter = new InternalExternalRecordConverter();
        }

        return sInternalExternalRecordConverter;
    }

    /** Returns a record for {@param record} */
    @NonNull
    public List<RecordInternal<?>> getInternalRecords(@NonNull List<? extends Record> records) {
        List<RecordInternal<?>> internalRecordListInternal = new ArrayList<>(records.size());

        for (Record record : records) {
            internalRecordListInternal.add(getInternalRecord(record));
        }

        return internalRecordListInternal;
    }

    /** Converts {@link Record} to {@link RecordInternal}. */
    @NonNull
    public RecordInternal<?> getInternalRecord(@NonNull Record record) {
        Objects.requireNonNull(record);

        final RecordInternal<?> recordInternal = newInternalRecord(record.getRecordType());
        recordInternal.populateUsing(record);

        return recordInternal;
    }

    /** Returns a new instance of {@link RecordInternal} for the provided {@code type }. */
    @NonNull
    public RecordInternal<?> newInternalRecord(@RecordTypeIdentifier.RecordType int type) {
        Class<? extends RecordInternal<?>> recordClass =
                mRecordIdToInternalRecordClassMap.get(type);
        Objects.requireNonNull(recordClass);
        RecordInternal<?> recordInternal;
        try {
            recordInternal = recordClass.getConstructor().newInstance();
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return recordInternal;
    }

    /** Returns a record for {@param record} */
    @NonNull
    public List<Record> getExternalRecords(@NonNull List<RecordInternal<?>> recordInternals) {
        List<Record> externalRecordList = new ArrayList<>(recordInternals.size());

        for (RecordInternal<?> recordInternal : recordInternals) {
            externalRecordList.add(recordInternal.toExternalRecord());
        }

        return externalRecordList;
    }
}
