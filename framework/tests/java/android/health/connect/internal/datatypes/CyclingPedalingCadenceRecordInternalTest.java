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

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.internal.datatypes.CyclingPedalingCadenceRecordInternal.CyclingPedalingCadenceRecordSample;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CyclingPedalingCadenceRecordInternalTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    @EnableFlags(Flags.FLAG_SAMPLE_TIME_ORDERING)
    public void testSamplesConstructedInTimeOrder() {
        // Construct a set of samples such that they are unlikely to be sorted by chance.
        HashSet<CyclingPedalingCadenceRecordSample> samples = new HashSet<>();
        for (long i = 20L; i < 25L; i++) {
            samples.add(new CyclingPedalingCadenceRecordSample(100, i));
        }
        for (long i = 0L; i < 5L; i++) {
            samples.add(new CyclingPedalingCadenceRecordSample(100, i));
        }
        for (long i = 1000L; i < 1005L; i++) {
            samples.add(new CyclingPedalingCadenceRecordSample(100, i));
        }

        CyclingPedalingCadenceRecordInternal recordInternal =
                new CyclingPedalingCadenceRecordInternal(samples);
        Set<CyclingPedalingCadenceRecordSample> resultSamples = recordInternal.getSamples();

        List<CyclingPedalingCadenceRecordSample> expected =
                samples.stream()
                        .sorted(
                                Comparator.comparing(
                                        CyclingPedalingCadenceRecordSample::getEpochMillis))
                        .toList();
        assertThat(new ArrayList<>(resultSamples)).isEqualTo(expected);
    }

    @Test
    @EnableFlags(Flags.FLAG_SAMPLE_TIME_ORDERING)
    public void testSamplesWithDuplicateTimes_dropsDuplicates() {
        // Construct a set of samples such that they are unlikely to be sorted by chance.
        HashSet<CyclingPedalingCadenceRecordSample> samples = new HashSet<>();
        for (int rpm = 70; rpm < 100; rpm++) {
            samples.add(new CyclingPedalingCadenceRecordSample(rpm, /* epochMillis= */ 20));
        }

        CyclingPedalingCadenceRecordInternal recordInternal =
                new CyclingPedalingCadenceRecordInternal(samples);
        Set<CyclingPedalingCadenceRecordSample> resultSamples = recordInternal.getSamples();

        assertThat(resultSamples).hasSize(1);
    }
}
