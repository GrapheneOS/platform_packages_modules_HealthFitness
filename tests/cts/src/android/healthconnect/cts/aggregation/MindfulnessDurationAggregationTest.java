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

package android.healthconnect.cts.aggregation;

import static android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL;
import static android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata;

import android.health.connect.HealthDataCategory;
import android.health.connect.datatypes.MindfulnessSessionRecord;

import androidx.annotation.Nullable;

import com.android.compatibility.common.util.ApiTest;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

@ApiTest(
        apis = {
            "android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL"
        })
public class MindfulnessDurationAggregationTest
        extends BaseDurationAggregationTest<MindfulnessSessionRecord, Long> {

    public MindfulnessDurationAggregationTest() {
        super(MINDFULNESS_DURATION_TOTAL, HealthDataCategory.WELLNESS);
    }

    @Override
    Long getExpectedValueFromDuration(Duration duration) {
        return duration.toMillis();
    }

    @Override
    MindfulnessSessionRecord createRecord(
            Instant startTime,
            Instant endTime,
            @Nullable ZoneOffset startZoneOffset,
            @Nullable ZoneOffset endZoneOffset) {
        var builder =
                new MindfulnessSessionRecord.Builder(
                        newEmptyMetadata(),
                        startTime,
                        endTime,
                        MINDFULNESS_SESSION_TYPE_MEDITATION);

        if (startZoneOffset != null) {
            builder.setStartZoneOffset(startZoneOffset);
        }

        if (endZoneOffset != null) {
            builder.setEndZoneOffset(endZoneOffset);
        }

        return builder.build();
    }
}
