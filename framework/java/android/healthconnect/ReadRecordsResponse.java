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
 * Response containing list of Records for {@link
 * android.healthconnect.HealthConnectManager#readRecords}.
 *
 * @param <T> the type of the Record for Read record Response
 */
public class ReadRecordsResponse<T extends Record> {
    private final List<T> mRecords;
    private final long mPageToken;

    /**
     * @param records List of records of type T
     * @param pageToken the token value of the read result which can be used as input token for next
     *     read request
     * @hide
     */
    public ReadRecordsResponse(@NonNull List<T> records, long pageToken) {
        mRecords = records;
        mPageToken = pageToken;
    }

    @NonNull
    public List<T> getRecords() {
        return mRecords;
    }

    /** Returns a page token to read the next page of the result. -1 if none available */
    public long getPageToken() {
        return mPageToken;
    }
}
