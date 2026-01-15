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
import static android.health.connect.datatypes.ActivityIntensityRecord.DURATION_TOTAL;
import static android.health.connect.datatypes.ActivityIntensityRecord.MODERATE_DURATION_TOTAL;
import static android.health.connect.datatypes.ActivityIntensityRecord.VIGOROUS_DURATION_TOTAL;
import static android.healthconnect.testing.cts.TestUtils.getAggregateResponse;
import static android.healthconnect.testing.cts.TestUtils.insertRecord;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.cts.TestUtils.setupAggregation;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.YESTERDAY_11AM;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthDataCategory;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.DataOrigin;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.recordfactory.ActivityIntensityRecordFactory;

import androidx.annotation.Nullable;

import com.android.compatibility.common.util.ApiTest;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@ApiTest(apis = {"android.health.connect.datatypes.ActivityIntensityRecord#DURATION_TOTAL"})
public class ActivityIntensityDurationAggregationTest
        extends BaseDurationAggregationTest<ActivityIntensityRecord, Duration> {

    private final ActivityIntensityRecordFactory mRecordFactory =
            new ActivityIntensityRecordFactory();

    public ActivityIntensityDurationAggregationTest() {
        super(DURATION_TOTAL, HealthDataCategory.ACTIVITY);
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
    public void moderateRecord_multiplierIsOne() throws Exception {
        ActivityIntensityRecord record =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusMinutes(1).plusSeconds(32).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_MODERATE);

        TestUtils.insertRecords(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Duration> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Duration>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(DURATION_TOTAL)
                                .build());

        assertThat(response.get(DURATION_TOTAL)).isNotNull();
        assertThat(response.get(DURATION_TOTAL)).isEqualTo(getRecordDuration(record));
    }

    @Test
    public void vigorousRecord_multiplierIsOne() throws Exception {
        ActivityIntensityRecord record =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusMinutes(1).plusSeconds(32).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_VIGOROUS);

        TestUtils.insertRecords(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), HealthDataCategory.ACTIVITY);

        AggregateRecordsResponse<Duration> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Duration>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(DURATION_TOTAL)
                                .build());

        assertThat(response.get(DURATION_TOTAL)).isNotNull();
        assertThat(response.get(DURATION_TOTAL)).isEqualTo(getRecordDuration(record));
    }

    @Test
    public void multiApp_higherPriorityModerateRecord_multiplierIsOneForBothTypes()
            throws Exception {
        ActivityIntensityRecord higherPriorityModerateRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusHours(1).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_MODERATE);
        ActivityIntensityRecord lowerPriorityVigorousRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.minusMinutes(5).toInstant(),
                        YESTERDAY_11AM.plusHours(1).plusMinutes(7).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_VIGOROUS);
        insertRecord(higherPriorityModerateRecord);
        APP_WITH_WRITE_PERMS_ONLY.insertRecords(lowerPriorityVigorousRecord);
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
                                .addAggregationType(DURATION_TOTAL)
                                .build());

        assertThat(response.get(DURATION_TOTAL)).isNotNull();
        assertThat(response.get(DURATION_TOTAL))
                .isEqualTo(getRecordDuration(lowerPriorityVigorousRecord));
        assertThat(response.getDataOrigins(DURATION_TOTAL))
                .containsExactly(
                        new DataOrigin.Builder().setPackageName(TEST_PACKAGE_NAME).build(),
                        new DataOrigin.Builder()
                                .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                .build());
    }

    @Test
    public void multiApp_higherPriorityVigorousRecord_multiplierIsOneForBothTypes()
            throws Exception {
        ActivityIntensityRecord higherPriorityVigorousRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.toInstant(),
                        YESTERDAY_11AM.plusHours(1).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_VIGOROUS);
        ActivityIntensityRecord lowerPriorityModerateRecord =
                mRecordFactory.newRecord(
                        YESTERDAY_11AM.minusMinutes(5).toInstant(),
                        YESTERDAY_11AM.plusHours(1).plusMinutes(17).toInstant(),
                        ACTIVITY_INTENSITY_TYPE_MODERATE);
        insertRecord(higherPriorityVigorousRecord);
        APP_WITH_WRITE_PERMS_ONLY.insertRecords(lowerPriorityModerateRecord);
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
                                .addAggregationType(DURATION_TOTAL)
                                .build());

        assertThat(response.get(DURATION_TOTAL)).isNotNull();
        assertThat(response.get(DURATION_TOTAL))
                .isEqualTo(getRecordDuration(lowerPriorityModerateRecord));
        assertThat(response.getDataOrigins(DURATION_TOTAL))
                .containsExactly(
                        new DataOrigin.Builder().setPackageName(TEST_PACKAGE_NAME).build(),
                        new DataOrigin.Builder()
                                .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                .build());
    }

    @Test
    public void overlappingRecords_equalsToModeratePlusVigorous() throws Exception {
        var higherPriorityRecords =
                List.of(
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(10).toInstant(),
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(20).toInstant(),
                                YESTERDAY_11AM.plusMinutes(30).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_VIGOROUS),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(40).toInstant(),
                                YESTERDAY_11AM.plusMinutes(50).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE));

        var lowerPriorityRecords =
                List.of(
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(9).toInstant(),
                                YESTERDAY_11AM.plusMinutes(19).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_VIGOROUS),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(17).toInstant(),
                                YESTERDAY_11AM.plusMinutes(32).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_MODERATE),
                        mRecordFactory.newRecord(
                                YESTERDAY_11AM.plusMinutes(41).toInstant(),
                                YESTERDAY_11AM.plusMinutes(55).toInstant(),
                                ACTIVITY_INTENSITY_TYPE_VIGOROUS));

        insertRecords(higherPriorityRecords);
        APP_WITH_WRITE_PERMS_ONLY.insertRecords(lowerPriorityRecords);
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
                                .addAggregationType(DURATION_TOTAL)
                                .addAggregationType(MODERATE_DURATION_TOTAL)
                                .addAggregationType(VIGOROUS_DURATION_TOTAL)
                                .build());

        assertThat(response.get(DURATION_TOTAL)).isNotNull();
        assertThat(response.get(MODERATE_DURATION_TOTAL)).isNotNull();
        assertThat(response.get(VIGOROUS_DURATION_TOTAL)).isNotNull();
        assertThat(response.get(DURATION_TOTAL))
                .isEqualTo(
                        response.get(MODERATE_DURATION_TOTAL)
                                .plus(response.get(VIGOROUS_DURATION_TOTAL)));
    }
}
