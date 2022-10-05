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

import android.annotation.NonNull;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.util.ArrayMap;

import com.android.server.healthconnect.storage.datatypehelpers.HeartRateRecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.StepsRecordHelper;

import java.util.Collections;
import java.util.Map;

/**
 * Store for all the record helpers
 *
 * @hide
 */
public final class RecordHelperProvider {
    private static RecordHelperProvider sRecordHelperProvider;

    private final Map<Integer, RecordHelper<?>> mRecordIDToHelperMap;

    private RecordHelperProvider() {
        Map<Integer, RecordHelper<?>> recordIDToHelperMap = new ArrayMap<>();
        recordIDToHelperMap.put(RecordTypeIdentifier.RECORD_TYPE_STEPS, new StepsRecordHelper());
        recordIDToHelperMap.put(
                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE, new HeartRateRecordHelper());

        mRecordIDToHelperMap = Collections.unmodifiableMap(recordIDToHelperMap);
    }

    @NonNull
    public static RecordHelperProvider getInstance() {
        if (sRecordHelperProvider == null) {
            sRecordHelperProvider = new RecordHelperProvider();
        }

        return sRecordHelperProvider;
    }

    @NonNull
    public Map<Integer, RecordHelper<?>> getRecordHelpers() {
        return mRecordIDToHelperMap;
    }

    @NonNull
    public RecordHelper<?> getRecordHelper(int recordType) {
        return mRecordIDToHelperMap.get(recordType);
    }
}
