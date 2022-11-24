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

import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Power;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

public class BasalMetabolicRateRecordTest {
    static List<Record> getBasalMetabolicRateRecords() {
        return Arrays.asList(
                new BasalMetabolicRateRecord.Builder(
                                new Metadata.Builder().build(),
                                Instant.now(),
                                Power.fromWatts(100.0))
                        .build(),
                new BasalMetabolicRateRecord.Builder(
                                new Metadata.Builder().build(),
                                Instant.now(),
                                Power.fromWatts(100.0))
                        .setZoneOffset(
                                ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                        .build());
    }

    static Record getBasalMetabolicRateRecord(double power) {
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Power.fromWatts(power))
                .build();
    }
}
