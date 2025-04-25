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

package android.health.connect.internal.datatypes;

import android.health.connect.internal.datatypes.SkinTemperatureRecordInternal.SkinTemperatureDeltaSample;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import com.google.common.testing.EqualsTester;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SkinTemperatureRecordInternalTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    @EnableFlags(Flags.FLAG_SAMPLE_TIME_ORDERING)
    public void testSamplesEqualsHashcode() {
        new EqualsTester()
                .addEqualityGroup(
                        new SkinTemperatureDeltaSample(1.1, 20),
                        new SkinTemperatureDeltaSample(1.1, 20))
                .addEqualityGroup(
                        new SkinTemperatureDeltaSample(1.1, 30),
                        new SkinTemperatureDeltaSample(1.1, 30))
                .addEqualityGroup(
                        new SkinTemperatureDeltaSample(-1.1, 20),
                        new SkinTemperatureDeltaSample(-1.1, 20))
                .testEquals();
    }
}
