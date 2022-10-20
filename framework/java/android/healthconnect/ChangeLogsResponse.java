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
 * Response class for {@link HealthConnectManager#getChangeLogs}
 *
 * @hide
 */
public class ChangeLogsResponse {
    private final List<Record> mUpsertedRecords;
    private final List<String> mDeletedIds;

    /**
     * Response for {@link HealthConnectManager#getChangeLogs}
     *
     * @hide
     */
    public ChangeLogsResponse(
            @NonNull List<Record> upsertedRecords, @NonNull List<String> deletedIds) {
        mUpsertedRecords = upsertedRecords;
        mDeletedIds = deletedIds;
    }

    /**
     * @return records that have been updated or inserted post the time when the given token was
     *     generated.
     */
    @NonNull
    public List<Record> getUpsertedRecords() {
        return mUpsertedRecords;
    }

    /**
     * @return records that have been deleted post the time when the token was requested from {@link
     *     HealthConnectManager#getChangeLogToken}
     */
    @NonNull
    public List<String> getDeletedRecordIds() {
        return mDeletedIds;
    }
}
