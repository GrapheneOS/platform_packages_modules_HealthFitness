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

package android.health.connect.internal;

import static com.google.common.truth.Truth.assertThat;

import android.os.IBinder;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.stream.IntStream;

@RunWith(AndroidJUnit4.class)
public class ParcelUtilsTest {

    @Test
    public void roundTripViaSharedMemory() {
        int[] data = IntStream.range(0, IBinder.getSuggestedMaxIpcSizeBytes() / 4).toArray();
        Parcel dest = Parcel.obtain();
        ParcelUtils.putToRequiredMemory(dest, /* flags= */ 0, parcel -> parcel.writeIntArray(data));
        dest.setDataPosition(0);
        Parcel result = ParcelUtils.getParcelForSharedMemoryIfRequired(dest);
        assertThat(result.createIntArray()).isEqualTo(data);
    }
}
