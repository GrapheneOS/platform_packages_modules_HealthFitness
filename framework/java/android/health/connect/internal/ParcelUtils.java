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

package android.health.connect.internal;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.SharedMemory;
import android.system.ErrnoException;
import java.nio.ByteBuffer;

/** @hide */
public final class ParcelUtils {
    @NonNull
    public static Parcel getParcelForSharedMemory(Parcel in) {
        try (SharedMemory memory = SharedMemory.CREATOR.createFromParcel(in)) {
            Parcel dataParcel = Parcel.obtain();
            ByteBuffer buffer = memory.mapReadOnly();
            byte[] payload = new byte[buffer.limit()];
            buffer.get(payload);
            dataParcel.unmarshall(payload, 0, payload.length);
            dataParcel.setDataPosition(0);
            return dataParcel;
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
    }

    public static SharedMemory getSharedMemoryForParcel(Parcel dataParcel, int dataParcelSize) {
        try {
            SharedMemory sharedMemory =
                    SharedMemory.create("RecordsParcelSharedMemory", dataParcelSize);
            ByteBuffer buffer = sharedMemory.mapReadWrite();
            byte[] data = dataParcel.marshall();
            buffer.put(data, 0, dataParcelSize);
            return sharedMemory;
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
    }
}
