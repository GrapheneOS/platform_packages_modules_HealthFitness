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

package android.healthconnect;

import android.annotation.NonNull;
import android.healthconnect.datatypes.Record;

import java.util.List;

/**
 * Response for {@link android.healthconnect.HealthConnectManager#insertRecords}. The response
 * contains records in same order as records in {@link
 * android.healthconnect.HealthConnectManager#insertRecords}
 */
public class InsertRecordsResponse {
    final List<Record> mRecords;

    // @hide
    public InsertRecordsResponse(@NonNull List<Record> records) {
        mRecords = records;
    }

    @NonNull
    public List<Record> getRecords() {
        return mRecords;
    }
}
