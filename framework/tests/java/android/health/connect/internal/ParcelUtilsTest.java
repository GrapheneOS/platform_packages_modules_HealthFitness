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
import android.platform.test.flag.junit.FlagsParameterization;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import platform.test.runner.parameterized.ParameterizedAndroidJunit4;
import platform.test.runner.parameterized.Parameters;

import java.util.List;
import java.util.stream.IntStream;

@RunWith(ParameterizedAndroidJunit4.class)
public class ParcelUtilsTest {

    @Parameters(name = "{0}")
    public static List<FlagsParameterization> getParams() {
        return FlagsParameterization.allCombinationsOf(Flags.FLAG_REDUCE_PARCEL_MARSHALLING_COPIES);
    }

    @Rule public final SetFlagsRule mSetFlagsRule;

    public ParcelUtilsTest(FlagsParameterization flags) {
        mSetFlagsRule = new SetFlagsRule(flags);
    }

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
