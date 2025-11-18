/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE;
import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS;
import static android.health.connect.datatypes.ActivityIntensityRecord.VIGOROUS_DURATION_TOTAL;
import static android.healthconnect.testing.cts.TestUtils.getAggregateResponse;
import static android.healthconnect.testing.cts.TestUtils.insertRecord;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.cts.TestUtils.setupAggregation;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.YESTERDAY_11AM;

import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY;

import static com.google.common.truth.Truth.assertThat;

import static java.time.Duration.ofMinutes;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthDataCategory;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.DataOrigin;
import android.healthconnect.testing.shared.recordfactory.ActivityIntensityRecordFactory;
import android.platform.test.annotations.RequiresFlagsEnabled;

import androidx.annotation.Nullable;

import com.android.compatibility.common.util.ApiTest;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@RequiresFlagsEnabled({FLAG_ACTIVITY_INTENSITY})
@ApiTest(
        apis = {"android.health.connect.datatypes.ActivityIntensityRecord#VIGOROUS_DURATION_TOTAL"})
public class ActivityIntensityVigorousDurationAggregationTest
        extends BaseDurationAggregationTest<ActivityIntensityRecord, Duration> {

    private final ActivityIntensityRecordFactory mRecordFactory =
            new ActivityIntensityRecordFactory();

    public ActivityIntensityVigorousDurationAggregationTest() {
        super(VIGOROUS_DURATION_TOTAL, HealthDataCategory.ACTIVITY);
    }

    @Override
    Duration getExpectedValueFromDuration(Duration duration) {
        return duration;
    }

    @Override
    ActivityIntensityRecord createRecord(
            Instant startTime,
            Instant endTime,
            @Nullable ZoneOffset startZoneOffset,
            @Nullable ZoneOffset endZoneOffset) {
        return mRecordFactory.newRecord(
                startTime,
                endTime,
                ACTIVITY_INTENSITY_TYPE_VIGOROUS,
                startZoneOffset,
                endZoneOffset);
    }

    @Test
    public void ignoresNonVigorousRecords() throws Exception {
        List<ActivityIntensityRecord> records =
                List.of(
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.minusHours(1).toInstant(),
                                YESTERDAY_11AM.minusHours(1).plusMinutes(23).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.toInstant(),
                                YESTERDAY_11AM.plusMinutes(37).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_VIGOROUS),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusHours(1).toInstant(),
                                YESTERDAY_11AM.plusHours(1).plusMinutes(42).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE));
        insertRecords(records);
        setupAggregation(List.of(TEST_PACKAGE_NAME), HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Duration> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Duration>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(VIGOROUS_DURATION_TOTAL)
                                .build());

        assertThat(response.get(VIGOROUS_DURATION_TOTAL)).isNotNull();
        assertThat(response.get(VIGOROUS_DURATION_TOTAL))
                .isEqualTo(getRecordDuration(records.get(1)));
    }

    @Test
    public void multiApp_higherPriorityNonVigorousRecord_masksOutLowerPriorityVigorousRecord()
            throws Exception {
        ActivityIntensityRecord higherPriorityNonVigorousRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusHours(1).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_MODERATE);
        ActivityIntensityRecord lowerPriorityVigorousRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.minusMinutes(17).toInstant(),
                        YESTERDAY_11AM.plusHours(1).plusMinutes(24).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_VIGOROUS);
        insertRecord(higherPriorityNonVigorousRecord);
        APP_WITH_WRITE_PERMS_ONLY.insertRecord(lowerPriorityVigorousRecord);
        setupAggregation(
                List.of(TEST_PACKAGE_NAME, APP_WITH_WRITE_PERMS_ONLY.getPackageName()),
                HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Duration> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Duration>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(VIGOROUS_DURATION_TOTAL)
                                .build());

        assertThat(response.get(VIGOROUS_DURATION_TOTAL)).isNotNull();
        assertThat(response.get(VIGOROUS_DURATION_TOTAL)).isEqualTo(ofMinutes(17).plusMinutes(24));
        assertThat(response.getDataOrigins(VIGOROUS_DURATION_TOTAL))
                .containsExactly(
                        new DataOrigin.Builder().setPackageName(TEST_PACKAGE_NAME).build(),
                        new DataOrigin.Builder()
                                .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                .build());
    }

    @Test
    public void multiApp_ignoresLowerPriorityNonVigorousRecord() throws Exception {
        ActivityIntensityRecord higherPriorityVigorousRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusHours(1).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_VIGOROUS);
        ActivityIntensityRecord lowerPriorityNonVigorousRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.minusHours(1).toInstant(),
                        YESTERDAY_11AM.plusHours(2).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_MODERATE);
        insertRecord(higherPriorityVigorousRecord);
        APP_WITH_WRITE_PERMS_ONLY.insertRecords(lowerPriorityNonVigorousRecord);
        setupAggregation(
                List.of(TEST_PACKAGE_NAME, APP_WITH_WRITE_PERMS_ONLY.getPackageName()),
                HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Duration> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Duration>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(VIGOROUS_DURATION_TOTAL)
                                .build());

        assertThat(response.get(VIGOROUS_DURATION_TOTAL)).isNotNull();
        assertThat(response.get(VIGOROUS_DURATION_TOTAL))
                .isEqualTo(getRecordDuration(higherPriorityVigorousRecord));
        assertThat(response.getDataOrigins(VIGOROUS_DURATION_TOTAL))
                .containsExactly(
                        new DataOrigin.Builder().setPackageName(TEST_PACKAGE_NAME).build(),
                        new DataOrigin.Builder()
                                .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                .build());
    }
}
