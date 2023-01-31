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

package android.health.connect.aidl;

import android.annotation.NonNull;
import android.health.connect.AccessLog;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.RecordMapper;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** @hide */
public class AccessLogsResponseParcel implements Parcelable {
    public static final Creator<AccessLogsResponseParcel> CREATOR =
            new Creator<AccessLogsResponseParcel>() {
                @Override
                public AccessLogsResponseParcel createFromParcel(Parcel in) {
                    return new AccessLogsResponseParcel(in);
                }

                @Override
                public AccessLogsResponseParcel[] newArray(int size) {
                    return new AccessLogsResponseParcel[size];
                }
            };
    private final List<AccessLog> mAccessLogsList;

    protected AccessLogsResponseParcel(@NonNull Parcel in) {
        int size = in.readInt();
        mAccessLogsList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mAccessLogsList.add(
                    new AccessLog(
                            in.readString(),
                            Arrays.stream(in.createIntArray()).boxed().collect(Collectors.toList()),
                            in.readLong(),
                            in.readInt()));
        }
    }

    public AccessLogsResponseParcel(@NonNull List<AccessLog> accessLogs) {
        mAccessLogsList = accessLogs;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mAccessLogsList.size());
        RecordMapper recordMapper = RecordMapper.getInstance();
        mAccessLogsList.forEach(
                (accessLog -> {
                    dest.writeString(accessLog.getPackageName());
                    int recordTypeCount = accessLog.getRecordTypes().size();
                    @RecordTypeIdentifier.RecordType int[] recordTypes = new int[recordTypeCount];
                    for (int i = 0; i < recordTypeCount; i++) {
                        recordTypes[i] =
                                recordMapper.getRecordType(accessLog.getRecordTypes().get(i));
                    }
                    dest.writeIntArray(recordTypes);
                    dest.writeLong(accessLog.getAccessTime().toEpochMilli());
                    dest.writeInt(accessLog.getOperationType());
                }));
    }

    @NonNull
    public List<AccessLog> getAccessLogs() {
        return mAccessLogsList;
    }
}
