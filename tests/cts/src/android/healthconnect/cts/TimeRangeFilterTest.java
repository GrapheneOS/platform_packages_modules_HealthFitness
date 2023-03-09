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

package android.healthconnect.cts;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.platform.test.annotations.AppModeFull;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.LocalDateTime;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class TimeRangeFilterTest {
    private static final String TAG = "TimeRangeFilterTest";

    @Test
    public void testLocalTimeRangeFilter() {
        LocalDateTime startTime = LocalDateTime.MIN;
        LocalDateTime endTime = LocalDateTime.MAX;
        LocalTimeRangeFilter timeRangeFilter =
                new LocalTimeRangeFilter.Builder()
                        .setStartTime(startTime)
                        .setEndTime(endTime)
                        .build();

        assertThat(timeRangeFilter.getStartTime()).isEqualTo(startTime);
        assertThat(timeRangeFilter.getEndTime()).isEqualTo(endTime);
        assertThat(timeRangeFilter.isBounded()).isTrue();
    }

    @Test
    public void testTimeInstantRangeFilter() {
        Instant startTime = Instant.now();
        Instant endTime = Instant.now().plusMillis(1000);
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(startTime)
                        .setEndTime(endTime)
                        .build();

        assertThat(timeRangeFilter.getStartTime()).isEqualTo(startTime);
        assertThat(timeRangeFilter.getEndTime()).isEqualTo(endTime);
        assertThat(timeRangeFilter.isBounded()).isTrue();
    }
}
