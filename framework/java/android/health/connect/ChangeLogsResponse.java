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

package android.health.connect;

import android.annotation.NonNull;
import android.health.connect.datatypes.Record;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Response class for {@link HealthConnectManager#getChangeLogs} */
public final class ChangeLogsResponse {
    private final List<Record> mUpsertedRecords;
    private final List<DeletedLog> mDeletedLogs;
    private final String mNextChangesToken;
    private final boolean mHasMorePages;

    /**
     * Response for {@link HealthConnectManager#getChangeLogs}
     *
     * @hide
     */
    public ChangeLogsResponse(
            @NonNull List<Record> upsertedRecords,
            @NonNull List<DeletedLog> deletedLogs,
            @NonNull String nextChangesToken,
            boolean hasMorePages) {
        mUpsertedRecords = upsertedRecords;
        mDeletedLogs = deletedLogs;
        mNextChangesToken = nextChangesToken;
        mHasMorePages = hasMorePages;
    }

    /**
     * Returns records that have been updated or inserted post the time when the given token was
     * generated.
     */
    @NonNull
    public List<Record> getUpsertedRecords() {
        return mUpsertedRecords;
    }

    /**
     * Returns delete logs for records that have been deleted post the time when the token was
     * requested from {@link HealthConnectManager#getChangeLogToken}
     */
    @NonNull
    public List<DeletedLog> getDeletedLogs() {
        return mDeletedLogs;
    }

    /** Returns token for future reads using {@link HealthConnectManager#getChangeLogs} */
    @NonNull
    public String getNextChangesToken() {
        return mNextChangesToken;
    }

    /** Returns whether there are more pages available for read */
    public boolean hasMorePages() {
        return mHasMorePages;
    }

    /** A class to represent a delete log in ChangeLogsResponse */
    public static final class DeletedLog {
        private final String mDeletedRecordId;
        private final Instant mDeletedTime;

        /** @hide */
        public DeletedLog(@NonNull String deletedRecordId, long deletedTime) {
            Objects.requireNonNull(deletedRecordId);
            mDeletedRecordId = deletedRecordId;
            mDeletedTime = Instant.ofEpochMilli(deletedTime);
        }

        /** Returns record id of the record deleted */
        @NonNull
        public String getDeletedRecordId() {
            return mDeletedRecordId;
        }

        /** Returns timestamp when the record was deleted */
        @NonNull
        public Instant getDeletedTime() {
            return mDeletedTime;
        }
    }
}
