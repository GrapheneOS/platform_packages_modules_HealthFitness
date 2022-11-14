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

package android.healthconnect.cts;

import android.healthconnect.datatypes.HeartRateRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HeartRateRecordTest {
    static List<Record> getHeartRateRecords() {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(72, Instant.now());
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);

        return Arrays.asList(
                new HeartRateRecord.Builder(
                                new Metadata.Builder().build(),
                                Instant.now(),
                                Instant.now(),
                                heartRateSamples)
                        .build(),
                new HeartRateRecord.Builder(
                                new Metadata.Builder().build(),
                                Instant.now(),
                                Instant.now(),
                                heartRateSamples)
                        .setStartZoneOffset(
                                ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                        .setEndZoneOffset(
                                ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                        .build());
    }
}
